package com.asla.denge.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.AuthRepository
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.asla.denge.domain.model.MusicGenre
import com.asla.denge.domain.repository.GenreRepository

data class GenreSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val tracks: List<Track>,
)

enum class OnboardingStep {
    NAME_INPUT,
    GENRE_SELECTION,
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val genreSections: List<GenreSection> = emptyList(),
    val showOnboarding: Boolean = false,
    val onboardingStep: OnboardingStep = OnboardingStep.NAME_INPUT,
    val availableGenres: List<MusicGenre> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(
    private val musicRepository: MusicRepository,
    private val playerManager: PlayerManager,
    private val authRepository: AuthRepository,
    private val genreRepository: GenreRepository,
) : ViewModel() {

    private val isFirstLaunch = !genreRepository.hasCompletedOnboarding()

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoading = !isFirstLaunch,
            showOnboarding = isFirstLaunch,
            onboardingStep = if (authRepository.hasCustomUserName()) OnboardingStep.GENRE_SELECTION else OnboardingStep.NAME_INPUT,
            availableGenres = genreRepository.getAllGenres(),
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("Sobat Musik")
    val userName: StateFlow<String> = _userName.asStateFlow()

    val recentHistory: StateFlow<List<Track>> = musicRepository.getPlaybackHistory()
        .map { it.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val mostPlayedTracks: StateFlow<List<Track>> = musicRepository.getMostPlayedTracks(limit = 5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _recommendedFromLiked = MutableStateFlow<Pair<Track, List<Track>>?>(null)
    val recommendedFromLiked: StateFlow<Pair<Track, List<Track>>?> = _recommendedFromLiked.asStateFlow()

    private var loadFeedJob: kotlinx.coroutines.Job? = null

    init {
        loadUserProfile()
        observeGenres()
        loadRecommendationsFromLiked()
    }

    private fun observeGenres() {
        viewModelScope.launch {
            genreRepository.getGenresFlow().collect { allGenres ->
                _uiState.update { it.copy(availableGenres = allGenres) }
                loadGenreFeed()
            }
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val name = authRepository.getUserName() ?: "Sobat Musik"
            _userName.value = name
        }
    }

    fun loadRecommendationsFromLiked() {
        viewModelScope.launch {
            try {
                val rec = musicRepository.getRecommendationsBasedOnLiked()
                _recommendedFromLiked.value = rec
            } catch (_: Exception) {
                _recommendedFromLiked.value = null
            }
        }
    }

    fun loadGenreFeed() {
        loadRecommendationsFromLiked()
        loadFeedJob?.cancel()
        loadFeedJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val selectedGenres = genreRepository.getSelectedGenres()
            if (selectedGenres.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, genreSections = emptyList()) }
                return@launch
            }

            try {
                // Fetch all selected genres in parallel via Dispatchers.IO for 5x speedup
                val sections = selectedGenres.map { genre ->
                    async(Dispatchers.IO) {
                        try {
                            val tracks = musicRepository.getGenreTracks(genre.searchQuery).take(5)
                            if (tracks.isNotEmpty()) {
                                GenreSection(
                                    id = genre.id,
                                    title = "${genre.icon} ${genre.name}",
                                    subtitle = genre.subtitle,
                                    icon = genre.icon,
                                    tracks = tracks,
                                )
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                _uiState.update { it.copy(isLoading = false, genreSections = sections) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load music recommendations")
                }
            }
        }
    }

    fun toggleGenreSelection(genreId: String) {
        genreRepository.toggleGenreSelection(genreId)
    }

    fun addCustomGenre(name: String, query: String) {
        if (name.isNotBlank()) {
            genreRepository.addCustomGenre(name, query)
        }
    }



    fun setUserName(name: String) {
        val cleanName = name.trim().ifBlank { "Sobat Musik" }
        viewModelScope.launch {
            authRepository.setUserName(cleanName)
            _userName.value = cleanName
        }
    }

    fun submitOnboardingName(name: String) {
        val cleanName = name.trim().ifBlank { "Sobat Musik" }
        viewModelScope.launch {
            authRepository.setUserName(cleanName)
            _userName.value = cleanName
            _uiState.update { it.copy(onboardingStep = OnboardingStep.GENRE_SELECTION) }
        }
    }

    fun completeOnboarding() {
        genreRepository.setCompletedOnboarding(true)
        _uiState.update { it.copy(showOnboarding = false) }
        loadGenreFeed()
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue)
    }

    fun playNext(track: Track) {
        playerManager.playNextInQueue(track)
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }
}

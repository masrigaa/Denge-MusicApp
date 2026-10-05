package com.asla.denge.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asla.denge.data.local.entity.EqPresetEntity
import com.asla.denge.domain.model.AudioQuality
import com.asla.denge.domain.model.MusicGenre
import com.asla.denge.domain.repository.AuthRepository
import com.asla.denge.domain.repository.GenreRepository
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.domain.repository.SettingsRepository
import com.asla.denge.player.AudioEffectsManager
import com.asla.denge.player.PlaybackCacheManager
import com.asla.denge.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val audioEffectsManager: AudioEffectsManager,
    private val musicRepository: MusicRepository,
    private val genreRepository: GenreRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackCacheManager: PlaybackCacheManager,
    private val playerManager: PlayerManager,
) : ViewModel() {

    private val _userName = MutableStateFlow("Sobat Musik")
    val userName: StateFlow<String> = _userName.asStateFlow()

    val availableGenres: StateFlow<List<MusicGenre>> = genreRepository.getGenresFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = genreRepository.getAllGenres()
        )

    val eqPresets: StateFlow<List<EqPresetEntity>> = audioEffectsManager.getPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val audioQuality: StateFlow<AudioQuality> = settingsRepository.getAudioQuality()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioQuality.HIGH
        )

    private val _usedCacheSizeFormatted = MutableStateFlow("0 MB")
    val usedCacheSizeFormatted: StateFlow<String> = _usedCacheSizeFormatted.asStateFlow()

    val sleepTimerRemainingSeconds: StateFlow<Long?> = playerManager.sleepTimerRemainingSeconds
    val activeSleepTimerOption: StateFlow<Int?> = playerManager.activeSleepTimerOption

    init {
        loadUserProfile()
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val bytes = playbackCacheManager.getUsedCacheSizeBytes()
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            _usedCacheSizeFormatted.value = String.format(java.util.Locale.US, "%.1f MB", mb)
        }
    }

    fun clearPlaybackCache() {
        viewModelScope.launch(Dispatchers.IO) {
            playbackCacheManager.clearCache()
            refreshCacheSize()
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val name = authRepository.getUserName() ?: "Sobat Musik"
            _userName.value = name
        }
    }

    fun setUserName(name: String) {
        val cleanName = name.trim().ifBlank { "Sobat Musik" }
        viewModelScope.launch {
            authRepository.setUserName(cleanName)
            _userName.value = cleanName
        }
    }

    fun applyPreset(preset: EqPresetEntity) {
        viewModelScope.launch {
            audioEffectsManager.applyPreset(preset)
        }
    }

    fun toggleGenre(genreId: String) {
        genreRepository.toggleGenreSelection(genreId)
    }

    fun addCustomGenre(name: String, query: String) {
        if (name.isNotBlank()) {
            genreRepository.addCustomGenre(name, query)
        }
    }

    fun deleteCustomGenre(genreId: String) {
        genreRepository.deleteCustomGenre(genreId)
    }
}

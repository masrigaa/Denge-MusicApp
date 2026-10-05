package com.asla.denge.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asla.denge.data.local.dao.SearchHistoryDao
import com.asla.denge.data.local.entity.SearchHistoryEntity
import com.asla.denge.domain.model.SearchResults
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.player.PlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: SearchResults = SearchResults(),
    val error: String? = null,
)

class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playerManager: PlayerManager,
    private val searchHistoryDao: SearchHistoryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Recent search history (maximum 5 items, lightweight via local SQLite)
    val recentSearches: StateFlow<List<SearchHistoryEntity>> = searchHistoryDao.getRecent()
        .map { it.take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(results = SearchResults(), isLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400L) // debounce
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val results = musicRepository.search(newQuery)
                _uiState.update { it.copy(isLoading = false, results = results) }
                if (results.songs.isNotEmpty()) {
                    recordSearch(newQuery)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Search failed")
                }
            }
        }
    }

    fun selectHistoryQuery(selectedQuery: String) {
        onQueryChange(selectedQuery)
    }

    fun recordSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.length >= 2) {
            viewModelScope.launch(Dispatchers.IO) {
                searchHistoryDao.deleteByQuery(trimmed)
                searchHistoryDao.insert(SearchHistoryEntity(query = trimmed, searchedAt = System.currentTimeMillis()))
                searchHistoryDao.trimOldEntries()
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDao.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDao.clearAll()
        }
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        recordSearch(_uiState.value.query)
        playerManager.playTrack(track, queue)
    }

    fun playNext(track: Track) {
        playerManager.playNextInQueue(track)
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }
}

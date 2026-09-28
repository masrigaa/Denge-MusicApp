package com.asla.denge.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.player.PlayerManager
import com.asla.denge.player.PlayerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerManager.playerState
    val currentPositionMs: StateFlow<Long> = playerManager.currentPositionMs

    private val _isCurrentTrackLiked = MutableStateFlow(false)
    val isCurrentTrackLiked: StateFlow<Boolean> = _isCurrentTrackLiked.asStateFlow()

    init {
        viewModelScope.launch {
            playerState.collect { state ->
                val videoId = state.currentTrack?.videoId
                if (videoId != null) {
                    _isCurrentTrackLiked.value = musicRepository.isTrackLikedSync(videoId)
                } else {
                    _isCurrentTrackLiked.value = false
                }
            }
        }
    }

    val userPlaylists: Flow<List<Playlist>> = musicRepository.getPlaylists()

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeatMode()
    }

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue)
    }

    fun playNextInQueue(track: Track) {
        playerManager.playNextInQueue(track)
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerManager.moveQueueItem(fromIndex, toIndex)
    }

    fun closePlayback() {
        playerManager.closePlayback()
    }

    fun fetchRadioTracks() {
        playerManager.fetchRelatedTracks(force = true)
    }

    fun toggleLikeCurrentTrack() {
        val current = playerState.value.currentTrack ?: return
        val currentLiked = _isCurrentTrackLiked.value
        // Instant visual feedback
        _isCurrentTrackLiked.value = !currentLiked
        viewModelScope.launch {
            try {
                val actualState = musicRepository.toggleLike(current)
                _isCurrentTrackLiked.value = actualState
            } catch (_: Exception) {
                _isCurrentTrackLiked.value = currentLiked // Rollback on error
            }
        }
    }

    fun addCurrentTrackToPlaylist(playlistId: String) {
        val current = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            musicRepository.addTrackToPlaylist(playlistId, current)
        }
    }

    fun createPlaylistAndAddCurrent(title: String) {
        val current = playerState.value.currentTrack ?: return
        viewModelScope.launch {
            val plId = musicRepository.createPlaylist(title)
            musicRepository.addTrackToPlaylist(plId, current)
        }
    }
}

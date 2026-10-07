package com.asla.denge.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.player.DownloadManager
import com.asla.denge.player.PlayerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val playerManager: PlayerManager,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    val downloadedTracks: StateFlow<List<Track>> = musicRepository.getDownloadedTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playlists: StateFlow<List<Playlist>> = musicRepository.getPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val likedSongs: StateFlow<List<Track>> = musicRepository.getLikedSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<Track>> = musicRepository.getPlaybackHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )



    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue, isOfflineQueue = false)
    }

    fun playDownloadedTrack(track: Track, queue: List<Track> = listOf(track)) {
        playerManager.playTrack(track, queue, isOfflineQueue = true)
    }

    fun playNext(track: Track) {
        playerManager.playNextInQueue(track)
    }

    fun addToQueue(track: Track) {
        playerManager.addToQueue(track)
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(title)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun getPlaylistTracks(playlistId: String): Flow<List<Track>> {
        return musicRepository.getPlaylistTracks(playlistId)
    }

    fun deleteDownloadedTrack(videoId: String) {
        downloadManager.deleteDownloadedTrack(videoId)
    }
}


package com.asla.denge.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.util.getFallbackThumbnailUrl
import com.asla.denge.util.toHdThumbnailUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepeatMode {
    OFF, ONE, ALL
}

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoadingRadio: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
)

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val playbackCacheManager: PlaybackCacheManager,
    private val settingsRepository: com.asla.denge.domain.repository.SettingsRepository,
    private val downloadManager: DownloadManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(30_000)
        .setReadTimeoutMs(30_000)
        .setAllowCrossProtocolRedirects(true)
        .setKeepPostFor302Redirects(true)
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

    private val cachedDataSourceFactory =
        playbackCacheManager.createCacheDataSourceFactory(httpDataSourceFactory)

    // DefaultDataSource.Factory handles BOTH local file:// URIs AND remote http(s)://
    // It automatically delegates file:// to FileDataSource, and http:// to the cache upstream.
    private val compositeDataSourceFactory = DefaultDataSource.Factory(context, cachedDataSourceFactory)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 60_000,
            /* maxBufferMs = */ 180_000,
            /* bufferForPlaybackMs = */ 1_000,
            /* bufferForPlaybackAfterRebufferMs = */ 2_500
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(30_000, true)
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(compositeDataSourceFactory))
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handleAudioFocus
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build().apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setAudioOffloadPreferences(
                    androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                        .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                        .setIsGaplessSupportRequired(false)
                        .setIsSpeedChangeSupportRequired(false)
                        .build()
                )
                .build()
        }

    var onAudioSessionIdAvailable: ((Int) -> Unit)? = null
    private var progressJob: Job? = null
    private var lastKnownPositionMs: Long = 0L

    var mediaSession: MediaSession? = null
        private set

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    init {
        try {
            val intent = Intent(context, com.asla.denge.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val sessionActivityPendingIntent = android.app.PendingIntent.getActivity(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val forwardingPlayer = object : ForwardingPlayer(exoPlayer) {
                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .build()
                }

                override fun isCommandAvailable(command: Int): Boolean {
                    return when (command) {
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                        else -> super.isCommandAvailable(command)
                    }
                }

                override fun seekToNext() {
                    playNext()
                }

                override fun seekToPrevious() {
                    playPrevious()
                }

                override fun seekToNextMediaItem() {
                    playNext()
                }

                override fun seekToPreviousMediaItem() {
                    playPrevious()
                }
            }

            mediaSession = MediaSession.Builder(context, forwardingPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()
        } catch (_: Exception) {
        }

        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
                refreshNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.update { it.copy(isBuffering = true) }
                    }
                    Player.STATE_READY -> {
                        val duration = exoPlayer.duration.coerceAtLeast(0L)
                        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                durationMs = duration,
                                currentPositionMs = pos
                            )
                        }
                        if (pos > 0L) {
                            lastKnownPositionMs = pos
                        }
                        val sessionId = exoPlayer.audioSessionId
                        if (sessionId != 0) {
                            onAudioSessionIdAvailable?.invoke(sessionId)
                        }
                        refreshNotification()
                    }
                    Player.STATE_ENDED -> {
                        if (stopAfterCurrentTrack) {
                            stopAfterCurrentTrack = false
                            _sleepTimerRemainingSeconds.value = null
                            _activeSleepTimerOption.value = null
                            exoPlayer.pause()
                            return
                        }
                        val duration = exoPlayer.duration
                        val pos = exoPlayer.currentPosition.coerceAtLeast(lastKnownPositionMs)
                        val isLocal = _playerState.value.currentTrack?.let { downloadManager.getLocalAudioFile(it.videoId) != null } ?: false
                        // If track unexpectedly ended with more than 15s remaining, it was a network drop (only relevant for online streams)
                        if (!isLocal && duration > 30_000L && (duration - pos) > 15_000L) {
                            val current = _playerState.value.currentTrack
                            if (current != null) {
                                loadAndPlay(current, startPositionMs = pos)
                                return
                            }
                        }
                        if (_playerState.value.repeatMode == RepeatMode.ONE) {
                            val current = _playerState.value.currentTrack
                            if (current != null) {
                                lastKnownPositionMs = 0L
                                loadAndPlay(current, startPositionMs = 0L)
                                return
                            }
                        }
                        lastKnownPositionMs = 0L
                        playNext()
                    }
                    Player.STATE_IDLE -> {
                        _playerState.update { it.copy(isBuffering = false) }
                    }
                }
            }

            private var errorRetryCount = 0

            override fun onPlayerError(error: PlaybackException) {
                val currentTrack = _playerState.value.currentTrack
                val resumePos = lastKnownPositionMs
                    .coerceAtLeast(exoPlayer.currentPosition)
                    .coerceAtLeast(_currentPositionMs.value)
                if (resumePos > 0L) {
                    lastKnownPositionMs = resumePos
                }

                if (currentTrack != null && errorRetryCount < 3) {
                    errorRetryCount++
                    scope.launch {
                        _playerState.update { it.copy(isBuffering = true) }
                        delay(1000L)
                        try {
                            loadAndPlay(currentTrack, startPositionMs = resumePos)
                        } catch (_: Exception) {
                            _playerState.update {
                                it.copy(
                                    isBuffering = false,
                                    isPlaying = false,
                                    error = error.localizedMessage ?: "Playback error"
                                )
                            }
                        }
                    }
                } else {
                    errorRetryCount = 0
                    _playerState.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = false,
                            error = error.localizedMessage ?: "Playback error"
                        )
                    }
                }
            }
        })
    }

    private var isServiceStarted = false

    fun refreshNotification() {
        try {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_REFRESH_NOTIFICATION
            }
            context.startService(intent)
        } catch (_: Exception) {
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            if (!isServiceStarted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isServiceStarted = true
            } else {
                refreshNotification()
            }
        } catch (_: Exception) {
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val index = newQueue.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
        lastKnownPositionMs = 0L
        _playerState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                currentIndex = index,
                isBuffering = true,
                error = null,
                currentPositionMs = 0L,
            )
        }

        loadAndPlay(track, startPositionMs = 0L)
    }

    private var loadJob: Job? = null
    private var relatedJob: Job? = null

    private fun loadAndPlay(track: Track, startPositionMs: Long = 0L) {
        loadJob?.cancel()
        relatedJob?.cancel()

        loadJob = scope.launch {
            try {
                lastKnownPositionMs = startPositionMs
                _currentPositionMs.value = startPositionMs
                _playerState.update {
                    it.copy(
                        currentTrack = track,
                        isPlaying = true,
                        isBuffering = true,
                        error = null,
                        currentPositionMs = startPositionMs
                    )
                }

                val artUrl = toHdThumbnailUrl(track.thumbnailUrl, track.videoId)
                    ?: getFallbackThumbnailUrl(track.videoId)

                val localAudioFile = downloadManager.getLocalAudioFile(track.videoId)
                val preloaded = preloadedUrls.remove(track.videoId)
                val (streamUrl, artBytes) = coroutineScope {
                    val streamDeferred = async(Dispatchers.IO) {
                        if (localAudioFile != null && localAudioFile.exists()) {
                            Uri.fromFile(localAudioFile).toString()
                        } else {
                            preloaded ?: run {
                                val quality = settingsRepository.getAudioQualitySync().id
                                musicRepository.getStreamUrl(track.videoId, quality)
                            }
                        }
                    }
                    val artDeferred = async(Dispatchers.IO) {
                        val localThumb = downloadManager.getLocalThumbnailFile(track.videoId)
                        if (localThumb != null && localThumb.exists() && localThumb.length() > 5_000) {
                            localThumb.readBytes()
                        } else {
                            getOrFetchArtworkBytes(track.videoId, artUrl)
                        }
                    }
                    Pair(streamDeferred.await(), artDeferred.await())
                }

                val mediaMetadataBuilder = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artistName)
                    .setArtworkUri(artUrl?.let { Uri.parse(it) })
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

                if (artBytes != null && artBytes.isNotEmpty()) {
                    mediaMetadataBuilder.setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }

                val mediaMetadata = mediaMetadataBuilder.build()

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(track.videoId)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                exoPlayer.setMediaItem(mediaItem, startPositionMs)
                exoPlayer.prepare()
                exoPlayer.play()
                startPlaybackService()
                startProgressTracker()
                scope.launch {
                    delay(150)
                    refreshNotification()
                }

                val sessionId = exoPlayer.audioSessionId
                if (sessionId != 0) {
                    onAudioSessionIdAvailable?.invoke(sessionId)
                }

                // Background fetch related tracks when repeat is OFF
                if (_playerState.value.repeatMode == RepeatMode.OFF) {
                    val shouldForceRadio = _playerState.value.queue.size <= 1
                    fetchRelatedTracksInternal(track, force = shouldForceRadio)
                }

                // Pre-fetch artwork for the next track in queue to ensure 0ms instant notification update
                scope.launch(Dispatchers.IO) {
                    val q = _playerState.value.queue
                    val nextIdx = _playerState.value.currentIndex + 1
                    if (nextIdx in q.indices) {
                        val nextT = q[nextIdx]
                        val nextArt = toHdThumbnailUrl(nextT.thumbnailUrl, nextT.videoId)
                            ?: getFallbackThumbnailUrl(nextT.videoId)
                        getOrFetchArtworkBytes(nextT.videoId, nextArt)
                    }
                }

                // Record history safely with full track metadata
                withContext(Dispatchers.IO) {
                    try {
                        musicRepository.recordPlay(track, 0L, "player")
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                _playerState.update {
                    it.copy(
                        isBuffering = false,
                        error = e.localizedMessage ?: "Could not load audio stream"
                    )
                }
            }
        }
    }

    private val artworkCache = object : android.util.LruCache<String, ByteArray>(30) {}

    private suspend fun getOrFetchArtworkBytes(videoId: String, url: String?): ByteArray? {
        if (url.isNullOrBlank()) return null
        synchronized(artworkCache) {
            artworkCache.get(videoId)?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 4000
                connection.instanceFollowRedirects = true
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        val isYtThumbnail = url.contains("ytimg.com") || url.contains("youtube.com")
                        val croppedBitmap = if (isYtThumbnail && bitmap.width > 0 && bitmap.height > 0) {
                            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            if (ratio in 1.25f..1.4f) {
                                // 4:3 letterboxed thumbnail (e.g. 480x360 with 45px top/bottom black bars)
                                val activeH = (bitmap.height * 0.75f).toInt()
                                val top = (bitmap.height - activeH) / 2
                                val squareDim = activeH.coerceAtMost(bitmap.width)
                                val left = (bitmap.width - squareDim) / 2
                                android.graphics.Bitmap.createBitmap(bitmap, left, top, squareDim, squareDim)
                            } else {
                                bitmap
                            }
                        } else {
                            bitmap
                        }

                        val maxDim = 512
                        val width = croppedBitmap.width
                        val height = croppedBitmap.height
                        val scaled = if (width > maxDim || height > maxDim) {
                            val ratio = width.toFloat() / height.toFloat()
                            val (newW, newH) = if (ratio > 1f) {
                                maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
                            } else {
                                (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
                            }
                            android.graphics.Bitmap.createScaledBitmap(croppedBitmap, newW, newH, true)
                        } else {
                            croppedBitmap
                        }
                        val outputStream = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                        val compressedBytes = outputStream.toByteArray()
                        synchronized(artworkCache) {
                            artworkCache.put(videoId, compressedBytes)
                        }
                        return@withContext compressedBytes
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun normalizeArtist(name: String): String {
        return name.trim().lowercase()
    }

    private fun filterDiverseTracks(candidates: List<Track>, currentQueue: List<Track>, limit: Int = 10): List<Track> {
        val existingIds = currentQueue.map { it.videoId }.toSet()
        val artistCounts = currentQueue
            .map { normalizeArtist(it.artistName) }
            .filter { it.isNotBlank() && it != "artis" && it != "artist" }
            .groupingBy { it }
            .eachCount()
            .toMutableMap()

        val result = mutableListOf<Track>()
        // Maximum 3 tracks per artist across the entire queue
        for (cand in candidates) {
            if (cand.videoId in existingIds) continue
            val normArtist = normalizeArtist(cand.artistName)
            val count = artistCounts[normArtist] ?: 0
            if (normArtist.isNotBlank() && normArtist != "artis" && normArtist != "artist" && count >= 3) {
                continue
            }
            result.add(cand)
            artistCounts[normArtist] = count + 1
            if (result.size >= limit) return result
        }

        return result
    }

    fun fetchRelatedTracks(force: Boolean = false) {
        val track = _playerState.value.currentTrack ?: return
        fetchRelatedTracksInternal(track, force = force)
    }

    private fun fetchRelatedTracksInternal(track: Track, force: Boolean = false) {
        if (_playerState.value.repeatMode != RepeatMode.OFF) return
        relatedJob?.cancel()
        relatedJob = scope.launch {
            try {
                if (_playerState.value.repeatMode != RepeatMode.OFF) return@launch
                val currentQueue = _playerState.value.queue
                val currentIndex = _playerState.value.currentIndex
                val remainingAhead = (currentQueue.size - 1 - currentIndex).coerceAtLeast(0)

                if (force || remainingAhead < 3) {
                    _playerState.update { it.copy(isLoadingRadio = true) }
                    val related = withContext(Dispatchers.IO) {
                        musicRepository.getRelatedTracks(track.videoId, track.artistName, track.title)
                    }
                    if (_playerState.value.repeatMode != RepeatMode.OFF) return@launch
                    if (related.isNotEmpty()) {
                        val latestQueue = _playerState.value.queue
                        val countToAdd = if (force || latestQueue.size <= 2) 10 else 5
                        val newTracks = filterDiverseTracks(related, latestQueue, limit = countToAdd)
                        if (newTracks.isNotEmpty()) {
                            _playerState.update { state ->
                                state.copy(queue = state.queue + newTracks)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                _playerState.update { it.copy(isLoadingRadio = false) }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            val pos = exoPlayer.currentPosition
            if (pos > 0L) {
                lastKnownPositionMs = pos
            }
            exoPlayer.pause()
        } else {
            val track = _playerState.value.currentTrack
            if (track != null) {
                val state = exoPlayer.playbackState
                if (state == Player.STATE_IDLE) {
                    // Recover from idle / error by reloading and seeking to lastKnownPositionMs seamlessly!
                    loadAndPlay(track, startPositionMs = lastKnownPositionMs)
                } else if (state == Player.STATE_ENDED) {
                    // Song completed, restart from 0
                    lastKnownPositionMs = 0L
                    loadAndPlay(track, startPositionMs = 0L)
                } else {
                    // Paused: verify and restore position if drifted
                    if (lastKnownPositionMs > 0L && Math.abs(exoPlayer.currentPosition - lastKnownPositionMs) > 3000L) {
                        exoPlayer.seekTo(lastKnownPositionMs)
                    }
                    exoPlayer.play()
                }
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        lastKnownPositionMs = positionMs
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun playNext() {
        val currentState = _playerState.value
        val queue = currentState.queue
        if (queue.isEmpty()) return

        val nextIndex = if (currentState.isShuffleEnabled) {
            queue.indices.random()
        } else {
            currentState.currentIndex + 1
        }

        if (nextIndex in queue.indices) {
            val nextTrack = queue[nextIndex]
            _playerState.update {
                it.copy(
                    currentTrack = nextTrack,
                    currentIndex = nextIndex,
                )
            }
            loadAndPlay(nextTrack, startPositionMs = 0L)
        } else if (currentState.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            // Loop back to the beginning of the playlist/queue without adding new tracks
            val firstTrack = queue[0]
            _playerState.update {
                it.copy(
                    currentTrack = firstTrack,
                    currentIndex = 0,
                )
            }
            loadAndPlay(firstTrack, startPositionMs = 0L)
        } else if (currentState.repeatMode == RepeatMode.OFF) {
            // Queue is exhausted & repeat is OFF: fetch related and auto-play seamlessly
            scope.launch {
                val current = currentState.currentTrack
                if (current != null) {
                    _playerState.update { it.copy(isLoadingRadio = true) }
                    val related = try {
                        withContext(Dispatchers.IO) {
                            musicRepository.getRelatedTracks(current.videoId, current.artistName, current.title)
                        }
                    } catch (_: Exception) {
                        emptyList()
                    } finally {
                        _playerState.update { it.copy(isLoadingRadio = false) }
                    }
                    val newTracks = filterDiverseTracks(related, queue, limit = 3)
                    if (newTracks.isNotEmpty()) {
                        val nextTrack = newTracks.first()
                        val updatedQueue = queue + newTracks
                        val newIdx = queue.size
                        _playerState.update {
                            it.copy(
                                currentTrack = nextTrack,
                                queue = updatedQueue,
                                currentIndex = newIdx,
                            )
                        }
                        loadAndPlay(nextTrack, startPositionMs = 0L)
                    }
                }
            }
        }
    }

    fun playPrevious() {
        val currentState = _playerState.value
        if (exoPlayer.currentPosition > 3000L) {
            lastKnownPositionMs = 0L
            exoPlayer.seekTo(0L)
            return
        }

        val prevIndex = currentState.currentIndex - 1
        if (prevIndex in currentState.queue.indices) {
            val prevTrack = currentState.queue[prevIndex]
            _playerState.update {
                it.copy(
                    currentTrack = prevTrack,
                    currentIndex = prevIndex,
                )
            }
            loadAndPlay(prevTrack, startPositionMs = 0L)
        } else {
            lastKnownPositionMs = 0L
            exoPlayer.seekTo(0L)
        }
    }

    fun playNextInQueue(track: Track) {
        val currentQueue = _playerState.value.queue.toMutableList()
        val currentIndex = _playerState.value.currentIndex
        val currentTrack = _playerState.value.currentTrack

        if (currentTrack == null || currentQueue.isEmpty()) {
            playTrack(track)
            return
        }

        // Remove if already present in queue after currentIndex to avoid immediate duplicate
        val existingIndex = currentQueue.indexOfFirst { it.videoId == track.videoId }
        if (existingIndex > currentIndex) {
            currentQueue.removeAt(existingIndex)
        }

        val insertIndex = (currentIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(insertIndex, track)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun addToQueue(track: Track) {
        val currentQueue = _playerState.value.queue.toMutableList()
        val currentTrack = _playerState.value.currentTrack

        if (currentTrack == null || currentQueue.isEmpty()) {
            playTrack(track)
            return
        }

        currentQueue.add(track)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        val currentIndex = _playerState.value.currentIndex

        currentQueue.removeAt(index)
        val newIndex = when {
            index < currentIndex -> currentIndex - 1
            index == currentIndex -> currentIndex.coerceAtMost(currentQueue.size - 1)
            else -> currentIndex
        }
        _playerState.update {
            it.copy(
                queue = currentQueue,
                currentIndex = newIndex,
                currentTrack = if (currentQueue.isNotEmpty() && newIndex in currentQueue.indices) currentQueue[newIndex] else it.currentTrack
            )
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val currentTrack = _playerState.value.currentTrack
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        val newCurrentIndex = if (currentTrack != null) {
            val foundIdx = currentQueue.indexOfFirst { it.videoId == currentTrack.videoId }
            if (foundIdx >= 0) foundIdx else _playerState.value.currentIndex
        } else _playerState.value.currentIndex
        _playerState.update {
            it.copy(
                queue = currentQueue,
                currentIndex = newCurrentIndex,
            )
        }
    }

    fun closePlayback() {
        loadJob?.cancel()
        relatedJob?.cancel()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        stopProgressTracker()
        _playerState.update {
            it.copy(
                currentTrack = null,
                isPlaying = false,
                isBuffering = false,
                queue = emptyList(),
                currentIndex = 0,
                durationMs = 0L,
                currentPositionMs = 0L,
                error = null,
            )
        }
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.stopService(intent)
            isServiceStarted = false
        } catch (_: Exception) {}
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        if (nextMode != RepeatMode.OFF) {
            relatedJob?.cancel()
            _playerState.update { it.copy(isLoadingRadio = false) }
        }
        exoPlayer.repeatMode = when (nextMode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _playerState.update { it.copy(repeatMode = nextMode) }
    }

    private var isAppInForeground: Boolean = true
    private val preloadedUrls = java.util.concurrent.ConcurrentHashMap<String, String>()
    private var prefetchJob: Job? = null

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _activeSleepTimerOption = MutableStateFlow<Int?>(null)
    val activeSleepTimerOption: StateFlow<Int?> = _activeSleepTimerOption.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var stopAfterCurrentTrack: Boolean = false

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground && exoPlayer.isPlaying) {
            _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
        }
    }

    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _activeSleepTimerOption.value = minutes
        if (minutes == -1) {
            stopAfterCurrentTrack = true
            _sleepTimerRemainingSeconds.value = -1L
            return
        }
        if (minutes <= 0) return

        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSeconds.value = totalSeconds
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
                if (remaining in 1..5) {
                    val targetVol = (remaining / 5f).coerceIn(0.1f, 1f)
                    exoPlayer.volume = targetVol
                }
            }
            if (isActive) {
                exoPlayer.pause()
                exoPlayer.volume = 1f
                _sleepTimerRemainingSeconds.value = null
                _activeSleepTimerOption.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        stopAfterCurrentTrack = false
        _sleepTimerRemainingSeconds.value = null
        _activeSleepTimerOption.value = null
        exoPlayer.volume = 1f
    }

    private fun maybePrefetchNextTrack(currentPos: Long, duration: Long) {
        if (duration <= 0L) return
        val remainingMs = duration - currentPos
        val passedEightyPercent = duration > 0 && (currentPos.toFloat() / duration.toFloat()) > 0.8f
        val shouldPrefetch = remainingMs in 1L..25_000L || passedEightyPercent
        if (!shouldPrefetch || prefetchJob?.isActive == true) return

        val state = _playerState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx in state.queue.indices) {
            val nextTrack = state.queue[nextIdx]
            if (!preloadedUrls.containsKey(nextTrack.videoId)) {
                prefetchJob = scope.launch(Dispatchers.IO) {
                    try {
                        val quality = settingsRepository.getAudioQualitySync().id
                        val url = musicRepository.getStreamUrl(nextTrack.videoId, quality)
                        if (url.isNotBlank()) {
                            preloadedUrls[nextTrack.videoId] = url
                            val nextArt = toHdThumbnailUrl(nextTrack.thumbnailUrl, nextTrack.videoId)
                                ?: getFallbackThumbnailUrl(nextTrack.videoId)
                            getOrFetchArtworkBytes(nextTrack.videoId, nextArt)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    if (pos > 0L) {
                        lastKnownPositionMs = pos
                    }
                    maybePrefetchNextTrack(pos, dur)
                }
                val interval = if (isAppInForeground) 300L else 2500L
                delay(interval)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        cancelSleepTimer()
        prefetchJob?.cancel()
        preloadedUrls.clear()
        stopProgressTracker()
        loadJob?.cancel()
        relatedJob?.cancel()
        isServiceStarted = false
        mediaSession?.release()
        exoPlayer.release()
    }
}

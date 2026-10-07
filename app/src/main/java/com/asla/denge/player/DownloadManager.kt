package com.asla.denge.player

import android.content.Context
import android.os.Environment
import android.util.Log
import com.asla.denge.data.local.dao.DownloadDao
import com.asla.denge.data.local.entity.DownloadedTrackEntity
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.MusicRepository
import com.asla.denge.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val videoId: String, val progress: Float = 0f) : DownloadStatus()
    data class Success(val videoId: String) : DownloadStatus()
    data class Error(val videoId: String, val message: String) : DownloadStatus()
}

class DownloadManager(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val downloadDao: DownloadDao,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Primary download directory: /storage/emulated/0/Download/Denge
    private val publicDownloadsDir: File
        get() {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dengeDir = File(publicDir, "Denge")
            if (!dengeDir.exists()) {
                dengeDir.mkdirs()
            }
            return if (dengeDir.exists() && dengeDir.canWrite()) {
                dengeDir
            } else {
                // Fallback to app external or internal filesDir if public download folder is inaccessible
                val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "Denge")
                if (!fallback.exists()) fallback.mkdirs()
                fallback
            }
        }

    // App internal downloads dir for backwards compatibility / fallback
    private val legacyDownloadsDir = File(context.filesDir, "downloads")
    private val thumbnailsDir = File(context.filesDir, "download_thumbnails").apply { if (!exists()) mkdirs() }

    private val localPathCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    init {
        scope.launch {
            try {
                downloadDao.getAllDownloadedTracks().collect { list ->
                    list.forEach { track ->
                        if (track.localFilePath.isNotBlank()) {
                            localPathCache[track.videoId] = track.localFilePath
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("DownloadManager", "Error observing downloaded tracks cache: ${e.message}")
            }
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100)
    }

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    fun getLocalAudioFile(videoId: String): File? {
        // 1. Check mapped local path from database cache
        val cachedPath = localPathCache[videoId]
        if (!cachedPath.isNullOrBlank()) {
            val cachedFile = File(cachedPath)
            if (cachedFile.exists() && cachedFile.length() > 1024) return cachedFile
        }

        // 2. Check public Denge folder with videoId (legacy / backwards compatibility)
        val publicLegacy = File(publicDownloadsDir, "$videoId.m4a")
        if (publicLegacy.exists() && publicLegacy.length() > 1024) return publicLegacy

        // 3. Fallback: check legacy app internal filesDir
        val internalLegacy = File(legacyDownloadsDir, "$videoId.m4a")
        if (internalLegacy.exists() && internalLegacy.length() > 1024) return internalLegacy

        return null
    }

    fun getLocalThumbnailFile(videoId: String): File? {
        val file = File(thumbnailsDir, "$videoId.jpg")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun downloadTrack(track: Track) {
        val current = _downloadStatus.value[track.videoId]
        if (current is DownloadStatus.Downloading) return

        scope.launch {
            _downloadStatus.value = _downloadStatus.value + (track.videoId to DownloadStatus.Downloading(track.videoId, 0f))
            try {
                // 1. Resolve stream URL
                val quality = settingsRepository.getAudioQualitySync().id
                val streamUrl = musicRepository.getStreamUrl(track.videoId, quality)
                if (streamUrl.isBlank()) {
                    throw IllegalStateException("Failed to resolve audio stream URL")
                }

                // 2. Download audio file into Download/Denge using range-based chunked download
                //    Format: Judul Lagu - Artis.m4a
                val targetDir = publicDownloadsDir
                val cleanTitle = sanitizeFilename(track.title).ifBlank { track.videoId }
                val cleanArtist = sanitizeFilename(track.artistName).ifBlank { "Artis" }
                val fileName = "$cleanTitle - $cleanArtist.m4a"
                val targetAudioFile = File(targetDir, fileName)
                val tempAudioFile = File(targetDir, "${track.videoId}.tmp")

                // First, do a HEAD request to get total file size
                val totalBytes = getContentLength(streamUrl)

                if (totalBytes > 0) {
                    // Range-based chunked download for maximum speed
                    downloadWithRangeRequests(streamUrl, tempAudioFile, totalBytes, track.videoId)
                } else {
                    // Fallback: single connection download if content-length unavailable
                    downloadSingleStream(streamUrl, tempAudioFile, track.videoId)
                }

                if (tempAudioFile.length() < 1024) {
                    tempAudioFile.delete()
                    throw IllegalStateException("Downloaded file is empty or corrupted")
                }

                if (targetAudioFile.exists()) targetAudioFile.delete()
                tempAudioFile.renameTo(targetAudioFile)

                localPathCache[track.videoId] = targetAudioFile.absolutePath

                // 3. Save thumbnail locally in full HD resolution if available
                var localThumbPath: String? = null
                val hdThumbUrl = com.asla.denge.util.toHdThumbnailUrl(track.thumbnailUrl, track.videoId)
                if (hdThumbUrl.isNotBlank()) {
                    try {
                        val thumbFile = File(thumbnailsDir, "${track.videoId}.jpg")
                        val thumbConn = (URL(hdThumbUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 10_000
                            readTimeout = 10_000
                            connect()
                        }
                        thumbConn.inputStream.use { thumbIn ->
                            FileOutputStream(thumbFile).use { thumbOut ->
                                thumbIn.copyTo(thumbOut)
                            }
                        }
                        if (thumbFile.exists() && thumbFile.length() > 0) {
                            localThumbPath = thumbFile.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.w("DownloadManager", "Could not cache thumbnail locally: ${e.message}")
                    }
                }

                // 4. Save to Room database
                val entity = DownloadedTrackEntity(
                    videoId = track.videoId,
                    title = track.title,
                    artistName = track.artistName,
                    durationMs = track.durationMs,
                    thumbnailUrl = hdThumbUrl.ifBlank { track.thumbnailUrl },
                    localFilePath = targetAudioFile.absolutePath,
                    localThumbnailPath = localThumbPath,
                    fileSizeBytes = targetAudioFile.length(),
                    downloadedAt = System.currentTimeMillis(),
                )
                downloadDao.insert(entity)

                _downloadStatus.value = _downloadStatus.value + (track.videoId to DownloadStatus.Success(track.videoId))
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed for ${track.title}", e)
                _downloadStatus.value = _downloadStatus.value + (track.videoId to DownloadStatus.Error(track.videoId, e.message ?: "Download failed"))
            }
        }
    }

    /**
     * Get content length via HEAD request, falling back to GET with Range header.
     */
    private fun getContentLength(url: String): Long {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 10_000
                readTimeout = 5_000
                setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
                instanceFollowRedirects = true
            }
            val length = conn.contentLengthLong
            conn.disconnect()
            if (length > 0) return length
        } catch (_: Exception) {}

        // Fallback: partial GET to discover content-range
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 5_000
                setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
                setRequestProperty("Range", "bytes=0-0")
                instanceFollowRedirects = true
            }
            val contentRange = conn.getHeaderField("Content-Range") // e.g. "bytes 0-0/1234567"
            conn.disconnect()
            if (contentRange != null) {
                val totalStr = contentRange.substringAfter("/", "")
                totalStr.toLongOrNull()?.let { return it }
            }
        } catch (_: Exception) {}

        return -1L
    }

    /**
     * Download using HTTP Range requests in 2MB chunks.
     * This bypasses YouTube's per-connection throttling and achieves full speed.
     */
    private fun downloadWithRangeRequests(url: String, targetFile: File, totalBytes: Long, videoId: String) {
        val chunkSize = 2L * 1024L * 1024L // 2MB chunks - fast enough to avoid throttle
        val buffer = ByteArray(128 * 1024) // 128KB read buffer for maximum I/O throughput
        var downloaded = 0L
        var lastReportedPercent = -1

        FileOutputStream(targetFile).use { output ->
            while (downloaded < totalBytes) {
                val rangeEnd = (downloaded + chunkSize - 1).coerceAtMost(totalBytes - 1)
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
                    setRequestProperty("Range", "bytes=$downloaded-$rangeEnd")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Accept-Encoding", "identity") // No compression, raw bytes
                    setRequestProperty("Connection", "keep-alive")
                    instanceFollowRedirects = true
                    connect()
                }

                try {
                    conn.inputStream.use { input ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val currentPercent = ((downloaded * 100) / totalBytes).toInt()
                            if (currentPercent != lastReportedPercent) {
                                lastReportedPercent = currentPercent
                                _downloadStatus.value = _downloadStatus.value +
                                    (videoId to DownloadStatus.Downloading(videoId, downloaded.toFloat() / totalBytes))
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
            output.flush()
        }
    }

    /**
     * Fallback single-stream download when content length is unknown.
     */
    private fun downloadSingleStream(url: String, targetFile: File, videoId: String) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            setRequestProperty("Accept-Encoding", "identity")
            instanceFollowRedirects = true
            connect()
        }

        val totalBytes = conn.contentLengthLong
        var downloaded = 0L
        var lastReportedPercent = -1
        val buffer = ByteArray(128 * 1024)

        conn.inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        val currentPercent = ((downloaded * 100) / totalBytes).toInt()
                        if (currentPercent != lastReportedPercent) {
                            lastReportedPercent = currentPercent
                            _downloadStatus.value = _downloadStatus.value +
                                (videoId to DownloadStatus.Downloading(videoId, downloaded.toFloat() / totalBytes))
                        }
                    }
                }
                output.flush()
            }
        }
    }

    companion object {
        private const val DOWNLOAD_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }

    fun deleteDownloadedTrack(videoId: String) {
        scope.launch {
            try {
                // Delete from cached path
                val cachedPath = localPathCache.remove(videoId)
                if (!cachedPath.isNullOrBlank()) {
                    val f = File(cachedPath)
                    if (f.exists()) f.delete()
                }

                // Delete from DB record path if exists
                val record = downloadDao.getDownloadedTrack(videoId)
                if (record != null && record.localFilePath.isNotBlank()) {
                    val f = File(record.localFilePath)
                    if (f.exists()) f.delete()
                }

                // Delete from public Denge folder (legacy videoId.m4a)
                val publicAudioFile = File(publicDownloadsDir, "$videoId.m4a")
                if (publicAudioFile.exists()) publicAudioFile.delete()

                // Delete from legacy internal folder if exists
                val legacyAudioFile = File(legacyDownloadsDir, "$videoId.m4a")
                if (legacyAudioFile.exists()) legacyAudioFile.delete()

                val thumbFile = File(thumbnailsDir, "$videoId.jpg")
                if (thumbFile.exists()) thumbFile.delete()

                downloadDao.delete(videoId)
                _downloadStatus.value = _downloadStatus.value - videoId
            } catch (e: Exception) {
                Log.e("DownloadManager", "Failed to delete downloaded track $videoId", e)
            }
        }
    }
}

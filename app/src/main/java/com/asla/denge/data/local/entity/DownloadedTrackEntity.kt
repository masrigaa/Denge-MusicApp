package com.asla.denge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracks downloaded and saved locally for offline playback.
 */
@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String,

    @ColumnInfo(name = "local_thumbnail_path")
    val localThumbnailPath: String? = null,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0L,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis(),
)

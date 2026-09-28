package com.asla.denge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached track metadata.
 * Maps to `cached_tracks` table in Schema.md.
 */
@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String,

    @ColumnInfo(name = "artist_id")
    val artistId: String? = null,

    @ColumnInfo(name = "album_name")
    val albumName: String? = null,

    @ColumnInfo(name = "album_id")
    val albumId: String? = null,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "explicit", defaultValue = "0")
    val explicit: Int = 0,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null,
)

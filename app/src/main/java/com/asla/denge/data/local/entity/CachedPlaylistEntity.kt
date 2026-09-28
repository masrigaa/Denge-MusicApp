package com.asla.denge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached playlist metadata.
 * Maps to `cached_playlists` table in Schema.md.
 */
@Entity(tableName = "cached_playlists")
data class CachedPlaylistEntity(
    @PrimaryKey
    @ColumnInfo(name = "playlist_id")
    val playlistId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "track_count", defaultValue = "0")
    val trackCount: Int = 0,

    @ColumnInfo(name = "is_liked_music", defaultValue = "0")
    val isLikedMusic: Int = 0,

    @ColumnInfo(name = "is_editable", defaultValue = "1")
    val isEditable: Int = 1,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,
)

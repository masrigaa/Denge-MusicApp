package com.asla.denge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Join table linking tracks to playlists with ordering.
 * Maps to `playlist_tracks` table in Schema.md.
 */
@Entity(
    tableName = "playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = CachedPlaylistEntity::class,
            parentColumns = ["playlist_id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlist_id", "position"]),
        Index(value = ["playlist_id", "video_id"], unique = true),
        Index(value = ["video_id"]),
    ],
)
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "playlist_id")
    val playlistId: String,

    @ColumnInfo(name = "video_id")
    val videoId: String,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)

package com.asla.denge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Playback history log entry.
 */
@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["played_at"]),
        Index(value = ["video_id"]),
    ],
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "video_id")
    val videoId: String,

    @ColumnInfo(name = "played_at")
    val playedAt: Long,

    @ColumnInfo(name = "duration_played_ms", defaultValue = "0")
    val durationPlayedMs: Long = 0,

    @ColumnInfo(name = "source")
    val source: String,
)

/**
 * Current playback queue item.
 */
@Entity(
    tableName = "queue_items",
    indices = [
        Index(value = ["position"]),
        Index(value = ["video_id"]),
    ],
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "video_id")
    val videoId: String,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "is_current", defaultValue = "0")
    val isCurrent: Int = 0,

    @ColumnInfo(name = "progress_ms", defaultValue = "0")
    val progressMs: Long = 0,
)

/**
 * Search history entry.
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "query")
    val query: String,

    @ColumnInfo(name = "searched_at")
    val searchedAt: Long,
)

/**
 * Equalizer preset.
 */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bands_json")
    val bandsJson: String,

    @ColumnInfo(name = "is_active", defaultValue = "0")
    val isActive: Int = 0,

    @ColumnInfo(name = "is_builtin", defaultValue = "0")
    val isBuiltin: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

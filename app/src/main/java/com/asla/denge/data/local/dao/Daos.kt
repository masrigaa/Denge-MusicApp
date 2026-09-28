package com.asla.denge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asla.denge.data.local.entity.CachedPlaylistEntity
import com.asla.denge.data.local.entity.CachedTrackEntity
import com.asla.denge.data.local.entity.EqPresetEntity
import com.asla.denge.data.local.entity.PlaybackHistoryEntity
import com.asla.denge.data.local.entity.PlaylistTrackEntity
import com.asla.denge.data.local.entity.QueueItemEntity
import com.asla.denge.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(track: CachedTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<CachedTrackEntity>)

    @Query("SELECT * FROM cached_tracks WHERE video_id = :videoId")
    suspend fun getById(videoId: String): CachedTrackEntity?

    @Query("SELECT * FROM cached_tracks ORDER BY last_played_at DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<CachedTrackEntity>>

    @Query("UPDATE cached_tracks SET last_played_at = :timestamp WHERE video_id = :videoId")
    suspend fun updateLastPlayed(videoId: String, timestamp: Long)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: CachedPlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(playlists: List<CachedPlaylistEntity>)

    @Query("SELECT * FROM cached_playlists ORDER BY title ASC")
    fun getAll(): Flow<List<CachedPlaylistEntity>>

    @Query("SELECT * FROM cached_playlists WHERE playlist_id = :playlistId LIMIT 1")
    suspend fun getById(playlistId: String): CachedPlaylistEntity?

    @Query("SELECT * FROM cached_playlists WHERE is_liked_music = 1 LIMIT 1")
    suspend fun getLikedMusicPlaylist(): CachedPlaylistEntity?

    @Query("UPDATE cached_playlists SET track_count = :count WHERE playlist_id = :playlistId")
    suspend fun updateTrackCount(playlistId: String, count: Int)

    @Query("DELETE FROM cached_playlists WHERE playlist_id = :playlistId")
    suspend fun delete(playlistId: String)
}

@Dao
interface PlaylistTrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlaylistTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaylistTrackEntity>)

    @Query("""
        SELECT t.* FROM cached_tracks t
        INNER JOIN playlist_tracks pt ON t.video_id = pt.video_id
        WHERE pt.playlist_id = :playlistId
        ORDER BY pt.added_at DESC, pt.position ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<CachedTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_tracks WHERE playlist_id = :playlistId AND video_id = :videoId)")
    fun isTrackInPlaylist(playlistId: String, videoId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = :playlistId AND video_id = :videoId")
    suspend fun hasTrack(playlistId: String, videoId: String): Int

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun getTrackCount(playlistId: String): Int

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId AND video_id = :videoId")
    suspend fun removeTrack(playlistId: String, videoId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Query("""
        SELECT t.* FROM cached_tracks t
        INNER JOIN playlist_tracks pt ON t.video_id = pt.video_id
        WHERE pt.playlist_id = :playlistId
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    suspend fun getRandomTracksFromPlaylist(playlistId: String, limit: Int = 1): List<CachedTrackEntity>
}

data class TrackWithPlayCount(
    @androidx.room.Embedded val track: CachedTrackEntity,
    @androidx.room.ColumnInfo(name = "monthly_play_count") val playCount: Int = 1,
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE video_id = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("""
        SELECT t.* FROM cached_tracks t
        INNER JOIN (
            SELECT video_id, MAX(played_at) as max_played_at
            FROM playback_history
            GROUP BY video_id
        ) latest ON t.video_id = latest.video_id
        ORDER BY latest.max_played_at DESC
        LIMIT :limit
    """)
    fun getRecentHistory(limit: Int = 50): Flow<List<CachedTrackEntity>>

    @Query("""
        SELECT t.*, stats.play_count as monthly_play_count FROM cached_tracks t
        INNER JOIN (
            SELECT video_id, COUNT(*) as play_count, MAX(played_at) as max_played_at
            FROM playback_history
            WHERE played_at >= :startOfMonth
            GROUP BY video_id
        ) stats ON t.video_id = stats.video_id
        ORDER BY stats.play_count DESC, stats.max_played_at DESC
        LIMIT :limit
    """)
    fun getMostPlayedThisMonth(startOfMonth: Long, limit: Int = 2): Flow<List<TrackWithPlayCount>>

    @Query("""
        SELECT t.* FROM cached_tracks t
        INNER JOIN (
            SELECT video_id, COUNT(*) as play_count, MAX(played_at) as max_played_at
            FROM playback_history
            GROUP BY video_id
        ) stats ON t.video_id = stats.video_id
        ORDER BY stats.play_count DESC, stats.max_played_at DESC
        LIMIT :limit
    """)
    fun getMostPlayed(limit: Int = 10): Flow<List<CachedTrackEntity>>

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY played_at DESC LIMIT :keepCount)")
    suspend fun trimOldHistory(keepCount: Int = 1000)
}

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueItemEntity>)

    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun getAll(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrent(): QueueItemEntity?

    @Query("DELETE FROM queue_items")
    suspend fun clearAll()

    @Query("UPDATE queue_items SET is_current = 0")
    suspend fun clearCurrent()

    @Query("UPDATE queue_items SET is_current = 1, progress_ms = :progressMs WHERE video_id = :videoId")
    suspend fun setCurrent(videoId: String, progressMs: Long = 0)
}

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY searched_at DESC LIMIT 20")
    fun getRecent(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY searched_at DESC LIMIT 50)")
    suspend fun trimOldEntries()
}

@Dao
interface EqPresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: EqPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(presets: List<EqPresetEntity>)

    @Query("SELECT * FROM eq_presets ORDER BY is_builtin DESC, name ASC")
    fun getAll(): Flow<List<EqPresetEntity>>

    @Query("SELECT * FROM eq_presets WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): EqPresetEntity?

    @Query("UPDATE eq_presets SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE eq_presets SET is_active = 1 WHERE id = :presetId")
    suspend fun activate(presetId: Long)

    @Query("DELETE FROM eq_presets WHERE id = :presetId AND is_builtin = 0")
    suspend fun delete(presetId: Long)
}

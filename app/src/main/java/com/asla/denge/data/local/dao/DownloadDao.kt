package com.asla.denge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.asla.denge.data.local.entity.DownloadedTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: DownloadedTrackEntity)

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloaded_at DESC")
    fun getAllDownloadedTracks(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks")
    suspend fun getAllDownloadedTracksList(): List<DownloadedTrackEntity>

    @Query("SELECT * FROM downloaded_tracks WHERE video_id = :videoId LIMIT 1")
    suspend fun getDownloadedTrack(videoId: String): DownloadedTrackEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE video_id = :videoId)")
    fun isTrackDownloaded(videoId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE video_id = :videoId)")
    suspend fun isTrackDownloadedSync(videoId: String): Boolean

    @Query("DELETE FROM downloaded_tracks WHERE video_id = :videoId")
    suspend fun delete(videoId: String)

    @Query("SELECT COUNT(*) FROM downloaded_tracks")
    fun getDownloadedCount(): Flow<Int>
}

package com.asla.denge.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.asla.denge.data.local.dao.EqPresetDao
import com.asla.denge.data.local.dao.HistoryDao
import com.asla.denge.data.local.dao.PlaylistDao
import com.asla.denge.data.local.dao.PlaylistTrackDao
import com.asla.denge.data.local.dao.QueueDao
import com.asla.denge.data.local.dao.SearchHistoryDao
import com.asla.denge.data.local.dao.TrackDao
import com.asla.denge.data.local.entity.CachedPlaylistEntity
import com.asla.denge.data.local.entity.CachedTrackEntity
import com.asla.denge.data.local.entity.EqPresetEntity
import com.asla.denge.data.local.entity.PlaybackHistoryEntity
import com.asla.denge.data.local.entity.PlaylistTrackEntity
import com.asla.denge.data.local.entity.QueueItemEntity
import com.asla.denge.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room database for AdsFreeMusic.
 * Version 1 — initial schema matching Schema.md.
 */
@Database(
    entities = [
        CachedTrackEntity::class,
        CachedPlaylistEntity::class,
        PlaylistTrackEntity::class,
        PlaybackHistoryEntity::class,
        QueueItemEntity::class,
        SearchHistoryEntity::class,
        EqPresetEntity::class,
        com.asla.denge.data.local.entity.DownloadedTrackEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AdsFreeDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun downloadDao(): com.asla.denge.data.local.dao.DownloadDao

    companion object {
        private const val DB_NAME = "adsfreemusic.db"

        fun create(context: Context): AdsFreeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AdsFreeDatabase::class.java,
                DB_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(SeedCallback())
                .build()
        }
    }
}

/**
 * Seeds built-in EQ presets on first database creation.
 */
private class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        try {
            val now = System.currentTimeMillis()
            db.execSQL(
                """INSERT OR IGNORE INTO cached_playlists (playlist_id, title, description, track_count, is_liked_music, is_editable, cached_at, synced_at)
                   VALUES ('liked_songs', 'Liked Songs', 'Your favorite tracks', 0, 1, 0, $now, $now)"""
            )
            val presets = listOf(
                Triple("Flat", "[0,0,0,0,0]", now),
                Triple("Bass Boost", "[6,4,1,0,0]", now),
                Triple("Treble Boost", "[0,0,1,4,6]", now),
                Triple("Rock", "[4,2,-1,2,4]", now),
                Triple("Pop", "[-1,2,4,2,-1]", now),
                Triple("Jazz", "[3,1,0,1,3]", now),
            )
            presets.forEachIndexed { index, (name, bands, ts) ->
                val isActive = if (index == 0) 1 else 0
                db.execSQL(
                    """INSERT INTO eq_presets (name, bands_json, is_active, is_builtin, created_at) 
                       VALUES ('$name', '$bands', $isActive, 1, $ts)"""
                )
            }
        } catch (_: Throwable) {
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        try {
            val now = System.currentTimeMillis()
            db.execSQL(
                """INSERT OR IGNORE INTO cached_playlists (playlist_id, title, description, track_count, is_liked_music, is_editable, cached_at, synced_at)
                   VALUES ('liked_songs', 'Liked Songs', 'Your favorite tracks', 0, 1, 0, $now, $now)"""
            )
        } catch (_: Throwable) {
        }
    }
}

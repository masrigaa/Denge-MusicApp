package com.asla.denge.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages ExoPlayer audio disk cache with a strict 150 MB LRU rolling limit.
 * Ensures downloaded audio chunks are kept locally so the cellular/Wi-Fi modem
 * can go to sleep, significantly reducing heat and battery usage.
 */
@OptIn(UnstableApi::class)
class PlaybackCacheManager(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "denge_audio_cache")
    private val maxCacheSizeBytes = 150L * 1024L * 1024L // 150 MB hard limit

    @Volatile
    private var simpleCache: SimpleCache? = null

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            initCache()
        }
    }

    @Synchronized
    private fun initCache() {
        if (simpleCache == null) {
            try {
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)
                val databaseProvider = StandaloneDatabaseProvider(context)
                simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
            } catch (e: Exception) {
                android.util.Log.e("PlaybackCacheManager", "Failed to initialize SimpleCache", e)
            }
        }
    }

    fun getCache(): SimpleCache? {
        if (simpleCache == null) {
            initCache()
        }
        return simpleCache
    }

    fun createCacheDataSourceFactory(upstreamFactory: DataSource.Factory): DataSource.Factory {
        val cache = getCache() ?: return upstreamFactory
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun getUsedCacheSizeBytes(): Long {
        return try {
            simpleCache?.cacheSpace ?: getFolderSize(cacheDir)
        } catch (_: Exception) {
            getFolderSize(cacheDir)
        }
    }

    fun clearCache() {
        try {
            val cache = simpleCache
            if (cache != null) {
                val keys = cache.keys.toList()
                for (key in keys) {
                    try {
                        cache.removeResource(key)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackCacheManager", "Error clearing cache keys", e)
        }
    }

    private fun getFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    fun release() {
        try {
            simpleCache?.release()
            simpleCache = null
        } catch (_: Exception) {}
    }
}

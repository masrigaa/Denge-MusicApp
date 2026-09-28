package com.asla.denge.data.repository

import com.asla.denge.data.local.dao.HistoryDao
import com.asla.denge.data.local.dao.PlaylistDao
import com.asla.denge.data.local.dao.PlaylistTrackDao
import com.asla.denge.data.local.dao.TrackDao
import com.asla.denge.data.local.entity.CachedPlaylistEntity
import com.asla.denge.data.local.entity.PlaybackHistoryEntity
import com.asla.denge.data.local.entity.PlaylistTrackEntity
import com.asla.denge.data.mapper.toDomain
import com.asla.denge.data.mapper.toEntity
import com.asla.denge.data.remote.innertube.InnertubeClient
import com.asla.denge.domain.model.Album
import com.asla.denge.domain.model.Artist
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.SearchResults
import com.asla.denge.domain.model.Track
import com.asla.denge.domain.repository.AuthRepository
import com.asla.denge.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class MusicRepositoryImpl(
    private val innertubeClient: InnertubeClient,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val playlistTrackDao: PlaylistTrackDao,
    private val historyDao: HistoryDao,
    private val authRepository: AuthRepository,
) : MusicRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun search(query: String): SearchResults {
        val results = innertubeClient.search(query)
        val cleanQuery = query.trim().lowercase()
        val wantsInstrumental = listOf("instrument", "inst", "karaoke", "off vocal").any { cleanQuery.contains(it) }

        // Clean query terms for relevance ranking
        val terms = cleanQuery.split(Regex("[\\s,._\\-]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }

        val validSongs = results.songs
            .filter { track ->
                // Must be a valid music track
                if (!isMusicTrack(track)) {
                    // If user explicitly searched for instrumental and track is instrumental, allow it
                    if (!(wantsInstrumental && isInstrumental(track.title))) {
                        return@filter false
                    }
                }

                // If user didn't ask for instrumental, strictly reject instrumental
                if (!wantsInstrumental && isInstrumental(track.title)) {
                    return@filter false
                }

                true
            }
            .sortedByDescending { track ->
                // Relevance scoring: songs matching more query terms appear first
                val lowerTitle = track.title.lowercase()
                val lowerArtist = track.artistName.lowercase()
                var score = 0
                for (term in terms) {
                    if (lowerTitle.contains(term)) score += 3
                    if (lowerArtist.contains(term)) score += 2
                }
                score
            }
            .take(50)

        if (validSongs.isNotEmpty()) {
            trackDao.upsertAll(validSongs.map { it.toEntity() })
        }
        return results.copy(songs = validSongs)
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getPlaylistTracks(playlistId: String): Flow<List<Track>> {
        return playlistTrackDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getLikedSongs(): Flow<List<Track>> {
        return playlistTrackDao.getTracksForPlaylist(LIKED_SONGS_PLAYLIST_ID).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun isTrackLiked(videoId: String): Flow<Boolean> {
        return playlistTrackDao.isTrackInPlaylist(LIKED_SONGS_PLAYLIST_ID, videoId)
    }

    override suspend fun isTrackLikedSync(videoId: String): Boolean {
        return playlistTrackDao.hasTrack(LIKED_SONGS_PLAYLIST_ID, videoId) > 0
    }

    override suspend fun toggleLike(track: Track): Boolean {
        ensureLikedPlaylistExists()
        trackDao.upsert(track.toEntity())

        val isAlreadyLiked = playlistTrackDao.hasTrack(LIKED_SONGS_PLAYLIST_ID, track.videoId) > 0
        val newState = if (isAlreadyLiked) {
            playlistTrackDao.removeTrack(LIKED_SONGS_PLAYLIST_ID, track.videoId)
            false
        } else {
            playlistTrackDao.insert(
                PlaylistTrackEntity(
                    playlistId = LIKED_SONGS_PLAYLIST_ID,
                    videoId = track.videoId,
                    position = (System.currentTimeMillis() % 1000000).toInt(),
                    addedAt = System.currentTimeMillis(),
                )
            )
            true
        }

        val totalCount = playlistTrackDao.getTrackCount(LIKED_SONGS_PLAYLIST_ID)
        playlistDao.updateTrackCount(LIKED_SONGS_PLAYLIST_ID, totalCount)

        return newState
    }

    override suspend fun syncLikedSongsFromYouTubeMusic(): Boolean {
        return try {
            val cookie = authRepository.getAuthCookie()
            if (cookie.isNullOrBlank()) return false

            val tracks = innertubeClient.fetchLikedMusicTracks(cookie)
            if (tracks.isNotEmpty()) {
                ensureLikedPlaylistExists()
                trackDao.upsertAll(tracks.map { it.toEntity() })
                var pos = 0
                tracks.forEach { track ->
                    if (!playlistTrackDao.isTrackInPlaylist(LIKED_SONGS_PLAYLIST_ID, track.videoId).first()) {
                        playlistTrackDao.insert(
                            PlaylistTrackEntity(
                                playlistId = LIKED_SONGS_PLAYLIST_ID,
                                videoId = track.videoId,
                                position = pos++,
                                addedAt = System.currentTimeMillis(),
                            )
                        )
                    }
                }
                val totalCount = playlistTrackDao.getTrackCount(LIKED_SONGS_PLAYLIST_ID)
                playlistDao.updateTrackCount(LIKED_SONGS_PLAYLIST_ID, totalCount)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun ensureLikedPlaylistExists() {
        val existing = playlistDao.getById(LIKED_SONGS_PLAYLIST_ID)
        if (existing == null) {
            val now = System.currentTimeMillis()
            playlistDao.upsert(
                CachedPlaylistEntity(
                    playlistId = LIKED_SONGS_PLAYLIST_ID,
                    title = "Liked Songs",
                    description = "Lagu-lagu yang Anda sukai",
                    thumbnailUrl = null,
                    trackCount = 0,
                    isLikedMusic = 1,
                    isEditable = 0,
                    cachedAt = now,
                    syncedAt = now,
                )
            )
        }
    }

    override suspend fun createPlaylist(title: String, description: String?): String {
        val id = "pl_" + UUID.randomUUID().toString().replace("-", "").take(10)
        val now = System.currentTimeMillis()
        playlistDao.upsert(
            CachedPlaylistEntity(
                playlistId = id,
                title = title.ifBlank { "Playlist Baru" },
                description = description,
                thumbnailUrl = null,
                trackCount = 0,
                isLikedMusic = 0,
                isEditable = 1,
                cachedAt = now,
                syncedAt = now,
            )
        )
        return id
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        trackDao.upsert(track.toEntity())
        playlistTrackDao.insert(
            PlaylistTrackEntity(
                playlistId = playlistId,
                videoId = track.videoId,
                position = (System.currentTimeMillis() % 1000000).toInt(),
                addedAt = System.currentTimeMillis(),
            )
        )
        val count = playlistTrackDao.getTrackCount(playlistId)
        playlistDao.updateTrackCount(playlistId, count)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        if (playlistId == LIKED_SONGS_PLAYLIST_ID) return
        playlistTrackDao.clearPlaylist(playlistId)
        playlistDao.delete(playlistId)
    }

    override suspend fun getTrack(videoId: String): Track? {
        val cached = trackDao.getById(videoId)
        return cached?.toDomain()
    }

    override suspend fun getStreamUrl(videoId: String): String {
        val cookie = authRepository.getAuthCookie()
        return innertubeClient.getStreamUrl(videoId, cookie)
    }

    override suspend fun getArtist(artistId: String): Pair<Artist, List<Track>> {
        return Artist(artistId = artistId, name = "Artist") to emptyList()
    }

    override suspend fun getAlbum(albumId: String): Pair<Album, List<Track>> {
        return Album(albumId = albumId, title = "Album", artistName = "Artist") to emptyList()
    }

    override suspend fun getHomeFeed(): List<Track> {
        val cookie = authRepository.getAuthCookie()
        val tracks = innertubeClient.getHomeFeed(cookie).filter { isMusicTrack(it) && !isInstrumental(it.title) }
        if (tracks.isNotEmpty()) {
            trackDao.upsertAll(tracks.map { it.toEntity() })
        }
        return tracks
    }

    override suspend fun getGenreTracks(genre: String): List<Track> {
        return try {
            val results = innertubeClient.search(genre).songs.filter { isMusicTrack(it) && !isInstrumental(it.title) }
            if (results.isNotEmpty()) {
                trackDao.upsertAll(results.map { it.toEntity() })
            }
            results.take(5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isInstrumental(title: String): Boolean {
        val lower = title.lowercase()
        val patterns = listOf(
            "instrumental", "instrument", "inst.", "(inst", "[inst", "- inst",
            "off vocal", "off-vocal", "backing track", "karaoke", "minus one", "minus-one", "bgm"
        )
        return patterns.any { lower.contains(it) }
    }

    private fun isMusicTrack(track: Track): Boolean {
        val lowerTitle = track.title.lowercase()

        // Reject instrumental tracks
        if (isInstrumental(track.title)) {
            return false
        }

        val nonMusicTitleKeywords = listOf(
            // Broadcasts & Talk
            "podcast", "talkshow", "wawancara", "interview",
            "berita", "audiobook", "stand up comedy",
            // Gaming & Stream Archive
            "gameplay", "walkthrough", "playthrough",
            "genshin", "minecraft", "valorant", "apex legends", "roblox", "gta", "mobile legends",
            // Social Media & Videos
            "reaction", "unboxing", "vlog", "tutorial", "how to",
            "trailer", "teaser", "behind the scenes", "bloopers",
            // Compilations & Long Loops
            "full album", "compilation", "1 hour", "10 hours",
            // VTuber talk archives (not songs)
            "zatsudan", "free talk"
        )
        for (kw in nonMusicTitleKeywords) {
            if (lowerTitle.contains(kw)) {
                return false
            }
        }
        if (track.durationMs > 600_000L) { // Longer than 10 minutes is likely not a standard single song
            return false
        }
        return true
    }

    override suspend fun getRelatedTracks(videoId: String, artistName: String, title: String): List<Track> {
        return try {
            // 1. First attempt: Official YouTube Music Related / Up Next tracks via /next -> /browse
            var radioCandidates = try {
                innertubeClient.getRelatedTracks(videoId).filter { it.videoId != videoId && isMusicTrack(it) }
            } catch (_: Exception) {
                emptyList()
            }

            // 2. Fallback if official related endpoint returned empty: intelligent radio search
            if (radioCandidates.isEmpty()) {
                val query = if (title.isNotBlank() && artistName.isNotBlank() && artistName != "Artis") {
                    "$title $artistName radio mix"
                } else if (artistName.isNotBlank() && artistName != "Artis") {
                    "$artistName radio mix song"
                } else if (title.isNotBlank()) {
                    "$title song radio"
                } else {
                    "top hits music radio mix"
                }
                val songs = innertubeClient.searchSongsOnly(query)
                radioCandidates = songs.filter { it.videoId != videoId && isMusicTrack(it) }
            }

            // 3. Fetch user's Liked Songs for local taste profiling
            val likedEntities = try {
                playlistTrackDao.getTracksForPlaylist(LIKED_SONGS_PLAYLIST_ID).first()
            } catch (_: Exception) {
                emptyList()
            }
            val likedTracks = likedEntities.map { it.toDomain() }
            val likedArtists = likedTracks.map { it.artistName.trim().lowercase() }
                .filter { it.isNotBlank() && it != "artis" && it != "artist" }
                .toSet()

            // 4. Smart Blending: Separate candidates matching user taste vs fresh discoveries
            val normalizedCurrentArtist = artistName.trim().lowercase()
            val likedMatchingCandidates = mutableListOf<Track>()
            val discoveryCandidates = mutableListOf<Track>()

            for (track in radioCandidates) {
                val normArtist = track.artistName.trim().lowercase()
                if (normArtist in likedArtists && normArtist != normalizedCurrentArtist) {
                    likedMatchingCandidates.add(track)
                } else {
                    discoveryCandidates.add(track)
                }
            }

            // 5. Interleaving: Combine fresh discovery + taste-matched tracks
            val blendedResult = mutableListOf<Track>()
            var discoveryIndex = 0
            var likedMatchIndex = 0

            while (blendedResult.size < 15 && (discoveryIndex < discoveryCandidates.size || likedMatchIndex < likedMatchingCandidates.size)) {
                if (discoveryIndex < discoveryCandidates.size) {
                    blendedResult.add(discoveryCandidates[discoveryIndex++])
                }
                if (discoveryIndex < discoveryCandidates.size) {
                    blendedResult.add(discoveryCandidates[discoveryIndex++])
                }
                if (likedMatchIndex < likedMatchingCandidates.size) {
                    blendedResult.add(likedMatchingCandidates[likedMatchIndex++])
                }
            }

            // Optional: Blend 1 liked track if not already in queue to add familiar taste delight
            val eligibleLikedSong = likedTracks
                .filter { it.videoId != videoId && it.artistName.trim().lowercase() != normalizedCurrentArtist }
                .shuffled()
                .firstOrNull()

            if (eligibleLikedSong != null && blendedResult.none { it.videoId == eligibleLikedSong.videoId }) {
                val insertPos = 2.coerceAtMost(blendedResult.size)
                blendedResult.add(insertPos, eligibleLikedSong)
            }

            val finalResult = blendedResult.distinctBy { it.videoId }.take(15)

            if (finalResult.isNotEmpty()) {
                trackDao.upsertAll(finalResult.map { it.toEntity() })
            }
            finalResult
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getPlaybackHistory(): Flow<List<Track>> {
        return historyDao.getRecentHistory(50).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMostPlayedTracks(limit: Int): Flow<List<Track>> {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfMonth = calendar.timeInMillis
        return historyDao.getMostPlayedThisMonth(startOfMonth, limit = 2).map { list ->
            list.map { item ->
                item.track.toDomain().copy(playCount = item.playCount)
            }
        }
    }

    override suspend fun getRecommendationsBasedOnLiked(): Pair<Track, List<Track>>? {
        return try {
            val randomLikedEntity = playlistTrackDao.getRandomTracksFromPlaylist(LIKED_SONGS_PLAYLIST_ID, 1).firstOrNull() ?: return null
            val likedTrack = randomLikedEntity.toDomain()
            val related = getRelatedTracks(likedTrack.videoId, likedTrack.artistName, likedTrack.title).take(6)
            if (related.isEmpty()) null else Pair(likedTrack, related)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun recordPlay(track: Track, durationPlayedMs: Long, source: String) {
        val now = System.currentTimeMillis()
        try {
            // Save full track metadata with current timestamp
            trackDao.upsert(track.toEntity().copy(lastPlayedAt = now))
            historyDao.trimOldHistory(1000)
            historyDao.insert(
                PlaybackHistoryEntity(
                    videoId = track.videoId,
                    playedAt = now,
                    durationPlayedMs = durationPlayedMs,
                    source = source,
                )
            )

            // Report playback to YouTube Music watch history if authenticated
            scope.launch {
                try {
                    val cookie = authRepository.getAuthCookie()
                    if (!cookie.isNullOrBlank()) {
                        innertubeClient.reportPlayback(track.videoId, cookie)
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override suspend fun recordPlay(videoId: String, durationPlayedMs: Long, source: String) {
        val now = System.currentTimeMillis()
        try {
            val cached = trackDao.getById(videoId)
            if (cached == null) {
                trackDao.upsert(
                    com.asla.denge.data.local.entity.CachedTrackEntity(
                        videoId = videoId,
                        title = "Track",
                        artistName = "Artist",
                        durationMs = durationPlayedMs,
                        cachedAt = now,
                        lastPlayedAt = now,
                    )
                )
            } else {
                trackDao.updateLastPlayed(videoId, now)
            }
            historyDao.trimOldHistory(1000)
            historyDao.insert(
                PlaybackHistoryEntity(
                    videoId = videoId,
                    playedAt = now,
                    durationPlayedMs = durationPlayedMs,
                    source = source,
                )
            )
        } catch (_: Exception) {}
    }

    companion object {
        const val LIKED_SONGS_PLAYLIST_ID = "liked_songs"
    }
}

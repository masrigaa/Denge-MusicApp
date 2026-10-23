package com.asla.denge.domain.repository

import com.asla.denge.domain.model.Album
import com.asla.denge.domain.model.Artist
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.SearchResults
import com.asla.denge.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for music data operations.
 * Implemented in data layer — coordinates remote (Innertube) + local (Room) sources.
 */
interface MusicRepository {

    /** Search YouTube Music for tracks, artists, albums, playlists. */
    suspend fun search(query: String): SearchResults

    /** Get the user's playlists from their YouTube Music library. */
    fun getPlaylists(): Flow<List<Playlist>>

    /** Get tracks in a specific playlist. */
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>>

    /** Get the user's liked songs. */
    fun getLikedSongs(): Flow<List<Track>>

    /** Check if track is in liked songs. */
    fun isTrackLiked(videoId: String): Flow<Boolean>

    /** Direct one-shot check if track is in liked songs. */
    suspend fun isTrackLikedSync(videoId: String): Boolean

    /** Toggle liked status of a track. Returns new liked state (true if liked, false if unliked). */
    suspend fun toggleLike(track: Track): Boolean

    /** Sync liked songs from YouTube Music account if authenticated. Returns true if successful. */
    suspend fun syncLikedSongsFromYouTubeMusic(): Boolean

    /** Create a new local playlist. Returns created playlist ID. */
    suspend fun createPlaylist(title: String, description: String? = null): String

    /** Add track to an existing playlist. */
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)

    /** Delete a custom playlist. */
    suspend fun deletePlaylist(playlistId: String)

    /** Get track details by video ID. */
    suspend fun getTrack(videoId: String): Track?

    /** Resolve a streamable audio URL for a track (ad-free). */
    suspend fun getStreamUrl(videoId: String, quality: String = "high"): String

    /** Get artist details and top songs. */
    suspend fun getArtist(artistId: String): Pair<Artist, List<Track>>

    /** Get album details and tracks. */
    suspend fun getAlbum(albumId: String): Pair<Album, List<Track>>

    /** Get home/recommendations feed. */
    suspend fun getHomeFeed(): List<Track>

    /** Get tracks for a specific genre or search tag. */
    suspend fun getGenreTracks(genre: String): List<Track>

    /** Get related tracks for YouTube Music radio / Up Next queue. */
    suspend fun getRelatedTracks(videoId: String, artistName: String, title: String): List<Track>

    /** Get recent playback history. */
    fun getPlaybackHistory(): Flow<List<Track>>

    /** Get most played tracks from history. */
    fun getMostPlayedTracks(limit: Int = 10): Flow<List<Track>>

    /** Get recommendations based on a random liked song. Returns Pair(likedSong, recommendations) or null. */
    suspend fun getRecommendationsBasedOnLiked(): Pair<Track, List<Track>>?

    /** Record a track play in history with full metadata. */
    suspend fun recordPlay(track: Track, durationPlayedMs: Long = 0L, source: String = "player")

    /** Record a track play by videoId (legacy fallback). */
    suspend fun recordPlay(videoId: String, durationPlayedMs: Long, source: String)
}

/**
 * Repository interface for local user profile and session management.
 */
interface AuthRepository {

    /** Check if user has explicitly set a custom display name. */
    fun hasCustomUserName(): Boolean

    /** Get user's local display name. */
    suspend fun getUserName(): String?

    /** Set user's custom display name. */
    suspend fun setUserName(name: String)

    /** Get optional streaming session cookie. */
    suspend fun getAuthCookie(): String?

    /** Reset or clear local profile state. */
    suspend fun signOut()
}


package com.asla.denge.domain.model

/**
 * Core domain model representing a music track.
 * No Android framework dependencies — pure Kotlin.
 */
data class Track(
    val videoId: String,
    val title: String,
    val artistName: String,
    val artistId: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val durationMs: Long,
    val thumbnailUrl: String? = null,
    val isExplicit: Boolean = false,
    val playCount: Int = 0,
)

/**
 * Core domain model representing a playlist.
 */
data class Playlist(
    val playlistId: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val trackCount: Int = 0,
    val isLikedMusic: Boolean = false,
    val isEditable: Boolean = true,
)

/**
 * Core domain model representing an artist.
 */
data class Artist(
    val artistId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscriberCount: String? = null,
)

/**
 * Core domain model representing an album.
 */
data class Album(
    val albumId: String,
    val title: String,
    val artistName: String,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val trackCount: Int = 0,
)

/**
 * Search results grouped by category.
 */
data class SearchResults(
    val songs: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

/**
 * YouTube Music & Google user account model.
 */
@kotlinx.serialization.Serializable
data class UserAccount(
    val email: String,
    val displayName: String,
    val channelName: String,
    val handle: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val cookie: String? = null,
)

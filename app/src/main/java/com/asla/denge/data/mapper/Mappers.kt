package com.asla.denge.data.mapper

import com.asla.denge.data.local.entity.CachedPlaylistEntity
import com.asla.denge.data.local.entity.CachedTrackEntity
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.Track

fun CachedTrackEntity.toDomain(): Track {
    return Track(
        videoId = videoId,
        title = title,
        artistName = artistName,
        artistId = artistId,
        albumName = albumName,
        albumId = albumId,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        isExplicit = explicit != 0,
    )
}

fun Track.toEntity(cachedAt: Long = System.currentTimeMillis()): CachedTrackEntity {
    return CachedTrackEntity(
        videoId = videoId,
        title = title,
        artistName = artistName,
        artistId = artistId,
        albumName = albumName,
        albumId = albumId,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        explicit = if (isExplicit) 1 else 0,
        cachedAt = cachedAt,
    )
}

fun CachedPlaylistEntity.toDomain(): Playlist {
    return Playlist(
        playlistId = playlistId,
        title = title,
        description = description,
        thumbnailUrl = thumbnailUrl,
        trackCount = trackCount,
        isLikedMusic = isLikedMusic != 0,
        isEditable = isEditable != 0,
    )
}

fun Playlist.toEntity(cachedAt: Long = System.currentTimeMillis()): CachedPlaylistEntity {
    return CachedPlaylistEntity(
        playlistId = playlistId,
        title = title,
        description = description,
        thumbnailUrl = thumbnailUrl,
        trackCount = trackCount,
        isLikedMusic = if (isLikedMusic) 1 else 0,
        isEditable = if (isEditable) 1 else 0,
        cachedAt = cachedAt,
    )
}

fun com.asla.denge.data.local.entity.DownloadedTrackEntity.toDomain(): Track {
    return Track(
        videoId = videoId,
        title = title,
        artistName = artistName,
        durationMs = durationMs,
        thumbnailUrl = localThumbnailPath ?: thumbnailUrl,
    )
}



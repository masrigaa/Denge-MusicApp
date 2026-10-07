package com.asla.denge.util

/**
 * Utility to convert YouTube & YouTube Music thumbnails to High Definition (HD).
 * Normalizes URLs and provides robust fallbacks for videos, live streams, and music tracks.
 * Eliminates invalid square-query signatures and 404s.
 */
fun toHdThumbnailUrl(url: String?, videoId: String? = null): String {
    val cleanVideoId = videoId?.trim()?.ifBlank { null } ?: extractVideoIdFromUrl(url)

    val rawUrl = url?.trim() ?: ""

    // If it's a YouTube Music album/artist Google CDN URL (high quality album covers)
    if (rawUrl.contains("googleusercontent.com") || rawUrl.contains("ggpht.com")) {
        var hdUrl = rawUrl
        if (hdUrl.startsWith("//")) {
            hdUrl = "https:$hdUrl"
        }
        return hdUrl
            .replace(Regex("=w\\d+-h\\d+[^&?]*"), "=w800-h800-l90-rj")
            .replace(Regex("=s\\d+[^&?]*"), "=s800")
    }

    // For any YouTube video or music video where videoId is available:
    // https://i.ytimg.com/vi/{id}/hqdefault.jpg is 100% static, signature-free, and universally reliable!
    if (!cleanVideoId.isNullOrBlank()) {
        return "https://i.ytimg.com/vi/$cleanVideoId/hqdefault.jpg"
    }

    if (rawUrl.isBlank()) return ""

    var normalized = rawUrl
    if (normalized.startsWith("//")) {
        normalized = "https:$normalized"
    }

    // If ytimg or youtube video URL without known videoId, strip query params to prevent signature mismatch
    if (normalized.contains("ytimg.com") || normalized.contains("youtube.com")) {
        val base = normalized.substringBefore("?")
        return base
            .replace("/default.jpg", "/hqdefault.jpg")
            .replace("/mqdefault.jpg", "/hqdefault.jpg")
            .replace("/sddefault.jpg", "/hqdefault.jpg")
    }

    return normalized
}

/**
 * Extracts 11-character YouTube video ID from various YouTube thumbnail URL patterns.
 */
fun extractVideoIdFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val regex = Regex("""/(?:vi|vi_webp)/([a-zA-Z0-9_-]{11})[/.]""")
    val match = regex.find(url)
    return match?.groupValues?.getOrNull(1)
}

/**
 * Reliable fallback thumbnail for any video ID.
 */
fun getFallbackThumbnailUrl(videoId: String?): String {
    if (videoId.isNullOrBlank()) return ""
    return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}

/**
 * Medium quality fallback for smaller cards or fallback loading.
 */
fun getMediumFallbackThumbnailUrl(videoId: String?): String {
    if (videoId.isNullOrBlank()) return ""
    return "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
}

/**
 * Secondary snapshot fallback for video frames.
 */
fun getVideoFrameFallbackUrl(videoId: String?): String {
    if (videoId.isNullOrBlank()) return ""
    return "https://img.youtube.com/vi/$videoId/0.jpg"
}


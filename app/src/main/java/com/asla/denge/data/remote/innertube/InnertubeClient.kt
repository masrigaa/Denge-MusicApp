package com.asla.denge.data.remote.innertube

import com.asla.denge.domain.model.Album
import com.asla.denge.domain.model.Artist
import com.asla.denge.domain.model.Playlist
import com.asla.denge.domain.model.SearchResults
import com.asla.denge.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * YouTube Music & YouTube Innertube API Client.
 * Provides resilient search, browse, recommendations, and stream resolution.
 */
class InnertubeClient(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    companion object {
        private const val YTM_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val YT_BASE_URL = "https://www.youtube.com/youtubei/v1"

        private const val WEB_REMIX_CLIENT_NAME = "WEB_REMIX"
        private const val WEB_REMIX_CLIENT_VERSION = "1.20241118.01.00"

        private const val VISIONOS_CLIENT_NAME = "VISIONOS"
        private const val VISIONOS_CLIENT_VERSION = "1.02"
        private const val VISIONOS_CLIENT_ID = "101"

        private const val DEFAULT_VISITOR_ID =
            "CgtxR3d3TlpKYUVWMCjn69XVBjIKCgJJRBIEGgAgVWLfAgrcAjIyLllUPXA0Ykg1SDV0MTEzc1ktYXRxeVVsX0lxTmhWYnZIR1JzYlV1TEo4cjdvYWVDYUphZVVMbHFjRTJCcFZaWkpPTWJXVnhpQkE1eTZWRkd2LWJyUXc5VXJ6dGV3Q3JJWEFtM0tfWkVnRmVHT0pNU2RpMXg3ejhCRGt2SWVXekRSU18yODhBOTFRYmNUS2JCOUZkMHJpRjdoRWx2UTU5c3BjVWZCZVFpTTVITEVaN1JqTGFIMUs0cWZUSUlzUHBpTHNlWEVmbWhwb193aFYyLTdkRVlaUkgwUmh4enZFNzlnUDVXN2htUFEyVHBMc3BCQ0dRcmlwOHZUVUtxaHFpVk5Oc0k2cS1fa1dtUkc1NENrUE9OenpLLTNOOHczV3doWFRHSU0tT2NzX2tHOEhjektveWFRRkRoT2hUdTNobkVpc0dtaFhlUG1FWU5xdWRCeUlaYzNtOVRIQQ%3D%3D"
    }

    private fun buildWebRemixContext(): JsonObject = buildJsonObject {
        putJsonObject("client") {
            put("clientName", WEB_REMIX_CLIENT_NAME)
            put("clientVersion", WEB_REMIX_CLIENT_VERSION)
            put("hl", "id")
            put("gl", "ID")
        }
    }

    private fun buildVisionOsContext(): JsonObject = buildJsonObject {
        putJsonObject("client") {
            put("clientName", VISIONOS_CLIENT_NAME)
            put("clientVersion", VISIONOS_CLIENT_VERSION)
            put("deviceMake", "Apple")
            put("deviceModel", "RealityDevice17,1")
            put("userAgent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15")
            put("osName", "visionOS")
            put("osVersion", "26.5.23O471")
            put("hl", "en")
            put("timeZone", "UTC")
            put("utcOffsetMinutes", 0)
        }
    }

    /**
     * Search songs, artists, albums, and playlists.
     * Combines official YouTube Music Songs shelf + YTM search + YouTube Web audio search
     * to guarantee rich, comprehensive results (30-50+ tracks) for any artist or song.
     */
    suspend fun search(query: String): SearchResults {
        val songsOnly = searchSongsOnly(query)
        val ytmResults = searchYouTubeMusic(query)
        val webResults = searchYouTubeWeb(query)

        val allSongs = (songsOnly + ytmResults.songs + webResults.songs)
            .distinctBy { it.videoId }

        return ytmResults.copy(
            songs = allSongs.take(50),
            artists = ytmResults.artists.ifEmpty { webResults.artists },
            albums = ytmResults.albums.ifEmpty { webResults.albums },
            playlists = ytmResults.playlists.ifEmpty { webResults.playlists }
        )
    }

    /**
     * Search ONLY official songs from YouTube Music (excludes videos, clips, and web uploads).
     * Uses the official Songs filter param: EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D
     */
    suspend fun searchSongsOnly(query: String): List<Track> {
        val requestBody = buildJsonObject {
            put("context", buildWebRemixContext())
            put("query", query)
            put("params", "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
        }

        val responseText = try {
            httpClient.post("$YTM_BASE_URL/search") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                setBody(requestBody.toString())
            }.bodyAsText()
        } catch (_: Exception) {
            return emptyList()
        }

        val results = parseSearchResults(responseText)
        if (results.songs.isNotEmpty()) {
            return results.songs
        }

        // Secondary fallback to regular YTM search (still pure music, not YT Web)
        return searchYouTubeMusic(query).songs
    }

    private suspend fun searchYouTubeMusic(query: String): SearchResults {
        val requestBody = buildJsonObject {
            put("context", buildWebRemixContext())
            put("query", query)
        }

        val responseText = try {
            httpClient.post("$YTM_BASE_URL/search") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                setBody(requestBody.toString())
            }.bodyAsText()
        } catch (_: Exception) {
            return SearchResults()
        }

        return parseSearchResults(responseText)
    }

    private suspend fun searchYouTubeWeb(query: String): SearchResults {
        val webBody = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20241118.01.00")
                    put("hl", "id")
                    put("gl", "ID")
                }
            }
            put("query", "$query audio")
        }

        val responseText = try {
            httpClient.post("$YT_BASE_URL/search") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://www.youtube.com/")
                header("Origin", "https://www.youtube.com")
                setBody(webBody.toString())
            }.bodyAsText()
        } catch (_: Exception) {
            return SearchResults()
        }

        val songs = mutableListOf<Track>()
        try {
            val root = json.parseToJsonElement(responseText).jsonObject
            val sectionList = root["contents"]?.jsonObject
                ?.get("twoColumnSearchResultsRenderer")?.jsonObject
                ?.get("primaryContents")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
                ?: root["contents"]?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject
                    ?.get("contents")?.jsonArray

            sectionList?.forEach { section ->
                val itemSection = section.jsonObject["itemSectionRenderer"]?.jsonObject
                val contents = itemSection?.get("contents")?.jsonArray
                contents?.forEach { item ->
                    val videoRenderer = item.jsonObject["videoRenderer"]?.jsonObject
                    if (videoRenderer != null) {
                        val videoId = videoRenderer["videoId"]?.jsonPrimitive?.contentOrNull
                        val title = videoRenderer["title"]?.jsonObject
                            ?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull

                        val artist = videoRenderer["ownerText"]?.jsonObject
                            ?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: videoRenderer["shortBylineText"]?.jsonObject
                                ?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject
                                ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: "Artis"

                        val thumbs = videoRenderer["thumbnail"]?.jsonObject
                            ?.get("thumbnails")?.jsonArray
                        val rawThumbUrl = thumbs?.lastOrNull()?.jsonObject
                            ?.get("url")?.jsonPrimitive?.contentOrNull
                        val thumbUrl = com.asla.denge.util.toHdThumbnailUrl(rawThumbUrl, videoId)

                        if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                            songs.add(
                                Track(
                                    videoId = videoId,
                                    title = title,
                                    artistName = artist,
                                    durationMs = 0L,
                                    thumbnailUrl = thumbUrl,
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return SearchResults(songs = songs)
    }

    /**
     * Resolve direct, unciphered stream URL for audio playback without ads.
     * Uses the VisionOS client context with visitor id which yields direct, ready-to-play audio URLs (m4a/Opus).
     */
    suspend fun getStreamUrl(videoId: String, cookie: String? = null, quality: String = "high"): String {
        val playerBody = buildJsonObject {
            put("videoId", videoId)
            put("context", buildVisionOsContext())
            putJsonObject("playbackContext") {
                putJsonObject("contentPlaybackContext") {
                    put("html5Preference", "HTML5_PREF_WANTS")
                    put("signatureTimestamp", 20717)
                }
            }
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        try {
            val responseText = httpClient.post("$YT_BASE_URL/player") {
                contentType(ContentType.Application.Json)
                header("X-YouTube-Client-Name", VISIONOS_CLIENT_ID)
                header("X-YouTube-Client-Version", VISIONOS_CLIENT_VERSION)
                header("Origin", "https://www.youtube.com")
                header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15")
                header("X-Goog-Visitor-Id", DEFAULT_VISITOR_ID)
                if (!cookie.isNullOrBlank()) {
                    header("Cookie", cookie)
                    generateSapisidHash(cookie, "https://www.youtube.com")?.let {
                        header("Authorization", it)
                    }
                }
                setBody(playerBody.toString())
            }.bodyAsText()

            val root = json.parseToJsonElement(responseText).jsonObject
            val streamingData = root["streamingData"]?.jsonObject
            val adaptiveFormats = streamingData?.get("adaptiveFormats")?.jsonArray

            if (adaptiveFormats != null) {
                val audioFormats = adaptiveFormats.mapNotNull { it.jsonObject }
                    .filter {
                        val mimeType = it["mimeType"]?.jsonPrimitive?.contentOrNull ?: ""
                        mimeType.startsWith("audio/")
                    }

                // Select stream format based on quality preference
                val directAudio = if (quality == "data_saver") {
                    // Prioritize itag 250 (Opus ~70kbps), itag 249 (Opus ~50kbps), or itag 139 (AAC 48kbps)
                    audioFormats.firstOrNull {
                        it["url"] != null && it["itag"]?.jsonPrimitive?.intOrNull == 250
                    } ?: audioFormats.firstOrNull {
                        it["url"] != null && it["itag"]?.jsonPrimitive?.intOrNull == 249
                    } ?: audioFormats.firstOrNull {
                        it["url"] != null && it["itag"]?.jsonPrimitive?.intOrNull == 139
                    } ?: audioFormats
                        .filter { it["url"] != null }
                        .minByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE }
                } else {
                    // Lock & prioritize itag 251 (Opus ~160kbps High Quality), then fallback to itag 140 (AAC 128kbps) or highest bitrate
                    audioFormats.firstOrNull {
                        it["url"] != null && it["itag"]?.jsonPrimitive?.intOrNull == 251
                    } ?: audioFormats.firstOrNull {
                        it["url"] != null && it["itag"]?.jsonPrimitive?.intOrNull == 140
                    } ?: audioFormats
                        .filter { it["url"] != null }
                        .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                }

                val directUrl = directAudio?.get("url")?.jsonPrimitive?.contentOrNull
                if (!directUrl.isNullOrBlank()) {
                    return directUrl
                }
            }
        } catch (_: Exception) {
        }

        // Secondary fallback: Web Remix client
        try {
            val webBody = buildJsonObject {
                put("context", buildWebRemixContext())
                put("videoId", videoId)
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", 20717)
                    }
                }
            }

            val webResponseText = httpClient.post("$YTM_BASE_URL/player") {
                contentType(ContentType.Application.Json)
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                if (!cookie.isNullOrBlank()) {
                    header("Cookie", cookie)
                    generateSapisidHash(cookie, "https://music.youtube.com")?.let {
                        header("Authorization", it)
                    }
                }
                setBody(webBody.toString())
            }.bodyAsText()

            val root = json.parseToJsonElement(webResponseText).jsonObject
            val adaptiveFormats = root["streamingData"]?.jsonObject?.get("adaptiveFormats")?.jsonArray
            val audioList = adaptiveFormats?.mapNotNull { it.jsonObject }
                ?.filter { it["url"] != null && (it["mimeType"]?.jsonPrimitive?.contentOrNull ?: "").startsWith("audio/") }

            val fallbackAudio = audioList?.firstOrNull { it["itag"]?.jsonPrimitive?.intOrNull == 251 }
                ?: audioList?.firstOrNull { it["itag"]?.jsonPrimitive?.intOrNull == 140 }
                ?: audioList?.maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }

            val fallbackUrl = fallbackAudio?.get("url")?.jsonPrimitive?.contentOrNull

            if (!fallbackUrl.isNullOrBlank()) {
                return fallbackUrl
            }
        } catch (_: Exception) {
        }

        throw IllegalStateException("Could not resolve stream URL for track $videoId")
    }

    /**
     * Report playback to YouTube Music to synchronize watch history in user's Google account.
     */
    suspend fun reportPlayback(videoId: String, cookie: String? = null): Boolean {
        if (cookie.isNullOrBlank()) return false
        val authHeader = generateSapisidHash(cookie) ?: return false

        return try {
            val webBody = buildJsonObject {
                put("context", buildWebRemixContext())
                put("videoId", videoId)
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", 20717)
                    }
                }
            }

            val response = httpClient.post("$YTM_BASE_URL/player") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(webBody.toString())
            }
            response.status.value in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get home recommendations.
     * If user is authenticated, fetches real personalized feed from YouTube Music (FEmusic_home).
     * Otherwise returns top Indonesian and global hits.
     */
    suspend fun getHomeFeed(cookie: String? = null): List<Track> {
        // 1. Try personalized YouTube Music home feed if user is logged in
        if (!cookie.isNullOrBlank()) {
            try {
                val authHeader = generateSapisidHash(cookie)
                val requestBody = buildJsonObject {
                    put("context", buildWebRemixContext())
                    put("browseId", "FEmusic_home")
                }

                val responseText = httpClient.post("$YTM_BASE_URL/browse") {
                    contentType(ContentType.Application.Json)
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    header("Referer", "https://music.youtube.com/")
                    header("Origin", "https://music.youtube.com")
                    header("Cookie", cookie)
                    if (authHeader != null) {
                        header("Authorization", authHeader)
                    }
                    setBody(requestBody.toString())
                }.bodyAsText()

                val root = json.parseToJsonElement(responseText)
                val renderers = mutableListOf<JsonObject>()
                extractAllResponsiveItems(root, renderers)

                if (renderers.isNotEmpty()) {
                    val personalizedSongs = mutableListOf<Track>()
                    for (renderer in renderers) {
                        extractSearchItem(renderer, personalizedSongs, mutableListOf(), mutableListOf(), mutableListOf())
                    }
                    val validSongs = personalizedSongs.distinctBy { it.videoId }
                    if (validSongs.isNotEmpty()) {
                        return validSongs
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fetch trending popular songs for an immediate rich home feed
        val trendingSongs = search("Top Popular Music Hits").songs
        if (trendingSongs.isNotEmpty()) {
            return trendingSongs
        }

        // 3. Fallback to global top hits
        val globalHits = search("Top Music Hits").songs
        return globalHits
    }

    /**
     * Fetch YouTube Music official related tracks (Radio / Up Next) for a given track.
     * Uses YouTube Music /next -> MUSIC_PAGE_TYPE_TRACK_RELATED -> /browse.
     * Works completely without any Google account or login!
     */
    suspend fun getRelatedTracks(videoId: String): List<Track> {
        return try {
            val nextBody = buildJsonObject {
                put("context", buildWebRemixContext())
                put("videoId", videoId)
            }

            val nextResponse = httpClient.post("$YTM_BASE_URL/next") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                setBody(nextBody.toString())
            }.bodyAsText()

            val nextRoot = json.parseToJsonElement(nextResponse).jsonObject
            val tabs = nextRoot["contents"]?.jsonObject
                ?.get("singleColumnMusicWatchNextResultsRenderer")?.jsonObject
                ?.get("tabbedRenderer")?.jsonObject
                ?.get("watchNextTabbedResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray

            var relatedBrowseId: String? = null
            if (tabs != null) {
                for (tab in tabs) {
                    val endpoint = tab.jsonObject["tabRenderer"]?.jsonObject
                        ?.get("endpoint")?.jsonObject
                        ?.get("browseEndpoint")?.jsonObject
                    val cfg = endpoint?.get("browseEndpointContextSupportedConfigs")?.jsonObject
                        ?.get("browseEndpointContextMusicConfig")?.jsonObject
                    if (cfg?.get("pageType")?.jsonPrimitive?.contentOrNull == "MUSIC_PAGE_TYPE_TRACK_RELATED") {
                        relatedBrowseId = endpoint["browseId"]?.jsonPrimitive?.contentOrNull
                        break
                    }
                }
            }

            if (relatedBrowseId.isNullOrBlank()) {
                return emptyList()
            }

            val browseBody = buildJsonObject {
                put("context", buildWebRemixContext())
                put("browseId", relatedBrowseId)
            }

            val browseResponse = httpClient.post("$YTM_BASE_URL/browse") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                setBody(browseBody.toString())
            }.bodyAsText()

            val browseRoot = json.parseToJsonElement(browseResponse)
            val renderers = mutableListOf<JsonObject>()
            extractAllResponsiveItems(browseRoot, renderers)

            val songs = mutableListOf<Track>()
            for (renderer in renderers) {
                extractSearchItem(renderer, songs, mutableListOf(), mutableListOf(), mutableListOf())
            }

            songs.distinctBy { it.videoId }.filter { it.videoId != videoId }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Parse search results into categorised SearchResults object.
     * Accurately parses musicShelfRenderer, musicCardShelfRenderer, and itemSectionRenderer.
     */
    private fun parseSearchResults(responseText: String): SearchResults {
        val songs = mutableListOf<Track>()
        val artists = mutableListOf<Artist>()
        val albums = mutableListOf<Album>()
        val playlists = mutableListOf<Playlist>()

        try {
            val root = json.parseToJsonElement(responseText).jsonObject
            val tabs = root["contents"]?.jsonObject
                ?.get("tabbedSearchResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray

            val sectionList = tabs?.getOrNull(0)?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
                ?: root["contents"]?.jsonObject
                    ?.get("sectionListRenderer")?.jsonObject
                    ?.get("contents")?.jsonArray

            sectionList?.forEach { sectionElement ->
                val sectionObj = sectionElement.jsonObject

                // 1. musicShelfRenderer or musicCardShelfRenderer
                val shelf = sectionObj["musicShelfRenderer"]?.jsonObject
                    ?: sectionObj["musicCardShelfRenderer"]?.jsonObject

                shelf?.get("contents")?.jsonArray?.forEach { item ->
                    item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject?.let {
                        extractSearchItem(it, songs, artists, albums, playlists)
                    }
                }

                // 2. itemSectionRenderer (Primary container in modern YouTube Music)
                val itemSection = sectionObj["itemSectionRenderer"]?.jsonObject
                itemSection?.get("contents")?.jsonArray?.forEach { item ->
                    item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject?.let {
                        extractSearchItem(it, songs, artists, albums, playlists)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return SearchResults(
            songs = songs,
            artists = artists,
            albums = albums,
            playlists = playlists,
        )
    }

    private fun extractSearchItem(
        renderer: JsonObject,
        songs: MutableList<Track>,
        artists: MutableList<Artist>,
        albums: MutableList<Album>,
        playlists: MutableList<Playlist>,
    ) {
        val pvr = renderer["playlistVideoRenderer"]?.jsonObject
            ?: if (renderer.containsKey("videoId") && !renderer.containsKey("flexColumns")) renderer else null

        if (pvr != null) {
            val pvrVideoId = pvr["videoId"]?.jsonPrimitive?.contentOrNull
            val pvrTitle = pvr["title"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?: pvr["title"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
            val pvrArtist = pvr["shortBylineText"]?.jsonObject?.get("runs")?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                ?: pvr["shortBylineText"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
                ?: "Artis"
            val pvrThumbs = pvr["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
            val pvrThumbUrl = pvrThumbs?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            if (!pvrVideoId.isNullOrBlank() && !pvrTitle.isNullOrBlank()) {
                songs.add(
                    Track(
                        videoId = pvrVideoId,
                        title = pvrTitle,
                        artistName = pvrArtist,
                        durationMs = 0L,
                        thumbnailUrl = com.asla.denge.util.toHdThumbnailUrl(pvrThumbUrl, pvrVideoId),
                    )
                )
                return
            }
        }

        val flexColumns = renderer["flexColumns"]?.jsonArray ?: return
        if (flexColumns.isEmpty()) return

        val titleRuns = flexColumns.getOrNull(0)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray

        val title = titleRuns?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return

        val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray

        // Intelligently extract real artist name:
        // 1. First check runs that have browseEndpoint pointing to an artist/channel
        // 2. Otherwise filter out type words ("Song", "Lagu", "Video", "Album", "Single"), durations, and separators
        val artistName = if (subtitleRuns != null) {
            var foundArtist: String? = null
            for (run in subtitleRuns) {
                val runObj = run.jsonObject
                val text = runObj["text"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
                val pageType = runObj["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseEndpointContextSupportedConfigs")?.jsonObject
                    ?.get("browseEndpointContextMusicConfig")?.jsonObject
                    ?.get("pageType")?.jsonPrimitive?.contentOrNull
                val browseId = runObj["navigationEndpoint"]?.jsonObject
                    ?.get("browseEndpoint")?.jsonObject
                    ?.get("browseId")?.jsonPrimitive?.contentOrNull

                if (pageType == "MUSIC_PAGE_TYPE_ARTIST" || pageType == "MUSIC_PAGE_TYPE_USER_CHANNEL" ||
                    (browseId != null && (browseId.startsWith("UC") || browseId.startsWith("FEmusic_library_privately_owned_artist")))) {
                    foundArtist = text
                    break
                }
            }

            if (foundArtist.isNullOrBlank()) {
                val filterWords = setOf("song", "lagu", "video", "singel", "single", "album", "ep", "•", "·", "|")
                val cleanRuns = subtitleRuns.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.trim() }
                    .filter { runText ->
                        val lower = runText.lowercase()
                        runText.isNotBlank() &&
                                !filterWords.contains(lower) &&
                                !runText.matches(Regex("""^\d+:\d+$""")) &&
                                !runText.matches(Regex("""^\d{4}$""")) &&
                                !lower.contains("ditonton") &&
                                !lower.contains("views") &&
                                !lower.contains("subscribers")
                    }
                foundArtist = cleanRuns.firstOrNull()
            }

            foundArtist ?: "Artis"
        } else {
            "Artis"
        }

        val thumbnails = renderer["thumbnail"]?.jsonObject
            ?.get("musicThumbnailRenderer")?.jsonObject
            ?.get("thumbnail")?.jsonObject
            ?.get("thumbnails")?.jsonArray
        val thumbnailUrl = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

        val playlistItemData = renderer["playlistItemData"]?.jsonObject
        val videoId = playlistItemData?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: renderer["overlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
                ?.get("playNavigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: renderer["navigationEndpoint"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: titleRuns?.getOrNull(0)?.jsonObject
                ?.get("navigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull

        if (!videoId.isNullOrBlank()) {
            val hdThumbnailUrl = com.asla.denge.util.toHdThumbnailUrl(thumbnailUrl, videoId)
            songs.add(
                Track(
                    videoId = videoId,
                    title = title,
                    artistName = artistName,
                    durationMs = 0L,
                    thumbnailUrl = hdThumbnailUrl,
                )
            )
        }
    }

    /**
     * Fetch user's real Liked Songs directly from YouTube Music.
     */
    suspend fun fetchLikedMusicTracks(cookie: String): List<Track> {
        val authHeader = generateSapisidHash(cookie)

        // Try VLLM (Liked Music playlist browseId), LM, and FEmusic_liked_videos
        val browseConfigs = listOf(
            Triple(YTM_BASE_URL, "VLLM", "https://music.youtube.com/"),
            Triple(YTM_BASE_URL, "FEmusic_liked_videos", "https://music.youtube.com/"),
            Triple(YTM_BASE_URL, "LM", "https://music.youtube.com/"),
            Triple(YT_BASE_URL, "VLLL", "https://www.youtube.com/"),
            Triple(YT_BASE_URL, "LL", "https://www.youtube.com/"),
        )
        for ((baseUrl, browseId, referer) in browseConfigs) {
            try {
                val origin = if (baseUrl == YTM_BASE_URL) "https://music.youtube.com" else "https://www.youtube.com"
                val sapisidHeader = generateSapisidHash(cookie, origin) ?: authHeader
                val requestBody = buildJsonObject {
                    put("context", if (baseUrl == YTM_BASE_URL) buildWebRemixContext() else buildVisionOsContext())
                    put("browseId", browseId)
                }

                val responseText = httpClient.post("$baseUrl/browse") {
                    contentType(ContentType.Application.Json)
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    header("Referer", referer)
                    header("Origin", origin)
                    header("X-Origin", origin)
                    header("X-Goog-AuthUser", "0")
                    header("Cookie", cookie)
                    if (sapisidHeader != null) {
                        header("Authorization", sapisidHeader)
                    }
                    setBody(requestBody.toString())
                }.bodyAsText()

                val root = json.parseToJsonElement(responseText)
                val renderers = mutableListOf<JsonObject>()
                extractAllResponsiveItems(root, renderers)

                if (renderers.isNotEmpty()) {
                    val songs = mutableListOf<Track>()
                    for (renderer in renderers) {
                        extractSearchItem(renderer, songs, mutableListOf(), mutableListOf(), mutableListOf())
                    }
                    if (songs.isNotEmpty()) {
                        return songs
                    }
                }
            } catch (_: Exception) {}
        }

        return emptyList()
    }

    private fun extractAllResponsiveItems(
        element: kotlinx.serialization.json.JsonElement,
        list: MutableList<JsonObject>
    ) {
        when (element) {
            is JsonObject -> {
                for ((key, value) in element) {
                    if ((key == "musicResponsiveListItemRenderer" || key == "playlistVideoRenderer") && value is JsonObject) {
                        list.add(value)
                    } else {
                        extractAllResponsiveItems(value, list)
                    }
                }
            }
            is kotlinx.serialization.json.JsonArray -> {
                for (child in element) {
                    extractAllResponsiveItems(child, list)
                }
            }
            else -> {}
        }
    }

    /**
     * Send official 2-way Like / Unlike to YouTube Music account.
     */
    suspend fun setYouTubeMusicLike(videoId: String, isLiked: Boolean, cookie: String): Boolean {
        val authHeader = generateSapisidHash(cookie) ?: return false
        val action = if (isLiked) "like" else "removelike"

        // 1. Try YouTube Music endpoint
        try {
            val endpoint = "$YTM_BASE_URL/like/$action"
            val requestBody = buildJsonObject {
                put("context", buildWebRemixContext())
                putJsonObject("target") {
                    put("videoId", videoId)
                }
            }
            val response = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                header("X-Origin", "https://music.youtube.com")
                header("X-Goog-AuthUser", "0")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(requestBody.toString())
            }
            if (response.status.value in 200..299) return true
        } catch (_: Exception) {}

        // 2. Fallback to YouTube Web endpoint
        try {
            val ytAuthHeader = generateSapisidHash(cookie, "https://www.youtube.com") ?: authHeader
            val ytEndpoint = "$YT_BASE_URL/like/$action"
            val ytRequestBody = buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20241118.01.00")
                        put("hl", "id")
                        put("gl", "ID")
                    }
                }
                putJsonObject("target") {
                    put("videoId", videoId)
                }
            }
            val ytResponse = httpClient.post(ytEndpoint) {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://www.youtube.com/")
                header("Origin", "https://www.youtube.com")
                header("X-Origin", "https://www.youtube.com")
                header("X-Goog-AuthUser", "0")
                header("Cookie", cookie)
                header("Authorization", ytAuthHeader)
                setBody(ytRequestBody.toString())
            }
            return ytResponse.status.value in 200..299
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Fetch authentic user profile (name, email, avatar, handle) from YouTube Music / YouTube Innertube.
     */
    suspend fun fetchAccountProfile(cookie: String): GoogleUserProfile? {
        val authHeader = generateSapisidHash(cookie) ?: return null
        val requestBody = buildJsonObject {
            put("context", buildWebRemixContext())
        }

        // 1. Try YouTube Music account_menu
        try {
            val responseText = httpClient.post("$YTM_BASE_URL/account/account_menu") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                header("X-Origin", "https://music.youtube.com")
                header("X-Goog-AuthUser", "0")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(requestBody.toString())
            }.bodyAsText()

            val profile = parseAccountMenuResponse(responseText)
            if (profile != null) return profile
        } catch (_: Exception) {}

        // 2. Try YouTube Web account_menu
        try {
            val webAuthHeader = generateSapisidHash(cookie, "https://www.youtube.com") ?: authHeader
            val ytResponseText = httpClient.post("$YT_BASE_URL/account/account_menu") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://www.youtube.com/")
                header("Origin", "https://www.youtube.com")
                header("X-Origin", "https://www.youtube.com")
                header("X-Goog-AuthUser", "0")
                header("Cookie", cookie)
                header("Authorization", webAuthHeader)
                setBody(requestBody.toString())
            }.bodyAsText()

            val profile = parseAccountMenuResponse(ytResponseText)
            if (profile != null) return profile
        } catch (_: Exception) {}

        // 3. Fallback: accounts_list
        try {
            val listResponseText = httpClient.post("$YTM_BASE_URL/account/accounts_list") {
                contentType(ContentType.Application.Json)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Referer", "https://music.youtube.com/")
                header("Origin", "https://music.youtube.com")
                header("X-Origin", "https://music.youtube.com")
                header("X-Goog-AuthUser", "0")
                header("Cookie", cookie)
                header("Authorization", authHeader)
                setBody(requestBody.toString())
            }.bodyAsText()

            val root = json.parseToJsonElement(listResponseText).jsonObject
            val actions = root["actions"]?.jsonArray
            actions?.forEach { action ->
                val popup = action.jsonObject["openPopupAction"]?.jsonObject?.get("popup")?.jsonObject
                val sections = popup?.get("multiPageMenuRenderer")?.jsonObject?.get("sections")?.jsonArray
                sections?.forEach { section ->
                    val items = section.jsonObject["accountSectionListRenderer"]?.jsonObject
                        ?.get("contents")?.jsonArray
                    items?.forEach { itm ->
                        val renderer = itm.jsonObject["accountItemRenderer"]?.jsonObject
                        if (renderer != null) {
                            val accountName = renderer["accountName"]?.jsonObject?.get("runs")?.jsonArray
                                ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                                ?: renderer["accountName"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
                            val email = renderer["accountEmail"]?.jsonObject?.get("runs")?.jsonArray
                                ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                                ?: renderer["accountEmail"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull
                            val avatar = renderer["accountPhoto"]?.jsonObject?.get("thumbnails")?.jsonArray
                                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                            if (!accountName.isNullOrBlank() || !email.isNullOrBlank()) {
                                return GoogleUserProfile(
                                    email = email ?: accountName ?: "User",
                                    name = accountName ?: email ?: "User",
                                    handle = null,
                                    avatarUrl = avatar,
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun parseAccountMenuResponse(responseText: String): GoogleUserProfile? {
        return try {
            val root = json.parseToJsonElement(responseText).jsonObject
            val actions = root["actions"]?.jsonArray ?: return null
            for (action in actions) {
                val popup = action.jsonObject["openPopupAction"]?.jsonObject?.get("popup")?.jsonObject
                val header = popup?.get("multiPageMenuRenderer")?.jsonObject?.get("header")?.jsonObject
                val activeHeader = header?.get("activeAccountHeaderRenderer")?.jsonObject
                    ?: header?.get("googleAccountHeaderRenderer")?.jsonObject

                if (activeHeader != null) {
                    val name = activeHeader["accountName"]?.jsonObject?.get("runs")?.jsonArray
                        ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: activeHeader["accountName"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull

                    val email = activeHeader["email"]?.jsonObject?.get("runs")?.jsonArray
                        ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: activeHeader["email"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull

                    val handle = activeHeader["channelHandle"]?.jsonObject?.get("runs")?.jsonArray
                        ?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: activeHeader["channelHandle"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull

                    val avatarUrl = activeHeader["avatar"]?.jsonObject?.get("thumbnails")?.jsonArray
                        ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

                    if (!name.isNullOrBlank() || !email.isNullOrBlank() || !handle.isNullOrBlank()) {
                        val finalEmail = email ?: handle ?: "Google Account"
                        val candidateName = name?.trim()?.ifBlank { null }
                        val finalName = if (!candidateName.isNullOrBlank() &&
                            !candidateName.equals("Pengguna Google", ignoreCase = true) &&
                            !candidateName.equals("Akun Google", ignoreCase = true) &&
                            !candidateName.equals("Google Account", ignoreCase = true)
                        ) {
                            candidateName
                        } else if (!handle.isNullOrBlank()) {
                            handle.removePrefix("@").replace(".", " ")
                                .split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        } else {
                            finalEmail.substringBefore("@")
                                .replace(".", " ")
                                .split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        }

                        return GoogleUserProfile(
                            email = finalEmail,
                            name = finalName,
                            handle = handle,
                            avatarUrl = avatarUrl,
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractCookieValue(cookie: String, key: String): String? {
        val regex = Regex("""(?i)(?:^|[;\s])${Regex.escape(key)}=([^;]+)""")
        return regex.find(cookie)?.groupValues?.getOrNull(1)?.trim()?.removeSurrounding("\"")?.ifBlank { null }
    }

    /**
     * Compute authentic Google SAPISIDHASH for YouTube/YouTube Music APIs.
     */
    private fun generateSapisidHash(cookie: String, origin: String = "https://music.youtube.com"): String? {
        val sapisid = extractCookieValue(cookie, "SAPISID")
            ?: extractCookieValue(cookie, "__Secure-3PAPISID")
            ?: extractCookieValue(cookie, "__Secure-1PAPISID")
            ?: return null

        return try {
            val timestamp = System.currentTimeMillis() / 1000
            val input = "$timestamp $sapisid $origin"
            val md = java.security.MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            val hash = bytes.joinToString("") { "%02x".format(it) }
            "SAPISIDHASH ${timestamp}_$hash"
        } catch (_: Exception) {
            null
        }
    }
}

data class GoogleUserProfile(
    val email: String,
    val name: String,
    val handle: String? = null,
    val avatarUrl: String? = null,
)


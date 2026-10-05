package com.asla.denge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.asla.denge.domain.model.MusicGenre
import com.asla.denge.domain.repository.GenreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class GenreRepositoryImpl(
    private val context: Context,
    private val json: Json,
) : GenreRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hearmsc_genres_prefs", Context.MODE_PRIVATE)

    private val defaultGenres = listOf(
        MusicGenre("g1", "Indonesian Pop", "Viral and trending Indonesian pop hits", "pop indonesia hits terpopuler", "🌴", isSelected = false),
        MusicGenre("g2", "Global Pop", "Top international charts and global hits", "billboard hot 100 pop hits", "🌍", isSelected = false),
        MusicGenre("g3", "J-Pop Hits", "Popular Japanese pop melodies and charts", "jpop hits official", "🎌", isSelected = false),
        MusicGenre("g4", "K-Pop Trending", "Trending Korean idol tracks and hits", "kpop hits official", "⚡", isSelected = false),
        MusicGenre("g5", "Rock & Alternative", "Energetic guitars and classic rock anthems", "rock hits popular songs", "🎸", isSelected = false),
        MusicGenre("g6", "R&B & Soul", "Smooth vocals and mellow grooves", "rnb soul popular hits", "🌙", isSelected = false),
        MusicGenre("g7", "Acoustic & Chill", "Warm acoustic guitar and relaxing melodies", "acoustic chill relaxing songs", "☕", isSelected = false),
        MusicGenre("g8", "Dangdut Koplo", "Upbeat and popular modern dangdut koplo", "dangdut koplo hits terpopuler", "💃", isSelected = false),
        MusicGenre("g9", "EDM & Dance", "Electronic beats and high-energy anthems", "edm electronic dance music", "🎧", isSelected = false),
        MusicGenre("g10", "Jazz & Cafe", "Warm jazz tones and cozy cafe vibes", "cozy jazz coffee", "🎷", isSelected = false),
        MusicGenre("g11", "Anime OST", "Iconic anime openings and themes", "anime opening hits official", "🌸", isSelected = false),
        MusicGenre("g12", "Gaming Soundtracks", "Epic video game soundtracks and themes", "gaming soundtracks ost", "🎮", isSelected = false),
    )

    private val _genresFlow = MutableStateFlow<List<MusicGenre>>(loadGenres())

    override fun getGenresFlow(): Flow<List<MusicGenre>> = _genresFlow.asStateFlow()

    private fun loadGenres(): List<MusicGenre> {
        val savedJson = prefs.getString(KEY_GENRES_LIST, null)
        if (savedJson.isNullOrBlank()) {
            return defaultGenres
        }
        return try {
            val savedList = json.decodeFromString<List<MusicGenre>>(savedJson)
            val defaultMap = defaultGenres.associateBy { it.id }
            val mergedList = savedList.map { saved ->
                val default = defaultMap[saved.id]
                if (default != null) {
                    // Update to modern English name, subtitle, icon, searchQuery while preserving selection
                    default.copy(isSelected = saved.isSelected)
                } else {
                    saved
                }
            }
            val existingIds = mergedList.map { it.id }.toSet()
            val missingDefaults = defaultGenres.filter { it.id !in existingIds }
            val finalList = mergedList + missingDefaults
            try {
                val serialized = json.encodeToString(finalList)
                prefs.edit().putString(KEY_GENRES_LIST, serialized).apply()
            } catch (_: Exception) {}
            finalList
        } catch (_: Exception) {
            defaultGenres
        }
    }

    private fun persistGenres(list: List<MusicGenre>) {
        _genresFlow.value = list
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString(KEY_GENRES_LIST, serialized).apply()
        } catch (_: Exception) {}
    }

    override fun getSelectedGenres(): List<MusicGenre> {
        return _genresFlow.value.filter { it.isSelected }.take(5)
    }

    override fun getAllGenres(): List<MusicGenre> {
        return _genresFlow.value
    }

    override fun toggleGenreSelection(genreId: String): Boolean {
        val current = _genresFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == genreId }
        if (index == -1) return false

        val item = current[index]
        if (!item.isSelected) {
            val selectedCount = current.count { it.isSelected }
            if (selectedCount >= 5) {
                // Max 5 genres limit reached!
                return false
            }
            current[index] = item.copy(isSelected = true)
            persistGenres(current)
            return true
        } else {
            current[index] = item.copy(isSelected = false)
            persistGenres(current)
            return false
        }
    }

    override fun addCustomGenre(name: String, query: String): MusicGenre {
        val cleanName = name.trim()
        val cleanQuery = query.trim().ifBlank { cleanName }
        val id = "custom_" + UUID.randomUUID().toString().take(8)
        val selectedCount = _genresFlow.value.count { it.isSelected }

        val newGenre = MusicGenre(
            id = id,
            name = cleanName,
            subtitle = "Your custom genre",
            searchQuery = cleanQuery,
            icon = "✨",
            isCustom = true,
            isSelected = selectedCount < 5,
        )

        val current = _genresFlow.value.toMutableList()
        current.add(0, newGenre)
        persistGenres(current)
        return newGenre
    }

    override fun deleteCustomGenre(genreId: String) {
        val current = _genresFlow.value.filterNot { it.id == genreId && it.isCustom }
        persistGenres(current)
    }

    override fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_COMPLETED_ONBOARDING, false)
    }

    override fun setCompletedOnboarding(completed: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETED_ONBOARDING, completed).apply()
    }

    companion object {
        private const val KEY_GENRES_LIST = "saved_genres_list_v1"
        private const val KEY_COMPLETED_ONBOARDING = "has_completed_genre_onboarding"
    }
}

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
        MusicGenre("g1", "Pop Indonesia", "Lagu pop Indonesia hits & viral", "pop indonesia hits terpopuler", "🌴", isSelected = false),
        MusicGenre("g2", "Pop Barat & Global", "Tangga lagu internasional terpopuler", "billboard hot 100 pop hits", "🌍", isSelected = false),
        MusicGenre("g3", "J-Pop Hits", "Melodi pop Jepang terpopuler", "jpop hits official", "🎌", isSelected = false),
        MusicGenre("g4", "K-Pop Trending", "Hits idol Korea terpopuler", "kpop hits official", "⚡", isSelected = false),
        MusicGenre("g5", "Rock & Alternatif", "Distorsi gitar energik & lagu rock", "rock hits popular songs", "🎸", isSelected = false),
        MusicGenre("g6", "R&B & Soul", "Alunan vokal merdu dan ritme santai", "rnb soul popular hits", "🌙", isSelected = false),
        MusicGenre("g7", "Akustik & Santai", "Petikan gitar akustik pengiring santai", "akustik santai indonesia", "☕", isSelected = false),
        MusicGenre("g8", "Dangdut Koplo", "Irama dangdut koplo asik dan terpopuler", "dangdut koplo hits terpopuler", "💃", isSelected = false),
        MusicGenre("g9", "EDM & Dance", "Dentuman beat elektro pengisi semangat", "edm electronic dance music", "🎧", isSelected = false),
        MusicGenre("g10", "Jazz & Kafe", "Melodi jazz hangat suasana santai", "cozy jazz coffee", "🎷", isSelected = false),
        MusicGenre("g11", "Anime OST", "Lagu tema & opening anime terpopuler", "anime opening hits official", "🌸", isSelected = false),
        MusicGenre("g12", "Gaming Soundtracks", "Lagu tema dan soundtrack game epik", "gaming soundtracks ost", "🎮", isSelected = false),
    )

    private val _genresFlow = MutableStateFlow<List<MusicGenre>>(loadGenres())

    override fun getGenresFlow(): Flow<List<MusicGenre>> = _genresFlow.asStateFlow()

    private fun loadGenres(): List<MusicGenre> {
        val savedJson = prefs.getString(KEY_GENRES_LIST, null)
        if (savedJson.isNullOrBlank()) {
            return defaultGenres
        }
        return try {
            json.decodeFromString<List<MusicGenre>>(savedJson)
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
            subtitle = "Genre kustom Anda",
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

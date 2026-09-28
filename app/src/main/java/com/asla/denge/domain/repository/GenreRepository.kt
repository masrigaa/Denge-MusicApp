package com.asla.denge.domain.repository

import com.asla.denge.domain.model.MusicGenre
import kotlinx.coroutines.flow.Flow

interface GenreRepository {
    fun getGenresFlow(): Flow<List<MusicGenre>>
    fun getSelectedGenres(): List<MusicGenre>
    fun getAllGenres(): List<MusicGenre>
    fun toggleGenreSelection(genreId: String): Boolean
    fun addCustomGenre(name: String, query: String): MusicGenre
    fun deleteCustomGenre(genreId: String)
    fun hasCompletedOnboarding(): Boolean
    fun setCompletedOnboarding(completed: Boolean)
}

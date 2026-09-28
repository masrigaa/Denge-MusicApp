package com.asla.denge.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicGenre(
    val id: String,
    val name: String,
    val subtitle: String,
    val searchQuery: String,
    val icon: String = "🎵",
    val isCustom: Boolean = false,
    val isSelected: Boolean = false,
)

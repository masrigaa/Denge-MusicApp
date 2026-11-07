package com.asla.denge.domain.model

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "Sistem"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

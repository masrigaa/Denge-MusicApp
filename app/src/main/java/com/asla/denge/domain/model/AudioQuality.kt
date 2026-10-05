package com.asla.denge.domain.model

enum class AudioQuality(val id: String, val displayName: String, val subtitle: String) {
    HIGH("high", "High Quality", "Opus ~160 kbps (High fidelity)"),
    DATA_SAVER("data_saver", "Data Saver", "Opus ~70 kbps (50% less data & cooler device)"),
    ;

    companion object {
        fun fromId(id: String?): AudioQuality {
            return entries.firstOrNull { it.id == id } ?: HIGH
        }
    }
}

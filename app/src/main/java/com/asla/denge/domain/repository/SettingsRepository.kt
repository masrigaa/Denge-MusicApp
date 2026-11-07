package com.asla.denge.domain.repository

import com.asla.denge.domain.model.AudioQuality
import com.asla.denge.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAudioQuality(): Flow<AudioQuality>
    suspend fun setAudioQuality(quality: AudioQuality)
    fun getAudioQualitySync(): AudioQuality

    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun getThemeModeSync(): ThemeMode
}


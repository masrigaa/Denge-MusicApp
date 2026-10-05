package com.asla.denge.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.asla.denge.domain.model.AudioQuality
import com.asla.denge.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepositoryImpl(
    context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("denge_settings_prefs", Context.MODE_PRIVATE)

    private val _audioQuality = MutableStateFlow(
        AudioQuality.fromId(prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.HIGH.id))
    )

    override fun getAudioQuality(): Flow<AudioQuality> = _audioQuality.asStateFlow()

    override suspend fun setAudioQuality(quality: AudioQuality) {
        prefs.edit().putString(KEY_AUDIO_QUALITY, quality.id).apply()
        _audioQuality.value = quality
    }

    override fun getAudioQualitySync(): AudioQuality {
        return _audioQuality.value
    }

    companion object {
        private const val KEY_AUDIO_QUALITY = "audio_quality"
    }
}

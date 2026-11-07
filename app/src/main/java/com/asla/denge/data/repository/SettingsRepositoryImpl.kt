package com.asla.denge.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.asla.denge.domain.model.AudioQuality
import com.asla.denge.domain.model.ThemeMode
import com.asla.denge.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "denge_user_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("denge_settings_prefs", Context.MODE_PRIVATE)

    private val _audioQuality = MutableStateFlow(
        AudioQuality.fromId(prefs.getString(KEY_AUDIO_QUALITY, AudioQuality.HIGH.id))
    )

    private val initialTheme = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id)
    private val _themeMode = MutableStateFlow(ThemeMode.fromId(initialTheme))

    init {
        CoroutineScope(Dispatchers.IO).launch {
            context.settingsDataStore.data
                .map { preferences ->
                    preferences[THEME_MODE_PREF_KEY]?.let { ThemeMode.fromId(it) }
                        ?: ThemeMode.fromId(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id))
                }
                .collect { mode ->
                    _themeMode.value = mode
                }
        }
    }

    override fun getAudioQuality(): Flow<AudioQuality> = _audioQuality.asStateFlow()

    override suspend fun setAudioQuality(quality: AudioQuality) {
        prefs.edit().putString(KEY_AUDIO_QUALITY, quality.id).apply()
        _audioQuality.value = quality
    }

    override fun getAudioQualitySync(): AudioQuality {
        return _audioQuality.value
    }

    override fun getThemeMode(): Flow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE_PREF_KEY] = mode.id
        }
    }

    override fun getThemeModeSync(): ThemeMode {
        return _themeMode.value
    }

    companion object {
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_THEME_MODE = "theme_mode"
        val THEME_MODE_PREF_KEY = stringPreferencesKey("theme_mode")
    }
}


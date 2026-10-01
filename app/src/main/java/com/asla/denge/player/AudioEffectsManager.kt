package com.asla.denge.player

import android.media.audiofx.Equalizer
import com.asla.denge.data.local.dao.EqPresetDao
import com.asla.denge.data.local.entity.EqPresetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class AudioEffectsManager(
    private val playerManager: PlayerManager,
    private val eqPresetDao: EqPresetDao,
) {
    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = 0
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        playerManager.onAudioSessionIdAvailable = { sessionId ->
            attachToAudioSession(sessionId)
        }
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            if (equalizer == null || audioSessionId != currentSessionId) {
                try { equalizer?.release() } catch (_: Throwable) {}
                currentSessionId = audioSessionId
                val newEq = Equalizer(0, audioSessionId)
                newEq.enabled = true
                equalizer = newEq
            } else {
                equalizer?.enabled = true
            }

            // ALWAYS reapply active preset to hardware whenever audio session is attached or re-verified!
            reapplyCurrentPreset()
        } catch (_: Throwable) {
        }
    }

    fun reapplyCurrentPreset() {
        scope.launch {
            try {
                val active = eqPresetDao.getActive() ?: ensureDefaultPresets()
                active?.let { applyPresetToHardware(it) }
            } catch (_: Throwable) {}
        }
    }

    suspend fun ensureDefaultPresets(): EqPresetEntity? {
        val all = eqPresetDao.getAll().first()
        val defaultPresets = listOf(
            EqPresetEntity(name = "Flat (Normal)", bandsJson = "[0,0,0,0,0]", isActive = 1, isBuiltin = 1, createdAt = 1),
            EqPresetEntity(name = "Acoustic", bandsJson = "[4,3,2,3,4]", isActive = 0, isBuiltin = 1, createdAt = 2),
            EqPresetEntity(name = "Bass Booster", bandsJson = "[9,6,2,0,0]", isActive = 0, isBuiltin = 1, createdAt = 3),
            EqPresetEntity(name = "Bass Reducer", bandsJson = "[-9,-6,-2,0,0]", isActive = 0, isBuiltin = 1, createdAt = 4),
            EqPresetEntity(name = "Classical", bandsJson = "[5,3,-1,3,4]", isActive = 0, isBuiltin = 1, createdAt = 5),
            EqPresetEntity(name = "Dance", bandsJson = "[6,4,1,3,4]", isActive = 0, isBuiltin = 1, createdAt = 6),
            EqPresetEntity(name = "Deep", bandsJson = "[6,4,0,-2,-4]", isActive = 0, isBuiltin = 1, createdAt = 7),
            EqPresetEntity(name = "Electronic", bandsJson = "[6,4,0,2,6]", isActive = 0, isBuiltin = 1, createdAt = 8),
            EqPresetEntity(name = "Hip-Hop", bandsJson = "[7,4,0,2,5]", isActive = 0, isBuiltin = 1, createdAt = 9),
            EqPresetEntity(name = "Jazz", bandsJson = "[4,2,-1,2,4]", isActive = 0, isBuiltin = 1, createdAt = 10),
            EqPresetEntity(name = "Latin", bandsJson = "[4,2,0,2,4]", isActive = 0, isBuiltin = 1, createdAt = 11),
            EqPresetEntity(name = "Loudness", bandsJson = "[8,4,-2,2,7]", isActive = 0, isBuiltin = 1, createdAt = 12),
            EqPresetEntity(name = "Lounge", bandsJson = "[-3,-1,2,4,1]", isActive = 0, isBuiltin = 1, createdAt = 13),
            EqPresetEntity(name = "Piano", bandsJson = "[3,2,0,3,4]", isActive = 0, isBuiltin = 1, createdAt = 14),
            EqPresetEntity(name = "Pop", bandsJson = "[-2,1,4,3,-1]", isActive = 0, isBuiltin = 1, createdAt = 15),
            EqPresetEntity(name = "R&B", bandsJson = "[3,7,2,2,4]", isActive = 0, isBuiltin = 1, createdAt = 16),
            EqPresetEntity(name = "Rock", bandsJson = "[6,3,-2,3,6]", isActive = 0, isBuiltin = 1, createdAt = 17),
            EqPresetEntity(name = "Small Speakers", bandsJson = "[7,5,2,0,-2]", isActive = 0, isBuiltin = 1, createdAt = 18),
            EqPresetEntity(name = "Spoken Word", bandsJson = "[-4,0,5,3,-2]", isActive = 0, isBuiltin = 1, createdAt = 19),
            EqPresetEntity(name = "Treble Booster", bandsJson = "[0,0,2,5,9]", isActive = 0, isBuiltin = 1, createdAt = 20),
            EqPresetEntity(name = "Treble Reducer", bandsJson = "[0,0,-2,-5,-9]", isActive = 0, isBuiltin = 1, createdAt = 21),
            EqPresetEntity(name = "Vocal Booster", bandsJson = "[-2,0,6,4,1]", isActive = 0, isBuiltin = 1, createdAt = 22),
        )
        // Detect previously active preset to preserve user selection
        val activePreset = all.firstOrNull { it.isActive == 1 }
        val activeName = activePreset?.name?.lowercase()?.trim()

        // Remove any obsolete or duplicate builtins from previous versions (e.g. "Flat", "Bass Boost")
        eqPresetDao.deleteBuiltins()

        val presetsToInsert = defaultPresets.map { preset ->
            val isMatchingActive = when (activeName) {
                "flat", "flat (normal)" -> preset.name.equals("Flat (Normal)", ignoreCase = true)
                "bass boost", "bass booster" -> preset.name.equals("Bass Booster", ignoreCase = true)
                "treble boost", "treble booster" -> preset.name.equals("Treble Booster", ignoreCase = true)
                else -> preset.name.equals(activeName, ignoreCase = true)
            }
            if (activePreset != null && activePreset.isBuiltin == 0) {
                // If user had a custom preset active, keep all builtins inactive
                preset.copy(isActive = 0)
            } else if (isMatchingActive) {
                preset.copy(isActive = 1)
            } else if (activeName == null && preset.name.equals("Flat (Normal)", ignoreCase = true)) {
                preset.copy(isActive = 1)
            } else {
                preset.copy(isActive = 0)
            }
        }

        eqPresetDao.upsertAll(presetsToInsert)
        val refreshed = eqPresetDao.getAll().first()
        return refreshed.firstOrNull { it.isActive == 1 } ?: refreshed.firstOrNull()
    }

    fun getPresets(): Flow<List<EqPresetEntity>> {
        scope.launch {
            ensureDefaultPresets()
        }
        return eqPresetDao.getAll()
    }

    suspend fun applyPreset(preset: EqPresetEntity) {
        eqPresetDao.deactivateAll()
        eqPresetDao.activate(preset.id)
        applyPresetToHardware(preset)
    }

    fun applyPresetToHardware(preset: EqPresetEntity) {
        try {
            var eq = equalizer
            if (eq == null && currentSessionId > 0) {
                eq = Equalizer(0, currentSessionId)
                equalizer = eq
            }
            if (eq == null) return

            if (!eq.enabled) {
                eq.enabled = true
            }

            val bands: List<Int> = try {
                val element = json.parseToJsonElement(preset.bandsJson)
                element.jsonArray.map { it.jsonPrimitive.content.toIntOrNull() ?: 0 }
            } catch (_: Exception) {
                emptyList()
            }

            val numBands = eq.numberOfBands.toInt()
            val minEq = eq.bandLevelRange[0] // e.g. -1500 mB (-15 dB)
            val maxEq = eq.bandLevelRange[1] // e.g. +1500 mB (+15 dB)

            // Direct decibel-to-millibel mapping matching Spotify's 5-band EQ
            // Each band value in bands is in dB (-12 to +12 dB = -1200 to +1200 mB)
            bands.forEachIndexed { index, value ->
                if (index < numBands) {
                    val targetMillibels = (value * 100).coerceIn(minEq.toInt(), maxEq.toInt())
                    eq.setBandLevel(index.toShort(), targetMillibels.toShort())
                }
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        currentSessionId = 0
    }
}


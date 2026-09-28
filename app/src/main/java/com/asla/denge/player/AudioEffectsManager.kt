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
            EqPresetEntity(name = "Flat", bandsJson = "[0,0,0,0,0]", isActive = 1, isBuiltin = 1, createdAt = 1),
            EqPresetEntity(name = "Bass Boost", bandsJson = "[5,3,1,0,0]", isActive = 0, isBuiltin = 1, createdAt = 2),
            EqPresetEntity(name = "Treble Boost", bandsJson = "[0,0,1,3,5]", isActive = 0, isBuiltin = 1, createdAt = 3),
            EqPresetEntity(name = "Rock", bandsJson = "[4,2,-1,2,4]", isActive = 0, isBuiltin = 1, createdAt = 4),
            EqPresetEntity(name = "Pop", bandsJson = "[-1,2,4,2,-1]", isActive = 0, isBuiltin = 1, createdAt = 5),
            EqPresetEntity(name = "Jazz", bandsJson = "[3,1,0,1,3]", isActive = 0, isBuiltin = 1, createdAt = 6),
        )
        if (all.isEmpty()) {
            eqPresetDao.upsertAll(defaultPresets)
            return defaultPresets.first()
        } else {
            // Update builtin preset band levels to ensure clean, non-distorting sound
            val updated = all.map { existing ->
                if (existing.isBuiltin == 1) {
                    val matching = defaultPresets.firstOrNull { it.name.equals(existing.name, ignoreCase = true) }
                    if (matching != null && existing.bandsJson != matching.bandsJson) {
                        existing.copy(bandsJson = matching.bandsJson)
                    } else existing
                } else existing
            }
            eqPresetDao.upsertAll(updated)
            return updated.firstOrNull { it.isActive == 1 } ?: updated.firstOrNull()
        }
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
            val minEq = eq.bandLevelRange[0] // Typically -1500 mB (-15 dB)
            val maxEq = eq.bandLevelRange[1] // Typically +1500 mB (+15 dB)

            // Scale to punchy, warm musical level (+8 to +9 dB max, i.e. 800-900 mB)
            // Adds +2 to 3 dB over previous setting while maintaining clean headroom
            val scaledMax = (maxEq * 0.58).coerceIn(700.0, 900.0)
            val scaledMin = (minEq * 0.58).coerceIn(-900.0, -700.0)

            bands.forEachIndexed { index, value ->
                if (index < numBands) {
                    val fraction = (value / 10.0).coerceIn(-1.0, 1.0)
                    val level = if (fraction >= 0) {
                        (fraction * scaledMax).toInt()
                    } else {
                        (kotlin.math.abs(fraction) * scaledMin).toInt()
                    }
                    eq.setBandLevel(index.toShort(), level.coerceIn(minEq.toInt(), maxEq.toInt()).toShort())
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


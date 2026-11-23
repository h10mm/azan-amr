package com.github.meypod.al_azan.main.azkar

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class AzkarPreferences(
    val favoriteIds: Set<String> = emptySet(),
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
) {
    companion object {
        const val MIN_FONT_SIZE_SP = 18f
        const val MAX_FONT_SIZE_SP = 36f
        const val DEFAULT_FONT_SIZE_SP = 24f
        const val FONT_STEP_SP = 2f
    }
}

@Singleton
class AzkarPreferencesRepository
@Inject constructor() {
    private val mmkv: MMKV by lazy { MMKV.defaultMMKV() }

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AzkarPreferences> = _preferences.asStateFlow()

    fun toggleFavorite(dhikrId: String) {
        _preferences.update { current ->
            val updated = if (dhikrId in current.favoriteIds) {
                current.favoriteIds - dhikrId
            } else {
                current.favoriteIds + dhikrId
            }
            mmkv.encode(KEY_FAVORITE_IDS, updated)
            current.copy(favoriteIds = updated)
        }
    }

    fun increaseFontSize() {
        _preferences.update { current ->
            val next = (current.fontSizeSp + AzkarPreferences.FONT_STEP_SP)
                .coerceAtMost(AzkarPreferences.MAX_FONT_SIZE_SP)
            mmkv.encode(KEY_FONT_SIZE_SP, next)
            current.copy(fontSizeSp = next)
        }
    }

    fun decreaseFontSize() {
        _preferences.update { current ->
            val next = (current.fontSizeSp - AzkarPreferences.FONT_STEP_SP)
                .coerceAtLeast(AzkarPreferences.MIN_FONT_SIZE_SP)
            mmkv.encode(KEY_FONT_SIZE_SP, next)
            current.copy(fontSizeSp = next)
        }
    }

    private fun loadPreferences(): AzkarPreferences {
        val favorites = runCatching {
            mmkv.decodeStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
        }.getOrDefault(emptySet())
        val fontSize = runCatching {
            mmkv.decodeFloat(KEY_FONT_SIZE_SP, AzkarPreferences.DEFAULT_FONT_SIZE_SP)
                .coerceIn(AzkarPreferences.MIN_FONT_SIZE_SP, AzkarPreferences.MAX_FONT_SIZE_SP)
        }.getOrDefault(AzkarPreferences.DEFAULT_FONT_SIZE_SP)
        return AzkarPreferences(
            favoriteIds = favorites,
            fontSizeSp = fontSize,
        )
    }

    private companion object {
        const val KEY_FAVORITE_IDS = "AZKAR_FAVORITE_IDS_V1"
        const val KEY_FONT_SIZE_SP = "AZKAR_FONT_SIZE_SP_V1"
    }
}

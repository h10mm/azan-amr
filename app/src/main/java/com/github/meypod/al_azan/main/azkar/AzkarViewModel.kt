package com.github.meypod.al_azan.main.azkar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class AzkarUiState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val fontSizeSp: Float = AzkarPreferences.DEFAULT_FONT_SIZE_SP,
    val remainingCounts: Map<String, Int> = emptyMap(),
) {
    fun remainingFor(item: AzkarItem): Int =
        remainingCounts[item.id] ?: item.repeatCount
}

@HiltViewModel
class AzkarViewModel
@Inject constructor(
    private val preferencesRepository: AzkarPreferencesRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _remainingCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    val uiState: StateFlow<AzkarUiState> = combine(
        _searchQuery,
        _showFavoritesOnly,
        preferencesRepository.preferences,
        _remainingCounts,
    ) { query, favoritesOnly, prefs, counts ->
        AzkarUiState(
            searchQuery = query,
            showFavoritesOnly = favoritesOnly,
            favoriteIds = prefs.favoriteIds,
            fontSizeSp = prefs.fontSizeSp,
            remainingCounts = counts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AzkarUiState(
            favoriteIds = preferencesRepository.preferences.value.favoriteIds,
            fontSizeSp = preferencesRepository.preferences.value.fontSizeSp,
        ),
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSetFavoritesOnly(enabled: Boolean) {
        _showFavoritesOnly.value = enabled
    }

    fun toggleFavorite(dhikrId: String) {
        preferencesRepository.toggleFavorite(dhikrId)
    }

    fun increaseFontSize() {
        preferencesRepository.increaseFontSize()
    }

    fun decreaseFontSize() {
        preferencesRepository.decreaseFontSize()
    }

    /**
     * Decrements the remaining count for [item].
     * Returns true if this tap just completed the dhikr (reached 0).
     */
    fun decrementCount(item: AzkarItem): Boolean {
        var justCompleted = false
        _remainingCounts.update { current ->
            val prev = current[item.id] ?: item.repeatCount
            if (prev <= 0) {
                current
            } else {
                val next = prev - 1
                if (next == 0) justCompleted = true
                current + (item.id to next)
            }
        }
        return justCompleted
    }

    fun resetDhikrCount(item: AzkarItem) {
        _remainingCounts.update { current ->
            current + (item.id to item.repeatCount)
        }
    }
}

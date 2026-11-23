package com.github.meypod.al_azan.main.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.repository.NateegaWidgetConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompactPrayerAppearanceViewModel @Inject constructor(
    private val repository: NateegaWidgetConfigRepository,
) : ViewModel() {

    val uiState: StateFlow<NateegaWidgetConfig> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NateegaWidgetConfig(),
    )

    fun onThemeModeChange(mode: NateegaThemeMode) {
        viewModelScope.launch {
            repository.update { it.copy(compactThemeMode = mode) }
        }
    }

    fun onDateDisplayModeChange(mode: NateegaDateDisplayMode) {
        viewModelScope.launch {
            repository.update { it.copy(compactDateDisplayMode = mode) }
        }
    }

    fun onDateOrderChange(order: NateegaDateOrder) {
        viewModelScope.launch {
            repository.update { it.copy(compactDateOrder = order) }
        }
    }

    fun onShowCountdownToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(compactShowCountdown = enabled) }
        }
    }

    fun onShowWisdomToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(compactShowWisdom = enabled) }
        }
    }
}

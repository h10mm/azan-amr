package com.github.meypod.al_azan.main.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.widget.NateegaClockPosition
import com.github.meypod.al_azan.core.domain.model.widget.NateegaLanguageMode
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
class NateegaAppearanceViewModel @Inject constructor(
    private val repository: NateegaWidgetConfigRepository,
) : ViewModel() {

    val uiState: StateFlow<NateegaWidgetConfig> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NateegaWidgetConfig(),
    )

    fun onClockPositionChange(position: NateegaClockPosition) {
        viewModelScope.launch {
            repository.update { it.copy(clockPosition = position) }
        }
    }

    fun onShowCopticDateToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(showCopticDate = enabled) }
        }
    }

    fun onShowCountdownToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(showCountdown = enabled) }
        }
    }

    fun onShowWisdomToggle(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(showWisdom = enabled) }
        }
    }

    fun onLanguageModeChange(mode: NateegaLanguageMode) {
        viewModelScope.launch {
            repository.update { it.copy(languageMode = mode) }
        }
    }

    fun onThemeModeChange(mode: NateegaThemeMode) {
        viewModelScope.launch {
            repository.update { it.copy(themeMode = mode) }
        }
    }
}

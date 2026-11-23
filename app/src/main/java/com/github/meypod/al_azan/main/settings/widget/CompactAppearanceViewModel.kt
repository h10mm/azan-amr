package com.github.meypod.al_azan.main.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerBackground
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import com.github.meypod.al_azan.core.domain.model.widget.WidgetColorOption
import com.github.meypod.al_azan.core.domain.repository.NateegaWidgetConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompactAppearanceViewModel @Inject constructor(
    private val repository: NateegaWidgetConfigRepository,
) : ViewModel() {

    val uiState: StateFlow<NateegaWidgetConfig> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NateegaWidgetConfig(),
    )

    fun onShapeChange(shape: NextPrayerShape) {
        viewModelScope.launch {
            repository.update { it.copy(nextPrayerShape = shape) }
        }
    }

    fun onBackgroundChange(background: NextPrayerBackground) {
        viewModelScope.launch {
            repository.update { it.copy(nextPrayerBackground = background) }
        }
    }

    fun onProgressColorChange(color: WidgetColorOption) {
        viewModelScope.launch {
            repository.update { it.copy(nextPrayerProgressColor = color) }
        }
    }

    fun onCountdownColorChange(color: WidgetColorOption) {
        viewModelScope.launch {
            repository.update { it.copy(nextPrayerCountdownColor = color) }
        }
    }

    fun onPrayerNameColorChange(color: WidgetColorOption) {
        viewModelScope.launch {
            repository.update { it.copy(nextPrayerNameColor = color) }
        }
    }
}

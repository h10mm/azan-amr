package com.github.meypod.al_azan.main.settings.widget

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig

@Immutable
data class WidgetSettingsUiState(
    val settings: Settings = Settings(selectedLocale = "en"),
    val nateegaConfig: NateegaWidgetConfig = NateegaWidgetConfig(),
)

sealed interface WidgetSettingsUiEvent {
    data class ShowMessage(@param:StringRes val messageRes: Int) : WidgetSettingsUiEvent
}

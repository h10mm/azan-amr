package com.github.meypod.al_azan.main.settings.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos
import com.github.meypod.al_azan.core.domain.model.widget.NateegaClockPosition
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode

sealed interface WidgetSettingsUiAction {
    data class OnShowNotificationWidgetToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnNotificationLayoutChange(
        val value: NotificationWidgetLayout,
    ) : WidgetSettingsUiAction

    data class OnShowCountdownToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnAdaptiveThemeToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnCityNamePosChange(
        val value: WidgetCityNamePos,
    ) : WidgetSettingsUiAction

    data class OnSwapLayoutDirectionToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnHighlightCurrentPrayerToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnHijriDayStartsAtMaghribToggle(
        val value: Boolean,
    ) : WidgetSettingsUiAction

    data class OnPrayerVisibilityChange(
        val prayer: Prayer,
        val visible: Boolean,
    ) : WidgetSettingsUiAction

    data class OnCountdownPrayerToggle(
        val prayer: Prayer,
        val enabled: Boolean,
    ) : WidgetSettingsUiAction

    // Nateega actions
    data class OnNateegaThemeModeChange(val value: NateegaThemeMode) : WidgetSettingsUiAction
    data class OnNateegaClockPositionChange(val value: NateegaClockPosition) : WidgetSettingsUiAction
    data class OnNateegaShowCopticToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnNateegaShowCountdownToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnNateegaShowWisdomToggle(val value: Boolean) : WidgetSettingsUiAction

    // Compact prayer actions
    data class OnCompactDateDisplayModeChange(val value: NateegaDateDisplayMode) : WidgetSettingsUiAction
    data class OnCompactDateOrderChange(val value: NateegaDateOrder) : WidgetSettingsUiAction
    data class OnCompactThemeModeChange(val value: NateegaThemeMode) : WidgetSettingsUiAction
    data class OnCompactShowCountdownToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnCompactShowWisdomToggle(val value: Boolean) : WidgetSettingsUiAction

    // Desk calendar actions
    data class OnDeskCalendarShowCountdownToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnDeskCalendarTypeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.DeskCalendarType) : WidgetSettingsUiAction
    data class OnDeskCalendarHighlightNextPrayerToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnDeskCalendarThemeModeChange(val value: NateegaThemeMode) : WidgetSettingsUiAction

    // Dial actions
    data class OnDialThemeModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.DialThemeMode) : WidgetSettingsUiAction
    data class OnSquareThemeModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.SquareThemeMode) : WidgetSettingsUiAction
    data class OnSlimBarThemeModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.SlimBarThemeMode) : WidgetSettingsUiAction
    data class OnSlimBarShowCityToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnSlimBarDateOrderChange(val value: com.github.meypod.al_azan.core.domain.model.widget.SlimBarDateOrder) : WidgetSettingsUiAction
    data class OnMosqueArchThemeModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.MosqueArchThemeMode) : WidgetSettingsUiAction
    data class OnSmartPillThemeModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode) : WidgetSettingsUiAction
    data class OnSmartPillShowWordBadgeToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnShowSpiritualPillNotificationToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnSpiritualCapsuleModeChange(val value: com.github.meypod.al_azan.core.domain.model.widget.SpiritualCapsuleMode) : WidgetSettingsUiAction
    data class OnCustomDhikrToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnCustomDhikrTimeChange(val minutes: Int) : WidgetSettingsUiAction
    data class OnCustomDhikrTextChange(val text: String) : WidgetSettingsUiAction
}

package com.github.meypod.al_azan.main.settings.menu

sealed interface SettingsMenuUiAction {
    object OnLocationClick : SettingsMenuUiAction
    object OnCalculationClick : SettingsMenuUiAction
    object OnScheduleAndMuezzinClick : SettingsMenuUiAction

    object OnNotificationAndSoundClick : SettingsMenuUiAction
    object OnRemindersClick : SettingsMenuUiAction

    object OnNateegaAppearanceClick : SettingsMenuUiAction
    object OnCompactPrayerAppearanceClick : SettingsMenuUiAction
    object OnWidgetSettingsClick : SettingsMenuUiAction
    object OnNotificationWidgetClick : SettingsMenuUiAction
    object OnInterfaceSettingsClick : SettingsMenuUiAction
    object OnTasbihClick : SettingsMenuUiAction
    object OnAzkarClick : SettingsMenuUiAction
    object OnCalendarClick : SettingsMenuUiAction
    object OnMonthlyViewClick : SettingsMenuUiAction

    object OnTroubleshootClick : SettingsMenuUiAction
    object OnBackupAndRestoreClick : SettingsMenuUiAction
    object OnAboutClick : SettingsMenuUiAction
}

package com.github.meypod.al_azan.main.settings.menu

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsMenuViewModel
@Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsMenuUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: SettingsMenuUiAction) {
        when (action) {
            SettingsMenuUiAction.OnLocationClick -> onLocationClick()
            SettingsMenuUiAction.OnCalculationClick -> onCalculationClick()
            SettingsMenuUiAction.OnScheduleAndMuezzinClick -> onScheduleAndMuezzinClick()

            SettingsMenuUiAction.OnNotificationAndSoundClick -> onNotificationAndSoundClick()
            SettingsMenuUiAction.OnRemindersClick -> onRemindersClick()

            SettingsMenuUiAction.OnNateegaAppearanceClick -> onNateegaAppearanceClick()
            SettingsMenuUiAction.OnCompactPrayerAppearanceClick -> onCompactPrayerAppearanceClick()
            SettingsMenuUiAction.OnWidgetSettingsClick -> onWidgetSettingsClick()
            SettingsMenuUiAction.OnNotificationWidgetClick -> onNotificationWidgetClick()
            SettingsMenuUiAction.OnInterfaceSettingsClick -> onInterfaceSettingsClick()
            SettingsMenuUiAction.OnTasbihClick -> onTasbihClick()
            SettingsMenuUiAction.OnAzkarClick -> onAzkarClick()
            SettingsMenuUiAction.OnCalendarClick -> onCalendarClick()
            SettingsMenuUiAction.OnMonthlyViewClick -> onMonthlyViewClick()

            SettingsMenuUiAction.OnTroubleshootClick -> onTroubleshootClick()
            SettingsMenuUiAction.OnBackupAndRestoreClick -> onBackupAndRestoreClick()
            SettingsMenuUiAction.OnAboutClick -> onAboutClick()
        }
    }

    private fun onLocationClick() {
        NavigationController.navigateTo(Route.Main.Location)
    }

    private fun onCalculationClick() {
        NavigationController.navigateTo(Route.Main.Settings.Calculations)
    }

    private fun onScheduleAndMuezzinClick() {
        NavigationController.navigateTo(Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin)
    }

    private fun onNotificationAndSoundClick() {
        NavigationController.navigateTo(Route.Main.Settings.SoundAndNotifications)
    }

    private fun onRemindersClick() {
        NavigationController.navigateTo(Route.Main.Reminder)
    }

    private fun onNateegaAppearanceClick() {
        NavigationController.navigateTo(Route.Main.Settings.WidgetSettings.NateegaAppearance)
    }

    private fun onCompactPrayerAppearanceClick() {
        NavigationController.navigateTo(Route.Main.Settings.WidgetSettings.CompactPrayerAppearance)
    }

    private fun onWidgetSettingsClick() {
        NavigationController.navigateTo(Route.Main.Settings.WidgetSettings)
    }

    private fun onNotificationWidgetClick() {
        NavigationController.navigateTo(Route.Main.Settings.NotificationWidget)
    }

    private fun onInterfaceSettingsClick() {
        NavigationController.navigateTo(Route.Main.Settings.InterfaceSettings)
    }

    private fun onTasbihClick() {
        NavigationController.navigateTo(Route.Main.Counter)
    }

    private fun onAzkarClick() {
        NavigationController.navigateTo(Route.Main.Azkar)
    }

    private fun onCalendarClick() {
        NavigationController.navigateTo(Route.Main.CalendarView)
    }

    private fun onMonthlyViewClick() {
        NavigationController.navigateTo(Route.Main.MonthlyView)
    }

    private fun onTroubleshootClick() {
        NavigationController.navigateTo(Route.Main.Settings.Troubleshoot)
    }

    private fun onBackupAndRestoreClick() {
        NavigationController.navigateTo(Route.Main.Settings.BackupAndRestore)
    }

    private fun onAboutClick() {
        NavigationController.navigateTo(Route.Main.About)
    }
}

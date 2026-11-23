package com.github.meypod.al_azan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.alarm.AlarmActivity
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.settings.DefaultAdhanEntryId
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.domain.model.settings.mapAdhanIdToEntry
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.deepLinkPatterns
import com.github.meypod.al_azan.core.presentation.navigation.deeplink.parseUriToRoute
import com.github.meypod.al_azan.di.LanguageSync
import com.github.meypod.al_azan.playback.PlaybackService
import kotlinx.coroutines.flow.first
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var alarmSettingsRepository: AlarmSettingsRepository

    @Inject
    lateinit var languageSync: LanguageSync

    @Inject
    lateinit var adhanFiringHandler: AdhanFiringHandler

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var nateegaWidgetConfigRepository: com.github.meypod.al_azan.core.domain.repository.NateegaWidgetConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        var initialSettings: Settings? by mutableStateOf(settingsRepository.getLatest())
        
        lifecycleScope.launch {
            languageSync.reconcile()
            val current = settingsRepository.fetch()
            val allDefaults = getDefaultAdhanEntries()
            val updatedSaved = (allDefaults + current.savedAdhanAudioEntries).distinctBy { it.id }
            val currentDefaultId = current.selectedAdhanEntries[AdhanKey.Default]?.id
            val isOldDefault = currentDefaultId == null ||
                currentDefaultId == DefaultAdhanEntryId.AbdulBasitAbduSamad.key ||
                currentDefaultId == DefaultAdhanEntryId.TakbeerOnly.key
            if (isOldDefault || updatedSaved.size != current.savedAdhanAudioEntries.size) {
                settingsRepository.update { s ->
                    val newSelected = if (isOldDefault) {
                        s.selectedAdhanEntries + (AdhanKey.Default to mapAdhanIdToEntry(DefaultAdhanEntryId.TakbeerOnly.key))
                    } else {
                        s.selectedAdhanEntries
                    }
                    s.copy(
                        savedAdhanAudioEntries = updatedSaved,
                        selectedAdhanEntries = newSelected,
                    )
                }
            }
            if (!current.prayerNotifDefaultDisabled) {
                settingsRepository.update { s ->
                    s.copy(
                        showWidget = false,
                        prayerNotifDefaultDisabled = true,
                    )
                }
            }
            val currentNateega = nateegaWidgetConfigRepository.data.first()
            if (!currentNateega.showSpiritualPillNotification) {
                nateegaWidgetConfigRepository.update { it.copy(showSpiritualPillNotification = true) }
            }
            widgetUpdater.update()
            initialSettings = settingsRepository.fetch()
        }

        val receivedIntent = intent
        val startingRoute = routeFromIntent(receivedIntent)
        intent = null // consume
        handleTestIntent(receivedIntent)

        setContent {
            val currentInitialSettings = initialSettings
            if (currentInitialSettings != null) {
                val settings by settingsRepository.data.collectAsState(initial = currentInitialSettings)

                AlAzanTheme(settings.themeColor, settings.displayScale) {
                    NavigationRoot(
                        appIntroDone = settings.appIntroDone,
                        startingRoute = startingRoute,
                    )
                }
            }
        }

        openAlarmScreenWhileSounding()
        handleTestIntent(intent)
    }

    /**
     * A sounding adhan or reminder takes over the app: whether it started while we were away or while the
     * user was mid-use, the alarm screen is what they need in front of them.
     *
     * Re-checked on every return to the foreground, so leaving the alarm screen without acting on it (Home,
     * a notification, the recents switcher) and coming back brings it up again. Dismissing or snoozing
     * retires the alarm, which is what stops this from re-opening what the user just dealt with.
     */
    private fun openAlarmScreenWhileSounding() {
        // Dispensed with: user opted for notification-only mode without full-screen AlarmActivity.
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val tasbihCallback = com.github.meypod.al_azan.main.tasbih.TasbihVolumeKeyBus.onVolumeKeyIncrement
        if (tasbihCallback != null &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                tasbihCallback.invoke()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    // A deep link / DND-rule tap that arrives while the Activity is already running comes here (the
    // PendingIntent is SINGLE_TOP | CLEAR_TOP), not through onCreate — route it onto the live backstack.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTestIntent(intent)
        routeFromIntent(intent)?.let { NavigationController.navigateTo(it) }
    }

    private fun handleTestIntent(intent: Intent?) {
        Log.d("MainActivity", "handleTestIntent action: ${intent?.action}")
        if (intent?.action == "com.github.meypod.al_azan.action.TEST_ADHAN") {
            lifecycleScope.launch {
                Log.d("MainActivity", "Calling onAdhanFired for Isha now")
                settingsRepository.update { it.copy(deliveredAlarmTimestamps = emptyMap()) }
                adhanFiringHandler.onAdhanFired(Prayer.Isha, false, System.currentTimeMillis())
            }
        } else if (intent?.action == "com.github.meypod.al_azan.action.TEST_MISSED_ADHAN") {
            lifecycleScope.launch {
                settingsRepository.update { it.copy(deliveredAlarmTimestamps = emptyMap()) }
                schedulerReconciler.onAdhanTimeSkipped(Prayer.Isha, System.currentTimeMillis() - 300_000L)
            }
        } else if (intent?.action == "com.github.meypod.al_azan.action.OPEN_TROUBLESHOOT") {
            lifecycleScope.launch {
                settingsRepository.update { it.copy(appIntroDone = true) }
                com.github.meypod.al_azan.core.presentation.navigation.NavigationController.navigateTo(
                    com.github.meypod.al_azan.core.presentation.navigation.Route.Main.Settings.Troubleshoot
                )
            }
        }
    }

    // MainActivity is exported, so the URI is attacker-reachable; a malformed deep link must not crash
    // launch. Fall back to the default start destination on any parse failure.
    private fun routeFromIntent(launchIntent: Intent?): Route? =
        launchIntent?.data?.let { runCatching { parseUriToRoute(it, deepLinkPatterns) }.getOrNull() }
}

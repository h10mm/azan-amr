package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.main.settings.widget.WidgetDetailType
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsScreen
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Unified AppWidget configuration Activity.
 * Opens automatically whenever the user adds or reconfigures any Al-Azan home screen widget,
 * landing directly on that widget's customization screen with live preview and theme selector.
 * Every toggle/theme choice persists immediately; tapping "تم" or pressing Back confirms and updates.
 */
@AndroidEntryPoint
class WidgetConfigureActivity : AppCompatActivity() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    private val viewModel: WidgetSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Pre-confirm RESULT_OK so any customization is immediately accepted without forcing a bottom Save tap
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)

        val confirmAndFinish: () -> Unit = {
            lifecycleScope.launch {
                widgetUpdater.update()
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmAndFinish()
            }
        })

        val providerClassName = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className

        val targetWidget = resolveWidgetDetailType(providerClassName)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AlAzanTheme(uiState.settings.themeColor, uiState.settings.displayScale) {
                WidgetSettingsScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    initialWidget = targetWidget,
                    onConfirmConfigure = confirmAndFinish,
                    onBackOverride = confirmAndFinish,
                )
            }
        }
    }

    private fun resolveWidgetDetailType(providerClassName: String?): WidgetDetailType? {
        if (providerClassName == null) return null
        return when {
            providerClassName.endsWith("NateegaWidget") -> WidgetDetailType.Nateega
            providerClassName.endsWith("CompactPrayerWidget") -> WidgetDetailType.CompactPrayer
            providerClassName.endsWith("DeskCalendarWidget") ||
                providerClassName.endsWith("DeskCalendarSquareWidget") -> WidgetDetailType.DeskCalendar
            providerClassName.endsWith("NextPrayerCircleWidget") -> WidgetDetailType.CircularDial
            providerClassName.endsWith("NextPrayerSquareWidget") -> WidgetDetailType.SquareDial
            providerClassName.endsWith("SlimBarWidget") -> WidgetDetailType.SlimBar
            providerClassName.endsWith("MosqueArchWidget") -> WidgetDetailType.MosqueArch
            providerClassName.endsWith("SmartPillWidget") -> WidgetDetailType.SmartPill
            else -> null
        }
    }
}

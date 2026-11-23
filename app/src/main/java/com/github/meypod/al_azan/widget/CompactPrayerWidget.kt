package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseCompactPrayerWidgetProvider(
    private val forcedTheme: ThemeColor? = null,
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            val dataToRender = if (forcedTheme != null) cached.copy(themeColor = forcedTheme) else cached
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(
                    id,
                    CompactPrayerWidgetRenderer.build(context, dataToRender, options),
                )
            }
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NateegaWidgetEntryPoint::class.java,
                )
                entryPoint.widgetUpdater().update()
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            val dataToRender = if (forcedTheme != null) cached.copy(themeColor = forcedTheme) else cached
            appWidgetManager.updateAppWidget(
                appWidgetId,
                CompactPrayerWidgetRenderer.build(context, dataToRender, newOptions),
            )
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NateegaWidgetEntryPoint::class.java,
                )
                entryPoint.widgetUpdater().update()
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** General / Auto Theme Compact Prayer & Dates Widget */
class CompactPrayerWidget : BaseCompactPrayerWidgetProvider()

/** Compact Prayer & Dates - Classic White Paper */
class CompactPrayerClassicWidget : BaseCompactPrayerWidgetProvider(ThemeColor.ClassicLight)

/** Compact Prayer & Dates - AMOLED True Black */
class CompactPrayerAmoledWidget : BaseCompactPrayerWidgetProvider(ThemeColor.Dark)

/** Compact Prayer & Dates - Midnight Blue */
class CompactPrayerMidnightWidget : BaseCompactPrayerWidgetProvider(ThemeColor.MidnightBlue)

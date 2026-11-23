package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 1x1 next-prayer app widget (default / legacy): shows upcoming prayer + live countdown chronometer.
 * Adapts to user's shape setting (Circle or Square).
 */
@AndroidEntryPoint
class NextPrayerWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, WidgetRenderer.buildNextPrayerWidget(context, cached, options))
            }
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetId, WidgetRenderer.buildNextPrayerWidget(context, cached, newOptions))
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Dedicated circular next-prayer widget (with outer concentric progress ring). */
@AndroidEntryPoint
class NextPrayerCircleWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            val circleData = cached.copy(shape = NextPrayerShape.Circle)
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, WidgetRenderer.buildNextPrayerWidget(context, circleData, options))
            }
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            val circleData = cached.copy(shape = NextPrayerShape.Circle)
            appWidgetManager.updateAppWidget(appWidgetId, WidgetRenderer.buildNextPrayerWidget(context, circleData, newOptions))
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Dedicated square next-prayer widget (with horizontal progress bar). */
@AndroidEntryPoint
class NextPrayerSquareWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            val squareData = cached.copy(shape = NextPrayerShape.Square)
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, WidgetRenderer.buildNextPrayerWidget(context, squareData, options))
            }
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            val squareData = cached.copy(shape = NextPrayerShape.Square)
            appWidgetManager.updateAppWidget(appWidgetId, WidgetRenderer.buildNextPrayerWidget(context, squareData, newOptions))
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

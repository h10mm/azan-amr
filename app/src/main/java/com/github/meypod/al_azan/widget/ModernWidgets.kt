package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 1. ودجت الشريط المودرن النحيف (4×1)
 */
@AndroidEntryPoint
class SlimBarWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, ModernWidgetsRenderer.buildSlimBar(context, cached, options))
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
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetId, ModernWidgetsRenderer.buildSlimBar(context, cached, newOptions))
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

/**
 * 2. ودجت محراب المسجد والقباب التراثي (4×2)
 */
@AndroidEntryPoint
class MosqueArchWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, ModernWidgetsRenderer.buildMosqueArch(context, cached, options))
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
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetId, ModernWidgetsRenderer.buildMosqueArch(context, cached, newOptions))
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

/**
 * 3. ودجت الكبسولة الذكية التفاعلية (2×1)
 */
@AndroidEntryPoint
class SmartPillWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(id, ModernWidgetsRenderer.buildSmartPill(context, cached, options))
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
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetId, ModernWidgetsRenderer.buildSmartPill(context, cached, newOptions))
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

/**
 * 4. ودجت التقويم المكتبي المتطور (يتكيف تلقائياً بين الأفقي والمربع حسب الأبعاد)
 */
@AndroidEntryPoint
class DeskCalendarWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(
                    id,
                    ModernWidgetsRenderer.buildDeskCalendar(context, cached, isSquare = null, options = options),
                )
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
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                ModernWidgetsRenderer.buildDeskCalendar(context, cached, isSquare = null, options = newOptions),
            )
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

/**
 * 5. ودجت التقويم المكتبي المربع (Legacy support for existing placed instances)
 */
@AndroidEntryPoint
class DeskCalendarSquareWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                appWidgetManager.updateAppWidget(
                    id,
                    ModernWidgetsRenderer.buildDeskCalendar(context, cached, isSquare = true, options = options),
                )
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
        val cached = NateegaWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                ModernWidgetsRenderer.buildDeskCalendar(context, cached, isSquare = true, options = newOptions),
            )
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

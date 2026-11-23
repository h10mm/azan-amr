package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NateegaWidgetEntryPoint {
    fun widgetUpdater(): WidgetUpdater
}

abstract class BaseNateegaWidgetProvider(
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
                    NateegaWidgetRenderer.build(context, dataToRender, options),
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
                NateegaWidgetRenderer.build(context, dataToRender, newOptions),
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

/** General / Customizable Nateega widget (follows app settings or configured theme) */
class NateegaWidget : BaseNateegaWidgetProvider()

/** Egyptian Wall Calendar - Classic Paper (ورق كلاسيكي) */
class NateegaClassicWidget : BaseNateegaWidgetProvider(ThemeColor.ClassicLight)

/** Egyptian Wall Calendar - AMOLED True Black (أسود فاحم) */
class NateegaAmoledWidget : BaseNateegaWidgetProvider(ThemeColor.Dark)

/** Egyptian Wall Calendar - Midnight Blue (أزرق منتصف الليل) */
class NateegaMidnightWidget : BaseNateegaWidgetProvider(ThemeColor.MidnightBlue)

package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import java.util.Locale

object CompactPrayerWidgetRenderer {

    private val prayerSlots = intArrayOf(
        R.id.compact_slot_fajr,
        R.id.compact_slot_sunrise,
        R.id.compact_slot_dhuhr,
        R.id.compact_slot_asr,
        R.id.compact_slot_maghrib,
        R.id.compact_slot_isha,
    )

    private val prayerLabels = intArrayOf(
        R.id.compact_label_fajr,
        R.id.compact_label_sunrise,
        R.id.compact_label_dhuhr,
        R.id.compact_label_asr,
        R.id.compact_label_maghrib,
        R.id.compact_label_isha,
    )

    fun build(context: Context, data: NateegaWidgetData, widgetOptions: Bundle? = null): RemoteViews {
        val layoutRes = when (data.themeColor) {
            ThemeColor.Dark -> R.layout.widget_compact_prayer_amoled
            ThemeColor.MidnightBlue -> R.layout.widget_compact_prayer_midnight
            ThemeColor.ClassicLight -> R.layout.widget_compact_prayer_classic
        }

        val activeSlotDrawable = when (data.themeColor) {
            ThemeColor.Dark -> R.drawable.nateega_slot_active_amoled
            ThemeColor.MidnightBlue -> R.drawable.nateega_slot_active_midnight
            ThemeColor.ClassicLight -> R.drawable.nateega_slot_active_classic
        }

        val views = RemoteViews(context.packageName, layoutRes)

        // Day and Month only (No year, no extra type labels)
        val hijriFormatted = "${data.hijriDay} ${data.hijriMonth}"
        val gregFormatted = "${data.gregorianDay} ${data.gregorianMonth}"

        // Date Display Configuration
        when (data.config.compactDateDisplayMode) {
            NateegaDateDisplayMode.Both -> {
                views.setViewVisibility(R.id.compact_dates_row, View.VISIBLE)
                views.setViewVisibility(R.id.compact_date_single_container, View.GONE)

                views.setTextViewText(R.id.compact_day_badge, data.dayOfWeekArabic)

                when (data.config.compactDateOrder) {
                    NateegaDateOrder.HijriRightGregorianLeft -> {
                        views.setTextViewText(R.id.compact_date_right, hijriFormatted)
                        views.setTextViewText(R.id.compact_date_left, gregFormatted)
                    }
                    NateegaDateOrder.GregorianRightHijriLeft -> {
                        views.setTextViewText(R.id.compact_date_right, gregFormatted)
                        views.setTextViewText(R.id.compact_date_left, hijriFormatted)
                    }
                }
            }
            NateegaDateDisplayMode.HijriOnly -> {
                views.setViewVisibility(R.id.compact_dates_row, View.GONE)
                views.setViewVisibility(R.id.compact_date_single_container, View.VISIBLE)
                views.setTextViewText(
                    R.id.compact_date_single_text,
                    "${data.dayOfWeekArabic}  •  $hijriFormatted"
                )
            }
            NateegaDateDisplayMode.GregorianOnly -> {
                views.setViewVisibility(R.id.compact_dates_row, View.GONE)
                views.setViewVisibility(R.id.compact_date_single_container, View.VISIBLE)
                views.setTextViewText(
                    R.id.compact_date_single_text,
                    "${data.dayOfWeekArabic}  •  $gregFormatted"
                )
            }
        }

        // Friday Highlight
        val isFriday = data.dayOfWeekArabic.contains("الجمعة")
        if (isFriday) {
            val fridayColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF059669.toInt() // Rich Emerald Green on Light
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFF34D399.toInt() // Emerald Green on Dark
            }
            views.setTextColor(R.id.compact_day_badge, fridayColor)
            views.setTextColor(R.id.compact_date_right, fridayColor)
            views.setTextColor(R.id.compact_date_left, fridayColor)
            views.setTextColor(R.id.compact_date_single_text, fridayColor)
        } else {
            val defaultColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF1E3A8A.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFFFFFFFF.toInt()
            }
            views.setTextColor(R.id.compact_day_badge, defaultColor)
            views.setTextColor(R.id.compact_date_right, defaultColor)
            views.setTextColor(R.id.compact_date_left, defaultColor)
            views.setTextColor(R.id.compact_date_single_text, defaultColor)
        }

        // Countdown & Progress
        val configLocale = Locale.forLanguageTag(data.locale)
        val localized = context.createConfigurationContext(
            context.resources.configuration.apply { setLocale(configLocale) }
        ).resources

        if (data.config.compactShowCountdown) {
            views.setViewVisibility(R.id.compact_countdown_container, View.VISIBLE)
            val nextPrayerName = localized.getString(data.nextPrayer.stringRes)
            views.setTextViewText(
                R.id.compact_next_prayer_title,
                "$nextPrayerName  ${data.nextPrayerTime}"
            )

            // Chronometer color: Sky Blue in Classic for crisp contrast, Amber in Dark
            val chronometerColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF0284C7.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFFFACC15.toInt()
            }
            views.setTextColor(R.id.compact_countdown_chronometer, chronometerColor)

            val diffMillis = data.countdownTargetMillis - System.currentTimeMillis()
            if (diffMillis > 0) {
                val base = android.os.SystemClock.elapsedRealtime() + diffMillis
                views.setChronometerCountDown(R.id.compact_countdown_chronometer, true)
                val formatPattern = if (data.locale == "ar") "متبقي %s" else "Remaining %s"
                views.setChronometer(R.id.compact_countdown_chronometer, base, formatPattern, true)
            } else {
                views.setChronometerCountDown(R.id.compact_countdown_chronometer, false)
                views.setChronometer(R.id.compact_countdown_chronometer, android.os.SystemClock.elapsedRealtime(), "00:00:00", false)
            }
            views.setProgressBar(R.id.compact_progress_bar, 100, data.progressPercent, false)
        } else {
            views.setViewVisibility(R.id.compact_countdown_container, View.GONE)
        }

        // Prayer times
        views.setTextViewText(R.id.compact_time_fajr, data.fajrTime)
        views.setTextViewText(R.id.compact_time_sunrise, data.sunriseTime)
        views.setTextViewText(R.id.compact_time_dhuhr, data.dhuhrTime)
        views.setTextViewText(R.id.compact_time_asr, data.asrTime)
        views.setTextViewText(R.id.compact_time_maghrib, data.maghribTime)
        views.setTextViewText(R.id.compact_time_isha, data.ishaTime)

        // Highlight next prayer slot (Model 2: Solid card background + Emerald stroke)
        prayerSlots.forEach { views.setInt(it, "setBackgroundResource", 0) }
        val defaultLabelColor = when (data.themeColor) {
            ThemeColor.Dark -> 0xFFA3A3A3.toInt()
            ThemeColor.MidnightBlue -> 0xFF94A3B8.toInt()
            ThemeColor.ClassicLight -> 0xFF475569.toInt()
        }
        val activeLabelColor = when (data.themeColor) {
            ThemeColor.Dark,
            ThemeColor.MidnightBlue -> 0xFF34D399.toInt()
            ThemeColor.ClassicLight -> 0xFF059669.toInt()
        }
        prayerLabels.forEach { views.setTextColor(it, defaultLabelColor) }

        val activeSlotId = when (data.nextPrayer) {
            Prayer.Fajr -> R.id.compact_slot_fajr
            Prayer.Sunrise -> R.id.compact_slot_sunrise
            Prayer.Dhuhr -> R.id.compact_slot_dhuhr
            Prayer.Asr -> R.id.compact_slot_asr
            Prayer.Maghrib -> R.id.compact_slot_maghrib
            Prayer.Isha -> R.id.compact_slot_isha
            else -> null
        }
        val activeLabelId = when (data.nextPrayer) {
            Prayer.Fajr -> R.id.compact_label_fajr
            Prayer.Sunrise -> R.id.compact_label_sunrise
            Prayer.Dhuhr -> R.id.compact_label_dhuhr
            Prayer.Asr -> R.id.compact_label_asr
            Prayer.Maghrib -> R.id.compact_label_maghrib
            Prayer.Isha -> R.id.compact_label_isha
            else -> null
        }
        if (activeSlotId != null) {
            views.setInt(activeSlotId, "setBackgroundResource", activeSlotDrawable)
        }
        if (activeLabelId != null) {
            views.setTextColor(activeLabelId, activeLabelColor)
        }

        // Daily Wisdom (Seamless Naskh Typography, Single Line, Canvas Bitmap Centering)
        if (data.config.compactShowWisdom && data.wisdomText.isNotBlank()) {
            views.setViewVisibility(R.id.compact_wisdom_box, View.VISIBLE)

            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val minW = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val maxW = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW) ?: minW
            val minH = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val maxH = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH) ?: minH

            val widthDp = (if (isLandscape) maxW.takeIf { it > 0 } ?: minW else minW.takeIf { it > 0 } ?: maxW)
                .let { if (it > 0) it else 260 }
            val heightDp = (if (isLandscape) {
                minH.takeIf { it > 0 } ?: maxH
            } else {
                if (minH > 0 && maxH > 0) ((minH + maxH) / 2) else maxH.takeIf { it > 0 } ?: minH
            }).let { if (it > 0) it else 135 }

            val hScale = (heightDp.toFloat() / 135f).coerceIn(0.70f, 1.25f)
            val wisdomColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF1E3A8A.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFFFFFFFF.toInt()
            }

            val wisdomBitmap = WisdomBitmapRenderer.renderWisdomBitmap(
                context = context,
                rawText = data.wisdomText,
                widthDp = (widthDp - 12).coerceAtLeast(130),
                textColor = wisdomColor,
                rowHeightDp = (20f * hScale).toInt().coerceIn(15, 28),
                maxFontSp = (14.5f * hScale).coerceIn(11.5f, 17.5f),
                minFontSp = 9.5f,
            )

            if (wisdomBitmap != null) {
                views.setImageViewBitmap(R.id.compact_wisdom, wisdomBitmap)
            } else {
                views.setViewVisibility(R.id.compact_wisdom_box, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.compact_wisdom_box, View.GONE)
        }

        // Interactive Click PendingIntents
        val homePendingIntent = launchPendingIntent(context, null, 200)
        views.setOnClickPendingIntent(R.id.compact_root, homePendingIntent)
        views.setOnClickPendingIntent(R.id.compact_dates_row, homePendingIntent)
        views.setOnClickPendingIntent(R.id.compact_date_single_container, homePendingIntent)

        val monthlyPendingIntent = launchPendingIntent(context, "al-azan://MonthlyView".toUri(), 201)
        views.setOnClickPendingIntent(R.id.compact_prayer_table, monthlyPendingIntent)
        views.setOnClickPendingIntent(R.id.compact_countdown_section, monthlyPendingIntent)

        val calendarPendingIntent = launchPendingIntent(context, "al-azan://CalendarView".toUri(), 202)
        views.setOnClickPendingIntent(R.id.compact_date_right, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.compact_date_left, calendarPendingIntent)

        return views
    }

    private fun launchPendingIntent(context: Context, dataUri: Uri?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            dataUri?.let { data = it }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

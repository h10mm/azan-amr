package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaClockPosition
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import kotlin.math.roundToInt

object NateegaWidgetRenderer {

    private val prayerSlots = intArrayOf(
        R.id.nateega_slot_fajr,
        R.id.nateega_slot_sunrise,
        R.id.nateega_slot_dhuhr,
        R.id.nateega_slot_asr,
        R.id.nateega_slot_maghrib,
        R.id.nateega_slot_isha,
    )

    private val prayerLabels = intArrayOf(
        R.id.nateega_label_fajr,
        R.id.nateega_label_sunrise,
        R.id.nateega_label_dhuhr,
        R.id.nateega_label_asr,
        R.id.nateega_label_maghrib,
        R.id.nateega_label_isha,
    )

    private val prayerTimes = intArrayOf(
        R.id.nateega_time_fajr,
        R.id.nateega_time_sunrise,
        R.id.nateega_time_dhuhr,
        R.id.nateega_time_asr,
        R.id.nateega_time_maghrib,
        R.id.nateega_time_isha,
    )

    fun build(context: Context, data: NateegaWidgetData, widgetOptions: Bundle? = null): RemoteViews {
        val localized = context.withAppLocale(data.locale)

        val layoutRes = when (data.themeColor) {
            ThemeColor.Dark -> R.layout.widget_nateega_amoled
            ThemeColor.MidnightBlue -> R.layout.widget_nateega_midnight
            ThemeColor.ClassicLight -> R.layout.widget_nateega_classic
        }

        val activeSlotDrawable = when (data.themeColor) {
            ThemeColor.Dark -> R.drawable.nateega_slot_active_amoled
            ThemeColor.MidnightBlue -> R.drawable.nateega_slot_active_midnight
            ThemeColor.ClassicLight -> R.drawable.nateega_slot_active_classic
        }

        val views = RemoteViews(context.packageName, layoutRes)

        // 1. Resolve accurate widget dimensions (dp)
        // Android AppWidgetManager contract:
        // Portrait: width = MIN_WIDTH, height = MAX_HEIGHT
        // Landscape: width = MAX_WIDTH, height = MIN_HEIGHT
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val minW = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val maxW = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW) ?: minW
        val minH = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val maxH = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH) ?: minH

        val widthDp = (if (isLandscape) maxW.takeIf { it > 0 } ?: minW else minW.takeIf { it > 0 } ?: maxW)
            .let { if (it > 0) it else 185 }
        val heightDp = (if (isLandscape) minH.takeIf { it > 0 } ?: maxH else maxH.takeIf { it > 0 } ?: minH)
            .let { if (it > 0) it else 210 }

        // 2. Determine section visibilities & calculate balanced scale factors
        // Baseline (scale = 1.0f) matches the classic 2x3 card (~175dp x 205dp) with full original bold sizes.
        // Only shrinks below 1.0f when widget is resized smaller than baseline; grows gently when expanded.
        val isSplitClock = data.config.clockPosition == NateegaClockPosition.Side ||
            data.config.clockPosition == NateegaClockPosition.InlineWithDay
        val isCenterClock = data.config.clockPosition == NateegaClockPosition.CenterLarge
        val showEnglishDay = data.dayOfWeekEnglish.isNotBlank() && heightDp >= 145
        val showCenterSubRow = !isSplitClock && (isCenterClock || showEnglishDay)
        val showCoptic = data.config.showCopticDate && data.copticDate.isNotBlank()
        val showGregSummary = data.gregorianDateSummary.isNotBlank() && heightDp >= 170
        val showSubBar = showCoptic || showGregSummary
        val showCountdown = data.config.showCountdown
        val showWisdom = data.config.showWisdom && data.wisdomText.isNotBlank()

        var baselineHeightDp = 132f // Header day + Top divider + Dates row + Prayer table + paddings
        if (showCenterSubRow || (isSplitClock && showEnglishDay)) baselineHeightDp += 16f
        if (showCoptic) baselineHeightDp += 19f
        if (showGregSummary) baselineHeightDp += 12f
        if (showCountdown) baselineHeightDp += 20f
        if (showWisdom) baselineHeightDp += 22f

        val heightScale = if (heightDp >= baselineHeightDp) {
            1.0f + ((heightDp - baselineHeightDp) / 160f).coerceIn(0f, 0.22f)
        } else {
            (heightDp.toFloat() / baselineHeightDp).coerceIn(0.70f, 1.0f)
        }

        val widthScale = if (widthDp >= 175) {
            1.0f + ((widthDp - 175f) / 340f).coerceIn(0f, 0.24f)
        } else {
            (widthDp.toFloat() / 175f).coerceIn(0.75f, 1.0f)
        }

        val mainScale = minOf(heightScale, widthScale * 1.15f).coerceIn(0.70f, 1.24f)
        val colScale = minOf(heightScale, widthScale).coerceIn(0.70f, 1.22f)
        val tableScale = minOf(heightScale, widthScale).coerceIn(0.70f, 1.22f)

        // 3. Apply proportional container paddings
        val density = context.resources.displayMetrics.density
        fun dpToPx(dp: Float): Int = (dp * density).roundToInt()

        val rootPadH = dpToPx(4f * widthScale.coerceIn(0.8f, 1.2f))
        val rootPadTop = dpToPx(3f * heightScale.coerceIn(0.6f, 1.2f))
        val rootPadBot = dpToPx(2f * heightScale.coerceIn(0.6f, 1.2f))
        views.setViewPadding(R.id.nateega_root, rootPadH, rootPadTop, rootPadH, rootPadBot)

        val copticPadH = dpToPx(12f * colScale.coerceIn(0.75f, 1.25f))
        val copticPadV = dpToPx(2f * heightScale.coerceIn(0.55f, 1.2f))
        views.setViewPadding(R.id.nateega_coptic_box, copticPadH, copticPadV, copticPadH, copticPadV)

        val tablePadH = dpToPx(2f)
        val tablePadV = dpToPx(2.5f * heightScale.coerceIn(0.55f, 1.2f))
        views.setViewPadding(R.id.nateega_prayer_table, tablePadH, tablePadV, tablePadH, tablePadV)

        val slotPadH = 0
        val slotPadV = dpToPx(2f * heightScale.coerceIn(0.55f, 1.2f))
        for (slotId in prayerSlots) {
            views.setViewPadding(slotId, slotPadH, slotPadV, slotPadH, slotPadV)
        }

        // 4. Apply full original bold font sizes scaled harmoniously
        views.setTextViewTextSize(R.id.nateega_day_ar, TypedValue.COMPLEX_UNIT_SP, 25.5f * mainScale)
        views.setTextViewTextSize(R.id.nateega_day_en, TypedValue.COMPLEX_UNIT_SP, 9.5f * colScale)
        views.setTextViewTextSize(R.id.nateega_clock_center, TypedValue.COMPLEX_UNIT_SP, 10.5f * colScale)
        views.setTextViewTextSize(R.id.nateega_day_ar_split, TypedValue.COMPLEX_UNIT_SP, 22.0f * colScale)
        views.setTextViewTextSize(R.id.nateega_day_en_split, TypedValue.COMPLEX_UNIT_SP, 9.0f * colScale)
        views.setTextViewTextSize(R.id.nateega_clock_split, TypedValue.COMPLEX_UNIT_SP, 24.0f * colScale)

        val dateNumberSp = 34.5f * mainScale
        val dateMonthSp = 13.0f * colScale
        val dateYearSp = 11.0f * mainScale
        views.setTextViewTextSize(R.id.nateega_greg_day, TypedValue.COMPLEX_UNIT_SP, dateNumberSp)
        views.setTextViewTextSize(R.id.nateega_hijri_day, TypedValue.COMPLEX_UNIT_SP, dateNumberSp)
        views.setTextViewTextSize(R.id.nateega_greg_month, TypedValue.COMPLEX_UNIT_SP, dateMonthSp)
        views.setTextViewTextSize(R.id.nateega_hijri_month, TypedValue.COMPLEX_UNIT_SP, dateMonthSp)
        views.setTextViewTextSize(R.id.nateega_greg_year, TypedValue.COMPLEX_UNIT_SP, dateYearSp)
        views.setTextViewTextSize(R.id.nateega_hijri_year, TypedValue.COMPLEX_UNIT_SP, dateYearSp)

        views.setTextViewTextSize(R.id.nateega_coptic_date, TypedValue.COMPLEX_UNIT_SP, 11.0f * mainScale)
        views.setTextViewTextSize(R.id.nateega_greg_summary, TypedValue.COMPLEX_UNIT_SP, 9.5f * colScale)

        views.setTextViewTextSize(R.id.nateega_next_prayer_title, TypedValue.COMPLEX_UNIT_SP, 11.0f * colScale)
        views.setTextViewTextSize(R.id.nateega_countdown_chronometer, TypedValue.COMPLEX_UNIT_SP, 11.0f * colScale)

        val prayerLabelSp = (9.5f * tableScale).coerceAtLeast(7.0f)
        val prayerTimeSp = (10.5f * tableScale).coerceAtLeast(8.0f)
        for (labelId in prayerLabels) {
            views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, prayerLabelSp)
        }
        for (timeId in prayerTimes) {
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, prayerTimeSp)
        }

        // 5. Top bar & Clock configuration
        when (data.config.clockPosition) {
            NateegaClockPosition.CenterLarge -> {
                views.setViewVisibility(R.id.nateega_clock_center_container, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_clock_center, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_split_top_bar, View.GONE)
            }
            NateegaClockPosition.Side,
            NateegaClockPosition.InlineWithDay -> {
                views.setViewVisibility(R.id.nateega_clock_center_container, View.GONE)
                views.setViewVisibility(R.id.nateega_split_top_bar, View.VISIBLE)
            }
            NateegaClockPosition.Hidden -> {
                views.setViewVisibility(R.id.nateega_clock_center_container, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_clock_center, View.GONE)
                views.setViewVisibility(R.id.nateega_split_top_bar, View.GONE)
            }
        }
        views.setViewVisibility(
            R.id.nateega_center_sub_row,
            if (showCenterSubRow) View.VISIBLE else View.GONE
        )

        // Day of week (Center & Split modes)
        if (data.dayOfWeekArabic.isNotBlank()) {
            views.setViewVisibility(R.id.nateega_day_ar, View.VISIBLE)
            views.setTextViewText(R.id.nateega_day_ar, data.dayOfWeekArabic)
            views.setViewVisibility(R.id.nateega_day_ar_split, View.VISIBLE)
            views.setTextViewText(R.id.nateega_day_ar_split, data.dayOfWeekArabic)
        } else {
            views.setViewVisibility(R.id.nateega_day_ar, View.GONE)
            views.setViewVisibility(R.id.nateega_day_ar_split, View.GONE)
        }

        if (showEnglishDay) {
            views.setViewVisibility(R.id.nateega_day_en, View.VISIBLE)
            views.setTextViewText(R.id.nateega_day_en, data.dayOfWeekEnglish)
            views.setViewVisibility(R.id.nateega_day_en_split, View.VISIBLE)
            views.setTextViewText(R.id.nateega_day_en_split, data.dayOfWeekEnglish)
        } else {
            views.setViewVisibility(R.id.nateega_day_en, View.GONE)
            views.setViewVisibility(R.id.nateega_day_en_split, View.GONE)
        }

        // 6. Gregorian & Hijri Dates (respecting dateDisplayMode)
        views.setTextViewText(R.id.nateega_greg_day, data.gregorianDay)
        views.setTextViewText(R.id.nateega_greg_month, data.gregorianMonth)
        views.setTextViewText(R.id.nateega_greg_year, data.gregorianYear)

        views.setTextViewText(R.id.nateega_hijri_day, data.hijriDay)
        views.setTextViewText(R.id.nateega_hijri_month, data.hijriMonth)
        views.setTextViewText(R.id.nateega_hijri_year, data.hijriYear)

        when (data.config.dateDisplayMode) {
            NateegaDateDisplayMode.Both -> {
                views.setViewVisibility(R.id.nateega_greg_col, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_hijri_col, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_dates_divider, View.VISIBLE)
            }
            NateegaDateDisplayMode.HijriOnly -> {
                views.setViewVisibility(R.id.nateega_greg_col, View.GONE)
                views.setViewVisibility(R.id.nateega_hijri_col, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_dates_divider, View.GONE)
            }
            NateegaDateDisplayMode.GregorianOnly -> {
                views.setViewVisibility(R.id.nateega_greg_col, View.VISIBLE)
                views.setViewVisibility(R.id.nateega_hijri_col, View.GONE)
                views.setViewVisibility(R.id.nateega_dates_divider, View.GONE)
            }
        }

        // Day & Full Date Styling: On Friday, highlight Day, Numbers, Month, and Year in Green
        val isFriday = data.dayOfWeekArabic.contains("الجمعة")
        if (isFriday) {
            val fridayColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF059669.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFF34D399.toInt()
            }
            views.setTextColor(R.id.nateega_day_ar, fridayColor)
            views.setTextColor(R.id.nateega_day_ar_split, fridayColor)
            views.setTextColor(R.id.nateega_greg_day, fridayColor)
            views.setTextColor(R.id.nateega_greg_month, fridayColor)
            views.setTextColor(R.id.nateega_greg_year, fridayColor)
            views.setTextColor(R.id.nateega_hijri_day, fridayColor)
            views.setTextColor(R.id.nateega_hijri_month, fridayColor)
            views.setTextColor(R.id.nateega_hijri_year, fridayColor)
        } else {
            val defaultColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF1E3A8A.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFFFFFFFF.toInt()
            }
            val defaultYearColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF64748B.toInt()
                ThemeColor.Dark -> 0xFF737373.toInt()
                ThemeColor.MidnightBlue -> 0xFF64748B.toInt()
            }
            views.setTextColor(R.id.nateega_day_ar, defaultColor)
            views.setTextColor(R.id.nateega_day_ar_split, defaultColor)
            views.setTextColor(R.id.nateega_greg_day, defaultColor)
            views.setTextColor(R.id.nateega_greg_month, defaultColor)
            views.setTextColor(R.id.nateega_greg_year, defaultYearColor)
            views.setTextColor(R.id.nateega_hijri_day, defaultColor)
            views.setTextColor(R.id.nateega_hijri_month, defaultColor)
            views.setTextColor(R.id.nateega_hijri_year, defaultYearColor)
        }

        // 7. Sub Bar: Centered Coptic Box & Gregorian Summary
        views.setViewVisibility(R.id.nateega_sub_bar, if (showSubBar) View.VISIBLE else View.GONE)
        if (showCoptic) {
            views.setViewVisibility(R.id.nateega_coptic_box, View.VISIBLE)
            views.setTextViewText(R.id.nateega_coptic_date, data.copticDate)
        } else {
            views.setViewVisibility(R.id.nateega_coptic_box, View.GONE)
        }

        if (showGregSummary) {
            views.setViewVisibility(R.id.nateega_greg_summary, View.VISIBLE)
            views.setTextViewText(R.id.nateega_greg_summary, data.gregorianDateSummary)
        } else {
            views.setViewVisibility(R.id.nateega_greg_summary, View.GONE)
        }

        // 8. Countdown & Progress
        if (showCountdown) {
            views.setViewVisibility(R.id.nateega_countdown_container, View.VISIBLE)
            val nextPrayerName = localized.getString(data.nextPrayer.stringRes)
            views.setTextViewText(
                R.id.nateega_next_prayer_title,
                "$nextPrayerName  ${data.nextPrayerTime}"
            )

            val diffMillis = data.countdownTargetMillis - System.currentTimeMillis()
            if (diffMillis > 0) {
                val base = android.os.SystemClock.elapsedRealtime() + diffMillis
                views.setChronometerCountDown(R.id.nateega_countdown_chronometer, true)
                val formatPattern = if (data.locale == "ar") "متبقي %s" else "Remaining %s"
                views.setChronometer(R.id.nateega_countdown_chronometer, base, formatPattern, true)
            } else {
                views.setChronometerCountDown(R.id.nateega_countdown_chronometer, false)
                views.setChronometer(R.id.nateega_countdown_chronometer, android.os.SystemClock.elapsedRealtime(), "00:00:00", false)
            }
            views.setProgressBar(R.id.nateega_progress_bar, 100, data.progressPercent, false)
        } else {
            views.setViewVisibility(R.id.nateega_countdown_container, View.GONE)
        }

        // 9. Prayer times
        views.setTextViewText(R.id.nateega_time_fajr, data.fajrTime)
        views.setTextViewText(R.id.nateega_time_sunrise, data.sunriseTime)
        views.setTextViewText(R.id.nateega_time_dhuhr, data.dhuhrTime)
        views.setTextViewText(R.id.nateega_time_asr, data.asrTime)
        views.setTextViewText(R.id.nateega_time_maghrib, data.maghribTime)
        views.setTextViewText(R.id.nateega_time_isha, data.ishaTime)

        // Highlight next prayer slot
        prayerSlots.forEach { views.setInt(it, "setBackgroundResource", 0) }
        val defaultLabelColor = when (data.themeColor) {
            ThemeColor.Dark -> 0xFFA3A3A3.toInt()
            ThemeColor.MidnightBlue -> 0xFF94A3B8.toInt()
            ThemeColor.ClassicLight -> 0xFF475569.toInt()
        }
        val activeLabelColor = when (data.themeColor) {
            ThemeColor.Dark, ThemeColor.MidnightBlue -> 0xFF34D399.toInt()
            ThemeColor.ClassicLight -> 0xFF059669.toInt()
        }
        prayerLabels.forEach { views.setTextColor(it, defaultLabelColor) }

        val activeSlotId = when (data.nextPrayer) {
            Prayer.Fajr -> R.id.nateega_slot_fajr
            Prayer.Sunrise -> R.id.nateega_slot_sunrise
            Prayer.Dhuhr -> R.id.nateega_slot_dhuhr
            Prayer.Asr -> R.id.nateega_slot_asr
            Prayer.Maghrib -> R.id.nateega_slot_maghrib
            Prayer.Isha -> R.id.nateega_slot_isha
            else -> null
        }
        val activeLabelId = when (data.nextPrayer) {
            Prayer.Fajr -> R.id.nateega_label_fajr
            Prayer.Sunrise -> R.id.nateega_label_sunrise
            Prayer.Dhuhr -> R.id.nateega_label_dhuhr
            Prayer.Asr -> R.id.nateega_label_asr
            Prayer.Maghrib -> R.id.nateega_label_maghrib
            Prayer.Isha -> R.id.nateega_label_isha
            else -> null
        }
        if (activeSlotId != null) {
            views.setInt(activeSlotId, "setBackgroundResource", activeSlotDrawable)
        }
        if (activeLabelId != null) {
            views.setTextColor(activeLabelId, activeLabelColor)
        }

        // 10. Daily Wisdom (Seamless Naskh Typography, Single Line, Canvas Bitmap Centering)
        if (showWisdom) {
            views.setViewVisibility(R.id.nateega_wisdom_box, View.VISIBLE)

            val wisdomColor = when (data.themeColor) {
                ThemeColor.ClassicLight -> 0xFF1E3A8A.toInt()
                ThemeColor.Dark,
                ThemeColor.MidnightBlue -> 0xFFFFFFFF.toInt()
            }

            val wisdomInnerWidthDp = (widthDp - 12).coerceAtLeast(125)
            val wisdomRowHeightDp = (23f * mainScale).roundToInt().coerceIn(16, 34)
            val wisdomMaxSp = (16.5f * mainScale).coerceIn(12.0f, 19.5f)
            // Keep min readable floor constant so expanding vertically NEVER forces premature truncation
            val wisdomMinSp = 10.0f

            val wisdomBitmap = WisdomBitmapRenderer.renderWisdomBitmap(
                context = context,
                rawText = data.wisdomText,
                widthDp = wisdomInnerWidthDp,
                textColor = wisdomColor,
                rowHeightDp = wisdomRowHeightDp,
                maxFontSp = wisdomMaxSp,
                minFontSp = wisdomMinSp,
            )

            if (wisdomBitmap != null) {
                views.setImageViewBitmap(R.id.nateega_wisdom, wisdomBitmap)
            } else {
                views.setViewVisibility(R.id.nateega_wisdom_box, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.nateega_wisdom_box, View.GONE)
        }

        // 11. Interactive Click PendingIntents:
        val homePendingIntent = launchPendingIntent(context, null, 100)
        views.setOnClickPendingIntent(R.id.nateega_root, homePendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_clock_center, homePendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_clock_split, homePendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_day_ar, homePendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_day_ar_split, homePendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_split_top_bar, homePendingIntent)

        val monthlyPendingIntent = launchPendingIntent(context, "al-azan://MonthlyView".toUri(), 101)
        views.setOnClickPendingIntent(R.id.nateega_prayer_table, monthlyPendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_countdown_section, monthlyPendingIntent)

        val calendarPendingIntent = launchPendingIntent(context, "al-azan://CalendarView".toUri(), 102)
        views.setOnClickPendingIntent(R.id.nateega_hijri_col, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_greg_col, calendarPendingIntent)
        views.setOnClickPendingIntent(R.id.nateega_coptic_box, calendarPendingIntent)

        return views
    }

    fun buildHint(context: Context, @StringRes messageRes: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_hint)
        views.setTextViewText(R.id.widget_hint_text, context.getString(messageRes))
        views.setOnClickPendingIntent(R.id.widget_hint_layout, launchPendingIntent(context, null, 0))
        return views
    }

    private fun launchPendingIntent(
        context: Context,
        deepLinkUri: Uri?,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (deepLinkUri != null) {
                data = deepLinkUri
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

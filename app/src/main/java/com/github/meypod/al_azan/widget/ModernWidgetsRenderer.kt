package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.core.net.toUri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.DeskCalendarType
import com.github.meypod.al_azan.core.domain.model.widget.MosqueArchThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.SlimBarDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.SlimBarThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode
import com.github.meypod.al_azan.core.domain.util.icuCalendar
import com.github.meypod.al_azan.core.presentation.navigation.Route
import java.util.Calendar

object ModernWidgetsRenderer {

    fun launchPendingIntent(context: Context, dataUri: Uri? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            dataUri?.let { data = it }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun resolveWidgetSizeDp(
        context: Context,
        options: Bundle?,
        defaultW: Float,
        defaultH: Float,
    ): Pair<Float, Float> {
        if (options == null) return defaultW to defaultH
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val w = if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        }
        val h = if (isLandscape) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        }
        return (if (w > 20) w.toFloat() else defaultW) to (if (h > 20) h.toFloat() else defaultH)
    }

    // ================= 1. الشريط المودرن النحيف (4×1) =================
    fun buildSlimBar(context: Context, data: NateegaWidgetData, options: Bundle? = null): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_slim_bar)
        val (wDp, hDp) = resolveWidgetSizeDp(context, options, defaultW = 280f, defaultH = 66f)
        val density = context.resources.displayMetrics.density
        val scale = minOf(wDp / 275f, hDp / 64f).coerceIn(0.68f, 1.75f)
        val padH = (10f * scale * density).toInt().coerceIn(6, 26)
        val padV = (4f * scale * density).toInt().coerceIn(2, 16)
        views.setViewPadding(R.id.widget_slim_bar_layout, padH, padV, padH, padV)
        views.setTextViewTextSize(R.id.slim_bar_clock, TypedValue.COMPLEX_UNIT_SP, (21f * scale).coerceIn(14f, 34f))
        views.setTextViewTextSize(R.id.slim_bar_city, TypedValue.COMPLEX_UNIT_SP, (10f * scale).coerceIn(8f, 16f))
        views.setTextViewTextSize(R.id.slim_bar_hijri, TypedValue.COMPLEX_UNIT_SP, (12.5f * scale).coerceIn(9.5f, 20f))
        views.setTextViewTextSize(R.id.slim_bar_gregorian, TypedValue.COMPLEX_UNIT_SP, (10.5f * scale).coerceIn(8.5f, 17f))
        views.setTextViewTextSize(R.id.slim_bar_prayer_name, TypedValue.COMPLEX_UNIT_SP, (11.5f * scale).coerceIn(9f, 19f))
        views.setTextViewTextSize(R.id.slim_bar_countdown, TypedValue.COMPLEX_UNIT_SP, (10.5f * scale).coerceIn(8.5f, 17f))
        val capPadH = (9f * scale * density).toInt().coerceIn(5, 20)
        val capPadV = (3f * scale * density).toInt().coerceIn(2, 10)
        views.setViewPadding(R.id.slim_bar_capsule, capPadH, capPadV, capPadH, capPadV)
        val prayerName = context.getString(data.nextPrayer.stringRes)
        val themeMode = if (data.config.slimBarThemeMode == SlimBarThemeMode.MatchAppTheme) {
            when (data.themeColor) {
                ThemeColor.ClassicLight -> SlimBarThemeMode.ClassicWhite
                ThemeColor.Dark -> SlimBarThemeMode.AmoledDark
                ThemeColor.MidnightBlue -> SlimBarThemeMode.MidnightBlue
            }
        } else {
            data.config.slimBarThemeMode
        }

        val bgRes = when (themeMode) {
            SlimBarThemeMode.MatchAppTheme,
            SlimBarThemeMode.AmoledDark -> R.drawable.widget_slim_bar_bg_amoled
            SlimBarThemeMode.MidnightBlue -> R.drawable.widget_slim_bar_bg_midnight
            SlimBarThemeMode.ClassicWhite -> R.drawable.widget_slim_bar_bg_classic
            SlimBarThemeMode.Transparent -> R.drawable.widget_slim_bar_bg_transparent
        }
        views.setInt(R.id.widget_slim_bar_layout, "setBackgroundResource", bgRes)

        val capsuleRes = when (themeMode) {
            SlimBarThemeMode.MatchAppTheme,
            SlimBarThemeMode.AmoledDark -> R.drawable.widget_slim_capsule_dark
            SlimBarThemeMode.MidnightBlue -> R.drawable.widget_slim_capsule_midnight
            SlimBarThemeMode.ClassicWhite -> R.drawable.widget_slim_capsule_classic
            SlimBarThemeMode.Transparent -> R.drawable.widget_slim_capsule_transparent
        }
        views.setInt(R.id.slim_bar_capsule, "setBackgroundResource", capsuleRes)

        when (themeMode) {
            SlimBarThemeMode.ClassicWhite -> {
                views.setTextColor(R.id.slim_bar_clock, 0xFF1E3A8A.toInt())
                views.setTextColor(R.id.slim_bar_city, 0xFF64748B.toInt())
                views.setTextColor(R.id.slim_bar_hijri, 0xFF0F172A.toInt())
                views.setTextColor(R.id.slim_bar_gregorian, 0xFF64748B.toInt())
                views.setTextColor(R.id.slim_bar_prayer_name, 0xFF065F46.toInt())
                views.setTextColor(R.id.slim_bar_countdown, 0xFF047857.toInt())
            }
            SlimBarThemeMode.MidnightBlue -> {
                views.setTextColor(R.id.slim_bar_clock, 0xFF38BDF8.toInt())
                views.setTextColor(R.id.slim_bar_city, 0xFF94A3B8.toInt())
                views.setTextColor(R.id.slim_bar_hijri, 0xFFF1F5F9.toInt())
                views.setTextColor(R.id.slim_bar_gregorian, 0xFF94A3B8.toInt())
                views.setTextColor(R.id.slim_bar_prayer_name, 0xFF38BDF8.toInt())
                views.setTextColor(R.id.slim_bar_countdown, 0xFFF59E0B.toInt())
            }
            SlimBarThemeMode.MatchAppTheme,
            SlimBarThemeMode.AmoledDark -> {
                views.setTextColor(R.id.slim_bar_clock, 0xFFFFFFFF.toInt())
                views.setTextColor(R.id.slim_bar_city, 0xFF94A3B8.toInt())
                views.setTextColor(R.id.slim_bar_hijri, 0xFFF1F5F9.toInt())
                views.setTextColor(R.id.slim_bar_gregorian, 0xFF94A3B8.toInt())
                views.setTextColor(R.id.slim_bar_prayer_name, 0xFF10B981.toInt())
                views.setTextColor(R.id.slim_bar_countdown, 0xFFF59E0B.toInt())
            }
            SlimBarThemeMode.Transparent -> {
                views.setTextColor(R.id.slim_bar_clock, 0xFFFFFFFF.toInt())
                views.setTextColor(R.id.slim_bar_city, 0xFFE2E8F0.toInt())
                views.setTextColor(R.id.slim_bar_hijri, 0xFFFFFFFF.toInt())
                views.setTextColor(R.id.slim_bar_gregorian, 0xFFE2E8F0.toInt())
                views.setTextColor(R.id.slim_bar_prayer_name, 0xFF10B981.toInt())
                views.setTextColor(R.id.slim_bar_countdown, 0xFFF59E0B.toInt())
            }
        }

        if (data.config.slimBarShowCity && data.cityName.isNotBlank()) {
            views.setViewVisibility(R.id.slim_bar_city, View.VISIBLE)
            views.setTextViewText(R.id.slim_bar_city, data.cityName)
        } else {
            views.setViewVisibility(R.id.slim_bar_city, View.GONE)
        }

        val hijriText = "${data.hijriDay} ${data.hijriMonth} ${data.hijriYear}"
        val gregorianText = "${data.dayOfWeekArabic}، ${data.gregorianDay} ${data.gregorianMonth}"
        if (data.config.slimBarDateOrder == SlimBarDateOrder.HijriFirst) {
            views.setTextViewText(R.id.slim_bar_hijri, hijriText)
            views.setTextViewText(R.id.slim_bar_gregorian, gregorianText)
        } else {
            views.setTextViewText(R.id.slim_bar_hijri, gregorianText)
            views.setTextViewText(R.id.slim_bar_gregorian, hijriText)
        }

        views.setTextViewText(R.id.slim_bar_prayer_name, prayerName)

        val diff = data.countdownTargetMillis - System.currentTimeMillis()
        val base = SystemClock.elapsedRealtime() + diff
        if (diff > 0) {
            views.setChronometerCountDown(R.id.slim_bar_countdown, true)
            views.setChronometer(R.id.slim_bar_countdown, base, null, true)
        } else {
            views.setChronometerCountDown(R.id.slim_bar_countdown, false)
            views.setChronometer(R.id.slim_bar_countdown, SystemClock.elapsedRealtime(), "00:00:00", false)
        }

        views.setOnClickPendingIntent(R.id.widget_slim_bar_layout, launchPendingIntent(context))
        return views
    }

    // ================= 2. محراب المسجد والقباب التراثي (4×2) =================
    fun buildMosqueArch(context: Context, data: NateegaWidgetData, options: Bundle? = null): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_mosque_arch)
        val (wDp, hDp) = resolveWidgetSizeDp(context, options, defaultW = 280f, defaultH = 150f)
        val density = context.resources.displayMetrics.density
        val scale = minOf(wDp / 275f, hDp / 145f).coerceIn(0.68f, 1.75f)
        val padH = (10f * scale * density).toInt().coerceIn(6, 24)
        val padV = (7f * scale * density).toInt().coerceIn(4, 20)
        views.setViewPadding(R.id.widget_mosque_arch_layout, padH, padV, padH, padV)
        views.setTextViewTextSize(R.id.mosque_arch_ayah, TypedValue.COMPLEX_UNIT_SP, (11.5f * scale).coerceIn(8.5f, 19f))
        views.setTextViewTextSize(R.id.mosque_arch_next_label, TypedValue.COMPLEX_UNIT_SP, (11f * scale).coerceIn(8.5f, 18f))
        views.setTextViewTextSize(R.id.mosque_arch_next_time, TypedValue.COMPLEX_UNIT_SP, (24f * scale).coerceIn(16f, 40f))
        views.setTextViewTextSize(R.id.mosque_arch_countdown, TypedValue.COMPLEX_UNIT_SP, (11f * scale).coerceIn(8.5f, 18f))
        val slotLabelSp = (9.5f * scale).coerceIn(7.5f, 15f)
        val slotTimeSp = (10.5f * scale).coerceIn(8.0f, 16.5f)
        listOf(
            R.id.mosque_arch_label_fajr to R.id.mosque_arch_time_fajr,
            R.id.mosque_arch_label_sunrise to R.id.mosque_arch_time_sunrise,
            R.id.mosque_arch_label_dhuhr to R.id.mosque_arch_time_dhuhr,
            R.id.mosque_arch_label_asr to R.id.mosque_arch_time_asr,
            R.id.mosque_arch_label_maghrib to R.id.mosque_arch_time_maghrib,
            R.id.mosque_arch_label_isha to R.id.mosque_arch_time_isha,
        ).forEach { (lblId, timeId) ->
            views.setTextViewTextSize(lblId, TypedValue.COMPLEX_UNIT_SP, slotLabelSp)
            views.setTextViewTextSize(timeId, TypedValue.COMPLEX_UNIT_SP, slotTimeSp)
        }
        val prayerName = context.getString(data.nextPrayer.stringRes)
        views.setTextViewText(R.id.mosque_arch_next_label, "الصلاة القادمة: $prayerName")
        views.setTextViewText(R.id.mosque_arch_next_time, data.nextPrayerTime)

        val diff = data.countdownTargetMillis - System.currentTimeMillis()
        val base = SystemClock.elapsedRealtime() + diff
        if (diff > 0) {
            views.setChronometerCountDown(R.id.mosque_arch_countdown, true)
            views.setChronometer(R.id.mosque_arch_countdown, base, null, true)
        } else {
            views.setChronometerCountDown(R.id.mosque_arch_countdown, false)
            views.setChronometer(R.id.mosque_arch_countdown, SystemClock.elapsedRealtime(), "00:00:00", false)
        }

        // Apply theme styling
        val theme = if (data.config.mosqueArchThemeMode == MosqueArchThemeMode.MatchAppTheme) {
            when (data.themeColor) {
                ThemeColor.ClassicLight -> MosqueArchThemeMode.ClassicWhite
                ThemeColor.Dark -> MosqueArchThemeMode.AmoledDark
                ThemeColor.MidnightBlue -> MosqueArchThemeMode.MidnightBlue
            }
        } else {
            data.config.mosqueArchThemeMode
        }
        val (bgRes, tableBgRes, activeSlotRes) = when (theme) {
            MosqueArchThemeMode.Transparent -> Triple(
                R.drawable.widget_square_bg_transparent,
                R.drawable.widget_table_bg_transparent,
                R.drawable.nateega_slot_active_midnight,
            )
            MosqueArchThemeMode.MatchAppTheme,
            MosqueArchThemeMode.AmoledDark -> Triple(
                R.drawable.widget_square_bg_amoled,
                R.drawable.nateega_table_bg_dark,
                R.drawable.nateega_slot_active_amoled,
            )
            MosqueArchThemeMode.MidnightBlue -> Triple(
                R.drawable.widget_square_bg_midnight,
                R.drawable.nateega_table_bg_midnight,
                R.drawable.nateega_slot_active_midnight,
            )
            MosqueArchThemeMode.ClassicWhite -> Triple(
                R.drawable.widget_square_bg_classic,
                R.drawable.nateega_table_bg_classic,
                R.drawable.nateega_slot_active_classic,
            )
        }

        views.setInt(R.id.widget_mosque_arch_layout, "setBackgroundResource", bgRes)
        views.setInt(R.id.mosque_arch_prayers_row, "setBackgroundResource", tableBgRes)

        val ayahColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFFB45309.toInt() // Dark Amber
            else -> 0xFFF59E0B.toInt() // Amber
        }
        val labelColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFF64748B.toInt()
            else -> 0xFF94A3B8.toInt()
        }
        val nextTimeColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFF0284C7.toInt()
            else -> 0xFF38BDF8.toInt()
        }
        val slotNormalLabelColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFF64748B.toInt()
            MosqueArchThemeMode.AmoledDark -> 0xFF71717A.toInt()
            else -> 0xFF94A3B8.toInt()
        }
        val slotNormalTimeColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFF0F172A.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
        val slotActiveColor = when (theme) {
            MosqueArchThemeMode.ClassicWhite -> 0xFF059669.toInt()
            else -> 0xFF34D399.toInt()
        }

        views.setTextColor(R.id.mosque_arch_ayah, ayahColor)
        views.setTextColor(R.id.mosque_arch_next_label, labelColor)
        views.setTextColor(R.id.mosque_arch_next_time, nextTimeColor)

        if (theme == MosqueArchThemeMode.ClassicWhite) {
            views.setInt(R.id.mosque_arch_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill_light)
            views.setTextColor(R.id.mosque_arch_countdown, 0xFF059669.toInt())
        } else {
            views.setInt(R.id.mosque_arch_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill)
            views.setTextColor(R.id.mosque_arch_countdown, 0xFF34D399.toInt())
        }

        // Times text
        views.setTextViewText(R.id.mosque_arch_time_fajr, data.fajrTime)
        views.setTextViewText(R.id.mosque_arch_time_sunrise, data.sunriseTime)
        views.setTextViewText(R.id.mosque_arch_time_dhuhr, data.dhuhrTime)
        views.setTextViewText(R.id.mosque_arch_time_asr, data.asrTime)
        views.setTextViewText(R.id.mosque_arch_time_maghrib, data.maghribTime)
        views.setTextViewText(R.id.mosque_arch_time_isha, data.ishaTime)

        // Reset and highlight active slot for next prayer
        val slots = listOf(
            Triple(Prayer.Fajr, R.id.mosque_arch_slot_fajr, Pair(R.id.mosque_arch_label_fajr, R.id.mosque_arch_time_fajr)),
            Triple(Prayer.Sunrise, R.id.mosque_arch_slot_sunrise, Pair(R.id.mosque_arch_label_sunrise, R.id.mosque_arch_time_sunrise)),
            Triple(Prayer.Dhuhr, R.id.mosque_arch_slot_dhuhr, Pair(R.id.mosque_arch_label_dhuhr, R.id.mosque_arch_time_dhuhr)),
            Triple(Prayer.Asr, R.id.mosque_arch_slot_asr, Pair(R.id.mosque_arch_label_asr, R.id.mosque_arch_time_asr)),
            Triple(Prayer.Maghrib, R.id.mosque_arch_slot_maghrib, Pair(R.id.mosque_arch_label_maghrib, R.id.mosque_arch_time_maghrib)),
            Triple(Prayer.Isha, R.id.mosque_arch_slot_isha, Pair(R.id.mosque_arch_label_isha, R.id.mosque_arch_time_isha)),
        )

        for ((prayer, slotId, textIds) in slots) {
            val isActive = (data.nextPrayer == prayer)
            if (isActive) {
                views.setInt(slotId, "setBackgroundResource", activeSlotRes)
                views.setTextColor(textIds.first, slotActiveColor)
                views.setTextColor(textIds.second, slotActiveColor)
            } else {
                views.setInt(slotId, "setBackgroundResource", 0)
                views.setTextColor(textIds.first, slotNormalLabelColor)
                views.setTextColor(textIds.second, slotNormalTimeColor)
            }
        }

        views.setOnClickPendingIntent(R.id.widget_mosque_arch_layout, launchPendingIntent(context))
        return views
    }

    // ================= 3. الكبسولة الذكية التفاعلية (2×1) =================
    private data class PillThemeColors(
        val bgRes: Int,
        val titleColor: Int,
        val subColor: Int,
        val badgeColor: Int,
    )

    fun buildSmartPill(context: Context, data: NateegaWidgetData, options: Bundle? = null): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_smart_pill)
        val (wDp, hDp) = resolveWidgetSizeDp(context, options, defaultW = 220f, defaultH = 62f)
        val density = context.resources.displayMetrics.density
        val scale = minOf(wDp / 210f, hDp / 60f).coerceIn(0.72f, 1.75f)
        val padH = (12f * scale * density).toInt().coerceIn(8, 28)
        val padV = (5f * scale * density).toInt().coerceIn(3, 18)
        views.setViewPadding(R.id.widget_smart_pill_layout, padH, padV, padH, padV)
        views.setTextViewTextSize(R.id.smart_pill_title, TypedValue.COMPLEX_UNIT_SP, (12.5f * scale).coerceIn(9.5f, 22f))
        views.setTextViewTextSize(R.id.smart_pill_status_badge, TypedValue.COMPLEX_UNIT_SP, (9.5f * scale).coerceIn(8f, 16f))
        views.setTextViewTextSize(R.id.smart_pill_subtitle, TypedValue.COMPLEX_UNIT_SP, (10f * scale).coerceIn(8f, 17f))
        views.setTextViewTextSize(R.id.smart_pill_word_tag, TypedValue.COMPLEX_UNIT_SP, (12f * scale).coerceIn(9f, 20f))
        val config = data.config
        val theme = if (config.smartPillThemeMode == SmartPillThemeMode.MatchAppTheme) {
            when (data.themeColor) {
                ThemeColor.ClassicLight -> SmartPillThemeMode.ClassicWhite
                ThemeColor.Dark -> SmartPillThemeMode.AmoledDark
                ThemeColor.MidnightBlue -> SmartPillThemeMode.MidnightBlue
            }
        } else {
            config.smartPillThemeMode
        }
        val showWordBadge = config.smartPillShowWordBadge

        val contextInfo = SpiritualContextResolver.resolve(data)

        // Bind text
        views.setTextViewText(R.id.smart_pill_word_tag, contextInfo.wordTag)
        views.setTextViewText(R.id.smart_pill_title, contextInfo.title)
        views.setTextViewText(R.id.smart_pill_status_badge, contextInfo.badge)
        views.setTextViewText(R.id.smart_pill_subtitle, contextInfo.subtitle)

        // Option 1 (Word tag badge) vs Option 3 (Pure typography)
        views.setViewVisibility(
            R.id.smart_pill_word_tag,
            if (showWordBadge) View.VISIBLE else View.GONE
        )

        val styling = when (theme) {
            SmartPillThemeMode.MatchAppTheme,
            SmartPillThemeMode.MidnightBlue -> PillThemeColors(
                bgRes = R.drawable.widget_pill_bg_midnight,
                titleColor = android.graphics.Color.WHITE,
                subColor = android.graphics.Color.parseColor("#94A3B8"),
                badgeColor = android.graphics.Color.parseColor("#38BDF8"),
            )
            SmartPillThemeMode.AmoledDark -> PillThemeColors(
                bgRes = R.drawable.widget_pill_bg_amoled,
                titleColor = android.graphics.Color.WHITE,
                subColor = android.graphics.Color.parseColor("#A1A1AA"),
                badgeColor = android.graphics.Color.parseColor("#34D399"),
            )
            SmartPillThemeMode.Transparent -> PillThemeColors(
                bgRes = R.drawable.widget_pill_bg_transparent,
                titleColor = android.graphics.Color.WHITE,
                subColor = android.graphics.Color.parseColor("#E2E8F0"),
                badgeColor = android.graphics.Color.parseColor("#38BDF8"),
            )
            SmartPillThemeMode.ClassicWhite -> PillThemeColors(
                bgRes = R.drawable.widget_pill_bg_classic,
                titleColor = android.graphics.Color.parseColor("#0F172A"),
                subColor = android.graphics.Color.parseColor("#475569"),
                badgeColor = android.graphics.Color.parseColor("#059669"),
            )
        }

        views.setInt(R.id.widget_smart_pill_layout, "setBackgroundResource", styling.bgRes)
        views.setTextColor(R.id.smart_pill_title, styling.titleColor)
        views.setTextColor(R.id.smart_pill_subtitle, styling.subColor)
        views.setTextColor(R.id.smart_pill_word_tag, styling.badgeColor)
        views.setTextColor(R.id.smart_pill_status_badge, styling.badgeColor)

        views.setOnClickPendingIntent(R.id.widget_smart_pill_layout, launchPendingIntent(context))
        return views
    }

    /**
     * Builds the seamless centered spiritual pill companion notification:
     * Native system integration, adaptive colors, perfectly centered with no floating box.
     */
    fun buildSmartPillNotification(context: Context, data: NateegaWidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notif_spiritual_pill_centered)
        val contextInfo = SpiritualContextResolver.resolve(data)

        views.setTextViewText(R.id.smart_pill_title, contextInfo.title)
        views.setTextViewText(R.id.smart_pill_subtitle, contextInfo.subtitle)

        val targetRouteUri = when {
            contextInfo.wordTag.contains("كَهْف") || contextInfo.title.contains("الكهف") -> Route.Main.QuranKahf.toUriString().toUri()
            contextInfo.wordTag.contains("مُلْك") || contextInfo.title.contains("الملك") -> Route.Main.QuranMulk.toUriString().toUri()
            contextInfo.wordTag.contains("وِتْر") || contextInfo.wordTag.contains("قِيَام") || contextInfo.title.contains("الضحى") -> Route.Main.Reminder.toUriString().toUri()
            else -> SpiritualPillDismissStore.resolveAzkarRoute(contextInfo.wordTag, contextInfo.title, contextInfo.subtitle).toUriString().toUri()
        }

        views.setOnClickPendingIntent(R.id.widget_smart_pill_layout, launchPendingIntent(context, targetRouteUri, 110))
        return views
    }

    /**
     * Builds the expanded rich spiritual pill notification:
     * Full Hadith, Dua, or wisdom completely visible without truncation, with seamless dark theme integration.
     */
    fun buildSmartPillExpandedNotification(context: Context, data: NateegaWidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.notif_spiritual_pill_expanded)
        val contextInfo = SpiritualContextResolver.resolve(data)

        views.setTextViewText(R.id.smart_pill_expanded_title, contextInfo.title)
        views.setTextViewText(R.id.smart_pill_expanded_body, contextInfo.subtitle)

        val targetRouteUri = when {
            contextInfo.wordTag.contains("كَهْف") || contextInfo.title.contains("الكهف") -> Route.Main.QuranKahf.toUriString().toUri()
            contextInfo.wordTag.contains("مُلْك") || contextInfo.title.contains("الملك") -> Route.Main.QuranMulk.toUriString().toUri()
            contextInfo.wordTag.contains("وِتْر") || contextInfo.wordTag.contains("قِيَام") || contextInfo.title.contains("الضحى") -> Route.Main.Reminder.toUriString().toUri()
            else -> SpiritualPillDismissStore.resolveAzkarRoute(contextInfo.wordTag, contextInfo.title, contextInfo.subtitle).toUriString().toUri()
        }

        views.setOnClickPendingIntent(R.id.widget_smart_pill_expanded_root, launchPendingIntent(context, targetRouteUri, 111))
        return views
    }

    private data class DeskCalendarInfo(
        val dayOfMonth: Int,
        val totalDays: Int,
        val firstDayOfWeek: Int,
        val monthTitle: String,
        val todayBadge: String,
    )

    // ================= 4. التقويم المكتبي المتطور (يتكيف تلقائياً بين الأفقي والمربع) =================
    fun buildDeskCalendar(
        context: Context,
        data: NateegaWidgetData,
        isSquare: Boolean? = null,
        options: Bundle? = null,
    ): RemoteViews {
        val (wDp, hDp) = resolveWidgetSizeDp(context, options, defaultW = 280f, defaultH = 220f)
        val useSquare = isSquare ?: ((wDp < hDp * 1.12f) || (wDp < 235f))
        val layoutRes = if (useSquare) R.layout.widget_desk_calendar_square else R.layout.widget_desk_calendar
        val views = RemoteViews(context.packageName, layoutRes)

        val density = context.resources.displayMetrics.density
        val scale = minOf(wDp / 260f, hDp / 215f).coerceIn(0.72f, 1.55f)
        val rootId = if (useSquare) R.id.widget_desk_calendar_square_layout else R.id.widget_desk_calendar_layout
        val rootPad = (10f * scale * density).toInt().coerceIn(6, 22)
        views.setViewPadding(rootId, rootPad, rootPad, rootPad, rootPad)

        // 1. Resolve Calendar System (Gregorian, Hijri, or Coptic) with full date in one side
        val calInfo = runCatching {
            when (data.config.deskCalendarType) {
                DeskCalendarType.Hijri -> {
                    val cal = icuCalendar("islamic-umalqura")
                    cal.timeInMillis = System.currentTimeMillis()
                    val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
                    val max = cal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
                    cal.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
                    val firstDay = cal.get(android.icu.util.Calendar.DAY_OF_WEEK)
                    val fullDate = "${data.dayOfWeekArabic}، ${data.hijriDay} ${data.hijriMonth} ${data.hijriYear} هـ"
                    DeskCalendarInfo(day, max, firstDay, fullDate, "")
                }
                DeskCalendarType.Coptic -> {
                    val cal = icuCalendar("coptic")
                    cal.timeInMillis = System.currentTimeMillis()
                    val day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
                    val max = cal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
                    cal.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
                    val firstDay = cal.get(android.icu.util.Calendar.DAY_OF_WEEK)
                    val copticLabel = data.copticDate.ifBlank { "التقويم القبطي" }
                    val fullDate = "${data.dayOfWeekArabic}، $copticLabel"
                    DeskCalendarInfo(day, max, firstDay, fullDate, "")
                }
                DeskCalendarType.Gregorian -> {
                    val cal = Calendar.getInstance()
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val firstDay = cal.get(Calendar.DAY_OF_WEEK)
                    val fullDate = "${data.dayOfWeekArabic}، ${data.gregorianDay} ${data.gregorianMonth} ${data.gregorianYear} م"
                    DeskCalendarInfo(day, max, firstDay, fullDate, "")
                }
            }
        }.getOrElse {
            val cal = Calendar.getInstance()
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val firstDay = cal.get(Calendar.DAY_OF_WEEK)
            DeskCalendarInfo(day, max, firstDay, "${data.dayOfWeekArabic}، ${data.gregorianDay} ${data.gregorianMonth} ${data.gregorianYear}", "")
        }

        // 0. Resolve Desk Calendar Theme (supports MatchAppTheme / ClassicPaper / Amoled / MidnightBlue)
        val effectiveDeskTheme = when (data.config.deskCalendarThemeMode) {
            NateegaThemeMode.MatchAppTheme -> data.themeColor
            NateegaThemeMode.ClassicPaper -> ThemeColor.ClassicLight
            NateegaThemeMode.Amoled -> ThemeColor.Dark
            NateegaThemeMode.MidnightBlue -> ThemeColor.MidnightBlue
        }
        val isDarkDesk = effectiveDeskTheme != ThemeColor.ClassicLight
        val deskBgRes = when (effectiveDeskTheme) {
            ThemeColor.ClassicLight -> R.drawable.nateega_bg_classic
            ThemeColor.Dark -> R.drawable.nateega_bg_amoled
            ThemeColor.MidnightBlue -> R.drawable.nateega_bg_midnight
        }
        val capsuleBgRes = when (effectiveDeskTheme) {
            ThemeColor.ClassicLight -> R.drawable.nateega_capsule_classic
            ThemeColor.Dark -> R.drawable.nateega_capsule_dark
            ThemeColor.MidnightBlue -> R.drawable.nateega_capsule_midnight
        }
        val tableBgRes = when (effectiveDeskTheme) {
            ThemeColor.ClassicLight -> R.drawable.nateega_table_bg_classic
            ThemeColor.Dark -> R.drawable.nateega_table_bg_dark
            ThemeColor.MidnightBlue -> R.drawable.nateega_table_bg_midnight
        }
        val activeSlotRes = when (effectiveDeskTheme) {
            ThemeColor.ClassicLight -> R.drawable.nateega_slot_active_classic
            ThemeColor.Dark -> R.drawable.nateega_slot_active_amoled
            ThemeColor.MidnightBlue -> R.drawable.nateega_slot_active_midnight
        }
        val dateColor = if (isDarkDesk) 0xFFFFFFFF.toInt() else 0xFF1E3A8A.toInt()

        views.setInt(rootId, "setBackgroundResource", deskBgRes)
        views.setTextColor(R.id.desk_cal_month_title, dateColor)
        views.setTextColor(R.id.desk_cal_clock, dateColor)
        views.setInt(R.id.desk_cal_clock, "setBackgroundResource", capsuleBgRes)

        views.setTextViewText(R.id.desk_cal_month_title, calInfo.monthTitle)
        views.setTextViewTextSize(R.id.desk_cal_month_title, TypedValue.COMPLEX_UNIT_SP, (12.0f * scale).coerceIn(9.5f, 18.5f))
        views.setTextViewTextSize(R.id.desk_cal_clock, TypedValue.COMPLEX_UNIT_SP, (10.5f * scale).coerceIn(9f, 16f))

        // 2. Countdown Strip Visibility
        if (data.config.deskCalendarShowCountdown) {
            views.setViewVisibility(R.id.desk_cal_countdown_strip, View.VISIBLE)
            views.setInt(R.id.desk_cal_countdown_strip, "setBackgroundResource", capsuleBgRes)
            val prayerName = context.getString(data.nextPrayer.stringRes)
            views.setTextViewText(R.id.desk_cal_countdown_label, "⏳ الصلاة القادمة: $prayerName")
            views.setTextColor(R.id.desk_cal_countdown_label, dateColor)
            views.setTextViewTextSize(R.id.desk_cal_countdown_label, TypedValue.COMPLEX_UNIT_SP, (10.5f * scale).coerceIn(8.5f, 16f))
            views.setTextViewTextSize(R.id.desk_cal_countdown, TypedValue.COMPLEX_UNIT_SP, (10.5f * scale).coerceIn(8.5f, 16f))

            val diff = data.countdownTargetMillis - System.currentTimeMillis()
            val base = SystemClock.elapsedRealtime() + diff
            if (diff > 0) {
                views.setChronometerCountDown(R.id.desk_cal_countdown, true)
                views.setChronometer(R.id.desk_cal_countdown, base, null, true)
            } else {
                views.setChronometerCountDown(R.id.desk_cal_countdown, false)
                views.setChronometer(R.id.desk_cal_countdown, SystemClock.elapsedRealtime(), "00:00:00", false)
            }
        } else {
            views.setViewVisibility(R.id.desk_cal_countdown_strip, View.GONE)
        }

        // 3. Draw High-Res Calendar Grid Bitmap sized proportionally to widget aspect ratio
        val bmpW = if (useSquare) (wDp * 1.9f).toInt().coerceIn(360, 720) else (wDp * 1.15f).toInt().coerceIn(320, 620)
        val bmpH = if (useSquare) (hDp * 1.25f).toInt().coerceIn(250, 560) else (hDp * 1.25f).toInt().coerceIn(230, 520)
        val matrixBitmap = drawCalendarMatrixBitmap(
            currentDay = calInfo.dayOfMonth,
            totalDays = calInfo.totalDays,
            startDayOfWeek = calInfo.firstDayOfWeek,
            width = bmpW,
            height = bmpH,
            isDark = isDarkDesk,
        )
        views.setImageViewBitmap(R.id.desk_cal_grid_image, matrixBitmap)

        val mutedTextColor = if (isDarkDesk) 0xFF94A3B8.toInt() else 0xFF64748B.toInt()
        val primaryTextColor = if (isDarkDesk) 0xFFFFFFFF.toInt() else 0xFF1E3A8A.toInt()
        val activeEmeraldColor = if (isDarkDesk) 0xFF34D399.toInt() else 0xFF047857.toInt()

        // 4. Prayer Times (Unified for both Square Nateega-style bar and Horizontal side list)
        if (useSquare) {
            views.setInt(R.id.desk_cal_prayers_row, "setBackgroundResource", tableBgRes)
        }
        views.setTextViewText(R.id.desk_cal_time_fajr, data.fajrTime)
        views.setTextViewText(R.id.desk_cal_time_sunrise, data.sunriseTime)
        views.setTextViewText(R.id.desk_cal_time_dhuhr, data.dhuhrTime)
        views.setTextViewText(R.id.desk_cal_time_asr, data.asrTime)
        views.setTextViewText(R.id.desk_cal_time_maghrib, data.maghribTime)
        views.setTextViewText(R.id.desk_cal_time_isha, data.ishaTime)

        val rows = listOf(
            R.id.desk_cal_row_fajr,
            R.id.desk_cal_row_sunrise,
            R.id.desk_cal_row_dhuhr,
            R.id.desk_cal_row_asr,
            R.id.desk_cal_row_maghrib,
            R.id.desk_cal_row_isha,
        )
        val labels = listOf(
            R.id.desk_cal_lbl_fajr,
            R.id.desk_cal_lbl_sunrise,
            R.id.desk_cal_lbl_dhuhr,
            R.id.desk_cal_lbl_asr,
            R.id.desk_cal_lbl_maghrib,
            R.id.desk_cal_lbl_isha,
        )
        val times = listOf(
            R.id.desk_cal_time_fajr,
            R.id.desk_cal_time_sunrise,
            R.id.desk_cal_time_dhuhr,
            R.id.desk_cal_time_asr,
            R.id.desk_cal_time_maghrib,
            R.id.desk_cal_time_isha,
        )
        val prayerLblSp = if (useSquare) (8.5f * scale).coerceIn(7.5f, 13.5f) else (9.5f * scale).coerceIn(8f, 15f)
        val prayerTimeSp = if (useSquare) (9.5f * scale).coerceIn(8.0f, 14.5f) else (10.0f * scale).coerceIn(8.5f, 15.5f)

        rows.forEach { views.setInt(it, "setBackgroundResource", 0) }
        labels.forEach {
            views.setTextColor(it, mutedTextColor)
            views.setTextViewTextSize(it, TypedValue.COMPLEX_UNIT_SP, prayerLblSp)
        }
        times.forEach {
            views.setTextColor(it, primaryTextColor)
            views.setTextViewTextSize(it, TypedValue.COMPLEX_UNIT_SP, prayerTimeSp)
        }

        if (data.config.deskCalendarHighlightNextPrayer) {
            val (activeRow, activeLbl, activeTime) = when (data.nextPrayer) {
                Prayer.Fajr -> Triple(R.id.desk_cal_row_fajr, R.id.desk_cal_lbl_fajr, R.id.desk_cal_time_fajr)
                Prayer.Sunrise -> Triple(R.id.desk_cal_row_sunrise, R.id.desk_cal_lbl_sunrise, R.id.desk_cal_time_sunrise)
                Prayer.Dhuhr -> Triple(R.id.desk_cal_row_dhuhr, R.id.desk_cal_lbl_dhuhr, R.id.desk_cal_time_dhuhr)
                Prayer.Asr -> Triple(R.id.desk_cal_row_asr, R.id.desk_cal_lbl_asr, R.id.desk_cal_time_asr)
                Prayer.Maghrib -> Triple(R.id.desk_cal_row_maghrib, R.id.desk_cal_lbl_maghrib, R.id.desk_cal_time_maghrib)
                Prayer.Isha -> Triple(R.id.desk_cal_row_isha, R.id.desk_cal_lbl_isha, R.id.desk_cal_time_isha)
                else -> Triple(null, null, null)
            }
            if (activeRow != null && activeLbl != null && activeTime != null) {
                val highlightRes = if (useSquare) activeSlotRes else R.drawable.desk_cal_row_active
                views.setInt(activeRow, "setBackgroundResource", highlightRes)
                views.setTextColor(activeLbl, activeEmeraldColor)
                views.setTextColor(activeTime, activeEmeraldColor)
            }
        }

        views.setOnClickPendingIntent(rootId, launchPendingIntent(context))
        return views
    }

    private fun drawCalendarMatrixBitmap(
        currentDay: Int,
        totalDays: Int,
        startDayOfWeek: Int, // Calendar.SUNDAY=1..SATURDAY=7
        width: Int,
        height: Int,
        isDark: Boolean = false,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cellWidth = width / 7f
        val cellHeight = height / 7f

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellHeight * 0.56f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            // Clear date color for weekday headers (matches month title & today badge)
            color = if (isDark) 0xFFFFFFFF.toInt() else 0xFF1E3A8A.toInt()
        }

        val fridayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellHeight * 0.55f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = if (isDark) 0xFF34D399.toInt() else 0xFF059669.toInt()
        }

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellHeight * 0.52f
            textAlign = Paint.Align.CENTER
            color = if (isDark) 0xFFF1F5F9.toInt() else 0xFF1E293B.toInt()
        }

        val activeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF2563EB.toInt()
        }

        val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellHeight * 0.54f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = 0xFFFFFFFF.toInt()
        }

        val fridayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellHeight * 0.52f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            color = 0xFF059669.toInt()
        }

        // Draw Headers: Saturday(س), Sunday(ح), Monday(ن), Tuesday(ث), Wednesday(ر), Thursday(خ), Friday(ج)
        val weekHeaders = arrayOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
        for (i in 0..6) {
            val cx = (6 - i) * cellWidth + cellWidth / 2f
            val cy = cellHeight * 0.7f
            val p = if (i == 6) fridayHeaderPaint else headerPaint
            canvas.drawText(weekHeaders[i], cx, cy, p)
        }

        // Map Calendar.SUNDAY(1)..SATURDAY(7) to index: Saturday=0, Sunday=1, .., Friday=6
        val firstCol = when (startDayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 1
        }

        var col = firstCol
        var row = 1

        for (day in 1..totalDays) {
            val cx = (6 - col) * cellWidth + cellWidth / 2f
            val cy = row * cellHeight + cellHeight * 0.72f

            if (day == currentDay) {
                val radius = (cellWidth.coerceAtMost(cellHeight) * 0.42f)
                canvas.drawCircle(cx, row * cellHeight + cellHeight / 2f, radius, activeCirclePaint)
                canvas.drawText(day.toString(), cx, cy, activeTextPaint)
            } else if (col == 6) { // Friday
                canvas.drawText(day.toString(), cx, cy, fridayTextPaint)
            } else {
                canvas.drawText(day.toString(), cx, cy, dayPaint)
            }

            col++
            if (col > 6) {
                col = 0
                row++
            }
        }

        return bitmap
    }
}

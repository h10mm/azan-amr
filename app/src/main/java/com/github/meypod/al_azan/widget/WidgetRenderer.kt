package com.github.meypod.al_azan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.withAppLocale
import com.github.meypod.al_azan.core.domain.model.widget.DialThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerBackground
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.SquareThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData

/**
 * Builds [RemoteViews] for the prayer-times widgets. Kotlin port of the legacy `WidgetUtils`,
 * driven by an already-computed [WidgetData] instead of a React bridge payload.
 */
object WidgetRenderer {

    private const val MAX_SLOTS = 6

    // Each prayer slot has a normal and an "active" variant; index = slot*2 (+1 for active).
    private val slotContainerIds = intArrayOf(
        R.id.prayer1,
        R.id.prayer1active,
        R.id.prayer2,
        R.id.prayer2active,
        R.id.prayer3,
        R.id.prayer3active,
        R.id.prayer4,
        R.id.prayer4active,
        R.id.prayer5,
        R.id.prayer5active,
        R.id.prayer6,
        R.id.prayer6active,
    )

    private val slotNameIds = intArrayOf(
        R.id.prayer1_name,
        R.id.prayer1active_name,
        R.id.prayer2_name,
        R.id.prayer2active_name,
        R.id.prayer3_name,
        R.id.prayer3active_name,
        R.id.prayer4_name,
        R.id.prayer4active_name,
        R.id.prayer5_name,
        R.id.prayer5active_name,
        R.id.prayer6_name,
        R.id.prayer6active_name,
    )

    private val slotTimeIds = intArrayOf(
        R.id.prayer1_time,
        R.id.prayer1active_time,
        R.id.prayer2_time,
        R.id.prayer2active_time,
        R.id.prayer3_time,
        R.id.prayer3active_time,
        R.id.prayer4_time,
        R.id.prayer4active_time,
        R.id.prayer5_time,
        R.id.prayer5active_time,
        R.id.prayer6_time,
        R.id.prayer6active_time,
    )

    fun build(
        context: Context,
        layoutResId: Int,
        data: WidgetData,
    ): RemoteViews {
        // Caller contexts (application context, receiver context) don't carry the per-app locale on
        // pre-API 33 — resolve prayer-name strings through a localized context instead.
        val localized = context.withAppLocale(data.locale)
        val views = RemoteViews(context.packageName, layoutResId)

        // Flip the widget's reading direction. RTL is the default (prayers laid out right-to-left);
        // the swap toggle forces LTR. Set explicitly each build so toggling off restores RTL.
        val direction = if (data.swapLayoutDirection) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        views.setInt(R.id.widget_header, "setLayoutDirection", direction)
        views.setInt(R.id.widget_content, "setLayoutDirection", direction)

        // Header texts. Ids are absent in the small notification layout; RemoteViews ignores those.
        views.setTextViewText(R.id.top_start_text, data.topStartText)
        views.setTextViewText(R.id.top_end_text, data.topEndText)

        // Hide every slot first, then reveal the ones we use.
        slotContainerIds.forEach { views.setViewVisibility(it, View.GONE) }

        data.rows.take(MAX_SLOTS).forEachIndexed { index, row ->
            val variant = if (row.isActive) index * 2 + 1 else index * 2
            views.setViewVisibility(slotContainerIds[variant], View.VISIBLE)
            views.setTextViewText(slotNameIds[variant], localized.getString(row.prayer.stringRes))
            views.setTextViewText(slotTimeIds[variant], row.timeText)
        }

        if (data.showCountdown && data.countdown != null) {
            val prefix = if (data.locale == "ar") "الصلاة القادمة: " else "Next: "
            views.setTextViewText(
                R.id.countdown_label,
                "$prefix${localized.getString(data.countdown.prayer.stringRes)}",
            )
            val base = SystemClock.elapsedRealtime() + (data.countdown.baseMillis - System.currentTimeMillis())
            views.setChronometer(R.id.countdown, base, null, true)
        }

        views.setOnClickPendingIntent(R.id.screen_widget_layout, launchPendingIntent(context))
        return views
    }

    /** Builds the home-screen app-widget RemoteViews, picking the layout from [data]. */
    fun buildScreenWidget(
        context: Context,
        data: WidgetData,
    ): RemoteViews = build(context, screenWidgetLayout(data.adaptiveTheme, data.showCountdown), data)

    fun screenWidgetLayout(
        adaptive: Boolean,
        countdown: Boolean,
    ): Int =
        when {
            countdown && adaptive -> R.layout.screen_widget_countdown_adaptive
            countdown -> R.layout.screen_widget_countdown
            adaptive -> R.layout.screen_widget_adaptive
            else -> R.layout.screen_widget
        }

    /**
     * Builds the next-prayer widget (Circle or Square) with true proportional scaling driven by BOTH
     * width and height from [options]. Prevents text/countdown overflow on shrink and empty margins on enlarge.
     */
    fun buildNextPrayerWidget(
        context: Context,
        data: NextPrayerWidgetData,
        options: Bundle? = null,
    ): RemoteViews {
        val layout = when (data.shape) {
            NextPrayerShape.Circle -> R.layout.next_prayer_widget_circle
            NextPrayerShape.Square -> R.layout.next_prayer_widget_square
        }
        val (widthDp, heightDp) = resolveWidgetSizeDp(context, options, defaultW = 140f, defaultH = 140f)
        return renderNextPrayer(context, data, widthDp, heightDp, layout)
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
        val finalW = if (w > 20) w.toFloat() else defaultW
        val finalH = if (h > 20) h.toFloat() else defaultH
        return finalW to finalH
    }

    /**
     * Builds the compact next-prayer layout for the notification widget. Uses notification-specific
     * layouts (transparent background + notification text theme) rather than the home-widget ones, which
     * carry a launcher background and colors. The size is fixed per view: the collapsed view is short and
     * only fits the small font; the expanded view has room for the larger one.
     */
    fun buildNextPrayerNotification(
        context: Context,
        data: NextPrayerWidgetData,
        expanded: Boolean,
    ): RemoteViews = buildCenteredPrayerNotification(context, data)

    /**
     * Builds the centered, modern prayer notification (Option 1):
     * Clean bold prayer title in the center, exact prayer time underneath,
     * no countdown chronometer, no city, and elegant dark luxury styling.
     */
    fun buildCenteredPrayerNotification(
        context: Context,
        data: NextPrayerWidgetData,
    ): RemoteViews {
        val localized = context.withAppLocale(data.locale)
        val rawPrayer = localized.getString(data.prayer.stringRes)
        val prayerTitle = if (data.locale == "ar") {
            if (data.prayer.name.equals("sunrise", ignoreCase = true)) {
                "وقت الشروق"
            } else {
                "صلاة $rawPrayer"
            }
        } else {
            rawPrayer
        }

        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = data.countdownBaseMillis
        }
        val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
        val suffix = if (data.locale == "ar") {
            if (isAm) " ص" else " م"
        } else {
            if (isAm) " AM" else " PM"
        }
        val rawTime = data.prayerTimeText.trim()
        val formattedTimeWithAmPm = if (
            rawTime.contains("ص") || rawTime.contains("م") ||
            rawTime.contains("AM", ignoreCase = true) || rawTime.contains("PM", ignoreCase = true)
        ) {
            rawTime
        } else {
            "$rawTime$suffix"
        }
        val diffMillis = (data.countdownBaseMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60
        val remainingText = if (data.locale == "ar") {
            if (hours > 0) "متبقي ${hours}س ${minutes}د" else "متبقي ${minutes}د"
        } else {
            if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
        }
        val subText = if (data.locale == "ar") {
            "الصلاة القادمة • $remainingText"
        } else {
            "Next Prayer • $remainingText"
        }

        return RemoteViews(context.packageName, R.layout.notif_next_prayer_centered).apply {
            setTextViewText(R.id.next_prayer_name, prayerTitle)
            setTextViewText(R.id.next_prayer_sub, subText)
            setTextViewText(R.id.next_prayer_time, formattedTimeWithAmPm)
            setOnClickPendingIntent(R.id.next_prayer_widget_layout, launchPendingIntent(context))
        }
    }

    private fun renderNextPrayer(
        context: Context,
        data: NextPrayerWidgetData,
        widthDp: Float,
        heightDp: Float,
        layout: Int,
    ): RemoteViews {
        val localized = context.withAppLocale(data.locale)
        val density = context.resources.displayMetrics.density
        val diff = data.countdownBaseMillis - System.currentTimeMillis()
        val base = SystemClock.elapsedRealtime() + diff
        val minDim = minOf(widthDp, heightDp)

        return RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.next_prayer_name, localized.getString(data.prayer.stringRes))
            setTextColor(R.id.next_prayer_name, data.prayerNameColorHex.toInt())

            if (data.prayerTimeText.isNotBlank()) {
                setTextViewText(R.id.next_prayer_time, data.prayerTimeText)
            }

            if (diff > 0) {
                setChronometerCountDown(R.id.next_prayer_countdown, true)
                setChronometer(R.id.next_prayer_countdown, base, null, true)
            } else {
                setChronometerCountDown(R.id.next_prayer_countdown, false)
                setChronometer(R.id.next_prayer_countdown, SystemClock.elapsedRealtime(), "00:00:00", false)
            }
            setTextColor(R.id.next_prayer_countdown, data.countdownColorHex.toInt())

            if (data.shape == NextPrayerShape.Circle) {
                // Circle fits inside minDim; 2x2 standard is ~115dp -> full readable sizes, shrinking only when tiny
                val scale = (minDim / 115f).coerceIn(0.55f, 1.85f)
                val nameSp = (20.0f * scale).coerceIn(11.5f, 36f)
                val timeSp = (17.0f * scale).coerceIn(10.0f, 30f)
                val countdownSp = (12.0f * scale).coerceIn(8.5f, 20f)

                val ringBitmap = drawCircularRingBitmap(
                    progress = data.progress,
                    colorInt = data.progressColorHex.toInt(),
                    themeMode = data.dialThemeMode,
                )
                setImageViewBitmap(R.id.next_prayer_canvas_bg, ringBitmap)
                if (data.dialThemeMode == DialThemeMode.ClassicWhite) {
                    setTextColor(R.id.next_prayer_name, 0xFF0F172A.toInt())
                    setTextColor(R.id.next_prayer_time, 0xFF334155.toInt())
                    setInt(R.id.next_prayer_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill_light)
                    setTextColor(R.id.next_prayer_countdown, 0xFF0F172A.toInt())
                } else if (data.dialThemeMode == DialThemeMode.Transparent) {
                    setTextColor(R.id.next_prayer_name, 0xFFFFFFFF.toInt())
                    setTextColor(R.id.next_prayer_time, 0xFFFFFFFF.toInt())
                    setInt(R.id.next_prayer_countdown, "setBackgroundResource", 0)
                    setTextColor(R.id.next_prayer_countdown, 0xFFFFFFFF.toInt())
                } else {
                    setTextColor(R.id.next_prayer_name, 0xFFFFFFFF.toInt())
                    setTextColor(R.id.next_prayer_time, 0xFFFFFFFF.toInt())
                    setInt(R.id.next_prayer_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill)
                    setTextColor(R.id.next_prayer_countdown, data.countdownColorHex.toInt())
                }

                val pillPadH = (6f * scale * density).toInt().coerceIn(2, 20)
                val pillPadV = (1.5f * scale * density).toInt().coerceIn(1, 8)
                setViewPadding(R.id.next_prayer_countdown, pillPadH, pillPadV, pillPadH, pillPadV)

                setTextViewTextSize(R.id.next_prayer_name, TypedValue.COMPLEX_UNIT_SP, nameSp)
                setTextViewTextSize(R.id.next_prayer_time, TypedValue.COMPLEX_UNIT_SP, timeSp)
                setTextViewTextSize(R.id.next_prayer_countdown, TypedValue.COMPLEX_UNIT_SP, countdownSp)
            } else {
                // Square card: 2x2 standard is ~112dp -> bold readable sizes, scaling smoothly with both width and height
                val scale = minOf(widthDp / 112f, heightDp / 112f).coerceIn(0.58f, 1.85f)
                val nameSp = (22.0f * scale).coerceIn(12.5f, 38f)
                val timeSp = (18.5f * scale).coerceIn(11.0f, 32f)
                val countdownSp = (13.0f * scale).coerceIn(9.0f, 21f)

                val rootPad = (7f * scale * density).toInt().coerceIn(4, 24)
                setViewPadding(R.id.next_prayer_widget_layout, rootPad, rootPad, rootPad, rootPad)

                val showProgressBar = heightDp >= 82f
                setViewVisibility(R.id.next_prayer_progress_bar, if (showProgressBar) View.VISIBLE else View.GONE)
                if (showProgressBar) {
                    setProgressBar(R.id.next_prayer_progress_bar, 1000, (data.progress * 1000).toInt(), false)
                }

                val bgRes = when (data.squareThemeMode) {
                    SquareThemeMode.MatchAppTheme,
                    SquareThemeMode.AmoledDark -> R.drawable.widget_square_bg_amoled
                    SquareThemeMode.MidnightBlue -> R.drawable.widget_square_bg_midnight
                    SquareThemeMode.ClassicWhite -> R.drawable.widget_square_bg_classic
                    SquareThemeMode.Transparent -> R.drawable.widget_square_bg_transparent
                }
                setInt(R.id.next_prayer_widget_layout, "setBackgroundResource", bgRes)

                when (data.squareThemeMode) {
                    SquareThemeMode.ClassicWhite -> {
                        setTextColor(R.id.next_prayer_name, 0xFF0F172A.toInt())
                        setTextColor(R.id.next_prayer_time, 0xFF334155.toInt())
                        setInt(R.id.next_prayer_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill_light)
                        setTextColor(R.id.next_prayer_countdown, 0xFF0F172A.toInt())
                    }
                    SquareThemeMode.Transparent -> {
                        // 100% transparent: crisp white text + strong dark shadow readable over any wallpaper
                        setTextColor(R.id.next_prayer_name, 0xFFFFFFFF.toInt())
                        setTextColor(R.id.next_prayer_time, 0xFFFFFFFF.toInt())
                        setInt(R.id.next_prayer_countdown, "setBackgroundResource", 0)
                        setTextColor(R.id.next_prayer_countdown, 0xFFFFFFFF.toInt())
                    }
                    else -> {
                        setTextColor(R.id.next_prayer_name, 0xFFFFFFFF.toInt())
                        setTextColor(R.id.next_prayer_time, 0xFFFFFFFF.toInt())
                        setInt(R.id.next_prayer_countdown, "setBackgroundResource", R.drawable.widget_chronometer_pill)
                        setTextColor(R.id.next_prayer_countdown, data.countdownColorHex.toInt())
                    }
                }

                val pillPadH = if (data.squareThemeMode == SquareThemeMode.Transparent) 0 else (7f * scale * density).toInt().coerceIn(3, 22)
                val pillPadV = if (data.squareThemeMode == SquareThemeMode.Transparent) 0 else (2f * scale * density).toInt().coerceIn(1, 9)
                setViewPadding(R.id.next_prayer_countdown, pillPadH, pillPadV, pillPadH, pillPadV)

                setTextViewTextSize(R.id.next_prayer_name, TypedValue.COMPLEX_UNIT_SP, nameSp)
                setTextViewTextSize(R.id.next_prayer_time, TypedValue.COMPLEX_UNIT_SP, timeSp)
                setTextViewTextSize(R.id.next_prayer_countdown, TypedValue.COMPLEX_UNIT_SP, countdownSp)
            }

            setOnClickPendingIntent(R.id.next_prayer_widget_layout, launchPendingIntent(context))
        }
    }

    private fun drawCircularRingBitmap(
        progress: Float,
        colorInt: Int,
        themeMode: DialThemeMode = DialThemeMode.AmoledDark,
        sizePx: Int = 512,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val strokeWidth = sizePx * 0.065f
        val ringRadius = (sizePx - strokeWidth) / 2f - 2f
        val rect = RectF(
            cx - ringRadius,
            cy - ringRadius,
            cx + ringRadius,
            cy + ringRadius,
        )

        // 1. Concentric circular card background according to selected theme
        if (themeMode != DialThemeMode.Transparent) {
            val innerCardRadius = ringRadius - strokeWidth / 2f + 1f
            val cardBgColor = when (themeMode) {
                DialThemeMode.MatchAppTheme,
                DialThemeMode.AmoledDark -> 0xFF080C14.toInt()
                DialThemeMode.MidnightBlue -> 0xFF0D172A.toInt()
                DialThemeMode.ClassicWhite -> 0xFFF8FAFC.toInt()
                DialThemeMode.Transparent -> 0
            }
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = cardBgColor
            }
            canvas.drawCircle(cx, cy, innerCardRadius, cardPaint)

            // 2. Subtle inner border stroke
            val borderColor = when (themeMode) {
                DialThemeMode.MatchAppTheme,
                DialThemeMode.AmoledDark -> 0x22FFFFFF
                DialThemeMode.MidnightBlue -> 0x3338BDF8
                DialThemeMode.ClassicWhite -> 0x1E000000
                DialThemeMode.Transparent -> 0
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = 2.5f
                color = borderColor
            }
            canvas.drawCircle(cx, cy, innerCardRadius, borderPaint)
        }

        // 3. Track circle for the progress ring
        val trackColor = when (themeMode) {
            DialThemeMode.Transparent -> 0x33FFFFFF
            DialThemeMode.MatchAppTheme,
            DialThemeMode.AmoledDark -> 0x22FFFFFF
            DialThemeMode.MidnightBlue -> 0x2238BDF8
            DialThemeMode.ClassicWhite -> 0x18000000
        }
        val bgTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = trackColor
        }
        canvas.drawCircle(cx, cy, ringRadius, bgTrackPaint)

        // 4. Circular progress arc with rounded end caps
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = colorInt
        }
        val sweepAngle = 360f * progress.coerceIn(0.01f, 1f)
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)

        return bitmap
    }

    /**
     * "The app needs configuring" hint for the table / next-prayer widgets when times can't be computed
     * (no calculation method or no location). Reuses the home screen's `set_calculation_hint` /
     * `set_location_hint` prompts; tapping opens the app to fix it.
     */
    fun buildHint(
        context: Context,
        @StringRes messageRes: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_hint)
        views.setTextViewText(R.id.widget_hint_text, context.getString(messageRes))
        views.setOnClickPendingIntent(R.id.widget_hint_layout, launchPendingIntent(context))
        return views
    }

    private fun launchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

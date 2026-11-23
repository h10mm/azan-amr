package com.github.meypod.al_azan.core.domain.model.widget

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

/**
 * Everything needed to render the 1x1 next-prayer widget: the upcoming prayer the countdown targets
 * and the chronometer base. Free of Android view concerns so it can be unit tested; the renderer
 * resolves the prayer name string.
 */
@Immutable
data class NextPrayerWidgetData(
    val prayer: Prayer,
    /** Wall-clock millis of the targeted prayer; the chronometer counts down to it. */
    val countdownBaseMillis: Long,
    val adaptiveTheme: Boolean,
    /** Wall-clock millis at which the widget should be redrawn next (the targeted prayer elapses). */
    val nextUpdateAtMillis: Long?,
    /**
     * Language tag the prayer-name string must resolve in. The renderer runs from contexts that never
     * get the AppCompat per-app locale (application context, receivers — pre-API 33), so it can't rely
     * on the context's own configuration.
     */
    val locale: String,
    val prayerTimeText: String = "",
    val progress: Float = 0.5f,
    val shape: NextPrayerShape = NextPrayerShape.Circle,
    val background: NextPrayerBackground = NextPrayerBackground.Transparent,
    val progressColorHex: Long = 0xFF10B981,
    val countdownColorHex: Long = 0xFFF59E0B,
    val prayerNameColorHex: Long = 0xFFFFFFFF,
    val dialThemeMode: DialThemeMode = DialThemeMode.AmoledDark,
    val squareThemeMode: SquareThemeMode = SquareThemeMode.AmoledDark,
)

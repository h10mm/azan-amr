package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.DialThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerBackground
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.SquareThemeMode
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Builds [NextPrayerWidgetData] for the 1x1 next-prayer widget. The countdown targets the next prayer
 * among the user-selected set ([Settings.countdownWidgetPrayers]); every other prayer is excluded from
 * the search. Returns null when calculation is not configured, the selection is empty, or no upcoming
 * prayer is found — in which case the caller leaves the widget on its placeholder.
 */
class BuildNextPrayerWidgetDataUseCase @Inject constructor(
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val formatter: WidgetFormatter,
) {
    operator fun invoke(
        instant: Instant,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
        config: NateegaWidgetConfig? = null,
    ): NextPrayerWidgetData? {
        val parameters = calcSettings.parameters ?: return null
        if (location == null) return null

        val selected = settings.countdownWidgetPrayers.toSet()
        val excluding = if (selected.isNotEmpty()) SHARIA_TIMES_IN_ORDER.toSet() - selected else emptySet()

        val next = getNextShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calcSettings.midnightMethod,
            excluding = excluding,
        ) ?: return null

        val nextMillis = next.prayerTime.toEpochMilliseconds()
        val formattedTime = formatter.formatPrayerTime(
            instant = next.prayerTime,
            is24Hour = settings.is24HourFormat,
            numberingSystem = settings.numberingSystem,
            locale = settings.selectedLocale,
        )

        val shariaTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calcSettings.midnightMethod,
        ).timesOrNull

        val progress = if (shariaTimes != null) {
            val prayersList = listOf(
                shariaTimes.fajr,
                shariaTimes.sunrise,
                shariaTimes.dhuhr,
                shariaTimes.asr,
                shariaTimes.maghrib,
                shariaTimes.isha,
            )
            val previousPrayerInstant = prayersList.filter { it <= instant }.maxOrNull()
                ?: (shariaTimes.isha - 1.days)
            val totalSpan = (next.prayerTime - previousPrayerInstant).inWholeMilliseconds.coerceAtLeast(1L)
            val remaining = (next.prayerTime - instant).inWholeMilliseconds.coerceIn(0L, totalSpan)
            (remaining.toFloat() / totalSpan).coerceIn(0.01f, 1f)
        } else {
            val nowMillis = instant.toEpochMilliseconds()
            val diff = (nextMillis - nowMillis).coerceAtLeast(0)
            val estimatedInterval = 3 * 3600 * 1000L
            (diff.toFloat() / estimatedInterval).coerceIn(0.05f, 1f)
        }

        val rawDialTheme = config?.dialThemeMode ?: DialThemeMode.MatchAppTheme
        val effectiveDialTheme = if (rawDialTheme == DialThemeMode.MatchAppTheme) {
            when (settings.themeColor) {
                ThemeColor.ClassicLight -> DialThemeMode.ClassicWhite
                ThemeColor.Dark -> DialThemeMode.AmoledDark
                ThemeColor.MidnightBlue -> DialThemeMode.MidnightBlue
            }
        } else {
            rawDialTheme
        }

        val rawSquareTheme = config?.squareThemeMode ?: SquareThemeMode.MatchAppTheme
        val effectiveSquareTheme = if (rawSquareTheme == SquareThemeMode.MatchAppTheme) {
            when (settings.themeColor) {
                ThemeColor.ClassicLight -> SquareThemeMode.ClassicWhite
                ThemeColor.Dark -> SquareThemeMode.AmoledDark
                ThemeColor.MidnightBlue -> SquareThemeMode.MidnightBlue
            }
        } else {
            rawSquareTheme
        }

        return NextPrayerWidgetData(
            prayer = next.prayer,
            countdownBaseMillis = nextMillis,
            adaptiveTheme = settings.adaptiveWidgets,
            nextUpdateAtMillis = nextMillis.takeIf { it > instant.toEpochMilliseconds() },
            locale = settings.selectedLocale,
            prayerTimeText = formattedTime,
            progress = progress,
            shape = config?.nextPrayerShape ?: NextPrayerShape.Circle,
            background = config?.nextPrayerBackground ?: NextPrayerBackground.Transparent,
            progressColorHex = config?.nextPrayerProgressColor?.colorHex ?: 0xFF10B981,
            countdownColorHex = config?.nextPrayerCountdownColor?.colorHex ?: 0xFFF59E0B,
            prayerNameColorHex = config?.nextPrayerNameColor?.colorHex ?: 0xFFFFFFFF,
            dialThemeMode = effectiveDialTheme,
            squareThemeMode = effectiveSquareTheme,
        )
    }
}

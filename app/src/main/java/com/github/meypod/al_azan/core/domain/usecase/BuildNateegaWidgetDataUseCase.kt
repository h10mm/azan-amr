package com.github.meypod.al_azan.core.domain.usecase

import android.icu.text.DateFormat
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaLanguageMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import com.github.meypod.al_azan.core.domain.util.NateegaWisdomProvider
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.maghribHijriDayShift
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BuildNateegaWidgetDataUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val formatter: WidgetFormatter,
) {
    operator fun invoke(
        instant: Instant,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
        config: NateegaWidgetConfig = NateegaWidgetConfig(),
    ): NateegaWidgetData? {
        val parameters = calcSettings.parameters ?: return null
        if (location == null) return null

        val hidden = settings.hiddenWidgetPrayers.toSet()

        val shariaTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            midnightMethod = calcSettings.midnightMethod,
            locationDetail = location,
        ).timesOrNull ?: return null

        val nextShariaTime = getNextShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calcSettings.midnightMethod,
            excluding = hidden,
        )

        val maghribShift = maghribHijriDayShift(
            now = instant,
            maghrib = shariaTimes.maghrib,
            enabled = settings.widgetHijriDayStartsAtMaghrib,
        )
        val hijriInstant = instant + (calcSettings.calculationAdjustments.hijriDate + maghribShift).days

        val numbering = settings.numberingSystem

        // تحديد اللغة والأسماء بدقة حسب اختيار المستخدم أو لغة التطبيق
        val isAppArabic = settings.selectedLocale.startsWith("ar")
        val showArabic = when (config.languageMode) {
            NateegaLanguageMode.AppLanguage -> isAppArabic
            NateegaLanguageMode.ArabicOnly, NateegaLanguageMode.Bilingual -> true
            NateegaLanguageMode.EnglishOnly -> false
        }
        val showEnglish = when (config.languageMode) {
            NateegaLanguageMode.AppLanguage -> !isAppArabic
            NateegaLanguageMode.EnglishOnly, NateegaLanguageMode.Bilingual -> true
            NateegaLanguageMode.ArabicOnly -> false
        }

        val primaryLocale = if (showArabic) "ar" else "en"

        // Day of week
        val dayArabic = if (showArabic) {
            formatInstant(instant, "ar", "gregorian", DateFormat.WEEKDAY, numbering)
        } else {
            ""
        }
        val dayEnglish = if (showEnglish) {
            formatInstant(instant, "en", "gregorian", DateFormat.WEEKDAY).uppercase()
        } else {
            ""
        }

        // Gregorian Date
        val gregDay = formatInstant(instant, primaryLocale, "gregorian", "d", numbering)
        val gregMonth = formatInstant(instant, primaryLocale, "gregorian", "MMMM", numbering)
        val gregYear = formatInstant(instant, primaryLocale, "gregorian", "yyyy", numbering)

        // Hijri Date
        val hijriDay = formatInstant(hijriInstant, primaryLocale, settings.selectedArabicCalendar, "d", numbering)
        val hijriMonth = formatInstant(hijriInstant, primaryLocale, settings.selectedArabicCalendar, "MMMM", numbering)
        val hijriYear = formatInstant(hijriInstant, primaryLocale, settings.selectedArabicCalendar, "yyyy", numbering)

        // Coptic Date
        val copticText = if (config.showCopticDate) {
            runCatching {
                formatInstant(instant, primaryLocale, "coptic", "d MMMM y", numbering)
            }.getOrDefault("")
        } else {
            ""
        }

        // Gregorian Summary (يظهر فقط إذا كان الإنجليزي مفعلاً)
        val gregSummary = if (showEnglish && showArabic) {
            runCatching {
                formatInstant(instant, "en", "gregorian", "d MMMM yyyy").uppercase()
            }.getOrDefault("")
        } else {
            ""
        }

        val is24h = settings.is24HourFormat
        val city = location.city?.toString() ?: location.label ?: ""

        val fajrTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.fajr, is24h, numbering, settings.selectedLocale))
        val sunriseTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.sunrise, is24h, numbering, settings.selectedLocale))
        val dhuhrTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.dhuhr, is24h, numbering, settings.selectedLocale))
        val asrTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.asr, is24h, numbering, settings.selectedLocale))
        val maghribTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.maghrib, is24h, numbering, settings.selectedLocale))
        val ishaTime = cleanPrayerTime(formatter.formatPrayerTime(shariaTimes.isha, is24h, numbering, settings.selectedLocale))

        val nextPrayer = nextShariaTime?.prayer ?: Prayer.Fajr
        val nextPrayerInstant = nextShariaTime?.prayerTime ?: shariaTimes.fajr
        val nextPrayerTime = cleanPrayerTime(formatter.formatPrayerTime(nextPrayerInstant, is24h, numbering, settings.selectedLocale))

        // Calculate countdown percentage remaining until next prayer (تناقصي من 100% إلى 0%)
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
        val totalSpan = (nextPrayerInstant - previousPrayerInstant).inWholeMilliseconds.coerceAtLeast(1L)
        val remaining = (nextPrayerInstant - instant).inWholeMilliseconds.coerceIn(0L, totalSpan)
        val progressPercent = ((remaining.toDouble() / totalSpan) * 100).toInt().coerceIn(0, 100)

        val wisdom = if (config.showWisdom || config.compactShowWisdom) {
            NateegaWisdomProvider.getWisdomForDate(instant)
        } else {
            ""
        }

        val effectiveTheme = when (config.themeMode) {
            NateegaThemeMode.MatchAppTheme -> settings.themeColor
            NateegaThemeMode.ClassicPaper -> ThemeColor.ClassicLight
            NateegaThemeMode.Amoled -> ThemeColor.Dark
            NateegaThemeMode.MidnightBlue -> ThemeColor.MidnightBlue
        }

        return NateegaWidgetData(
            dayOfWeekArabic = dayArabic,
            dayOfWeekEnglish = dayEnglish,
            gregorianDay = gregDay,
            gregorianMonth = gregMonth,
            gregorianYear = gregYear,
            hijriDay = hijriDay,
            hijriMonth = hijriMonth,
            hijriYear = hijriYear,
            copticDate = copticText,
            gregorianDateSummary = gregSummary,
            cityName = city,
            fajrTime = fajrTime,
            sunriseTime = sunriseTime,
            dhuhrTime = dhuhrTime,
            asrTime = asrTime,
            maghribTime = maghribTime,
            ishaTime = ishaTime,
            nextPrayer = nextPrayer,
            nextPrayerTime = nextPrayerTime,
            countdownTargetMillis = nextPrayerInstant.toEpochMilliseconds(),
            progressPercent = progressPercent,
            wisdomText = wisdom,
            themeColor = effectiveTheme,
            locale = settings.selectedLocale,
            config = config,
        )
    }

    private fun cleanPrayerTime(time: String): String {
        return time
            .replace(Regex("""[\s\u200E\u200F\u061C]*[صم](\.)?[\s\u200E\u200F\u061C]*"""), "")
            .replace(Regex("""(?i)[\s\u200E\u200F\u061C]*(am|pm)[\s\u200E\u200F\u061C]*"""), "")
            .trim()
    }
}

package com.github.meypod.al_azan.core.domain.model.widget

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

@Immutable
data class NateegaWidgetData(
    val dayOfWeekArabic: String,
    val dayOfWeekEnglish: String,
    val gregorianDay: String,
    val gregorianMonth: String,
    val gregorianYear: String,
    val hijriDay: String,
    val hijriMonth: String,
    val hijriYear: String,
    val copticDate: String,
    val gregorianDateSummary: String,
    val cityName: String,
    val fajrTime: String,
    val sunriseTime: String,
    val dhuhrTime: String,
    val asrTime: String,
    val maghribTime: String,
    val ishaTime: String,
    val nextPrayer: Prayer,
    val nextPrayerTime: String,
    val countdownTargetMillis: Long,
    val progressPercent: Int,
    val wisdomText: String,
    val themeColor: ThemeColor,
    val locale: String,
    val config: NateegaWidgetConfig = NateegaWidgetConfig(),
)

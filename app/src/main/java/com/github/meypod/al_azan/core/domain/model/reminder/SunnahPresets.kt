package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import kotlinx.datetime.DayOfWeek

data class SunnahPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val iconEmoji: String,
    val prayer: Prayer,
    val durationMinutes: Int,
    val durationModifier: Int, // -1 before, 1 after
    val days: Set<DayOfWeek> = PrayerAlarmSettings.ALL_DAYS,
    val isQuranSurah: Boolean = false,
    val surahNumber: Int = 18,
    val isAzkar: Boolean = false,
)

object SunnahPresets {
    const val ID_QIYAM = "preset_qiyam_layl"
    const val ID_DUHA = "preset_salat_duha"
    const val ID_MON_THU = "preset_fasting_mon_thu"
    const val ID_WHITE_DAYS = "preset_fasting_white_days"
    const val ID_KAHF = "preset_surah_kahf"
    const val ID_MULK = "preset_surah_mulk"
    const val ID_ISTIJABAH = "preset_friday_istijabah"
    const val ID_AZKAR = "preset_morning_evening_azkar"

    val list: List<SunnahPreset> = listOf(
        // صلوات النوافل والقيام
        SunnahPreset(
            id = ID_QIYAM,
            title = "قيام الليل والوتر",
            subtitle = "الثلث الأخير من الليل • قبل الفجر بـ 45 دقيقة",
            category = "صلوات النوافل والقيام",
            iconEmoji = "🌙",
            prayer = Prayer.Fajr,
            durationMinutes = 45,
            durationModifier = -1,
        ),
        SunnahPreset(
            id = ID_DUHA,
            title = "صلاة الضحى (الأوابين)",
            subtitle = "بعد شروق الشمس بـ 20 دقيقة",
            category = "صلوات النوافل والقيام",
            iconEmoji = "☀️",
            prayer = Prayer.Sunrise,
            durationMinutes = 20,
            durationModifier = 1,
        ),
        // صيام التطوع والسنن
        SunnahPreset(
            id = ID_MON_THU,
            title = "صيام الإثنين والخميس",
            subtitle = "تبييت النية وتجهيز السحور • مساء الأحد والأربعاء",
            category = "صيام التطوع والسنن",
            iconEmoji = "🌿",
            prayer = Prayer.Isha,
            durationMinutes = 60,
            durationModifier = 1,
            days = setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY),
        ),
        SunnahPreset(
            id = ID_WHITE_DAYS,
            title = "صيام الأيام البيض (13، 14، 15)",
            subtitle = "شهرياً بحسب التقويم الهجري المبارك",
            category = "صيام التطوع والسنن",
            iconEmoji = "🌕",
            prayer = Prayer.Maghrib,
            durationMinutes = 30,
            durationModifier = 1,
        ),
        // سنن يوم الجمعة المبارك
        SunnahPreset(
            id = ID_KAHF,
            title = "سورة الكهف",
            subtitle = "نورٌ ما بين الجمعتين • صباح كل جمعة",
            category = "سنن يوم الجمعة المبارك",
            iconEmoji = "📖",
            prayer = Prayer.Sunrise,
            durationMinutes = 180,
            durationModifier = 1,
            days = setOf(DayOfWeek.FRIDAY),
            isQuranSurah = true,
            surahNumber = 18,
        ),
        SunnahPreset(
            id = ID_ISTIJABAH,
            title = "ساعة الإجابة والدعاء",
            subtitle = "آخر ساعة قبل غروب شمس يوم الجمعة",
            category = "سنن يوم الجمعة المبارك",
            iconEmoji = "🤲",
            prayer = Prayer.Maghrib,
            durationMinutes = 60,
            durationModifier = -1,
            days = setOf(DayOfWeek.FRIDAY),
        ),
        // سنن وأذكار المساء والنوم
        SunnahPreset(
            id = ID_MULK,
            title = "سورة الملك",
            subtitle = "المانعة من عذاب القبر • كل ليلة قبل النوم",
            category = "سنن وأذكار المساء والنوم",
            iconEmoji = "🌌",
            prayer = Prayer.Isha,
            durationMinutes = 90,
            durationModifier = 1,
            isQuranSurah = true,
            surahNumber = 67,
        ),
        // الأذكار اليومية
        SunnahPreset(
            id = ID_AZKAR,
            title = "أذكار الصباح والمساء",
            subtitle = "بعد صلاة الفجر وبعد صلاة العصر",
            category = "الأذكار اليومية",
            iconEmoji = "📿",
            prayer = Prayer.Fajr,
            durationMinutes = 15,
            durationModifier = 1,
            isAzkar = true,
        ),
    )

    fun toReminder(preset: SunnahPreset, enabled: Boolean = true): Reminder {
        return Reminder(
            id = preset.id,
            label = preset.title,
            enabled = enabled,
            prayer = preset.prayer,
            duration = preset.durationMinutes,
            durationModifier = preset.durationModifier,
            days = PrayerAlarmSettings.fromDays(preset.days),
        )
    }
}

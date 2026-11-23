package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER

/**
 * The five obligatory prayers that are armed (adhan sound + notification) when onboarding completes on a
 * fresh install. Sunrise, sunset, midnight and tahajjud stay off: they are not prayers a user expects an
 * adhan for, and can still be switched on later from Settings.
 */
val DEFAULT_ADHAN_PRAYERS: List<Prayer> =
    listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)

/** True when the user (or a restored backup) already switched any sound or notification on. */
fun AlarmSettings.hasAnyPrayerScheduleConfigured(): Boolean =
    SHARIA_TIMES_IN_ORDER.any { prayer ->
        getNotifSettings(prayer).selectedDays().isNotEmpty() ||
            getSoundSettings(prayer).selectedDays().isNotEmpty()
    }

/** True when at least one prayer has its Adhan sound enabled. */
fun AlarmSettings.hasAnyPrayerSoundConfigured(): Boolean =
    SHARIA_TIMES_IN_ORDER.any { prayer ->
        getSoundSettings(prayer).selectedDays().isNotEmpty()
    }

/**
 * Arms [DEFAULT_ADHAN_PRAYERS] with sound **and** notification, on every day of the week.
 *
 * Both are always written together on purpose: the scheduler only arms an adhan when at least one
 * *notification* is enabled (`AdhanScheduler.hasAnyNotification`), and the toggle UI enforces the same
 * coupling (enabling a sound forces its notification on). A sound without its notification would never fire.
 *
 * This is deliberately an explicit write rather than a change to the `AlarmSettings` constructor defaults:
 * the settings store does not encode defaults (`encodeDefaults` is off), so changing a default would silently
 * flip the stored value of every existing user who left that field untouched, and would also change what a
 * restored backup resolves to. Vibration is left on the global default ([VibrationMode.Once]).
 *
 * Idempotent and non-destructive: if any prayer sound is already configured the settings are returned untouched.
 */
fun AlarmSettings.withDefaultAdhanSchedule(): AlarmSettings {
    if (hasAnyPrayerSoundConfigured()) return this
    return DEFAULT_ADHAN_PRAYERS.fold(this) { acc, prayer ->
        acc
            .setSoundSettings(prayer, PrayerAlarmSettings.Bool(true))
            .setNotifSettings(prayer, PrayerAlarmSettings.Bool(true))
    }
}

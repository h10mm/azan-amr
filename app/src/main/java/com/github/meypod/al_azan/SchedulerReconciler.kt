package com.github.meypod.al_azan

import android.os.SystemClock
import android.util.Log
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.alarm.DndSilenceController
import com.github.meypod.al_azan.alarm.MissedAlarmCatchUp
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.ramadan.RamadanNoticeScheduler
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Re-derives every scheduled alarm from the current settings + permission state. Used by the events
 * that invalidate the platform's alarm set or its scheduling capability: boot, time/timezone change,
 * and exact-alarm permission changes.
 *
 * Because [com.github.meypod.al_azan.core.data.repository.AlarmRepositoryImpl] picks the alarm type at
 * registration (exact when allowed, inexact otherwise), re-running the schedulers here reconciles
 * out-of-order permission grants/revokes: a later exact-alarm grant upgrades the alarms to exact, and a
 * revoke degrades them to inexact — no matter which permission the user toggled first.
 */
@Singleton
class SchedulerReconciler @Inject constructor(
    private val adhanScheduler: AdhanScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val ramadanNoticeScheduler: RamadanNoticeScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val dndSilenceController: DndSilenceController,
    private val missedAlarmCatchUp: MissedAlarmCatchUp,
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val localizedResources: LocalizedResources,
) {
    private val reconcileMutex = Mutex()
    private val notifiedSkippedTimestamps = mutableSetOf<Long>()

    @Volatile
    private var lastManualTimeChangeRealtime: Long = 0L

    fun markManualTimeChange() {
        lastManualTimeChangeRealtime = SystemClock.elapsedRealtime()
    }

    fun wasTimeManuallyChangedRecently(): Boolean {
        val last = lastManualTimeChangeRealtime
        return last > 0L && (SystemClock.elapsedRealtime() - last) < 5_000L
    }

    /**
     * Handles manual wall-clock or timezone changes:
     * 1. If the clock was advanced forward past an armed Adhan time, posts a "Missed Adhan" notice.
     * 2. If the clock was rewound backward before previously delivered timestamps, prunes those
     *    future timestamps from [Settings.deliveredAlarmTimestamps] so the Adhan can fire again.
     * 3. Reconciles all schedules and widgets.
     */
    suspend fun onSystemTimeChanged() {
        reconcileMutex.withLock {
            val now = System.currentTimeMillis()
            val scheduledAlarms = runCatching { alarmRepository.getScheduled() }.getOrDefault(emptyList())
            val adhanAlarm = scheduledAlarms.firstOrNull { it.action == AdhanContract.ACTION_ADHAN }
            var skippedEntry: Pair<String, Long>? = null
            if (adhanAlarm != null) {
                val triggerAt = adhanAlarm.triggerAtMillis
                val prayerName = adhanAlarm.extras[AdhanContract.EXTRA_PRAYER]
                val prayer = prayerName?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
                val passedPrayerMinute =
                    now > triggerAt && (now - triggerAt >= 60_000L || now / 60_000L > triggerAt / 60_000L)
                if (prayer != null && passedPrayerMinute) {
                    postMissedAdhanNotificationLocked(prayer, triggerAt, adhanAlarm.id)
                    skippedEntry = adhanAlarm.id to triggerAt
                }
            }

            // Clear delivered timestamps on manual clock change (keeping only a just-skipped prayer)
            // so rewinding the clock always re-arms and fires the prayer again.
            settingsRepository.update { current ->
                val newDelivered = if (skippedEntry != null) mapOf(skippedEntry) else emptyMap()
                current.copy(deliveredAlarmTimestamps = newDelivered)
            }
            if (skippedEntry == null) {
                notifiedSkippedTimestamps.clear()
            }

            reconcileAllInternal(catchUpMissed = false)
        }
    }

    suspend fun onAdhanTimeSkipped(
        prayer: Prayer,
        timestamp: Long,
    ) {
        reconcileMutex.withLock {
            val scheduledAlarms = runCatching { alarmRepository.getScheduled() }.getOrDefault(emptyList())
            val adhanAlarmId = scheduledAlarms.firstOrNull { it.action == AdhanContract.ACTION_ADHAN }?.id ?: "adhan"
            postMissedAdhanNotificationLocked(prayer, timestamp, adhanAlarmId)
            reconcileAllInternal(catchUpMissed = false)
        }
    }

    private suspend fun postMissedAdhanNotificationLocked(
        prayer: Prayer,
        timestamp: Long,
        alarmId: String,
    ) {
        val currentSettings = settingsRepository.fetch()
        val alreadyDelivered = currentSettings.deliveredAlarmTimestamps.values.any {
            it != null && abs(it - timestamp) < 30_000L
        }
        if (alreadyDelivered || !notifiedSkippedTimestamps.add(timestamp)) {
            return
        }
        settingsRepository.update { s ->
            s.copy(
                deliveredAlarmTimestamps = s.deliveredAlarmTimestamps + (alarmId to timestamp),
            )
        }
        val localized = localizedResources.current
        val prayerLabel = localized.getString(prayer.stringRes)
        val formattedTime = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(timestamp))
        val title = "إشعار أذان لم ينطلق — صلاة $prayerLabel"
        val body = "تم تجاوز وقت أذان $prayerLabel ($formattedTime) ولم ينطلق الإشعار"
        Log.d("SchedulerReconciler", "Posting missed Adhan notification for $prayer at $formattedTime")
        notificationRepository.notify(
            NotificationConfig(
                id = "missed_adhan_${prayer.name}_$timestamp",
                title = TextResource.Literal(title),
                body = TextResource.Literal(body),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID,
                    timestamp = System.currentTimeMillis(),
                    autoCancel = true,
                ),
            ),
        )
    }

    /**
     * @param catchUpMissed when true (boot only), first post "missed" notices for alarms that elapsed
     *   while the device was off — before the schedulers below overwrite their past-due armed entries.
     */
    suspend fun reconcileAll(catchUpMissed: Boolean = false) {
        reconcileMutex.withLock {
            reconcileAllInternal(catchUpMissed)
        }
    }

    private suspend fun reconcileAllInternal(catchUpMissed: Boolean) {
        if (catchUpMissed) missedAlarmCatchUp.catchUpMissed()
        adhanScheduler.schedule()
        reminderScheduler.schedule()
        ramadanNoticeScheduler.schedule()
        widgetUpdater.update()
        // Reboot strips the silence window's unsilence alarm + control notice while the suppression
        // state persists; rebuild it (or clean up if it already elapsed) so the user can still end it.
        dndSilenceController.reconcile()
    }
}

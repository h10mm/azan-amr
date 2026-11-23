package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.data.audio.AdhanPreviewPlaybackService
import com.github.meypod.al_azan.core.data.audio.SoftSoundPlayer
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.settings.SILENT_AUDIO_ID
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.playback.SoftSoundContract
import com.github.meypod.al_azan.ramadan.RamadanNoticeContract
import com.github.meypod.al_azan.ramadan.RamadanNoticeHandler
import com.github.meypod.al_azan.reminder.ReminderContract
import com.github.meypod.al_azan.reminder.ReminderFiringHandler
import com.github.meypod.al_azan.widget.WidgetContract
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for every fired [com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm].
 *
 * AlarmManager starts the process to deliver this broadcast even after a memory-kill, so the work runs
 * in the same wake-up — no deferrable worker in between.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_STOP_ADHAN_SOUND = "com.github.meypod.al_azan.action.STOP_ADHAN_SOUND"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var adhanFiringHandler: AdhanFiringHandler

    @Inject
    lateinit var reminderFiringHandler: ReminderFiringHandler

    @Inject
    lateinit var ramadanNoticeHandler: RamadanNoticeHandler

    @Inject
    lateinit var softSoundPlayer: SoftSoundPlayer

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        when (intent?.action) {
            WidgetContract.ACTION_WIDGET_UPDATE -> async { widgetUpdater.update() }

            AdhanContract.ACTION_ADHAN -> {
                val prayer = intent.prayer() ?: return
                val timestamp = intent.getStringExtra(AdhanContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async {
                    val now = System.currentTimeMillis()
                    val maxNaturalDelayMs = 15 * 60_000L
                    if (now > timestamp && (now - timestamp >= maxNaturalDelayMs || now / 60_000L > timestamp / 60_000L)) {
                        kotlinx.coroutines.delay(250)
                        if (now - timestamp >= maxNaturalDelayMs || schedulerReconciler.wasTimeManuallyChangedRecently()) {
                            schedulerReconciler.onAdhanTimeSkipped(prayer, timestamp)
                            return@async
                        }
                    }
                    val settings = settingsRepository.data.first()
                    val selectedEntry = settings.selectedAdhanEntries[prayer.toAdhanKey()]
                        ?: settings.selectedAdhanEntries[AdhanKey.Default]
                    val shouldPlaySound = selectedEntry?.id != SILENT_AUDIO_ID
                    if (shouldPlaySound) {
                        settingsRepository.update { s ->
                            s.copy(
                                deliveredAlarmTimestamps = s.deliveredAlarmTimestamps.filterValues { ts ->
                                    ts != null && kotlin.math.abs(ts - timestamp) > 60_000L && ts < now - 60_000L
                                },
                            )
                        }
                        adhanFiringHandler.onAdhanFired(prayer, false, timestamp)
                    }
                }
            }

            AdhanContract.ACTION_PRE_ADHAN -> {
                val prayer = intent.prayer() ?: return
                val timestamp = intent.getStringExtra(AdhanContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { adhanFiringHandler.onPreAdhanFired(prayer, timestamp) }
            }

            AdhanContract.ACTION_CANCEL_ADHAN -> async { adhanFiringHandler.onCancelAdhan() }

            ACTION_STOP_ADHAN_SOUND -> {
                softSoundPlayer.stop()
                PlaybackService.stop(context)
                AdhanPreviewPlaybackService.stop(context)
                intent.getStringExtra(EXTRA_NOTIFICATION_ID)?.toIntOrNull()?.let { notifId ->
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
            }

            AdhanContract.ACTION_UNSILENCE -> async { adhanFiringHandler.onUnsilence() }

            AdhanContract.ACTION_ADHAN_REMIND -> {
                val prayer = intent.prayer() ?: return
                val minutes = intent.getStringExtra(AdhanContract.EXTRA_REMIND_MINUTES)?.toIntOrNull() ?: 0
                async { adhanFiringHandler.onAdhanRemindFired(prayer, minutes) }
            }

            ReminderContract.ACTION_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                val timestamp = intent.getStringExtra(ReminderContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { reminderFiringHandler.onReminderFired(reminderId, timestamp) }
            }

            ReminderContract.ACTION_PRE_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                val timestamp = intent.getStringExtra(ReminderContract.EXTRA_TIMESTAMP)?.toLongOrNull()
                    ?: System.currentTimeMillis()
                async { reminderFiringHandler.onPreReminderFired(reminderId, timestamp) }
            }

            WidgetContract.ACTION_DISMISS_SPIRITUAL_PILL -> {
                val momentKey = intent.getStringExtra(WidgetContract.EXTRA_MOMENT_KEY)
                if (!momentKey.isNullOrBlank()) {
                    com.github.meypod.al_azan.widget.SpiritualPillDismissStore.markDismissed(context, momentKey)
                }
            }

            ReminderContract.ACTION_CANCEL_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderContract.EXTRA_REMINDER_ID) ?: return
                async { reminderFiringHandler.onCancelReminder(reminderId) }
            }

            // The user swiped a soft sound's notification away. Stopping happens inline — the sound is
            // short, so handing it to a coroutine would let it outlive the gesture.
            SoftSoundContract.ACTION_STOP_SOFT_SOUND -> {
                softSoundPlayer.stop()
                PlaybackService.stop(context)
            }

            RamadanNoticeContract.ACTION_RAMADAN_CHECK -> async { ramadanNoticeHandler.onCheckFired() }

            RamadanNoticeContract.ACTION_RAMADAN_REMIND_NEXT_YEAR -> async { ramadanNoticeHandler.onRemindNextYear() }

            RamadanNoticeContract.ACTION_RAMADAN_DONT_SHOW_AGAIN -> async { ramadanNoticeHandler.onDontShowAgain() }
        }
    }

    private fun Intent.prayer(): Prayer? =
        getStringExtra(AdhanContract.EXTRA_PRAYER)?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }

    /** Runs [block] off the main thread while keeping the broadcast alive via goAsync(). */
    private fun async(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Alarm handling failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

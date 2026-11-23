package com.github.meypod.al_azan.core.domain.usecase

import android.content.Context
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.audio.toAudioUri
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationImportance
import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig
import com.github.meypod.al_azan.core.domain.model.settings.SILENT_AUDIO_ID
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class EnsureNotificationChannelsUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val channelManager: NotificationChannelManager,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val PERMISSION_REVOKED_CHANNEL_ID = "permission_revoked_channel_id"
        const val TRAVEL_MODE_CHANNEL_ID = "travel_mode_channel_id"
        const val WIDGET_CHANNEL_ID = "widget_shade_channel_v1"
        private val LEGACY_WIDGET_CHANNEL_IDS = listOf(
            "widget_channel_id",
            "widget_channel_v1",
            "widget_channel_v2",
            "widget_channel_v6",
        )
        const val ADHAN_CHANNEL_ID = "adhan_shade_channel_v5"
        const val ADHAN_DND_CHANNEL_ID = "adhan_dnd_shade_channel_v5"
        private val LEGACY_ADHAN_CHANNEL_IDS = listOf(
            "adhan_channel_id",
            "adhan_dnd_channel_id",
            "adhan_channel_high_v2",
            "adhan_dnd_channel_high_v2",
            "adhan_native_channel_v1",
            "adhan_dnd_native_channel_v1",
            "adhan_shade_channel_v3",
            "adhan_dnd_shade_channel_v3",
            "adhan_shade_channel_v4",
            "adhan_dnd_shade_channel_v4",
        )
        const val PRE_ADHAN_CHANNEL_ID = "pre_adhan_channel_id"
        const val ADHAN_REMIND_CHANNEL_ID = "adhan_remind_channel_id"
        const val REMINDER_CHANNEL_ID = "reminder_channel_id"
        const val REMINDER_DND_CHANNEL_ID = "reminder_dnd_channel_id"
        const val PRE_REMINDER_CHANNEL_ID = "pre_reminder_channel_id"
        const val MISSED_CHANNEL_ID = "missed_channel_id"
        const val DND_ACTIVE_CHANNEL_ID = "dnd_active_channel_id"
        const val RAMADAN_NOTICE_CHANNEL_ID = "ramadan_notice_channel_id"
        const val IMPORTANT_NOTICE_CHANNEL_ID = "important_notice_channel_id"
    }

    operator fun invoke() {
        LEGACY_ADHAN_CHANNEL_IDS.forEach { channelManager.deleteChannel(it) }
        LEGACY_WIDGET_CHANNEL_IDS.forEach { channelManager.deleteChannel(it) }

        val settings = runCatching { runBlocking { settingsRepository.data.first() } }.getOrNull()
        val defaultAudioUri = "android.resource://${context.packageName}/${R.raw.amr}".toUri()
        val soundEntry = settings?.selectedAdhanEntries?.get(AdhanKey.Default)
            ?: settings?.savedAdhanAudioEntries?.firstOrNull()
        val isSilent = soundEntry?.id == SILENT_AUDIO_ID
        val adhanUri = if (isSilent) null else (soundEntry?.toAudioUri(context) ?: defaultAudioUri)

        channelManager.ensureChannelsExist(
            configs = listOf(
                NotificationChannelConfig(
                    id = PERMISSION_REVOKED_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.permission_revoked_channel_name),
                    description = TextResource.StringResId(R.string.permission_revoked_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = TRAVEL_MODE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.travel_mode_channel_name),
                    description = TextResource.StringResId(R.string.travel_mode_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = WIDGET_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.widget_channel_name),
                    description = TextResource.StringResId(R.string.widget_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                    showBadge = false,
                    vibrationEnabled = false,
                    soundEnabled = false,
                ),
                NotificationChannelConfig(
                    id = ADHAN_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_channel_name),
                    description = TextResource.StringResId(R.string.adhan_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                    soundEnabled = false,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = ADHAN_DND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_dnd_channel_name),
                    description = TextResource.StringResId(R.string.adhan_dnd_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                    soundEnabled = false,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = PRE_ADHAN_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.pre_adhan_channel_name),
                    description = TextResource.StringResId(R.string.pre_adhan_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                ),
                NotificationChannelConfig(
                    id = ADHAN_REMIND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_remind_channel_name),
                    description = TextResource.StringResId(R.string.adhan_remind_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = REMINDER_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.reminder_channel_name),
                    description = TextResource.StringResId(R.string.reminder_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_MAX,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                ),
                NotificationChannelConfig(
                    id = REMINDER_DND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.reminder_dnd_channel_name),
                    description = TextResource.StringResId(R.string.reminder_dnd_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_MAX,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = PRE_REMINDER_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.pre_reminder_channel_name),
                    description = TextResource.StringResId(R.string.pre_reminder_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                ),
                // Silent, low-importance: informs the user of adhan/reminders that passed during a
                // "Dismiss & silent" window without making a sound or punching through Do Not Disturb.
                NotificationChannelConfig(
                    id = MISSED_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.missed_channel_name),
                    description = TextResource.StringResId(R.string.missed_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                    vibrationEnabled = false,
                    soundHandledExternally = true,
                ),
                // Silent, low-importance, and DND-bypassing: this is the "Do Not Disturb on" control
                // notice itself, which must stay reachable while OUR total-silence DND window hides
                // everything else — so the user can always turn it off / swipe to restore sound.
                NotificationChannelConfig(
                    id = DND_ACTIVE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.dnd_active_channel_name),
                    description = TextResource.StringResId(R.string.dnd_active_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_MAX,
                    soundHandledExternally = true,
                    showBadge = false,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = RAMADAN_NOTICE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.ramadan_notice_channel_name),
                    description = TextResource.StringResId(R.string.ramadan_notice_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                // One-off notices about changes that alter the times the app shows, so the user is told
                // rather than left to notice a silent shift.
                NotificationChannelConfig(
                    id = IMPORTANT_NOTICE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.important_notice_channel_name),
                    description = TextResource.StringResId(R.string.important_notice_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
            ),
        )
    }
}

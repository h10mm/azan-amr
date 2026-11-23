package com.github.meypod.al_azan.core.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.AlarmReceiver
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.audio.AdhanPreviewPlaybackService
import com.github.meypod.al_azan.core.data.audio.toAudioUri
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.data.mapping.asString
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.notification.toNotificationCompat
import com.github.meypod.al_azan.core.domain.model.settings.SILENT_AUDIO_ID
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.util.randomString
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val activityClassForIntents: Class<*>,
    private val localizedResources: LocalizedResources,
    private val settingsRepository: SettingsRepository,
) : NotificationRepository {

    companion object {
        const val TAG = "NotificationRepositoryImpl"
    }

    private val notifManagerCompact by lazy { NotificationManagerCompat.from(context) }

    @SuppressLint("MissingPermission")
    override suspend fun notify(payload: NotificationConfig) {
        val channelId =
            payload.android?.channelId
                ?: throw IllegalArgumentException("android notification config should define channelId")

        val isAdhanChannel =
            channelId == EnsureNotificationChannelsUseCase.ADHAN_CHANNEL_ID ||
                channelId == EnsureNotificationChannelsUseCase.ADHAN_DND_CHANNEL_ID

        val allowed = isNotificationsAllowed()
        val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        Log.d(TAG, "notify() called. isNotificationsAllowed: $allowed, areNotificationsEnabled: $areEnabled, payload.id: ${payload.id}, channelId: $channelId")
        if (!allowed && !isAdhanChannel) {
            Log.w(TAG, "notify() aborted: notifications not allowed")
            return
        }
        val notificationId = payload.id.let { if (it.isNullOrBlank()) randomString(8) else it }.hashCode()

        val hasCustomView =
            payload.android.customContentView != null || payload.android.customBigContentView != null

        val stopAction = NotificationPressAction.Broadcast(
            action = AlarmReceiver.ACTION_STOP_ADHAN_SOUND,
            requestCode = AlarmReceiver.ACTION_STOP_ADHAN_SOUND.hashCode() xor notificationId,
            extras = mapOf(AlarmReceiver.EXTRA_NOTIFICATION_ID to notificationId.toString()),
        )
        val stopPendingIntent = createBroadcastPendingIntent(stopAction)

        val builder =
            NotificationCompat
                .Builder(context, channelId)
                .setSmallIcon(R.drawable.monochrome_notif)
                .apply {
                    if (isAdhanChannel) {
                        priority = NotificationCompat.PRIORITY_DEFAULT
                        setSilent(true)
                        setCategory(NotificationCompat.CATEGORY_ALARM)
                        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    }
                }

        // String resources must resolve in the app language; the plain application context doesn't
        // carry the per-app locale on pre-API 33.
        val localized = localizedResources.current
        var rawTitleForPlayback = ""

        val isPreAdhanChannel =
            channelId == EnsureNotificationChannelsUseCase.PRE_ADHAN_CHANNEL_ID ||
                channelId == EnsureNotificationChannelsUseCase.PRE_REMINDER_CHANNEL_ID

        val isReminderChannel =
            channelId == EnsureNotificationChannelsUseCase.REMINDER_CHANNEL_ID ||
                channelId == EnsureNotificationChannelsUseCase.REMINDER_DND_CHANNEL_ID

        val contentPendingIntent = if (!isAdhanChannel) {
            when (val action = payload.android.pressAction) {
                is NotificationPressAction.Route -> createNavDeepLinkPendingIntent(action.route)
                is NotificationPressAction.Default -> createNavDeepLinkPendingIntent(Route.Main.Home)
                is NotificationPressAction.Broadcast -> createBroadcastPendingIntent(action)
                null -> createNavDeepLinkPendingIntent(Route.Main.Home)
            }
        } else null

        if (isAdhanChannel) {
            val rawTitle = payload.title?.asString(localized) ?: ""
            rawTitleForPlayback = rawTitle
            val adhanTitle = when {
                rawTitle.startsWith("موعد أذان") -> rawTitle
                rawTitle.startsWith("أذان") -> "موعد $rawTitle"
                rawTitle.isNotBlank() -> "موعد أذان $rawTitle"
                else -> localized.getString(R.string.adhan)
            }
            val eventTimeMillis = payload.android.timestamp ?: System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = eventTimeMillis }
            val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
            val suffix = if (isAm) " ص" else " م"
            val timeDigits = java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(java.util.Date(eventTimeMillis))
            val formattedTime = "$timeDigits$suffix"
            val subText = "حيّ على الصلاة • حيّ على الفلاح"

            val customViews = RemoteViews(context.packageName, R.layout.notif_adhan_custom).apply {
                setTextViewText(R.id.adhan_notif_title, adhanTitle)
                setTextViewText(R.id.adhan_notif_sub, subText)
                setTextViewText(R.id.adhan_notif_time, formattedTime)
            }

            builder.setContentTitle(adhanTitle)
            builder.setContentText(formattedTime)
            builder.setCustomContentView(customViews)
            builder.setCustomBigContentView(customViews)
            builder.setLargeIcon(null as android.graphics.Bitmap?)
            builder.setContentIntent(null)
            builder.setDeleteIntent(null)
            builder.setAutoCancel(true)
            builder.setOngoing(false)
            builder.setDefaults(0)
        } else if (isPreAdhanChannel) {
            val rawBody = payload.body?.asString(localized) ?: ""
            val parts = rawBody.split(",")
            val prayerName = parts.firstOrNull()?.trim() ?: ""
            val timeText = if (parts.size > 1) parts[1].trim() else ""
            val timeSuffix = if (timeText.isNotBlank()) " ($timeText)" else ""
            val title = if (prayerName.isNotBlank()) "اقتراب موعد صلاة $prayerName$timeSuffix" else "اقتراب موعد الصلاة$timeSuffix"
            val subText = "استعد للوضوء وصلاة الجماعة"

            val preAdhanClickIntent = createNavDeepLinkPendingIntent(Route.Main.UpcomingAlarms)
            val customViews = RemoteViews(context.packageName, R.layout.notif_pre_adhan_custom).apply {
                setTextViewText(R.id.pre_adhan_title, title)
                setTextViewText(R.id.pre_adhan_sub, subText)
                setOnClickPendingIntent(R.id.pre_adhan_notif_root, preAdhanClickIntent)
            }

            builder.setContentTitle(title)
            builder.setContentText(subText)
            builder.setCustomContentView(customViews)
            builder.setCustomBigContentView(customViews)
            builder.setContentIntent(preAdhanClickIntent)
            builder.setLargeIcon(null as android.graphics.Bitmap?)
        } else if (isReminderChannel) {
            val title = payload.title?.asString(localized).orEmpty()
            val body = payload.body?.asString(localized).orEmpty()

            val customSmall = RemoteViews(context.packageName, R.layout.notif_spiritual_pill_centered).apply {
                setTextViewText(R.id.smart_pill_title, title)
                setTextViewText(R.id.smart_pill_subtitle, body)
                contentPendingIntent?.let { setOnClickPendingIntent(R.id.widget_smart_pill_layout, it) }
            }
            val customBig = RemoteViews(context.packageName, R.layout.notif_spiritual_pill_expanded).apply {
                setTextViewText(R.id.smart_pill_expanded_title, title)
                setTextViewText(R.id.smart_pill_expanded_body, body)
                contentPendingIntent?.let { setOnClickPendingIntent(R.id.widget_smart_pill_expanded_root, it) }
            }

            builder.setContentTitle(title)
            builder.setContentText(body)
            builder.setCustomContentView(customSmall)
            builder.setCustomBigContentView(customBig)
            contentPendingIntent?.let { builder.setContentIntent(it) }
            builder.setLargeIcon(null as android.graphics.Bitmap?)

            // Smart De-duplication: Dismiss background companion pill so it never duplicates active reminder
            runCatching {
                NotificationManagerCompat.from(context).cancel("spiritual_pill_companion_v2".hashCode())
            }
        } else if (hasCustomView) {
            payload.android.customBigContentView?.let { builder.setCustomBigContentView(it) }
            payload.android.customContentView?.let { builder.setCustomContentView(it) }
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        val hasAnyCustomLayout = hasCustomView || isAdhanChannel || isPreAdhanChannel || isReminderChannel

        if (!isAdhanChannel) {
            payload.title?.let { builder.setContentTitle(it.asString(localized)) }
            payload.subtitle?.let { builder.setSubText(it.asString(localized)) }
            payload.body?.let {
                val body = it.asString(localized)
                builder.setContentText(body)
                if (!hasAnyCustomLayout) {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                }
            }
        }
        payload.badgeCount?.let { builder.setNumber(it) }
        builder.setOngoing(if (isAdhanChannel) false else payload.android.ongoing)
        builder.setCategory(if (isAdhanChannel) NotificationCompat.CATEGORY_ALARM else payload.android.category.toNotificationCompat())
        builder.setOnlyAlertOnce(if (isAdhanChannel) false else payload.android.onlyAlertOnce)
        builder.setShowWhen(payload.android.showTimestamp)
        payload.android.timestamp?.let { builder.setWhen(it) }
        builder.setGroup(payload.android.group)
        builder.setVisibility(if (isAdhanChannel) NotificationCompat.VISIBILITY_PUBLIC else payload.android.visibility.toNotificationCompat())
        builder.setAutoCancel(if (isAdhanChannel) true else payload.android.autoCancel)
        if (!isAdhanChannel) {
            payload.android.timeoutAfterMillis?.let { builder.setTimeoutAfter(it) }
        }
        payload.android.sortKey?.let { builder.setSortKey(it) }

        if (!isAdhanChannel) {
            contentPendingIntent?.let { builder.setContentIntent(it) }

            when (val dismissAction = payload.android.dismissAction) {
                is NotificationPressAction.Broadcast ->
                    builder.setDeleteIntent(createBroadcastPendingIntent(dismissAction))

                is NotificationPressAction.Route ->
                    builder.setDeleteIntent(createNavDeepLinkPendingIntent(dismissAction.route))

                else -> {}
            }
        }

        val actionsList = payload.android.actions.orEmpty()
        // Adhan notifications stay clean and button-less per user requirement (swipe to dismiss stops audio).
        if (!isAdhanChannel && actionsList.isNotEmpty()) {
            actionsList.forEach { action ->
                val pendingIntent: PendingIntent? = when (val pressAction = action.pressAction) {
                    is NotificationPressAction.Route -> createNavDeepLinkPendingIntent(pressAction.route)
                    is NotificationPressAction.Broadcast -> createBroadcastPendingIntent(pressAction)
                    else -> createNavDeepLinkPendingIntent(Route.Main.Home)
                }
                pendingIntent?.let {
                    builder.addAction(NotificationCompat.Action(null, action.title.asString(localized), it))
                }
            }
        }

        try {
            val builtNotification = builder.build()
            if (allowed) {
                runCatching {
                    Log.d(TAG, "Attempting NotificationManagerCompat.notify($notificationId)...")
                    NotificationManagerCompat.from(context).notify(notificationId, builtNotification)
                    Log.d(TAG, "NotificationManagerCompat.notify($notificationId) succeeded!")
                }.onFailure { e ->
                    Log.e(TAG, "NotificationManagerCompat.notify($notificationId) failed, continuing playback", e)
                }
            }
            if (isAdhanChannel) {
                val settings = runCatching { settingsRepository.data.first() }.getOrNull()
                val matchedPrayer = Prayer.entries.firstOrNull { localized.getString(it.stringRes) == rawTitleForPlayback }
                val soundEntry = (matchedPrayer?.let { settings?.selectedAdhanEntries?.get(it.toAdhanKey()) })
                    ?: settings?.selectedAdhanEntries?.get(AdhanKey.Default)
                    ?: settings?.savedAdhanAudioEntries?.firstOrNull()
                val isSilent = soundEntry?.id == SILENT_AUDIO_ID
                val defaultAudioUri = "android.resource://${context.packageName}/${R.raw.amr}".toUri()
                val adhanUri = if (isSilent) null else (soundEntry?.toAudioUri(context) ?: defaultAudioUri)
                if (adhanUri != null) {
                    AdhanPreviewPlaybackService.playAdhanAlarm(
                        context = context,
                        uri = adhanUri,
                        volumePercent = settings?.alarmVolume ?: -1,
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push notification with ID $notificationId", e)
        }
    }

    override suspend fun cancelNotification(notificationId: String) {
        notifManagerCompact.cancel(notificationId.hashCode())
    }

    override suspend fun isNotificationsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun createNavDeepLinkPendingIntent(route: DeepLinkableRoute): PendingIntent {
        val link = route.toUriString()
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                link.toUri(),
                context,
                activityClassForIntents,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                link.hashCode(),
                intent,
                pendingIntentFlags,
            )

        return pendingIntent
    }

    fun createBroadcastPendingIntent(action: NotificationPressAction.Broadcast) =
        PendingIntentCompat.getBroadcast(
            context,
            action.requestCode,
            // Target our own package so it reaches a manifest receiver despite O+ implicit-broadcast limits.
            Intent(action.action).setPackage(context.packageName).apply {
                action.extras.forEach { (key, value) -> putExtra(key, value) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT,
            false,
        )
}

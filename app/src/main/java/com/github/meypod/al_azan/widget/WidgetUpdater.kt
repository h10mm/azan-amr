package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData
import com.github.meypod.al_azan.core.domain.model.widget.SpiritualCapsuleMode
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CustomWidgetConfigRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NateegaWidgetConfigRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.BuildCustomWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.BuildNateegaWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.BuildNextPrayerWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.BuildWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Recomputes the prayer-times widgets and applies them: redraws the home-screen app widget, posts or
 * cancels the persistent notification widget per the user's toggle, and schedules the next redraw.
 *
 * Runs directly from each trigger (alarm/settings/boot/time/locale/foreground/placement) — no
 * WorkManager — so the redraw happens in the same wake-up that fired it, with no deferral. A [Mutex]
 * serializes concurrent triggers.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val customWidgetConfigRepository: CustomWidgetConfigRepository,
    private val nateegaWidgetConfigRepository: NateegaWidgetConfigRepository,
    private val buildWidgetDataUseCase: BuildWidgetDataUseCase,
    private val buildNextPrayerWidgetDataUseCase: BuildNextPrayerWidgetDataUseCase,
    private val buildCustomWidgetDataUseCase: BuildCustomWidgetDataUseCase,
    private val buildNateegaWidgetDataUseCase: BuildNateegaWidgetDataUseCase,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }
    private val mutex = Mutex()

    private companion object {
        const val TAG = "WidgetUpdater"
    }

    suspend fun update() =
        mutex.withLock {
            val settings = settingsRepository.data.first()
            val calcSettings = calculationSettingsRepository.data.first()
            val locations = favoriteLocationsRepository.data.first()
            val location = locations.firstOrNull { it.id == calcSettings.locationId }?.locationDetail
            val now = Clock.System.now()
            // Adaptive (Material You) layouts only exist under -v31; on older devices their theme is
            // missing and the widget fails to render. Force the non-adaptive layout there regardless of
            // a stored "on" value (the setting is hidden pre-31, but old data may still carry it).
            val adaptiveSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            val nateegaIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NateegaWidget::class.java))
            val nateegaClassicIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NateegaClassicWidget::class.java))
            val nateegaAmoledIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NateegaAmoledWidget::class.java))
            val nateegaMidnightIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NateegaMidnightWidget::class.java))
            val compactPrayerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CompactPrayerWidget::class.java))
            val compactPrayerClassicIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CompactPrayerClassicWidget::class.java))
            val compactPrayerAmoledIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CompactPrayerAmoledWidget::class.java))
            val compactPrayerMidnightIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CompactPrayerMidnightWidget::class.java))
            val circlePrayerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NextPrayerCircleWidget::class.java))
            val squarePrayerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NextPrayerSquareWidget::class.java))
            val slimBarIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SlimBarWidget::class.java))
            val mosqueArchIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MosqueArchWidget::class.java))
            val smartPillIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SmartPillWidget::class.java))
            val deskCalIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DeskCalendarWidget::class.java))
            val deskCalSquareIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DeskCalendarSquareWidget::class.java))

            val anyModernPlaced = slimBarIds.isNotEmpty() || mosqueArchIds.isNotEmpty() || smartPillIds.isNotEmpty() ||
                deskCalIds.isNotEmpty() || deskCalSquareIds.isNotEmpty()
            val anyCompactPlaced = compactPrayerIds.isNotEmpty() || compactPrayerClassicIds.isNotEmpty() ||
                compactPrayerAmoledIds.isNotEmpty() || compactPrayerMidnightIds.isNotEmpty()
            val anyNextPrayerPlaced = circlePrayerIds.isNotEmpty() || squarePrayerIds.isNotEmpty()

            val nateegaConfig = nateegaWidgetConfigRepository.data.first()
            val showCompanionPill = nateegaConfig.showSpiritualPillNotification

            val anyNateegaPlaced = nateegaIds.isNotEmpty() || nateegaClassicIds.isNotEmpty() ||
                nateegaAmoledIds.isNotEmpty() || nateegaMidnightIds.isNotEmpty() || anyCompactPlaced || anyModernPlaced ||
                showCompanionPill

            // Compute only the data a live surface actually needs
            val showNotif = settings.showWidget
            val notifLayout = settings.notificationWidgetLayout

            val data = if (showNotif) {
                buildWidgetDataUseCase(now, settings, calcSettings, location)
                    ?.let { if (adaptiveSupported) it else it.copy(adaptiveTheme = false) }
            } else {
                null
            }
            val nextData = if (anyNextPrayerPlaced || (showNotif && notifLayout == NotificationWidgetLayout.Compact)) {
                buildNextPrayerWidgetDataUseCase(now, settings, calcSettings, location, nateegaConfig)
                    ?.let { if (adaptiveSupported) it else it.copy(adaptiveTheme = false) }
            } else {
                null
            }
            val customData = if (showNotif && notifLayout == NotificationWidgetLayout.Custom) {
                val customConfig = customWidgetConfigRepository.data.first()
                buildCustomWidgetDataUseCase(now, settings, calcSettings, location, customConfig, locations)
            } else {
                null
            }
            val nateegaData = if (anyNateegaPlaced) {
                buildNateegaWidgetDataUseCase(now, settings, calcSettings, location, nateegaConfig)
            } else {
                null
            }

            // When times can't be computed, every placed widget shows the same actionable hint (tap →
            // open app) instead of a stale 0:00 placeholder: point at whichever piece is missing.
            val configHintRes = if (calcSettings.parameters == null) {
                R.string.set_calculation_hint
            } else {
                R.string.set_location_hint
            }

            // Notification widget (Primary Prayer Times).
            WidgetRenderCache.lastData = data
            if (data == null || !showNotif) {
                notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
                notificationRepository.cancelNotification("prayer_times_widget_notification")
            } else {
                updateNotification(data, nextData, customData)
            }

            // Companion Notification (Smart Spiritual Capsule - Independent).
            val capsuleMode = nateegaConfig.spiritualCapsuleMode
            updateSpiritualPillNotification(nateegaData, showCompanionPill, capsuleMode)





            // Dedicated Circular and Square Next Prayer Dial Widgets
            NextPrayerRenderCache.lastData = nextData
            if (circlePrayerIds.isNotEmpty()) {
                if (nextData != null) {
                    val circleData = nextData.copy(shape = NextPrayerShape.Circle)
                    for (id in circlePrayerIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            WidgetRenderer.buildNextPrayerWidget(context, circleData, options)
                        )
                    }
                } else {
                    appWidgetManager.updateAppWidget(circlePrayerIds, WidgetRenderer.buildHint(context, configHintRes))
                }
            }
            if (squarePrayerIds.isNotEmpty()) {
                if (nextData != null) {
                    val squareData = nextData.copy(shape = NextPrayerShape.Square)
                    for (id in squarePrayerIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            WidgetRenderer.buildNextPrayerWidget(context, squareData, options)
                        )
                    }
                } else {
                    appWidgetManager.updateAppWidget(squarePrayerIds, WidgetRenderer.buildHint(context, configHintRes))
                }
            }
            NateegaWidgetRenderCache.lastData = nateegaData
            if (nateegaData != null) {
                if (nateegaIds.isNotEmpty()) {
                    val nateegaTheme = when (nateegaConfig.themeMode) {
                        NateegaThemeMode.MatchAppTheme -> settings.themeColor
                        NateegaThemeMode.ClassicPaper -> ThemeColor.ClassicLight
                        NateegaThemeMode.Amoled -> ThemeColor.Dark
                        NateegaThemeMode.MidnightBlue -> ThemeColor.MidnightBlue
                    }
                    val dataToRender = nateegaData.copy(themeColor = nateegaTheme)
                    for (id in nateegaIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            NateegaWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (nateegaClassicIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.ClassicLight)
                    for (id in nateegaClassicIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            NateegaWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (nateegaAmoledIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.Dark)
                    for (id in nateegaAmoledIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            NateegaWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (nateegaMidnightIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.MidnightBlue)
                    for (id in nateegaMidnightIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            NateegaWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (compactPrayerIds.isNotEmpty()) {
                    val compactTheme = when (nateegaConfig.compactThemeMode) {
                        NateegaThemeMode.MatchAppTheme -> settings.themeColor
                        NateegaThemeMode.ClassicPaper -> ThemeColor.ClassicLight
                        NateegaThemeMode.Amoled -> ThemeColor.Dark
                        NateegaThemeMode.MidnightBlue -> ThemeColor.MidnightBlue
                    }
                    val dataToRender = nateegaData.copy(themeColor = compactTheme)
                    for (id in compactPrayerIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            CompactPrayerWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (compactPrayerClassicIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.ClassicLight)
                    for (id in compactPrayerClassicIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            CompactPrayerWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (compactPrayerAmoledIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.Dark)
                    for (id in compactPrayerAmoledIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            CompactPrayerWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                if (compactPrayerMidnightIds.isNotEmpty()) {
                    val dataToRender = nateegaData.copy(themeColor = ThemeColor.MidnightBlue)
                    for (id in compactPrayerMidnightIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            CompactPrayerWidgetRenderer.build(context, dataToRender, options)
                        )
                    }
                }
                // Modern Widgets (each rendered with its own per-instance dimensions):
                if (slimBarIds.isNotEmpty()) {
                    for (id in slimBarIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            ModernWidgetsRenderer.buildSlimBar(context, nateegaData, options)
                        )
                    }
                }
                if (mosqueArchIds.isNotEmpty()) {
                    for (id in mosqueArchIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            ModernWidgetsRenderer.buildMosqueArch(context, nateegaData, options)
                        )
                    }
                }
                if (smartPillIds.isNotEmpty()) {
                    for (id in smartPillIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            ModernWidgetsRenderer.buildSmartPill(context, nateegaData, options)
                        )
                    }
                }
                if (deskCalIds.isNotEmpty()) {
                    for (id in deskCalIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            ModernWidgetsRenderer.buildDeskCalendar(context, nateegaData, isSquare = null, options = options)
                        )
                    }
                }
                if (deskCalSquareIds.isNotEmpty()) {
                    for (id in deskCalSquareIds) {
                        val options = appWidgetManager.getAppWidgetOptions(id)
                        appWidgetManager.updateAppWidget(
                            id,
                            ModernWidgetsRenderer.buildDeskCalendar(context, nateegaData, isSquare = true, options = options)
                        )
                    }
                }
            } else if (anyNateegaPlaced) {
                val hint = NateegaWidgetRenderer.buildHint(context, configHintRes)
                if (nateegaIds.isNotEmpty()) appWidgetManager.updateAppWidget(nateegaIds, hint)
                if (nateegaClassicIds.isNotEmpty()) appWidgetManager.updateAppWidget(nateegaClassicIds, hint)
                if (nateegaAmoledIds.isNotEmpty()) appWidgetManager.updateAppWidget(nateegaAmoledIds, hint)
                if (nateegaMidnightIds.isNotEmpty()) appWidgetManager.updateAppWidget(nateegaMidnightIds, hint)
                if (compactPrayerIds.isNotEmpty()) appWidgetManager.updateAppWidget(compactPrayerIds, hint)
                if (compactPrayerClassicIds.isNotEmpty()) appWidgetManager.updateAppWidget(compactPrayerClassicIds, hint)
                if (compactPrayerAmoledIds.isNotEmpty()) appWidgetManager.updateAppWidget(compactPrayerAmoledIds, hint)
                if (compactPrayerMidnightIds.isNotEmpty()) appWidgetManager.updateAppWidget(compactPrayerMidnightIds, hint)
                if (slimBarIds.isNotEmpty()) appWidgetManager.updateAppWidget(slimBarIds, hint)
                if (mosqueArchIds.isNotEmpty()) appWidgetManager.updateAppWidget(mosqueArchIds, hint)
                if (smartPillIds.isNotEmpty()) appWidgetManager.updateAppWidget(smartPillIds, hint)
                if (deskCalIds.isNotEmpty()) appWidgetManager.updateAppWidget(deskCalIds, hint)
                if (deskCalSquareIds.isNotEmpty()) appWidgetManager.updateAppWidget(deskCalSquareIds, hint)
            }

            // Only keep recomputing on a schedule while something is actually on screen. Each surface
            // contributes its own next transition; redraw at the earliest of them.
            val compactNotificationShown = data != null && data.showNotification &&
                data.notificationLayout == NotificationWidgetLayout.Compact && nextData != null
            val nextUpdateCandidates = buildList<Long> {
                if (data != null && data.showNotification) {
                    data.nextUpdateAtMillis?.let { add(it) }
                }
                // The compact notification and dedicated next-prayer widgets follow the next-prayer transition
                if (nextData != null && (compactNotificationShown || anyNextPrayerPlaced)) {
                    nextData.nextUpdateAtMillis?.let { add(it) }
                }
                // customData is non-null only when there's a consumer (placed widget / custom notification).
                if (customData != null) {
                    customData.nextUpdateAtMillis?.let { add(it) }
                }
                if (nateegaData != null && anyNateegaPlaced) {
                    add(nateegaData.countdownTargetMillis)
                }
                // Periodic non-wakeup redraw for smooth progress ring updates every 5 minutes while active
                if (anyNextPrayerPlaced || (anyNateegaPlaced && nateegaConfig.showCountdown)) {
                    add((now + 5.minutes).toEpochMilliseconds())
                }
            }
            scheduleNextRedraw(nextUpdateCandidates.minOrNull())
        }

    private suspend fun updateNotification(
        data: WidgetData,
        nextData: NextPrayerWidgetData?,
        customData: CustomWidgetData?,
    ) {
        if (!data.showNotification) {
            notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
            notificationRepository.cancelNotification("prayer_times_widget_notification")
            return
        }
        // Compact layout reuses the centered next-prayer rendering. Setting big to null prevents
        // the Android SystemUI from adding the expand/collapse chevron arrow.
        val small: RemoteViews
        val big: RemoteViews?
        if (data.notificationLayout == NotificationWidgetLayout.Custom && customData != null) {
            // The user-authored custom layout, rendered inline (the pager arrows can't work inside a
            // notification); same view for the collapsed and expanded states.
            val custom = CustomWidgetRenderer.build(context, customData.copy(pages = emptyList()))
            small = custom
            big = null
        } else if (data.notificationLayout == NotificationWidgetLayout.Compact && nextData != null) {
            small = WidgetRenderer.buildCenteredPrayerNotification(context, nextData)
            big = null
        } else {
            small = WidgetRenderer.build(context, notifSmallLayout(data.adaptiveTheme), data)
            big = WidgetRenderer.build(context, notifBigLayout(data.adaptiveTheme, data.showCountdown), data)
        }
        notificationRepository.notify(
            NotificationConfig(
                id = WidgetContract.NOTIFICATION_ID,
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.WIDGET_CHANNEL_ID,
                    ongoing = true,
                    category = AndroidNotificationCategory.CATEGORY_STATUS,
                    onlyAlertOnce = true,
                    autoCancel = false,
                    showTimestamp = false,
                    sortKey = "-1",
                    group = WidgetContract.NOTIFICATION_GROUP,
                    customContentView = small,
                    customBigContentView = big,
                ),
            ),
        )
    }

    private suspend fun updateSpiritualPillNotification(
        nateegaData: NateegaWidgetData?,
        showCompanion: Boolean,
        capsuleMode: SpiritualCapsuleMode = SpiritualCapsuleMode.SmartMoments,
    ) {
        if (!showCompanion || capsuleMode == SpiritualCapsuleMode.Off || nateegaData == null) {
            notificationRepository.cancelNotification(WidgetContract.SPIRITUAL_PILL_NOTIFICATION_ID)
            notificationRepository.cancelNotification("spiritual_pill_companion_notification")
            return
        }
        val contextInfo = SpiritualContextResolver.resolve(nateegaData)
        if (capsuleMode == SpiritualCapsuleMode.SmartMoments && !contextInfo.isSmartMoment) {
            notificationRepository.cancelNotification(WidgetContract.SPIRITUAL_PILL_NOTIFICATION_ID)
            notificationRepository.cancelNotification("spiritual_pill_companion_notification")
            return
        }

        val momentKey = SpiritualPillDismissStore.momentKeyForContext(contextInfo)
        val isOngoing = (capsuleMode == SpiritualCapsuleMode.PersistentShade)
        if (!isOngoing && SpiritualPillDismissStore.isDismissed(context, momentKey)) {
            notificationRepository.cancelNotification(WidgetContract.SPIRITUAL_PILL_NOTIFICATION_ID)
            notificationRepository.cancelNotification("spiritual_pill_companion_notification")
            return
        }

        // Smart De-duplication: If any active reminder is currently shown in the notification shade,
        // do not post the companion spiritual pill so the user never sees two notifications for the same subject!
        val activeNotifs = runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager)?.activeNotifications
        }.getOrNull().orEmpty()
        val hasActiveReminder = activeNotifs.any { notif ->
            val chId = notif.notification.channelId
            chId == EnsureNotificationChannelsUseCase.REMINDER_CHANNEL_ID ||
                chId == EnsureNotificationChannelsUseCase.REMINDER_DND_CHANNEL_ID ||
                chId == EnsureNotificationChannelsUseCase.PRE_REMINDER_CHANNEL_ID
        }
        if (hasActiveReminder) {
            notificationRepository.cancelNotification(WidgetContract.SPIRITUAL_PILL_NOTIFICATION_ID)
            notificationRepository.cancelNotification("spiritual_pill_companion_notification")
            return
        }
        val pill = ModernWidgetsRenderer.buildSmartPillNotification(context, nateegaData)
        val pillBig = ModernWidgetsRenderer.buildSmartPillExpandedNotification(context, nateegaData)

        val targetRoute: DeepLinkableRoute = when {
            contextInfo.wordTag.contains("كَهْف") || contextInfo.title.contains("الكهف") -> Route.Main.QuranKahf
            contextInfo.wordTag.contains("مُلْك") || contextInfo.title.contains("الملك") -> Route.Main.QuranMulk
            contextInfo.wordTag.contains("وِتْر") || contextInfo.wordTag.contains("قِيَام") || contextInfo.title.contains("الضحى") -> Route.Main.Reminder
            else -> SpiritualPillDismissStore.resolveAzkarRoute(contextInfo.wordTag, contextInfo.title, contextInfo.subtitle)
        }

        val dismissAction = if (!isOngoing) {
            NotificationPressAction.Broadcast(
                action = WidgetContract.ACTION_DISMISS_SPIRITUAL_PILL,
                requestCode = momentKey.hashCode(),
                extras = mapOf(WidgetContract.EXTRA_MOMENT_KEY to momentKey),
            )
        } else {
            null
        }

        notificationRepository.notify(
            NotificationConfig(
                id = WidgetContract.SPIRITUAL_PILL_NOTIFICATION_ID,
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.WIDGET_CHANNEL_ID,
                    ongoing = isOngoing,
                    category = AndroidNotificationCategory.CATEGORY_STATUS,
                    onlyAlertOnce = true,
                    autoCancel = !isOngoing,
                    showTimestamp = false,
                    sortKey = "-2",
                    group = WidgetContract.SPIRITUAL_PILL_NOTIFICATION_GROUP,
                    customContentView = pill,
                    customBigContentView = pillBig,
                    pressAction = NotificationPressAction.Route(targetRoute),
                    dismissAction = dismissAction,
                ),
            ),
        )
    }

    private suspend fun scheduleNextRedraw(nextMillis: Long?) {
        if (nextMillis == null) {
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        // small buffer so the targeted prayer/day has actually elapsed when the alarm recomputes
        val triggerAt = nextMillis + 1_000
        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            // A non-future target would fire immediately and loop forever — only happens in a degenerate
            // state (all prayer times in the past for the day); skip and let the next real trigger recompute.
            Log.w(TAG, "Skipping widget redraw: non-future target nextMillis=$nextMillis now=$now")
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        Log.i(TAG, "Next widget redraw in ${(triggerAt - now) / 1000}s")
        // Exact but non-wakeup: fires on time when the device is awake, and the OS coalesces it
        // efficiently while idle (repaints on the next wake) — a cosmetic redraw shouldn't wake the
        // device. Falls back to inexact (still non-wakeup) if exact-alarm permission is missing.
        alarmRepository.schedule(
            ScheduledAlarm(
                id = WidgetContract.REDRAW_ALARM_ID,
                triggerAtMillis = triggerAt,
                action = WidgetContract.ACTION_WIDGET_UPDATE,
                type = AlarmType.Exact,
                wakeup = false,
            ),
        )
    }

    private fun notifSmallLayout(adaptive: Boolean): Int =
        if (adaptive) R.layout.notif_widget_small_adaptive else R.layout.notif_widget_small

    private fun notifBigLayout(
        adaptive: Boolean,
        countdown: Boolean,
    ): Int =
        when {
            countdown && adaptive -> R.layout.notif_widget_big_countdown_adaptive
            countdown -> R.layout.notif_widget_big_countdown
            adaptive -> R.layout.notif_widget_big_adaptive
            else -> R.layout.notif_widget_big
        }
}

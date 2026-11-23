package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.github.meypod.al_azan.BuildConfig
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.presentation.navigation.deeplink.DeepLinkPattern
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Intro : Route {
        @Serializable
        data object LanguageSelection : Route

        @Serializable
        data object RestoreBackup : Route

        @Serializable
        data object Location : Route

        @Serializable
        data object Calculation : Route {
            @Serializable
            data object Adjustments : Route

            @Serializable
            data object AdvancedCalculation : Route
        }

        @Serializable
        data object Adhan : Route {
            @Serializable
            data class PrayerSchedule(
                val prayer: Prayer,
            ) : Route
        }

        @Serializable
        data object Troubleshoot : Route {
            @Serializable
            data object AdvancedTroubleshoot : Route
        }
    }

    @Serializable
    data object Main : Route {
        @Serializable
        data object Home : Route, DeepLinkableRoute

        /** Status + manual-end screen for an active "Dismiss & silent" window. Reached from the control
         *  notification and from the system DND-rule configuration entry point. */
        @Serializable
        data object SilenceStatus : Route, DeepLinkableRoute

        @Serializable
        data object Location : Route

        @Serializable
        data object CalendarView : Route, DeepLinkableRoute

        @Serializable
        data object MonthlyView : Route, DeepLinkableRoute

        @Serializable
        data object Reminder : Route, DeepLinkableRoute

        @Serializable
        data object Qibla : Route

        @Serializable
        data object QiblaCompass : Route, DeepLinkableRoute

        @Serializable
        data object Counter : Route, DeepLinkableRoute

        @Serializable
        data object Azkar : Route, DeepLinkableRoute

        @Serializable
        data class AzkarDetail(
            val categoryId: String,
            val initialDhikrId: String? = null,
        ) : Route, DeepLinkableRoute {
            override fun toUriString(): String =
                if (initialDhikrId != null) {
                    "al-azan://AzkarDetail/$categoryId?initialDhikrId=$initialDhikrId"
                } else {
                    "al-azan://AzkarDetail/$categoryId"
                }
        }

        @Serializable
        data object QuranKahf : Route, DeepLinkableRoute

        @Serializable
        data object QuranMulk : Route, DeepLinkableRoute

        @Serializable
        data object Settings : Route, DeepLinkableRoute {

            @Serializable
            data object InterfaceSettings : Route, DeepLinkableRoute

            @Serializable
            data object SoundAndNotifications : Route, DeepLinkableRoute {
                @Serializable
                data object ScheduleAndMuezzin : Route, DeepLinkableRoute

                @Serializable
                data class PrayerSchedule(
                    val prayer: Prayer,
                ) : Route
            }

            @Serializable
            data object Calculations : Route, DeepLinkableRoute {
                @Serializable
                data object Adjustments : Route

                @Serializable
                data object AdvancedCalculation : Route
            }

            @Serializable
            data object Troubleshoot : Route {
                @Serializable
                data object AdvancedTroubleshoot : Route
            }

            @Serializable
            data object WidgetSettings : Route, DeepLinkableRoute {
                @Serializable
                data object TableAppearance : Route

                @Serializable
                data object CompactAppearance : Route, DeepLinkableRoute

                @Serializable
                data object CustomBuilder : Route, DeepLinkableRoute

                @Serializable
                data object NateegaAppearance : Route, DeepLinkableRoute

                @Serializable
                data object CompactPrayerAppearance : Route, DeepLinkableRoute
            }

            @Serializable
            data object NotificationWidget : Route, DeepLinkableRoute

            @Serializable
            data object BackupAndRestore : Route

            @Serializable
            data object Developer : Route
        }

        @Serializable
        data object UpcomingAlarms : Route, DeepLinkableRoute

        @Serializable
        data object About : Route
    }
}

internal val deepLinkPatterns: List<DeepLinkPattern<out Route>> by lazy {
    buildList {
        // "al-azan://home"
        add(DeepLinkPattern(Route.Main.Home.serializer(), Route.Main.Home.toUriString().toUri()))
        // "al-azan://SilenceStatus"
        add(DeepLinkPattern(Route.Main.SilenceStatus.serializer(), Route.Main.SilenceStatus.toUriString().toUri()))
        // "al-azan://UpcomingAlarms" — opened by tapping a pre-alarm ("upcoming") notification.
        add(DeepLinkPattern(Route.Main.UpcomingAlarms.serializer(), Route.Main.UpcomingAlarms.toUriString().toUri()))
        // "al-azan://CustomBuilder" — opened by tapping the not-yet-configured custom widget's hint.
        add(
            DeepLinkPattern(
                Route.Main.Settings.WidgetSettings.CustomBuilder.serializer(),
                Route.Main.Settings.WidgetSettings.CustomBuilder.toUriString().toUri(),
            ),
        )
        // "al-azan://Calculations" — opened by tapping a notice about a calculation change.
        add(
            DeepLinkPattern(
                Route.Main.Settings.Calculations.serializer(),
                Route.Main.Settings.Calculations.toUriString().toUri(),
            ),
        )
        // Opened by tapping Nateega widget prayer table and calendar sections.
        add(DeepLinkPattern(Route.Main.MonthlyView.serializer(), Route.Main.MonthlyView.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.CalendarView.serializer(), Route.Main.CalendarView.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.Azkar.serializer(), Route.Main.Azkar.toUriString().toUri()))
        add(
            DeepLinkPattern(
                Route.Main.AzkarDetail.serializer(),
                "al-azan://AzkarDetail/{categoryId}?initialDhikrId={initialDhikrId}".toUri(),
            ),
        )
        add(DeepLinkPattern(Route.Main.QuranKahf.serializer(), Route.Main.QuranKahf.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.QuranMulk.serializer(), Route.Main.QuranMulk.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.Reminder.serializer(), Route.Main.Reminder.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.Counter.serializer(), Route.Main.Counter.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.About.serializer(), "al-azan://About".toUri()))
        add(DeepLinkPattern(Route.Main.Settings.serializer(), Route.Main.Settings.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.QiblaCompass.serializer(), Route.Main.QiblaCompass.toUriString().toUri()))
        add(DeepLinkPattern(Route.Main.Settings.Troubleshoot.serializer(), "al-azan://Troubleshoot".toUri()))

        // Settings sub-screens
        add(
            DeepLinkPattern(
                Route.Main.Settings.InterfaceSettings.serializer(),
                Route.Main.Settings.InterfaceSettings.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.SoundAndNotifications.serializer(),
                Route.Main.Settings.SoundAndNotifications.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin.serializer(),
                Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.WidgetSettings.serializer(),
                Route.Main.Settings.WidgetSettings.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.NotificationWidget.serializer(),
                Route.Main.Settings.NotificationWidget.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.WidgetSettings.NateegaAppearance.serializer(),
                Route.Main.Settings.WidgetSettings.NateegaAppearance.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.WidgetSettings.CompactPrayerAppearance.serializer(),
                Route.Main.Settings.WidgetSettings.CompactPrayerAppearance.toUriString().toUri(),
            ),
        )
        add(
            DeepLinkPattern(
                Route.Main.Settings.WidgetSettings.CompactAppearance.serializer(),
                Route.Main.Settings.WidgetSettings.CompactAppearance.toUriString().toUri(),
            ),
        )
    }
}

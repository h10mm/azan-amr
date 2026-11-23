package com.github.meypod.al_azan.widget

/** Shared identifiers for the prayer-times widgets. */
object WidgetContract {
    /** Broadcast action for the scheduled widget redraw, dispatched to AlarmReceiver. */
    const val ACTION_WIDGET_UPDATE = "com.github.meypod.al_azan.action.WIDGET_UPDATE"

    /** Stable id of the redraw alarm tracked by the AlarmRepository. */
    const val REDRAW_ALARM_ID = "widget_redraw"

    /** Notification id for the persistent notification widget. */
    const val NOTIFICATION_ID = "prayer_times_widget_v2"

    /** Own notification group so the widget never stacks with the app's other notifications. */
    const val NOTIFICATION_GROUP = "prayer_times_widget_v2"

    /** Notification id for the standalone spiritual pill companion notification. */
    const val SPIRITUAL_PILL_NOTIFICATION_ID = "spiritual_pill_companion_v2"

    /** Group for the companion spiritual pill notification. */
    const val SPIRITUAL_PILL_NOTIFICATION_GROUP = "spiritual_pill_companion_v2_group"

    /** Broadcast action fired when the user swipes away the spiritual pill notification. */
    const val ACTION_DISMISS_SPIRITUAL_PILL = "com.github.meypod.al_azan.action.DISMISS_SPIRITUAL_PILL"

    /** Extra carrying the dismissed moment key so it is never re-posted during the same period. */
    const val EXTRA_MOMENT_KEY = "extra_moment_key"
}

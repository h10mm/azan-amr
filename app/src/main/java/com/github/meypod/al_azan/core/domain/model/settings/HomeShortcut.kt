package com.github.meypod.al_azan.core.domain.model.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A destination the user can surface as a shortcut icon button in the home top app bar. */
@Serializable
enum class HomeShortcut(
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int = labelRes,
    @param:DrawableRes val iconRes: Int = R.drawable.counter,
) {
    @SerialName("counter")
    Counter(
        labelRes = R.string.counter,
        descriptionRes = R.string.shortcut_counter_desc,
        iconRes = R.drawable.ic_tasbih,
    ),

    @SerialName("silence")
    Silence(
        labelRes = R.string.shortcut_silence,
        descriptionRes = R.string.shortcut_silence_desc,
        iconRes = R.drawable.outline_volume_off,
    ),

    @SerialName("theme_toggle")
    ThemeToggle(
        labelRes = R.string.shortcut_theme_toggle,
        descriptionRes = R.string.shortcut_theme_toggle_desc,
        iconRes = R.drawable.ic_theme_mode,
    ),

    @SerialName("nateega_settings")
    NateegaSettings(
        labelRes = R.string.shortcut_nateega,
        descriptionRes = R.string.shortcut_nateega_desc,
        iconRes = R.drawable.ic_nateega_card,
    ),

    @Deprecated("Present in bottom bar")
    @SerialName("qibla")
    Qibla(
        labelRes = R.string.qibla,
        descriptionRes = R.string.shortcut_qibla_desc,
        iconRes = R.drawable.compass_outline,
    ),

    @Deprecated("Present in bottom bar")
    @SerialName("monthly_view")
    MonthlyView(
        labelRes = R.string.monthly_view_title,
        descriptionRes = R.string.shortcut_monthly_view_desc,
        iconRes = R.drawable.calendar_month_outline,
    ),

    @Deprecated("Legacy")
    @SerialName("reminders")
    Reminders(
        labelRes = R.string.reminders_title,
        descriptionRes = R.string.reminders_title,
        iconRes = R.drawable.alarm,
    ),

    @Deprecated("Legacy")
    @SerialName("upcoming_alarms")
    UpcomingAlarms(
        labelRes = R.string.upcoming_alarms,
        descriptionRes = R.string.upcoming_alarms,
        iconRes = R.drawable.outline_calendar_month_24,
    ),
}

@Composable
fun HomeShortcut.i18n() = stringResource(labelRes)

@Composable
fun HomeShortcut.description() = stringResource(descriptionRes)

val HOME_SHORTCUTS_IN_ORDER: List<HomeShortcut> = listOf(
    HomeShortcut.ThemeToggle,
    HomeShortcut.Counter,
    HomeShortcut.Silence,
    HomeShortcut.NateegaSettings,
)

/** The home top app bar stays uncluttered: at most this many shortcut buttons at once. */
const val MAX_HOME_SHORTCUTS = 3

/** Hiding the title bar calendar frees space for more shortcut buttons. */
const val MAX_HOME_SHORTCUTS_TOOLBAR_HIDDEN = 4

/** Max shortcuts allowed in the home top app bar, given whether the title bar calendar is hidden. */
fun maxHomeShortcuts(toolbarCalendarHidden: Boolean): Int =
    if (toolbarCalendarHidden) MAX_HOME_SHORTCUTS_TOOLBAR_HIDDEN else MAX_HOME_SHORTCUTS

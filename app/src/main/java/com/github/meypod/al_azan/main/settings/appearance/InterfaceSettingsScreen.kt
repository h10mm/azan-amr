package com.github.meypod.al_azan.main.settings.appearance

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.HOME_SHORTCUTS_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.SupportedLocales
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.settings.description
import com.github.meypod.al_azan.core.domain.model.settings.i18n
import com.github.meypod.al_azan.core.domain.model.settings.maxHomeShortcuts
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.domain.util.localeFirstDayOfWeek
import com.github.meypod.al_azan.core.domain.util.weekdaysFrom
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PrayerCheckboxTable
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.mapper.localized
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.DayOfWeek
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceSettingsScreen(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    events: Flow<InterfaceSettingsUiEvent> = emptyFlow(),
) {
    val resources = LocalResources.current
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is InterfaceSettingsUiEvent.ShowMessage ->
                    snackbarController.show(resources.getString(event.messageRes))
            }
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.interface_settings),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        // القسم الأول: المظهر والسمة
        SectionCategoryHeader(title = stringResource(R.string.interface_category_theme))
        ThemeAndDisplayCard(uiState, onAction)

        // القسم الثاني: اللغة والأرقام والوقت
        SectionCategoryHeader(title = stringResource(R.string.interface_category_language_time))
        LanguageAndTimeCard(uiState, onAction)

        // القسم الثالث: أوقات الصلاة والعد التنازلي
        SectionCategoryHeader(title = stringResource(R.string.interface_category_prayers_countdown))
        PrayersAndCountdownCard(uiState, onAction)

        // القسم الرابع: التقاويم واختصارات الشاشة
        SectionCategoryHeader(title = stringResource(R.string.interface_category_calendars_shortcuts))
        CalendarsAndShortcutsCard(uiState, onAction)
    }
}

@Composable
private fun SectionCategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private const val DisplayScaleMin = 0.6f
private const val DisplayScaleMax = 1.5f
private const val DisplayScaleSteps = 8
private const val DisplayScaleStep = 0.1f

private fun snapDisplayScale(value: Float): Float = ((value * 10).roundToInt() / 10f).coerceIn(DisplayScaleMin, DisplayScaleMax)

/** البطاقة الأولى: المظهر وحجم العرض مع شريط كبسولات السمة الفوري (1-Tap) */
@Composable
private fun ThemeAndDisplayCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    val savedDisplayScale = uiState.settings.displayScale
    var sliderValue by remember(savedDisplayScale) { mutableFloatStateOf(savedDisplayScale) }

    ACard { cardPadding ->
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            // عنوان السمة
            SettingLabel(
                text = stringResource(R.string.themes),
                fontWeight = FontWeight.SemiBold,
            )

            // شريط الكبسولات المقسمة الثلاث بنقرة واحدة سريعة (1-Tap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val themeItems = listOf(
                    Triple(ThemeColor.ClassicLight, "فاتح", Color.White),
                    Triple(ThemeColor.Dark, "داكن", Color(0xFF141414)),
                    Triple(ThemeColor.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
                )

                themeItems.forEach { (theme, label, previewColor) ->
                    val isSelected = uiState.settings.themeColor == theme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { onAction(InterfaceSettingsUiAction.OnThemeChange(theme)) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(previewColor)
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // حجم العرض وتكبير الخط
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingLabel(
                    text = stringResource(R.string.display_size),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${(sliderValue * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val commit = { value: Float ->
                val snapped = snapDisplayScale(value)
                sliderValue = snapped
                onAction(InterfaceSettingsUiAction.OnDisplayScaleChange(snapped))
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { commit(sliderValue - DisplayScaleStep) },
                    enabled = sliderValue > DisplayScaleMin + 0.001f,
                ) {
                    Icon(
                        painterResource(R.drawable.minus),
                        contentDescription = stringResource(R.string.decrease),
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onAction(InterfaceSettingsUiAction.OnDisplayScaleChange(sliderValue)) },
                    valueRange = DisplayScaleMin..DisplayScaleMax,
                    steps = DisplayScaleSteps,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { commit(sliderValue + DisplayScaleStep) },
                    enabled = sliderValue < DisplayScaleMax - 0.001f,
                ) {
                    Icon(
                        painterResource(R.drawable.add),
                        contentDescription = stringResource(R.string.increase),
                    )
                }
            }

            if (abs(sliderValue - 1f) > 0.001f) {
                TextButton(
                    onClick = {
                        sliderValue = 1f
                        onAction(InterfaceSettingsUiAction.OnDisplayScaleChange(1f))
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
    }
}

/** البطاقة الثانية: اللغة، نظام الأرقام، وتنسيق الوقت مدمجة في مكان واحد وبنصوص مقتضبة */
@Composable
private fun LanguageAndTimeCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    val resources = LocalResources.current
    ACard { cardPadding ->
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            // لغة الواجهة
            SettingLabel(
                text = stringResource(R.string.language),
                fontWeight = FontWeight.SemiBold,
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = SupportedLocales,
                optionKey = { it.value },
                optionLabel = { it.label },
                optionSearchTag = { it.tags },
                selectedKey = uiState.settings.selectedLocale,
                onSelect = { onAction(InterfaceSettingsUiAction.OnLanguageChange(it.value)) },
                searchable = true,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // نظام الأرقام
            SettingLabel(
                text = stringResource(R.string.numbering_system),
                fontWeight = FontWeight.SemiBold,
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = NumberingSystem.entries,
                optionKey = { it.name },
                optionLabel = {
                    val label = when (it) {
                        NumberingSystem.Default -> resources.getString(R.string.default_value)
                        NumberingSystem.Latn -> resources.getString(R.string.numbering_system_latn)
                        NumberingSystem.Arab -> resources.getString(R.string.numbering_system_arab)
                        NumberingSystem.Arabext -> resources.getString(R.string.numbering_system_arabext)
                    }
                    if (it == NumberingSystem.Default) label else "$label (${formatWithUnicodeDigits("456", it)})"
                },
                selectedKey = uiState.settings.numberingSystem.name,
                onSelect = { onAction(InterfaceSettingsUiAction.OnNumberingSystemChange(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // تنسيق 24 ساعة
            SettingSwitch(
                title = stringResource(R.string.use_24_hour_format),
                subtitle = null,
                checked = uiState.settings.is24HourFormat,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnTimeFormatToggle(it)) },
            )
        }
    }
}

/** البطاقة الثالثة: أوقات الصلاة والعد التنازلي مدمجة معاً */
@Composable
private fun PrayersAndCountdownCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            // مؤقت العد التنازلي
            SettingSwitch(
                title = stringResource(R.string.countdown_timer),
                subtitle = stringResource(R.string.countdown_timer_help),
                checked = uiState.settings.showHomeNextPrayerCountdown,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnCountdownTimerToggle(it)) },
            )

            // العد للصلوات فقط
            SettingSwitch(
                title = stringResource(R.string.countdown_skip_non_prayers),
                subtitle = stringResource(R.string.countdown_skip_non_prayers_help),
                checked = uiState.settings.countdownSkipNonPrayers,
                enabled = uiState.settings.showHomeNextPrayerCountdown,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnCountdownSkipNonPrayersToggle(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // إبراز وقت الصلاة الحالي
            SettingSwitch(
                title = stringResource(R.string.highlight_current_prayer_time),
                subtitle = stringResource(R.string.highlight_current_prayer_time_help),
                checked = uiState.settings.highlightCurrentPrayer,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // عرض أوقات الصلاة
            PrayerCheckboxTable(
                title = stringResource(R.string.show_prayer_times_title),
                helpText = stringResource(R.string.show_prayer_times_help),
                leftColumn = stringResource(R.string.time_column),
                rightColumn = stringResource(R.string.show_column),
                isChecked = { it !in uiState.settings.hiddenPrayers },
                onToggle = { prayer, visible ->
                    onAction(InterfaceSettingsUiAction.OnPrayerVisibilityChange(prayer, visible))
                },
            )
        }
    }
}

/** البطاقة الرابعة: التقاويم واختصارات الشاشة الرئيسية مدمجة معاً */
@Composable
private fun CalendarsAndShortcutsCard(
    uiState: InterfaceSettingsUiState,
    onAction: (InterfaceSettingsUiAction) -> Unit,
) {
    val resources = LocalResources.current
    val selected = uiState.settings.homeShortcuts.filter { it in HOME_SHORTCUTS_IN_ORDER }
    val maxAllowed = maxHomeShortcuts(uiState.settings.hideToolbarCalendar)

    ACard { cardPadding ->
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            // التقويم الثانوي
            SettingHeader(
                title = stringResource(R.string.secondary_calendar),
                subtitle = stringResource(R.string.secondary_calendar_help),
            )
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = SecondaryCalendar.entries,
                optionKey = { it.name },
                optionLabel = { resources.getString(it.stringRes()) },
                selectedKey = uiState.settings.selectedSecondaryCalendar.name,
                onSelect = { onAction(InterfaceSettingsUiAction.OnSecondaryCalendarChange(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // أول يوم في الأسبوع
            SettingHeader(
                title = stringResource(R.string.first_day_of_week),
                subtitle = stringResource(R.string.first_day_of_week_help),
            )
            val localeFirstDay = localeFirstDayOfWeek(uiState.settings.selectedLocale)
            val defaultFirstDayLabel = stringResource(R.string.default_with_value, localeFirstDay.localized())
            val weekdayLabels = DayOfWeek.entries.associateWith { resources.getString(it.stringRes()) }
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = listOf<DayOfWeek?>(null) + weekdaysFrom(localeFirstDay),
                optionKey = { it?.name ?: "default" },
                optionLabel = { it?.let(weekdayLabels::getValue) ?: defaultFirstDayLabel },
                selectedKey = uiState.settings.firstDayOfWeek?.name ?: "default",
                onSelect = { onAction(InterfaceSettingsUiAction.OnFirstDayOfWeekChange(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // لغة التقويم القمري
            SettingHeader(
                title = stringResource(R.string.lunar_calendar_language),
                subtitle = stringResource(R.string.lunar_calendar_language_help),
            )
            val langOptions = listOf<Pair<String?, String>>(
                null to stringResource(R.string.default_value),
            ) + SupportedLocales.map { it.value to it.label }
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = langOptions,
                optionKey = { it.first ?: "default" },
                optionLabel = { it.second },
                selectedKey = uiState.settings.selectedLocaleForArabicCalendar ?: "default",
                onSelect = { onAction(InterfaceSettingsUiAction.OnLunarLanguageChange(it.first)) },
                searchable = true,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // تبديل التقويم القمري والثانوي في الرأس
            SettingSwitch(
                title = stringResource(R.string.swap_home_calendars),
                subtitle = stringResource(R.string.swap_home_calendars_help),
                checked = uiState.settings.swapHomeCalendars,
                onCheckedChange = { onAction(InterfaceSettingsUiAction.OnSwapHomeCalendarsToggle(it)) },
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.8.dp,
            )

            // اختصارات الرأس العلوي (البند 11 المطور)
            SettingHeader(
                title = stringResource(R.string.home_shortcuts),
                subtitle = stringResource(R.string.shortcuts_selected_count, selected.size, maxAllowed),
            )

            HOME_SHORTCUTS_IN_ORDER.forEachIndexed { index, shortcut ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 0.8.dp,
                    )
                }
                val isChecked = shortcut in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            onAction(InterfaceSettingsUiAction.OnHomeShortcutToggle(shortcut, !isChecked))
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(shortcut.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = shortcut.i18n(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = shortcut.description(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            onAction(InterfaceSettingsUiAction.OnHomeShortcutToggle(shortcut, checked))
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun InterfaceSettingsPreview() {
    AlAzanThemePreview {
        InterfaceSettingsScreen(uiState = InterfaceSettingsUiState(), onAction = {})
    }
}

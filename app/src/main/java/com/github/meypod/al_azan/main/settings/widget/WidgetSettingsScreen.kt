package com.github.meypod.al_azan.main.settings.widget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.DeskCalendarType
import com.github.meypod.al_azan.core.domain.model.widget.DialThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.MosqueArchThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaClockPosition
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SlimBarDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.SlimBarThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SquareThemeMode
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

enum class WidgetDetailType(
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val subtitleRes: Int,
    val iconEmoji: String,
    val sizeBadge: String,
) {
    Nateega(
        titleRes = R.string.ws_nateega_title,
        subtitleRes = R.string.ws_nateega_sub,
        iconEmoji = "🗓️",
        sizeBadge = "4×2",
    ),
    DeskCalendar(
        titleRes = R.string.ws_desk_title,
        subtitleRes = R.string.ws_desk_sub,
        iconEmoji = "📅",
        sizeBadge = "4×3 / 3×3",
    ),
    CompactPrayer(
        titleRes = R.string.ws_compact_title,
        subtitleRes = R.string.ws_compact_sub,
        iconEmoji = "⏱️",
        sizeBadge = "4×1 / 4×2",
    ),
    CircularDial(
        titleRes = R.string.ws_circle_title,
        subtitleRes = R.string.ws_circle_sub,
        iconEmoji = "⭕",
        sizeBadge = "2×2",
    ),
    SquareDial(
        titleRes = R.string.ws_square_title,
        subtitleRes = R.string.ws_square_sub,
        iconEmoji = "⏹️",
        sizeBadge = "2×2",
    ),
    SlimBar(
        titleRes = R.string.ws_slim_title,
        subtitleRes = R.string.ws_slim_sub,
        iconEmoji = "➖",
        sizeBadge = "4×1",
    ),
    MosqueArch(
        titleRes = R.string.ws_arch_title,
        subtitleRes = R.string.ws_arch_sub,
        iconEmoji = "🕌",
        sizeBadge = "4×2",
    ),
    SmartPill(
        titleRes = R.string.ws_pill_title,
        subtitleRes = R.string.ws_pill_sub,
        iconEmoji = "💊",
        sizeBadge = "2×1",
    ),
}

@Composable
fun WidgetSettingsScreen(
    uiState: WidgetSettingsUiState,
    onAction: (WidgetSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    initialWidget: WidgetDetailType? = null,
    onConfirmConfigure: (() -> Unit)? = null,
    onBackOverride: (() -> Unit)? = null,
) {
    var selectedWidget by remember(initialWidget) { mutableStateOf(initialWidget) }

    BackHandler(enabled = selectedWidget != null && initialWidget == null) {
        selectedWidget = null
    }

    if (selectedWidget == null) {
        ScreenScaffold(
            title = stringResource(R.string.ws_title),
            onBackClick = { onBackOverride?.invoke() ?: NavigationController.navigateBack() },
            modifier = modifier,
            scrollable = true,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. التقاويم والرزمانة
            WidgetSectionCard(title = stringResource(R.string.ws_cat_calendars)) {
                WidgetMenuItem(WidgetDetailType.Nateega) { selectedWidget = it }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                WidgetMenuItem(WidgetDetailType.DeskCalendar) { selectedWidget = it }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                WidgetMenuItem(WidgetDetailType.CompactPrayer) { selectedWidget = it }
            }

            // 2. الأقراص والعدادات التناقصية
            WidgetSectionCard(title = stringResource(R.string.ws_cat_dials)) {
                WidgetMenuItem(WidgetDetailType.CircularDial) { selectedWidget = it }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                WidgetMenuItem(WidgetDetailType.SquareDial) { selectedWidget = it }
            }

            // 3. أشكال مودرن ومصغرة
            WidgetSectionCard(title = stringResource(R.string.ws_cat_modern)) {
                WidgetMenuItem(WidgetDetailType.SlimBar) { selectedWidget = it }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                WidgetMenuItem(WidgetDetailType.MosqueArch) { selectedWidget = it }
                HorizontalDivider(modifier = Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                WidgetMenuItem(WidgetDetailType.SmartPill) { selectedWidget = it }
            }
        }
    } else {
        val currentWidget = selectedWidget!!
        val appThemeColor = uiState.settings.themeColor
        ScreenScaffold(
            title = stringResource(currentWidget.titleRes),
            onBackClick = {
                when {
                    onBackOverride != null -> onBackOverride()
                    initialWidget != null -> NavigationController.navigateBack()
                    else -> selectedWidget = null
                }
            },
            actions = {
                if (onConfirmConfigure != null) {
                    TextButton(onClick = onConfirmConfigure) {
                        Text(
                            text = stringResource(R.string.ws_done),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            modifier = modifier,
            scrollable = true,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (currentWidget) {
                WidgetDetailType.Nateega -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    NateegaLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        NateegaThemePillSelector(
                            selected = uiState.nateegaConfig.themeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnNateegaThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_clock_section))
                    ACard { cardPadding ->
                        BottomSelect(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            options = listOf(
                                NateegaClockPosition.Hidden,
                                NateegaClockPosition.CenterLarge,
                                NateegaClockPosition.Side,
                            ),
                            optionKey = { it.name },
                            optionLabel = { it.labelArabic() },
                            selectedKey = if (uiState.nateegaConfig.clockPosition == NateegaClockPosition.InlineWithDay) {
                                NateegaClockPosition.Side.name
                            } else {
                                uiState.nateegaConfig.clockPosition.name
                            },
                            onSelect = { onAction(WidgetSettingsUiAction.OnNateegaClockPositionChange(it)) },
                            label = { Text(stringResource(R.string.ws_clock_pos_label)) },
                            placeholder = stringResource(R.string.ws_clock_pos_placeholder),
                            supportingText = { Text(stringResource(R.string.ws_clock_pos_desc)) },
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_elements_section))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            SettingSwitch(
                                title = stringResource(R.string.ws_show_coptic_title),
                                subtitle = stringResource(R.string.ws_show_coptic_sub),
                                checked = uiState.nateegaConfig.showCopticDate,
                                onCheckedChange = { onAction(WidgetSettingsUiAction.OnNateegaShowCopticToggle(it)) },
                            )
                            SettingSwitch(
                                title = stringResource(R.string.ws_show_countdown_title),
                                subtitle = stringResource(R.string.ws_show_countdown_sub),
                                checked = uiState.nateegaConfig.showCountdown,
                                onCheckedChange = { onAction(WidgetSettingsUiAction.OnNateegaShowCountdownToggle(it)) },
                            )
                            SettingSwitch(
                                title = stringResource(R.string.ws_show_wisdom_title),
                                subtitle = stringResource(R.string.ws_show_wisdom_sub),
                                checked = uiState.nateegaConfig.showWisdom,
                                onCheckedChange = { onAction(WidgetSettingsUiAction.OnNateegaShowWisdomToggle(it)) },
                            )
                        }
                    }
                }

                WidgetDetailType.DeskCalendar -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    DeskCalendarLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        NateegaThemePillSelector(
                            selected = uiState.nateegaConfig.deskCalendarThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnDeskCalendarThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_desk_cal_type_section))
                    ACard { cardPadding ->
                        BottomSelect(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            options = DeskCalendarType.entries,
                            optionKey = { it.name },
                            optionLabel = { it.labelArabic() },
                            selectedKey = uiState.nateegaConfig.deskCalendarType.name,
                            onSelect = { onAction(WidgetSettingsUiAction.OnDeskCalendarTypeChange(it)) },
                            label = { Text(stringResource(R.string.ws_desk_cal_type_label)) },
                            placeholder = stringResource(R.string.ws_desk_cal_type_placeholder),
                            supportingText = { Text(stringResource(R.string.ws_desk_cal_type_desc)) },
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_desk_countdown_section))
                    ACard {
                        SettingSwitch(
                            checked = uiState.nateegaConfig.deskCalendarShowCountdown,
                            onCheckedChange = { onAction(WidgetSettingsUiAction.OnDeskCalendarShowCountdownToggle(it)) },
                            title = stringResource(R.string.ws_desk_countdown_title),
                            subtitle = stringResource(R.string.ws_desk_countdown_sub),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_desk_highlight_section))
                    ACard {
                        SettingSwitch(
                            checked = uiState.nateegaConfig.deskCalendarHighlightNextPrayer,
                            onCheckedChange = { onAction(WidgetSettingsUiAction.OnDeskCalendarHighlightNextPrayerToggle(it)) },
                            title = stringResource(R.string.ws_desk_highlight_title),
                            subtitle = stringResource(R.string.ws_desk_highlight_sub),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "📅 تقويم مكتبي متكامل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "يجمع شبكة أيام الشهر كاملة مع تمييز اليوم الحالي ويوم الجمعة، بالإضافة لجدول كامل لمواقيت الصلوات مع الشروق. متوفر بنسخة مستطيلة (4×3) ونسخة مربعة (3×3).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ws_how_to_add),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                WidgetDetailType.CompactPrayer -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    CompactPrayerLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_date_options_section))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            BottomSelect(
                                modifier = Modifier.fillMaxWidth(),
                                options = NateegaDateDisplayMode.entries,
                                optionKey = { it.name },
                                optionLabel = { it.labelArabic() },
                                selectedKey = uiState.nateegaConfig.compactDateDisplayMode.name,
                                onSelect = { onAction(WidgetSettingsUiAction.OnCompactDateDisplayModeChange(it)) },
                                label = { Text(stringResource(R.string.ws_displayed_calendar)) },
                                placeholder = stringResource(R.string.ws_desk_cal_type_placeholder),
                                supportingText = { Text(stringResource(R.string.ws_displayed_calendar_desc)) },
                            )
                            if (uiState.nateegaConfig.compactDateDisplayMode == NateegaDateDisplayMode.Both) {
                                BottomSelect(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    options = NateegaDateOrder.entries,
                                    optionKey = { it.name },
                                    optionLabel = { it.labelArabic() },
                                    selectedKey = uiState.nateegaConfig.compactDateOrder.name,
                                    onSelect = { onAction(WidgetSettingsUiAction.OnCompactDateOrderChange(it)) },
                                    label = { Text(stringResource(R.string.ws_date_order_label)) },
                                    placeholder = stringResource(R.string.ws_date_order_placeholder),
                                    supportingText = { Text(stringResource(R.string.ws_date_order_desc)) },
                                )
                            }
                        }
                    }

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        NateegaThemePillSelector(
                            selected = uiState.nateegaConfig.compactThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnCompactThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_elements_section))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            SettingSwitch(
                                title = stringResource(R.string.ws_show_countdown_title),
                                subtitle = stringResource(R.string.ws_show_countdown_sub),
                                checked = uiState.nateegaConfig.compactShowCountdown,
                                onCheckedChange = { onAction(WidgetSettingsUiAction.OnCompactShowCountdownToggle(it)) },
                            )
                            SettingSwitch(
                                title = stringResource(R.string.ws_show_wisdom_title),
                                subtitle = stringResource(R.string.ws_show_wisdom_sub),
                                checked = uiState.nateegaConfig.compactShowWisdom,
                                onCheckedChange = { onAction(WidgetSettingsUiAction.OnCompactShowWisdomToggle(it)) },
                            )
                        }
                    }
                }

                WidgetDetailType.CircularDial -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    CircularDialLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        DialThemePillSelector(
                            selected = uiState.nateegaConfig.dialThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnDialThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "⭕ تصميم دائري عصري متناسق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "ودجت عصري مخصص للشاشة الرئيسية بحجم 2×2، يعرض اسم الصلاة القادمة ووقتها بدقة متناهية مع حلقة تقدم دائرية وشريط متناسق 100% بدون أي توقف، وعداد رقمي تناقصي دقيق حتى موعد الأذان.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ws_how_to_add),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                WidgetDetailType.SquareDial -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    SquareDialLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        SquareThemePillSelector(
                            selected = uiState.nateegaConfig.squareThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnSquareThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "⏹️ تصميم كارت مربع متناسق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "النسخة المربعة (2×2) لمن يفضل الزوايا المستديرة الكلاسيكية المتناسقة مع أيقونات التطبيقات، مع كبسولة العداد التناقصي وشريط التقدم الأفقي المتناسق 100% بدقة متناهية ودعم كامل للثيمات.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ws_how_to_add),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                WidgetDetailType.SlimBar -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    SlimBarLivePreview(config = uiState.nateegaConfig, appThemeColor = appThemeColor)

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        SlimBarThemePillSelector(
                            selected = uiState.nateegaConfig.slimBarThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnSlimBarThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_date_options_section))
                    ACard { cardPadding ->
                        BottomSelect(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(cardPadding),
                            options = SlimBarDateOrder.entries,
                            optionKey = { it.name },
                            optionLabel = { it.labelArabic() },
                            selectedKey = uiState.nateegaConfig.slimBarDateOrder.name,
                            onSelect = { onAction(WidgetSettingsUiAction.OnSlimBarDateOrderChange(it)) },
                            label = { Text(stringResource(R.string.ws_date_order_label)) },
                            placeholder = stringResource(R.string.ws_date_order_placeholder),
                            supportingText = { Text(stringResource(R.string.ws_date_order_desc)) },
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_slim_elements_section))
                    ACard {
                        SettingSwitch(
                            checked = uiState.nateegaConfig.slimBarShowCity,
                            onCheckedChange = { onAction(WidgetSettingsUiAction.OnSlimBarShowCityToggle(it)) },
                            title = stringResource(R.string.ws_show_city_title),
                            subtitle = stringResource(R.string.ws_show_city_sub),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "➖ تصميم فائق النحافة والأناقة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "يشغل صفاً واحداً فقط على الشاشة (4×1)، يدمج الساعة الكبيرة، التاريخ الهجري والميلادي، وكبسولة العد التنازلي للصلاة القادمة بأعلى كفاءة وتناسق تام مع دعم الثيمات المستقلة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ws_how_to_add),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                WidgetDetailType.MosqueArch -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    MosqueArchLivePreview(
                        themeMode = uiState.nateegaConfig.mosqueArchThemeMode,
                        appThemeColor = appThemeColor,
                    )

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        MosqueArchThemePillSelector(
                            selected = uiState.nateegaConfig.mosqueArchThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnMosqueArchThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "🕌 تصميم إسلامي تراثي مهيب",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "يعلوه إطار قوسي مع الآية القرآنية الكريمة، ويعرض وقت الصلاة القادمة بخط كبير وواضح مع شريط سفلي لجميع صلوات اليوم الست.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ws_how_to_add),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                WidgetDetailType.SmartPill -> {
                    SectionHeader(title = stringResource(R.string.ws_live_preview))
                    SmartPillLivePreview(
                        themeMode = uiState.nateegaConfig.smartPillThemeMode,
                        showWordBadge = true,
                        appThemeColor = appThemeColor,
                    )

                    SectionHeader(title = stringResource(R.string.ws_theme_section))
                    ACard { cardPadding ->
                        SmartPillThemePillSelector(
                            selected = uiState.nateegaConfig.smartPillThemeMode,
                            onSelect = { onAction(WidgetSettingsUiAction.OnSmartPillThemeModeChange(it)) },
                            modifier = Modifier.padding(cardPadding),
                        )
                    }

                    SectionHeader(title = stringResource(R.string.ws_about_widget))
                    ACard { cardPadding ->
                        Column(Modifier.padding(cardPadding)) {
                            Text(
                                text = "💊 كبسولة ذكية مدمجة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "تعرض حالة الوقت الشرعي الحالية (مثل صلاة الضحى أو قيام الليل) مع وقت الصلاة القادمة بتصميم عصري مدمج يناسب الصفوف الضيقة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        ACard(
            modifier = Modifier.fillMaxWidth(),
            border = null,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun WidgetMenuItem(
    item: WidgetDetailType,
    onClick: (WidgetDetailType) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .clickable(role = Role.Button) { onClick(item) }
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.size(44.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = item.iconEmoji,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(item.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = item.sizeBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(item.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
            )
        }

        Icon(
            painter = painterResource(R.drawable.baseline_navigate_next_24),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            contentDescription = null,
        )
    }
}

@Composable
private fun CircularDialLivePreview(
    config: com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val themeMode = if (config.dialThemeMode == DialThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> DialThemeMode.ClassicWhite
            ThemeColor.Dark -> DialThemeMode.AmoledDark
            ThemeColor.MidnightBlue -> DialThemeMode.MidnightBlue
        }
    } else {
        config.dialThemeMode
    }
    val circleBg = when (themeMode) {
        DialThemeMode.AmoledDark -> Color(0xFF080C14)
        DialThemeMode.MatchAppTheme, DialThemeMode.MidnightBlue -> Color(0xFF0D172A)
        DialThemeMode.ClassicWhite -> Color(0xFFF8FAFC)
        DialThemeMode.Transparent -> Color(0x1A000000)
    }
    val ringColor = when (themeMode) {
        DialThemeMode.AmoledDark -> Color(0xFF10B981)
        DialThemeMode.MatchAppTheme, DialThemeMode.MidnightBlue -> Color(0xFF38BDF8)
        DialThemeMode.ClassicWhite -> Color(0xFF10B981)
        DialThemeMode.Transparent -> Color(0xFF10B981)
    }
    val nameColor = when (themeMode) {
        DialThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color.White
    }
    val timeColor = when (themeMode) {
        DialThemeMode.ClassicWhite -> Color(0xFF334155)
        else -> Color.White
    }
    val pillBg = when (themeMode) {
        DialThemeMode.ClassicWhite -> Color(0xFFE2E8F0)
        else -> Color(0x73000000)
    }
    val pillTextColor = when (themeMode) {
        DialThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color(0xFFF59E0B)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = circleBg,
            border = BorderStroke(4.5.dp, ringColor),
            shadowElevation = if (themeMode == DialThemeMode.Transparent) 0.dp else 6.dp,
            modifier = Modifier.size(175.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "المغرب",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = nameColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "05:01 م",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                )
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillBg,
                    border = BorderStroke(1.dp, if (themeMode == DialThemeMode.ClassicWhite) Color(0xFFCBD5E1) else Color(0x33FFFFFF)),
                ) {
                    Text(
                        text = "00:34:12",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = pillTextColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SquareDialLivePreview(
    config: com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val themeMode = if (config.squareThemeMode == SquareThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> SquareThemeMode.ClassicWhite
            ThemeColor.Dark -> SquareThemeMode.AmoledDark
            ThemeColor.MidnightBlue -> SquareThemeMode.MidnightBlue
        }
    } else {
        config.squareThemeMode
    }
    val cardBg = when (themeMode) {
        SquareThemeMode.AmoledDark -> Color(0xFF080C14)
        SquareThemeMode.MatchAppTheme, SquareThemeMode.MidnightBlue -> Color(0xFF0D172A)
        SquareThemeMode.ClassicWhite -> Color(0xFFF8FAFC)
        SquareThemeMode.Transparent -> Color(0x1A000000)
    }
    val borderColor = when (themeMode) {
        SquareThemeMode.AmoledDark -> Color(0x33FFFFFF)
        SquareThemeMode.MatchAppTheme, SquareThemeMode.MidnightBlue -> Color(0x3338BDF8)
        SquareThemeMode.ClassicWhite -> Color(0xFFCBD5E1)
        SquareThemeMode.Transparent -> Color(0x33FFFFFF)
    }
    val nameColor = when (themeMode) {
        SquareThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color.White
    }
    val timeColor = when (themeMode) {
        SquareThemeMode.ClassicWhite -> Color(0xFF334155)
        else -> Color.White
    }
    val pillBg = when (themeMode) {
        SquareThemeMode.ClassicWhite -> Color(0xFFE2E8F0)
        else -> Color(0x73000000)
    }
    val pillTextColor = when (themeMode) {
        SquareThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color(0xFFF59E0B)
    }
    val trackBg = when (themeMode) {
        SquareThemeMode.ClassicWhite -> Color(0xFFE2E8F0)
        else -> Color(0x26FFFFFF)
    }
    val progressColor = Color(0xFF10B981)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = if (themeMode == SquareThemeMode.Transparent) 0.dp else 6.dp,
            modifier = Modifier.size(175.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "المغرب",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = nameColor,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "05:01 م",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = timeColor,
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pillBg,
                    border = BorderStroke(1.dp, if (themeMode == SquareThemeMode.ClassicWhite) Color(0xFFCBD5E1) else Color(0x33FFFFFF)),
                ) {
                    Text(
                        text = "00:34:12",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = pillTextColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(5.dp)
                        .background(trackBg, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(5.dp)
                            .background(progressColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun SlimBarLivePreview(
    config: com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val themeMode = if (config.slimBarThemeMode == SlimBarThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> SlimBarThemeMode.ClassicWhite
            ThemeColor.Dark -> SlimBarThemeMode.AmoledDark
            ThemeColor.MidnightBlue -> SlimBarThemeMode.MidnightBlue
        }
    } else {
        config.slimBarThemeMode
    }
    val cardBg = when (themeMode) {
        SlimBarThemeMode.AmoledDark -> Color(0xFF080C14)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0xFF0D172A)
        SlimBarThemeMode.ClassicWhite -> Color(0xFFF8FAFC)
        SlimBarThemeMode.Transparent -> Color(0x1A000000)
    }
    val borderColor = when (themeMode) {
        SlimBarThemeMode.AmoledDark -> Color(0x33FFFFFF)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0x3338BDF8)
        SlimBarThemeMode.ClassicWhite -> Color(0xFFCBD5E1)
        SlimBarThemeMode.Transparent -> Color(0x33FFFFFF)
    }
    val clockColor = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFF1E3A8A)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0xFF38BDF8)
        else -> Color.White
    }
    val hijriColor = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color(0xFFF1F5F9)
    }
    val subTextColor = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFF64748B)
        else -> Color(0xFF94A3B8)
    }
    val capsuleBg = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFFDCFCE7)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0x2638BDF8)
        SlimBarThemeMode.AmoledDark -> Color(0x2610B981)
        SlimBarThemeMode.Transparent -> Color(0x33000000)
    }
    val capsuleBorder = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFFA7F3D0)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0x4D38BDF8)
        SlimBarThemeMode.AmoledDark -> Color(0x4D10B981)
        SlimBarThemeMode.Transparent -> Color(0x33FFFFFF)
    }
    val prayerNameColor = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFF065F46)
        SlimBarThemeMode.MatchAppTheme, SlimBarThemeMode.MidnightBlue -> Color(0xFF38BDF8)
        else -> Color(0xFF10B981)
    }
    val countdownColor = when (themeMode) {
        SlimBarThemeMode.ClassicWhite -> Color(0xFF047857)
        else -> Color(0xFFF59E0B)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = if (themeMode == SlimBarThemeMode.Transparent) 0.dp else 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "05:32 م",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = clockColor,
                    )
                    if (config.slimBarShowCity) {
                        Text(
                            text = "القاهرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = subTextColor,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val topDateText = if (config.slimBarDateOrder == SlimBarDateOrder.HijriFirst) "٢٥ جمادى الأولى ١٤٤٨" else "الأحد، ١٦ نوفمبر"
                    val bottomDateText = if (config.slimBarDateOrder == SlimBarDateOrder.HijriFirst) "الأحد، ١٦ نوفمبر" else "٢٥ جمادى الأولى ١٤٤٨"
                    Text(
                        text = topDateText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = hijriColor,
                    )
                    Text(
                        text = bottomDateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = subTextColor,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = capsuleBg,
                    border = BorderStroke(1.dp, capsuleBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "المغرب",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = prayerNameColor,
                        )
                        Text(
                            text = "00:34:12",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = countdownColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MosqueArchLivePreview(
    themeMode: MosqueArchThemeMode,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val resolvedTheme = if (themeMode == MosqueArchThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> MosqueArchThemeMode.ClassicWhite
            ThemeColor.Dark -> MosqueArchThemeMode.AmoledDark
            ThemeColor.MidnightBlue -> MosqueArchThemeMode.MidnightBlue
        }
    } else {
        themeMode
    }
    val cardBg = when (resolvedTheme) {
        MosqueArchThemeMode.AmoledDark -> Color(0xFF080C14)
        MosqueArchThemeMode.MatchAppTheme, MosqueArchThemeMode.MidnightBlue -> Color(0xFF0D172A)
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFF8FAFC)
        MosqueArchThemeMode.Transparent -> Color(0x1A000000)
    }
    val borderColor = when (resolvedTheme) {
        MosqueArchThemeMode.AmoledDark -> Color(0x22FFFFFF)
        MosqueArchThemeMode.MatchAppTheme, MosqueArchThemeMode.MidnightBlue -> Color(0x3338BDF8)
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFCBD5E1)
        MosqueArchThemeMode.Transparent -> Color(0x33FFFFFF)
    }
    val ayahColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFB45309)
        else -> Color(0xFFF59E0B)
    }
    val labelColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF64748B)
        else -> Color(0xFF94A3B8)
    }
    val nextTimeColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF0284C7)
        else -> Color(0xFF38BDF8)
    }
    val countdownBg = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFD1FAE5)
        else -> Color(0xFF059669)
    }
    val countdownTextColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF059669)
        else -> Color.White
    }
    val tableBg = when (resolvedTheme) {
        MosqueArchThemeMode.AmoledDark -> Color(0xFF141414)
        MosqueArchThemeMode.MatchAppTheme, MosqueArchThemeMode.MidnightBlue -> Color(0xFF101C33)
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFF1F5F9)
        MosqueArchThemeMode.Transparent -> Color(0x20000000)
    }
    val slotLabelColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF64748B)
        MosqueArchThemeMode.AmoledDark -> Color(0xFF71717A)
        else -> Color(0xFF94A3B8)
    }
    val slotTimeColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF0F172A)
        else -> Color.White
    }
    val activeSlotBg = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFFD1FAE5)
        MosqueArchThemeMode.AmoledDark -> Color(0xFF09261E)
        MosqueArchThemeMode.MatchAppTheme, MosqueArchThemeMode.MidnightBlue -> Color(0xFF0F2B26)
        MosqueArchThemeMode.Transparent -> Color(0x3310B981)
    }
    val activeBorderColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF059669)
        else -> Color(0xFF34D399)
    }
    val activeTextColor = when (resolvedTheme) {
        MosqueArchThemeMode.ClassicWhite -> Color(0xFF059669)
        else -> Color(0xFF34D399)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = if (themeMode == MosqueArchThemeMode.Transparent) 0.dp else 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "﴿ إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَّوْقُوتًا ﴾",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ayahColor,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "الصلاة القادمة: المغرب",
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                )
                Text(
                    text = "05:01 م",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = nextTimeColor,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = countdownBg,
                ) {
                    Text(
                        text = "00:34:12",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = countdownTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tableBg, RoundedCornerShape(10.dp))
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    listOf("الفجر" to "05:02", "الشروق" to "06:28", "الظهر" to "11:45", "العصر" to "02:40", "المغرب" to "05:01", "العشاء" to "06:21").forEach { (name, time) ->
                        val isNext = (name == "المغرب")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isNext) Modifier
                                        .background(activeSlotBg, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.5.dp, activeBorderColor), RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isNext) activeTextColor else slotLabelColor,
                                )
                                Text(
                                    time,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNext) activeTextColor else slotTimeColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SmartPillPreviewPalette(
    val bg: Color,
    val border: Color,
    val title: Color,
    val sub: Color,
    val badge: Color,
    val badgeBg: Color,
)

@Composable
internal fun SmartPillLivePreview(
    themeMode: com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode,
    showWordBadge: Boolean,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val resolvedTheme = if (themeMode == SmartPillThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> SmartPillThemeMode.ClassicWhite
            ThemeColor.Dark -> SmartPillThemeMode.AmoledDark
            ThemeColor.MidnightBlue -> SmartPillThemeMode.MidnightBlue
        }
    } else {
        themeMode
    }
    val palette = when (resolvedTheme) {
        SmartPillThemeMode.MatchAppTheme,
        SmartPillThemeMode.MidnightBlue -> SmartPillPreviewPalette(
            bg = Color(0xF20D172A),
            border = Color(0x4038BDF8),
            title = Color.White,
            sub = Color(0xFF94A3B8),
            badge = Color(0xFF38BDF8),
            badgeBg = Color(0x2E38BDF8),
        )
        SmartPillThemeMode.AmoledDark -> SmartPillPreviewPalette(
            bg = Color(0xFF000000),
            border = Color(0x4027272A),
            title = Color.White,
            sub = Color(0xFFA1A1AA),
            badge = Color(0xFF34D399),
            badgeBg = Color(0x2E10B981),
        )
        SmartPillThemeMode.Transparent -> SmartPillPreviewPalette(
            bg = Color(0x730F172A),
            border = Color(0x40FFFFFF),
            title = Color.White,
            sub = Color(0xFFE2E8F0),
            badge = Color(0xFF38BDF8),
            badgeBg = Color(0x2E38BDF8),
        )
        SmartPillThemeMode.ClassicWhite -> SmartPillPreviewPalette(
            bg = Color(0xFFFFFFFF),
            border = Color(0xFFE2E8F0),
            title = Color(0xFF0F172A),
            sub = Color(0xFF475569),
            badge = Color(0xFF059669),
            badgeBg = Color(0x2E10B981),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.bg,
            border = BorderStroke(1.2.dp, palette.border),
            shadowElevation = 4.dp,
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showWordBadge) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(palette.badgeBg, RoundedCornerShape(13.dp))
                            .border(BorderStroke(1.dp, palette.badge.copy(alpha = 0.35f)), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "ضُحَى",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = palette.badge,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "صلاة الضحى قائمة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.title,
                                fontSize = 13.sp,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = palette.badgeBg,
                            ) {
                                Text(
                                    text = "الظهر 12:10",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.badge,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "صلاة الأوابين • صدقة عن 360 مفصل",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.sub,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "صلاة العشاء والوتر",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.title,
                            fontSize = 13.5.sp,
                        )
                        Spacer(Modifier.height(2.5.dp))
                        Text(
                            text = "أوتروا يا أهل القرآن • ركعة وتر تضيء ليلتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.sub,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskCalendarLivePreview(
    config: com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val resolvedTheme = if (config.deskCalendarThemeMode == NateegaThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> NateegaThemeMode.ClassicPaper
            ThemeColor.Dark -> NateegaThemeMode.Amoled
            ThemeColor.MidnightBlue -> NateegaThemeMode.MidnightBlue
        }
    } else {
        config.deskCalendarThemeMode
    }
    val isDark = resolvedTheme == NateegaThemeMode.Amoled || resolvedTheme == NateegaThemeMode.MidnightBlue
    val cardBg = when (resolvedTheme) {
        NateegaThemeMode.Amoled -> Color(0xFF000000)
        NateegaThemeMode.MidnightBlue -> Color(0xFF0F172A)
        else -> Color.White
    }
    val cardBorder = when (resolvedTheme) {
        NateegaThemeMode.Amoled -> Color(0xFF27272A)
        NateegaThemeMode.MidnightBlue -> Color(0xFF1E293B)
        else -> Color(0xFFCBD5E1)
    }
    val titleColor = if (isDark) Color.White else Color(0xFF1E3A8A)
    val gridBg = when (resolvedTheme) {
        NateegaThemeMode.Amoled -> Color(0xFF111114)
        NateegaThemeMode.MidnightBlue -> Color(0xFF162036)
        else -> Color(0xFFF8FAFC)
    }
    val dayColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val slotBg = when (resolvedTheme) {
        NateegaThemeMode.Amoled -> Color(0xFF18181B)
        NateegaThemeMode.MidnightBlue -> Color(0xFF1E293B)
        else -> Color(0xFFF1F5F9)
    }
    val slotNameColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val slotTimeColor = if (isDark) Color.White else Color(0xFF0F172A)
    val activeSlotBg = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    val activeSlotText = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    val monthTitle = when (config.deskCalendarType) {
        DeskCalendarType.Hijri -> "جمادى الأولى ١٤٤٨ هـ (نوفمبر)"
        DeskCalendarType.Coptic -> "٨ هاتور ١٧٤٣ (نوفمبر)"
        DeskCalendarType.Gregorian -> "نوفمبر ٢٠٢٦ م (جمادى)"
    }
    val todayBadge = when (config.deskCalendarType) {
        DeskCalendarType.Hijri -> "الأحد ١٧"
        DeskCalendarType.Coptic -> "الأحد ٨"
        DeskCalendarType.Gregorian -> "الأحد ١٦"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, cardBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = activeSlotBg,
                    ) {
                        Text(
                            text = todayBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = activeSlotText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                if (config.deskCalendarShowCountdown) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⏳ الصلاة القادمة: المغرب", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("00:34:12", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .background(gridBg, RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("شبكة أيام الشهر", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = titleColor)
                        Spacer(Modifier.height(4.dp))
                        Text("س  ح  ن  ث  ر  خ  ج", style = MaterialTheme.typography.labelSmall, color = slotNameColor)
                        Text("1   2   3   4   5   6   7", style = MaterialTheme.typography.labelSmall, color = dayColor)
                        Text("8   9  10 11 12 13 14", style = MaterialTheme.typography.labelSmall, color = dayColor)
                        Text("15 [16] 17 18 19 20 21", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.5.dp),
                    ) {
                        listOf(
                            "الفجر" to "05:02",
                            "الشروق" to "06:22",
                            "الظهر" to "11:45",
                            "العصر" to "02:40",
                            "المغرب" to "05:01",
                            "العشاء" to "06:21",
                        ).forEach { (p, t) ->
                            val active = config.deskCalendarHighlightNextPrayer && p == "المغرب"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (active) activeSlotBg else slotBg,
                                border = if (active) BorderStroke(1.2.dp, activeSlotText) else null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(p, style = MaterialTheme.typography.labelSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) activeSlotText else slotNameColor)
                                    Text(t, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (active) activeSlotText else slotTimeColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Preview
@Composable
private fun WidgetSettingsScreenPreview() {
    AlAzanThemePreview {
        WidgetSettingsScreen(uiState = WidgetSettingsUiState(), onAction = {})
    }
}

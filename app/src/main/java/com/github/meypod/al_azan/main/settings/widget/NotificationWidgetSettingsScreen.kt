package com.github.meypod.al_azan.main.settings.widget

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SpiritualCapsuleMode
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun NotificationWidgetSettingsScreen(
    uiState: WidgetSettingsUiState,
    onAction: (WidgetSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    val requestWidgetPermissions = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            if (!results.requiredAllGranted()) onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(false))
        },
    )

    val prayerLayoutOptions = listOf(
        NotificationWidgetLayout.Table,
        NotificationWidgetLayout.Compact,
    )
    val currentLayout = if (uiState.settings.notificationWidgetLayout in prayerLayoutOptions) {
        uiState.settings.notificationWidgetLayout
    } else {
        NotificationWidgetLayout.Table
    }

    ScreenScaffold(
        title = stringResource(R.string.nws_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        scrollable = true,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. مربع مواقيت الصلاة الدائم (مفتاح مستقل + خيارات النمط)
        SectionTitle(title = stringResource(R.string.nws_sec1_prayer))
        ACard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = if (uiState.settings.showWidget) 1.5.dp else 1.dp,
                color = if (uiState.settings.showWidget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
            tonalElevation = if (uiState.settings.showWidget) 2.dp else 1.dp,
        ) { cardPadding ->
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingSwitch(
                    title = stringResource(R.string.nws_enable_prayer_notif),
                    subtitle = stringResource(R.string.nws_enable_prayer_notif_sub),
                    checked = uiState.settings.showWidget,
                    onCheckedChange = { enabled ->
                        onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(enabled))
                        if (enabled) requestWidgetPermissions(SchedulingPermissionSteps.widget)
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                )

                Text(
                    text = stringResource(R.string.nws_choose_layout),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!uiState.settings.showWidget) Modifier.alpha(0.6f) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // مربع 1: جدول المواقيت الكامل
                    val isTableSelected = currentLayout == NotificationWidgetLayout.Table
                    SelectableTileCard(
                        modifier = Modifier.weight(1f),
                        icon = "📋",
                        title = stringResource(R.string.nws_layout_table),
                        subtitle = stringResource(R.string.nws_layout_table_sub),
                        isSelected = isTableSelected,
                        onClick = {
                            if (!uiState.settings.showWidget) {
                                onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(true))
                                requestWidgetPermissions(SchedulingPermissionSteps.widget)
                            }
                            onAction(WidgetSettingsUiAction.OnNotificationLayoutChange(NotificationWidgetLayout.Table))
                        },
                        miniPreview = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("عصر", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text("مغرب", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("عشاء", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("فجر", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )

                    // مربع 2: الشريط المصغر للصلاة القادمة
                    val isCompactSelected = currentLayout == NotificationWidgetLayout.Compact
                    SelectableTileCard(
                        modifier = Modifier.weight(1f),
                        icon = "🕌",
                        title = stringResource(R.string.nws_layout_compact),
                        subtitle = stringResource(R.string.nws_layout_compact_sub),
                        isSelected = isCompactSelected,
                        onClick = {
                            if (!uiState.settings.showWidget) {
                                onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(true))
                                requestWidgetPermissions(SchedulingPermissionSteps.widget)
                            }
                            onAction(WidgetSettingsUiAction.OnNotificationLayoutChange(NotificationWidgetLayout.Compact))
                        },
                        miniPreview = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text("صلاة الفجر", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("موعد الأذان  04:52 ص", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }

                if (currentLayout == NotificationWidgetLayout.Table) {
                    SettingSwitch(
                        title = stringResource(R.string.nws_countdown_title),
                        subtitle = stringResource(R.string.nws_countdown_sub),
                        checked = uiState.settings.showWidgetCountdown,
                        onCheckedChange = { onAction(WidgetSettingsUiAction.OnShowCountdownToggle(it)) },
                    )
                }
            }
        }

        // 2. مربع الكبسولة الشرعية الذكية المستقل (مفتاح مستقل بالكامل + خيارات المحرك اللحظي والإشعار الدائم)
        SectionTitle(title = stringResource(R.string.nws_sec2_capsule))
        ACard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                width = if (uiState.nateegaConfig.showSpiritualPillNotification) 1.5.dp else 1.dp,
                color = if (uiState.nateegaConfig.showSpiritualPillNotification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
            tonalElevation = if (uiState.nateegaConfig.showSpiritualPillNotification) 2.dp else 1.dp,
        ) { cardPadding ->
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("🧭", fontSize = 20.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.ws_pill_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.nws_capsule_badge),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(R.string.nws_capsule_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                }

                SettingSwitch(
                    title = stringResource(R.string.nws_enable_capsule),
                    subtitle = stringResource(R.string.nws_enable_capsule_sub),
                    checked = uiState.nateegaConfig.showSpiritualPillNotification,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnShowSpiritualPillNotificationToggle(it)) },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                )

                Text(
                    text = stringResource(R.string.nws_capsule_mode_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!uiState.nateegaConfig.showSpiritualPillNotification) Modifier.alpha(0.65f) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // الخيار 1: المحرك العبادي اللحظي (7 محطات ذكية) - افتراضي
                    val isSmartMoments = uiState.nateegaConfig.spiritualCapsuleMode == SpiritualCapsuleMode.SmartMoments
                    CapsuleOptionCard(
                        icon = "⚡",
                        title = stringResource(R.string.nws_mode_smart_title),
                        tag = stringResource(R.string.nws_mode_smart_tag),
                        subtitle = stringResource(R.string.nws_mode_smart_sub),
                        isSelected = isSmartMoments && uiState.nateegaConfig.showSpiritualPillNotification,
                        onClick = {
                            if (!uiState.nateegaConfig.showSpiritualPillNotification) {
                                onAction(WidgetSettingsUiAction.OnShowSpiritualPillNotificationToggle(true))
                            }
                            onAction(WidgetSettingsUiAction.OnSpiritualCapsuleModeChange(SpiritualCapsuleMode.SmartMoments))
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SmartMomentRow(tag = "دُعَاء مُجَاب", title = "بين الأذان والإقامة (أول 20 دقيقة)", desc = "اغتنام سنة الصلاة القبلية والدعاء الذي لا يُرد.")
                            SmartMomentRow(tag = "أَذْكَار", title = "أذكار ما بعد الفريضة", desc = "معقبات الصلاة (الاستغفار، آية الكرسي، والتسبيح 33).")
                            SmartMomentRow(tag = "إِمْسَاك", title = "أوقات الكراهة والنهي", desc = "تنبيه فقهي بالامتناع عن النوافل (بعد الفجر للشروق، وقبيل الظهر، وبعد العصر) والاشتغال بالذكر.")
                            SmartMomentRow(tag = "ضُحَى", title = "صلاة الضحى", desc = "صلاة الأوابين والصدقة اليومية عن مفاصل الجسد.")
                            SmartMomentRow(tag = "سَاعَة إِجَابَة", title = "يوم وساعة الجمعة", desc = "سورة الكهف، كثرة الصلاة على النبي ﷺ، وساعة الاستجابة قبل الغروب.")
                            SmartMomentRow(tag = "صِيَام", title = "صيام النوافل", desc = "تنبيه مسبق بالنية والسحور (الإثنين والخميس والأيام البيض 13 و14 و15).")
                            SmartMomentRow(tag = "قِيَام / سَحَر", title = "الثلث الأخير والأسحار", desc = "وقت التنزل الإلهي والاستغفار وصلاة الوتر.")
                        }
                    }

                    // الخيار 2: إشعار دائم مستمر
                    val isPersistent = uiState.nateegaConfig.spiritualCapsuleMode == SpiritualCapsuleMode.PersistentShade
                    CapsuleOptionCard(
                        icon = "📌",
                        title = stringResource(R.string.nws_mode_persistent_title),
                        tag = stringResource(R.string.nws_mode_persistent_tag),
                        subtitle = stringResource(R.string.nws_mode_persistent_sub),
                        isSelected = isPersistent && uiState.nateegaConfig.showSpiritualPillNotification,
                        onClick = {
                            if (!uiState.nateegaConfig.showSpiritualPillNotification) {
                                onAction(WidgetSettingsUiAction.OnShowSpiritualPillNotificationToggle(true))
                            }
                            onAction(WidgetSettingsUiAction.OnSpiritualCapsuleModeChange(SpiritualCapsuleMode.PersistentShade))
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                )

                // خيار إضافي: تنبيه وتذكير بذكر مخصص في وقت معين
                SettingSwitch(
                    title = stringResource(R.string.nws_daily_dhikr_title),
                    subtitle = stringResource(R.string.nws_daily_dhikr_sub),
                    checked = uiState.nateegaConfig.customDhikrEnabled,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnCustomDhikrToggle(it)) },
                )

                AnimatedVisibility(visible = uiState.nateegaConfig.customDhikrEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nws_daily_dhikr_header),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        val dhikrTimeOptions = listOf(
                            360 to "06:00 ص (بعد الشروق وأذكار الصباح)",
                            540 to "09:00 ص (صلاة الضحى المباركة)",
                            780 to "01:00 م (بعد صلاة الظهر)",
                            1020 to "05:00 م (قبل الغروب وأذكار المساء)",
                            1260 to "09:00 م (بعد صلاة العشاء)",
                            1380 to "11:00 م (قبل النوم والوتر)",
                            210 to "03:30 ص (وقت السحر والاستغفار)",
                        )

                        BottomSelect(
                            modifier = Modifier.fillMaxWidth(),
                            options = dhikrTimeOptions,
                            optionKey = { it.first.toString() },
                            optionLabel = { it.second },
                            selectedKey = uiState.nateegaConfig.customDhikrTimeMinutes.toString(),
                            onSelect = { onAction(WidgetSettingsUiAction.OnCustomDhikrTimeChange(it.first)) },
                            label = { Text(stringResource(R.string.nws_daily_time_label)) },
                            placeholder = stringResource(R.string.nws_daily_time_placeholder),
                        )

                        CompactOutlinedTextField(
                            value = uiState.nateegaConfig.customDhikrText,
                            onValueChange = { onAction(WidgetSettingsUiAction.OnCustomDhikrTextChange(it)) },
                            label = { Text(stringResource(R.string.nws_daily_text_label)) },
                            placeholder = stringResource(R.string.nws_daily_text_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = stringResource(R.string.nws_quick_suggestions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )

                        val quickDhikrList = listOf(
                            "سبحان الله وبحمده 100 مرة",
                            "أستغفر الله العظيم وأتوب إليه",
                            "اللهم صلِّ وسلم على نبينا محمد ﷺ",
                            "لا إله إلا الله وحده لا شريك له",
                            "لا حول ولا قوة إلا بالله العلي العظيم",
                            "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر",
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            quickDhikrList.forEach { dhikr ->
                                Surface(
                                    onClick = { onAction(WidgetSettingsUiAction.OnCustomDhikrTextChange(dhikr)) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                ) {
                                    Text(
                                        text = dhikr,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                )

                Text(
                    text = stringResource(R.string.nws_capsule_preview),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                SmartPillLivePreview(
                    themeMode = uiState.nateegaConfig.smartPillThemeMode,
                    showWordBadge = false,
                    appThemeColor = uiState.settings.themeColor,
                )

                SmartPillThemePillSelector(
                    selected = uiState.nateegaConfig.smartPillThemeMode,
                    onSelect = { onAction(WidgetSettingsUiAction.OnSmartPillThemeModeChange(it)) },
                )
            }
        }

        // 3. مربع استقرار الإشعار في الخلفية واستثناء البطارية
        SectionTitle(title = stringResource(R.string.nws_sec3_battery))
        ACard(
            modifier = Modifier.fillMaxWidth(),
            border = null,
            tonalElevation = 1.dp,
        ) { cardPadding ->
            val context = LocalContext.current
            var isBatteryIgnored by remember {
                mutableStateOf(!PowerManagerUtils.isBatteryOptimizationEnabled(context))
            }

            LifecycleResumeEffect(Unit) {
                isBatteryIgnored = !PowerManagerUtils.isBatteryOptimizationEnabled(context)
                onPauseOrDispose {}
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBatteryIgnored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (isBatteryIgnored) "🛡️" else "⚡", fontSize = 18.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.nws_battery_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBatteryIgnored) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = stringResource(if (isBatteryIgnored) R.string.nws_battery_protected else R.string.nws_battery_at_risk),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBatteryIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Text(
                            text = if (isBatteryIgnored)
                                stringResource(R.string.nws_battery_protected_desc)
                            else
                                stringResource(R.string.nws_battery_risk_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                if (!isBatteryIgnored) {
                    Button(
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            }.onFailure {
                                runCatching {
                                    val fallback = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        .apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                    context.startActivity(fallback)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nws_battery_exempt_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.nws_battery_settings_btn),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                // خيار إعدادات التشغيل التلقائي OEM (شاومي / سامسونج / أوبو / هواوي)
                TextButton(
                    onClick = {
                        val oemIntent = PowerManagerUtils.powerManagerIntent
                        if (oemIntent != null) {
                            runCatching { context.startActivity(oemIntent) }
                        } else {
                            runCatching {
                                val appDetails = android.content.Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:${context.packageName}")
                                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(appDetails)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.nws_autostart_btn),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableTileCard(
    icon: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    miniPreview: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = icon, fontSize = 22.sp)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }

            if (miniPreview != null) {
                miniPreview()
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun CapsuleOptionCard(
    icon: String,
    title: String,
    tag: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = icon, fontSize = 20.sp)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.padding(start = 6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            if (content != null) {
                content()
            }
        }
    }
}

@Composable
private fun SmartMomentRow(
    tag: String,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        ) {
            Text(
                text = tag,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

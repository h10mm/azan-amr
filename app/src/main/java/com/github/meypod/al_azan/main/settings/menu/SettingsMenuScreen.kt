package com.github.meypod.al_azan.main.settings.menu

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    uiState: SettingsMenuUiState,
    onAction: (SettingsMenuUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.settings),
        onBackClick = {},
        navigationIcon = {},
        modifier = modifier,
        scrollable = true,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 0. التقويم والإمساكية
            SettingsSectionCard(title = stringResource(R.string.settings_section_calendar_schedule)) {
                ModernMenuItem(
                    title = stringResource(R.string.settings_calendar_title),
                    subtitle = stringResource(R.string.settings_calendar_subtitle),
                    icon = R.drawable.calendar_month_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnCalendarClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.monthly_view_title),
                    subtitle = stringResource(R.string.shortcut_monthly_view_desc),
                    icon = R.drawable.calculator_variant_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnMonthlyViewClick) },
                )
            }

        // 1. الصلاة والمواقيت
            SettingsSectionCard(title = stringResource(R.string.settings_section_prayer_times)) {
                ModernMenuItem(
                    title = stringResource(R.string.location_title),
                    subtitle = "تحديد موقعك الجغرافي والإحداثيات",
                    icon = R.drawable.map_marker,
                    onClick = { onAction(SettingsMenuUiAction.OnLocationClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.calculation_title),
                    subtitle = "طريقة الحساب والزوايا والتعديلات اليدوية",
                    icon = R.drawable.calculator_variant_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnCalculationClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.schedule_and_muezzin_title),
                    subtitle = "تحديد صوت المؤذن لكل فريضة، أو التكبيرات فقط، أو الوضع الصامت",
                    icon = R.drawable.outline_volume_up_24,
                    onClick = { onAction(SettingsMenuUiAction.OnScheduleAndMuezzinClick) },
                )
            }

            // 2. تنبيهات الفرائض والسنن
            SettingsSectionCard(title = "تنبيهات الفرائض والسنن") {
                ModernMenuItem(
                    title = "صوت وضوابط الأذان",
                    subtitle = "مستوى الصوت، تخطي الصامت للفجر، كتم الهاتف في المسجد، والاهتزاز",
                    icon = R.drawable.bell_cog_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnNotificationAndSoundClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = "تذكيرات السنن والأذكار",
                    subtitle = "منبهات صلاة الوتر، الضحى، قيام الليل، وأذكار الصباح والمساء",
                    icon = R.drawable.alarm,
                    onClick = { onAction(SettingsMenuUiAction.OnRemindersClick) },
                )
            }

            // 3. المظهر والتخصيص والودجت
            SettingsSectionCard(title = "المظهر والتخصيص والودجت") {
                ModernMenuItem(
                    title = "إعدادات وتخصيص الودجات",
                    subtitle = "رزمانة الحائط، التقويم المكتبي، والودجات العصرية للشاشة الرئيسية",
                    icon = R.drawable.toy_brick_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnWidgetSettingsClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = "شريط الإشعار الدائم",
                    subtitle = "عرض مواقيت الصلوات والعد التنازلي في لوحة التنبيهات وشريط الحالة",
                    icon = R.drawable.bell_solid,
                    onClick = { onAction(SettingsMenuUiAction.OnNotificationWidgetClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.interface_settings),
                    subtitle = "السمة (أزرق داكن، داكن، فاتح)، مقياس الخط، ونظام الأرقام",
                    icon = R.drawable.shape_plus_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnInterfaceSettingsClick) },
                )
            }

            // 4. النظام والدعم
            SettingsSectionCard(title = stringResource(R.string.settings_section_system)) {
                ModernMenuItem(
                    title = stringResource(R.string.troubleshoot_title),
                    subtitle = "إصلاح مشاكل التنبيه وتحسين استهلاك البطارية",
                    icon = R.drawable.tools,
                    onClick = { onAction(SettingsMenuUiAction.OnTroubleshootClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.backup_and_restore_title),
                    subtitle = "حفظ واسترجاع إعداداتك بملف خارجي",
                    icon = R.drawable.outline_backup_24,
                    onClick = { onAction(SettingsMenuUiAction.OnBackupAndRestoreClick) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ModernMenuItem(
                    title = stringResource(R.string.about),
                    subtitle = "إهداء التطبيق ومعلومات الإصدار",
                    icon = R.drawable.info_variant_outline,
                    onClick = { onAction(SettingsMenuUiAction.OnAboutClick) },
                )
            }
    }
}

@Composable
private fun SettingsSectionCard(
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
private fun ModernMenuItem(
    title: String,
    subtitle: String? = null,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier.size(40.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(icon),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.baseline_navigate_next_24),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            contentDescription = null,
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun SettingsMenuPreview() {
    AlAzanTheme {
        SettingsMenuScreen(
            uiState = SettingsMenuUiState(),
            onAction = {},
        )
    }
}

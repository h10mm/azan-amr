package com.github.meypod.al_azan.main.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateDisplayMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaDateOrder
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun CompactPrayerAppearanceScreen(
    config: NateegaWidgetConfig,
    onDateDisplayModeChange: (NateegaDateDisplayMode) -> Unit,
    onDateOrderChange: (NateegaDateOrder) -> Unit,
    onThemeModeChange: (NateegaThemeMode) -> Unit,
    onShowCountdownToggle: (Boolean) -> Unit,
    onShowWisdomToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = "مظهر ودجت المواقيت والتقويم المدمج",
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        // معاينة حية تفاعلية للودجت في الوقت الفعلي
        SectionHeader(title = "معاينة حية لشكل الودجت")
        CompactPrayerLivePreview(config = config)

        // قسم خيارات عرض التاريخ والتقويم
        SectionHeader(title = "خيارات عرض التاريخ والتقويم")
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = NateegaDateDisplayMode.entries,
                    optionKey = { it.name },
                    optionLabel = { it.labelArabic() },
                    selectedKey = config.compactDateDisplayMode.name,
                    onSelect = onDateDisplayModeChange,
                    label = { Text("التقويم المعروض") },
                    placeholder = "اختر نوع التقويم",
                    supportingText = { Text("يمكنك عرض التاريخين معاً (هجري وميلادي)، أو الاكتفاء بالهجري فقط، أو الميلادي فقط") },
                )
                if (config.compactDateDisplayMode == NateegaDateDisplayMode.Both) {
                    BottomSelect(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        options = NateegaDateOrder.entries,
                        optionKey = { it.name },
                        optionLabel = { it.labelArabic() },
                        selectedKey = config.compactDateOrder.name,
                        onSelect = onDateOrderChange,
                        label = { Text("ترتيب التاريخين (يمين / يسار)") },
                        placeholder = "اختر الترتيب",
                        supportingText = { Text("حدد أي التاريخين يظهر في اليمين والآخر في اليسار") },
                    )
                }
            }
        }

        // قسم التصميم والثيم
        SectionHeader(title = "تصميم الودجت والثيم")
        ACard { cardPadding ->
            NateegaThemePillSelector(
                selected = config.compactThemeMode,
                onSelect = onThemeModeChange,
                modifier = Modifier.padding(cardPadding),
            )
        }

        // قسم العناصر الإضافية والتحكم
        SectionHeader(title = "العناصر المعروضة داخل الودجت")
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingSwitch(
                    title = "العد التنازلي ونسبة الصلاة",
                    subtitle = "إظهار شريط الوقت التناقصي للصلاة القادمة والعداد الدقيق",
                    checked = config.compactShowCountdown,
                    onCheckedChange = onShowCountdownToggle,
                )
                SettingSwitch(
                    title = "مأثورة وحكمة اليوم",
                    subtitle = "إظهار شريط كبسولة الحكمة اليومية المتجددة أسفل الودجت",
                    checked = config.compactShowWisdom,
                    onCheckedChange = onShowWisdomToggle,
                )
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
            .semantics { heading() },
    )
}

@Composable
fun CompactPrayerLivePreview(
    config: NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val resolvedTheme = if (config.compactThemeMode == NateegaThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> NateegaThemeMode.ClassicPaper
            ThemeColor.Dark -> NateegaThemeMode.Amoled
            ThemeColor.MidnightBlue -> NateegaThemeMode.MidnightBlue
        }
    } else {
        config.compactThemeMode
    }

    val isDark = when (resolvedTheme) {
        NateegaThemeMode.Amoled, NateegaThemeMode.MidnightBlue -> true
        NateegaThemeMode.ClassicPaper, NateegaThemeMode.MatchAppTheme -> false
    }

    val widgetBg = when (resolvedTheme) {
        NateegaThemeMode.ClassicPaper, NateegaThemeMode.MatchAppTheme -> Color(0xFFFFFDF7)
        NateegaThemeMode.Amoled -> Color(0xFF000000)
        NateegaThemeMode.MidnightBlue -> Color(0xFF0F172A)
    }

    val widgetBorder = when (resolvedTheme) {
        NateegaThemeMode.ClassicPaper, NateegaThemeMode.MatchAppTheme -> Color(0xFFE5E0D8)
        NateegaThemeMode.Amoled -> Color(0xFF27272A)
        NateegaThemeMode.MidnightBlue -> Color(0xFF1E293B)
    }

    val textColor = when (resolvedTheme) {
        NateegaThemeMode.ClassicPaper, NateegaThemeMode.MatchAppTheme -> Color(0xFF1E3A8A)
        NateegaThemeMode.Amoled, NateegaThemeMode.MidnightBlue -> Color(0xFFFFFFFF)
    }
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val activePrayerBg = if (isDark) Color(0xFF059669) else Color(0xFF047857)
    val slotBg = if (isDark) Color(0xFF18181B) else Color(0xFFF9F7F1)
    val wisdomBg = if (isDark) Color(0xFF18181B) else Color(0xFFF5EFEB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(widgetBg)
            .border(1.dp, widgetBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header Row: Day + Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "الاثنين",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                )

                // Date Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val hijriPill = @Composable {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(textColor.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "🌙 24 جمادى الأولى", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }

                    val gregPill = @Composable {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(textColor.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "☀️ 16 نوفمبر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }

                    when (config.compactDateDisplayMode) {
                        NateegaDateDisplayMode.Both -> {
                            if (config.compactDateOrder == NateegaDateOrder.HijriRightGregorianLeft) {
                                hijriPill()
                                gregPill()
                            } else {
                                gregPill()
                                hijriPill()
                            }
                        }
                        NateegaDateDisplayMode.HijriOnly -> hijriPill()
                        NateegaDateDisplayMode.GregorianOnly -> gregPill()
                    }
                }
            }

            // Countdown Card if enabled
            if (config.compactShowCountdown) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⏳ الصلاة القادمة: المغرب",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                    )
                    Text(
                        text = "00:42:15",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF59E0B),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Prayers Grid (6 slots)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val prayers = listOf(
                    "الفجر" to "04:52",
                    "الشروق" to "06:21",
                    "الظهر" to "11:40",
                    "العصر" to "02:37",
                    "المغرب" to "05:00",
                    "العشاء" to "06:20",
                )
                prayers.forEachIndexed { index, (name, time) ->
                    val isActive = index == 4
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) activePrayerBg else slotBg)
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else textMuted,
                        )
                        Text(
                            text = time,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isActive) Color.White else textColor,
                        )
                    }
                }
            }

            // Wisdom text if enabled
            if (config.compactShowWisdom) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(wisdomBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\"أحب الأعمال إلى الله الصلاة على وقتها\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

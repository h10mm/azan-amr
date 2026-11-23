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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.widget.NateegaClockPosition
import com.github.meypod.al_azan.core.domain.model.widget.NateegaLanguageMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun NateegaAppearanceScreen(
    config: NateegaWidgetConfig,
    onClockPositionChange: (NateegaClockPosition) -> Unit,
    onShowCopticDateToggle: (Boolean) -> Unit,
    onShowCountdownToggle: (Boolean) -> Unit,
    onShowWisdomToggle: (Boolean) -> Unit,
    onLanguageModeChange: (NateegaLanguageMode) -> Unit,
    onThemeModeChange: (NateegaThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = "مظهر ودجت النتيجة (الكبير)",
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        // معاينة حية تفاعلية للودجت في الوقت الفعلي
        SectionHeader(title = "معاينة حية لشكل الودجت")
        NateegaLivePreview(config = config)

        // قسم التصميم والثيم
        SectionHeader(title = "تصميم الودجت والثيم")
        ACard { cardPadding ->
            NateegaThemePillSelector(
                selected = config.themeMode,
                onSelect = onThemeModeChange,
                modifier = Modifier.padding(cardPadding),
            )
        }

        // قسم الساعة الرقمية
        SectionHeader(title = "الساعة الرقمية")
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
                selectedKey = if (config.clockPosition == NateegaClockPosition.InlineWithDay) {
                    NateegaClockPosition.Side.name
                } else {
                    config.clockPosition.name
                },
                onSelect = onClockPositionChange,
                label = { Text("موضع وحجم الساعة") },
                placeholder = "اختر موضع الساعة",
                supportingText = { Text("اختر بين عرض الساعة كبيرة في المنتصف، أو جانبية مع اليوم (اليوم يمين والساعة يسار لاستغلال المساحة)، أو إخفائها") },
            )
        }

        // قسم اللغة وأسماء الأيام
        SectionHeader(title = "اللغة وأسماء الأيام")
        ACard { cardPadding ->
            BottomSelect(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                options = NateegaLanguageMode.entries,
                optionKey = { it.name },
                optionLabel = { it.labelArabic() },
                selectedKey = config.languageMode.name,
                onSelect = onLanguageModeChange,
                label = { Text("لغة عرض اليوم والتاريخ") },
                placeholder = "اختر لغة الودجت",
                supportingText = { Text("الوضع التلقائي يتبع لغة التطبيق، أو يمكنك تحديد اللغة العربية فقط أو الإنجليزية يدوياً") },
            )
        }

        // قسم العناصر الإضافية والتحكم
        SectionHeader(title = "العناصر المعروضة داخل الودجت")
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingSwitch(
                    title = "عرض التاريخ القبطي",
                    subtitle = "إظهار مربع التاريخ القبطي في المنتصف (مثل: ٤ هاتور)",
                    checked = config.showCopticDate,
                    onCheckedChange = onShowCopticDateToggle,
                )
                SettingSwitch(
                    title = "العد التنازلي ونسبة الصلاة",
                    subtitle = "إظهار الوقت المتبقي للصلاة القادمة مع شريط التقدم التناقصي",
                    checked = config.showCountdown,
                    onCheckedChange = onShowCountdownToggle,
                )
                SettingSwitch(
                    title = "مأثورة وحكمة اليوم",
                    subtitle = "إظهار حكمة ومأثورة يومية متجددة أسفل الودجت",
                    checked = config.showWisdom,
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
fun NateegaLivePreview(
    config: NateegaWidgetConfig,
    appThemeColor: ThemeColor = ThemeColor.ClassicLight,
    modifier: Modifier = Modifier,
) {
    val resolvedTheme = if (config.themeMode == NateegaThemeMode.MatchAppTheme) {
        when (appThemeColor) {
            ThemeColor.ClassicLight -> NateegaThemeMode.ClassicPaper
            ThemeColor.Dark -> NateegaThemeMode.Amoled
            ThemeColor.MidnightBlue -> NateegaThemeMode.MidnightBlue
        }
    } else {
        config.themeMode
    }

    // Theme colors based on selection
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
    val dateColor = textColor // Both Hijri and Gregorian dates use the authentic default color
    val activePrayerBg = if (isDark) Color(0xFF059669) else Color(0xFF047857)
    val slotBg = if (isDark) Color(0xFF18181B) else Color(0xFFF9F7F1)
    val capsuleBg = if (isDark) Color(0xFF18181B) else Color(0xFFF1ECE1)
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Day Header
            Text(
                text = "الاثنين",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                textAlign = TextAlign.Center,
            )

            // Sub-row: English day + Clock (if side) + Location
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(capsuleBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Monday",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMuted,
                    )
                }

                if (config.clockPosition == NateegaClockPosition.Side || config.clockPosition == NateegaClockPosition.InlineWithDay) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(capsuleBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⏰ 05:42 م",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "مصر / القاهرة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                    )
                }
            }

            // Center large clock if selected
            if (config.clockPosition == NateegaClockPosition.CenterLarge) {
                Text(
                    text = "05:42 م",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            // Dual Date Columns (Side by Side)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Hijri Date
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(8.dp),
                ) {
                    Text(text = "24", fontSize = 38.sp, fontWeight = FontWeight.Black, color = dateColor)
                    Text(text = "جمادى الأولى", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = dateColor)
                    Text(text = "1448 هـ", fontSize = 10.sp, color = textMuted)
                }

                Spacer(Modifier.width(8.dp))

                // Gregorian Date
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(8.dp),
                ) {
                    Text(text = "16", fontSize = 38.sp, fontWeight = FontWeight.Black, color = dateColor)
                    Text(text = "نوفمبر", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = dateColor)
                    Text(text = "2026 م", fontSize = 10.sp, color = textMuted)
                }
            }

            // Coptic strip and countdown
            if (config.showCopticDate || config.showCountdown) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(capsuleBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (config.showCopticDate) {
                        Text(text = "🌾 7 هاتور 1743", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (config.showCountdown) {
                        Text(text = "⏳ العشاء بعد 01:14:20", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

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
                    val isActive = index == 4 // Maghrib active
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

            // Wisdom box (Seamless Naskh Typography, Single Line, No Border)
            if (config.showWisdom) {
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "من توكل على الله كفاه، ومن استعان به هداه",
                        fontFamily = FontFamily(Font(R.font.amiri_regular)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

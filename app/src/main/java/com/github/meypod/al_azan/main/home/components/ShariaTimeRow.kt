package com.github.meypod.al_azan.main.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.formatInstant

@Composable
fun ShariaTimeRow(
    state: ShariaTimeRowUiState,
    compact: Boolean = false,
) {
    val isHighlighted = state.highlightState == HighlightState.Highlighted
    val dynamicFontSize = if (compact) 15.sp else 20.sp

    // 1. لون خلفية الصلاة الحالية حسب السمة المحددة
    val targetHighlightBg = when (state.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFFC9EDEE) // خلفية بترولية فاتحة ناعمة متناسقة مع كروت التطبيق
        ThemeColor.MidnightBlue -> Color(0xFF1E3566) // كحلي ملكي مميز عالي التباين
        ThemeColor.Dark -> Color(0xFF0C2425)         // خلفية داكنة راقية بلمسة بترولية ناعمة متناسقة مع الأسود
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isHighlighted) targetHighlightBg else when (state.themeColor) {
            ThemeColor.ClassicLight -> Color.White // كرت أبيض ناصع متناسق وأنيق
            ThemeColor.MidnightBlue -> Color(0xFF111D3B) // كرت كحلي داكن منفصل بوضوح تام عن الخلفية
            ThemeColor.Dark -> Color(0xFF141414)         // كرت أسود ناعم منفصل بوضوح تام عن الخلفية السوداء
        },
        animationSpec = tween(300),
        label = "rowBgAnimation"
    )

    // لون التمييز المخصص للحدود والنصوص للصلاة النشطة
    val accentColor = when (state.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFF00585A) // لون بترولي رئيسي للهوية في الوضع الفاتح
        ThemeColor.MidnightBlue -> Color(0xFFFFD166) // أصفر ذهبي كهرماني دافئ ومشرق (متوافق مع عمى الألوان)
        ThemeColor.Dark -> Color(0xFF76D5D8)         // تركواز مشرق من هوية التطبيق الأساسية
    }

    // 2. لون النصوص لضمان أعلى درجات التباين والوضوح
    val textColor = when {
        isHighlighted && state.themeColor == ThemeColor.ClassicLight -> Color(0xFF003738)
        isHighlighted -> accentColor
        state.themeColor == ThemeColor.ClassicLight -> Color(0xFF181C1C)
        state.themeColor == ThemeColor.MidnightBlue -> Color(0xFFFFFFFF) // أبيض ناصع 100% لكافة النصوص والأرقام
        state.themeColor == ThemeColor.Dark -> Color(0xFFFFFFFF)         // أبيض ناصع 100% لكافة نصوص وأرقام الوضع الداكن
        else -> Color.White.copy(alpha = 0.92f)
    }

    val rowShape = RoundedCornerShape(16.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 6.dp else 14.dp, vertical = if (compact) 2.dp else 3.dp)
            .clip(rowShape)
            .background(animatedBgColor)
            .then(
                when {
                    isHighlighted && state.themeColor == ThemeColor.MidnightBlue ->
                        Modifier.border(2.dp, accentColor, rowShape)
                    isHighlighted && state.themeColor == ThemeColor.Dark ->
                        Modifier.border(2.dp, accentColor, rowShape)
                    isHighlighted ->
                        Modifier.border(2.dp, accentColor, rowShape)
                    state.themeColor == ThemeColor.ClassicLight ->
                        Modifier.border(1.dp, Color(0xFFD2E4E4), rowShape)
                    state.themeColor == ThemeColor.MidnightBlue ->
                        Modifier.border(1.dp, Color(0xFF2B4170), rowShape)
                    state.themeColor == ThemeColor.Dark ->
                        Modifier.border(1.dp, Color(0xFF303030), rowShape)
                    else -> Modifier
                }
            )
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = if (compact) 7.dp else 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.prayer.i18n(),
                fontSize = dynamicFontSize,
                fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
            )
            if (state.skipped) {
                Icon(
                    painterResource(R.drawable.outline_volume_off),
                    contentDescription = stringResource(R.string.upcoming_alarm_skipped),
                    tint = textColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (state.instant == null) {
            Text(
                text = "--:--",
                fontSize = dynamicFontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        } else {
            Text(
                text = formatInstant(
                    state.instant,
                    state.locale,
                    "gregorian",
                    if (state.is24Hours) "HH:mm" else "hh:mm",
                    state.numberingSystem,
                ),
                fontSize = dynamicFontSize,
                fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
            )
        }
    }
}

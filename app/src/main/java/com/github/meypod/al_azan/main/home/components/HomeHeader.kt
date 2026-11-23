package com.github.meypod.al_azan.main.home.components

import android.icu.text.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.HomeShortcut
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.model.settings.i18n
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.ClassicHighlightBackground
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import com.github.meypod.al_azan.main.home.HomeUiAction
import com.github.meypod.al_azan.main.home.HomeUiState
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Composable
fun HomeHeader(
    uiState: HomeUiState,
    wideLayout: Boolean = false,
    onAction: (HomeUiAction) -> Unit,
    headerButtonScale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val classic = uiState.themeColor.isClassic()
    val headerContentColor = if (classic) MaterialTheme.colorScheme.onSurface else Color.White
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)

    val patternBackgroundColor = when (uiState.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFFE9F1F1)
        ThemeColor.MidnightBlue -> Color(0xFF0A1128)
        ThemeColor.Dark -> Color(0xFF000000) // سواد حقيقي كامل لخلفية الوضع الداكن (True AMOLED Black)
    }

    // لون التمييز المتوافق مع السمات الثلاث
    val targetHighlightColor = when (uiState.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFF007A7E) // لون بترولي/تركوازي مريح ومتناسق مع السمة الفاتحة
        ThemeColor.MidnightBlue -> Color(0xFFFFD166) // أصفر ذهبي كهرماني دافئ ومشرق فائق التباين لمصابي عمى الألوان
        ThemeColor.Dark -> Color(0xFF76D5D8)         // تركواز مشرق أنيق متطابق مع بطاقات الصلوات
    }

    val animatedHighlightColor by animateColorAsState(
        targetValue = targetHighlightColor,
        animationSpec = tween(400),
        label = "highlightColorAnimation"
    )

    val buttonAndBoxBaseColor = when (uiState.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFF00585A).copy(alpha = 0.08f)
        ThemeColor.MidnightBlue -> Color(0xFF132042)
        ThemeColor.Dark -> Color(0xFF141414) // أزرار وعداد بخلفية سوداء راقية
    }

    Column(
        modifier
            .then(if (wideLayout) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
            .then(
                if (classic) {
                    Modifier.patternedBackground(
                        pattern = patternImage,
                        backgroundColor = Color(0xFFE9F1F1),
                        patternAlpha = 0.03f,
                    )
                } else {
                    Modifier.patternedBackground(
                        pattern = patternImage,
                        backgroundColor = patternBackgroundColor,
                        patternAlpha = 0.03f,
                    )
                }
            )
            .padding(
                top = if (wideLayout) 12.dp else 4.dp,
                bottom = if (wideLayout) 10.dp else 6.dp,
                start = 14.dp,
                end = 14.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (wideLayout) Arrangement.Center else Arrangement.Top,
    ) {
        val fullFormattedTime = if (uiState.is24Hour) {
            formatInstant(uiState.currentInstant, uiState.locale, uiState.calendar, DateFormat.HOUR24_MINUTE, uiState.numberingSystem)
        } else {
            formatInstant(uiState.currentInstant, uiState.locale, uiState.calendar, DateFormat.HOUR_MINUTE, uiState.numberingSystem)
        }
        val timeParts = fullFormattedTime.trim().split(" ")
        val timeDigits = timeParts.firstOrNull() ?: fullFormattedTime

        val clockFontSize = if (wideLayout) 52.sp else 76.sp
        val clockLineHeight = if (wideLayout) 52.sp else 76.sp

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // شريط الأزرار السريعة في أعلى الرأس (مقسم يمين ويسار في الزوايا، بدون دفع الساعة للأسفل)
            if (uiState.homeShortcuts.isNotEmpty()) {
                val shortcuts = uiState.homeShortcuts
                val half = (shortcuts.size + 1) / 2
                val startShortcuts = shortcuts.take(half)
                val endShortcuts = shortcuts.drop(half)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // الجانب الأول (أقصى اليمين في العربية)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        startShortcuts.forEach { shortcut ->
                            HomeShortcutButton(shortcut, headerContentColor, onAction)
                        }
                    }

                    // الجانب الثاني (أقصى اليسار في العربية)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        endShortcuts.forEach { shortcut ->
                            HomeShortcutButton(shortcut, headerContentColor, onAction)
                        }
                    }
                }
            }

            Text(
                text = timeDigits,
                fontSize = clockFontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = headerContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (uiState.homeShortcuts.isNotEmpty()) 10.dp else 0.dp),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFeatureSettings = "tnum",
                    lineHeight = clockLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
            )
        }

        Spacer(Modifier.height(if (wideLayout) 6.dp else 4.dp))

        HomeDateInlineRow(
            uiState = uiState,
            contentColor = headerContentColor,
            dateRowFontSize = if (wideLayout) 12.sp else 13.sp,
            onAction = onAction,
        )

        Spacer(Modifier.height(if (wideLayout) 12.dp else 10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = headerButtonScale
                    scaleY = headerButtonScale
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // زر الموقع
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Button(
                    onClick = { onAction(HomeUiAction.OnLocationTextClick) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonColors(
                        buttonAndBoxBaseColor,
                        headerContentColor,
                        headerContentColor.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.map_marker),
                            contentDescription = stringResource(R.string.location_title)
                        )
                        Text(
                            text = uiState.location?.locationDetail?.toDisplayString()
                                ?: stringResource(R.string.set_location_hint),
                            color = headerContentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            // حساب نسبة الوقت المتبقي للعداد التنازلي بدقة بين الصلاة السابقة والصلاة القادمة
            val targetRatio = run {
                val nextTime = uiState.nextShariaTime
                if (nextTime != null && uiState.currentInstant < nextTime.prayerTime) {
                    val remainingMs = (nextTime.prayerTime - uiState.currentInstant).inWholeMilliseconds
                    val prevPrayerTime = SHARIA_TIMES_IN_ORDER
                        .filter { it !in uiState.hiddenPrayers }
                        .mapNotNull { uiState.shariaTimes?.forPrayer(it) }
                        .filter { it <= uiState.currentInstant }
                        .maxOrNull()
                        ?: SHARIA_TIMES_IN_ORDER
                            .mapNotNull { uiState.shariaTimes?.forPrayer(it) }
                            .filter { it <= uiState.currentInstant }
                            .maxOrNull()
                        ?: (nextTime.prayerTime - 6.hours)
                    val totalSpanMs = (nextTime.prayerTime - prevPrayerTime).inWholeMilliseconds.coerceAtLeast(1L)
                    (remainingMs.toFloat() / totalSpanMs.toFloat()).coerceIn(0.03f, 1f)
                } else {
                    0f
                }
            }

            // حركة خطية مستمرة (LinearEasing 1000ms) متزامنة مع الثواني لتناقص فائق الانسيابية
            val animatedRemainingRatio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                label = "countdownProgress"
            )

            // صندوق العداد التنازلي المطور
            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(buttonAndBoxBaseColor)
                    .border(1.dp, animatedHighlightColor.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
            ) {
                // شريط تناقصي خلفي واضح وانسيابي يعبر عن نسبة الوقت المتبقي
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRemainingRatio)
                        .fillMaxHeight()
                        .background(animatedHighlightColor.copy(alpha = 0.26f))
                )

                // خط مؤشر سفلي مشرق وواضح
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRemainingRatio)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 4.dp))
                        .align(Alignment.BottomStart)
                        .background(animatedHighlightColor)
                )

                if (uiState.showNextPrayerCountdown && uiState.nextShariaTime != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(animatedHighlightColor)
                            )
                            Text(
                                text = uiState.nextShariaTime.prayer.i18n(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = animatedHighlightColor,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.countdownText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = headerContentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // زر طريقة الحساب
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Button(
                    onClick = { onAction(HomeUiAction.OnCalculationLinkClick) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonColors(
                        buttonAndBoxBaseColor,
                        headerContentColor,
                        headerContentColor.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.calculator_variant_outline),
                            contentDescription = stringResource(R.string.calculation_title)
                        )
                        Text(
                            text = stringResource(R.string.calculation_title),
                            color = headerContentColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDateInlineRow(
    uiState: HomeUiState,
    contentColor: Color,
    dateRowFontSize: TextUnit = 13.sp,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayName = formatInstant(
        uiState.viewingInstant,
        uiState.locale,
        "gregorian",
        DateFormat.WEEKDAY,
        numberingSystem = uiState.numberingSystem,
    )

    val gregorianDate = formatInstant(
        uiState.viewingInstant,
        uiState.locale,
        "gregorian",
        DateFormat.YEAR_MONTH_DAY,
        numberingSystem = uiState.numberingSystem,
    )

    val hijriDate = formatInstant(
        addDaysTimeZoneAware(uiState.viewingInstant, uiState.hijriDateAdjustment),
        uiState.arabicCalendarLocale,
        uiState.arabicCalendar,
        numberingSystem = uiState.numberingSystem,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClickLabel = stringResource(R.string.choose_date),
                role = Role.Button,
                onClick = { onAction(HomeUiAction.OnCalendarClick) },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (uiState.themeColor) {
                            ThemeColor.MidnightBlue -> Color(0xFF132042)
                            ThemeColor.Dark -> Color(0xFF141414)
                            else -> contentColor.copy(alpha = 0.08f)
                        }
                    )
                    .then(
                        when (uiState.themeColor) {
                            ThemeColor.MidnightBlue -> Modifier.border(1.dp, Color(0xFF3B528A), RoundedCornerShape(8.dp))
                            ThemeColor.Dark -> Modifier.border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
                            else -> Modifier
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = gregorianDate,
                    color = contentColor,
                    fontSize = dateRowFontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (uiState.themeColor) {
                        ThemeColor.MidnightBlue -> Color(0xFF1E3566)
                        ThemeColor.Dark -> Color(0xFF0C2425)
                        else -> contentColor.copy(alpha = 0.18f)
                    }
                )
                .then(
                    when (uiState.themeColor) {
                        ThemeColor.MidnightBlue -> Modifier.border(1.dp, Color(0xFFFFD166).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        ThemeColor.Dark -> Modifier.border(1.dp, Color(0xFF76D5D8).copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        else -> Modifier
                    }
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayName,
                color = when (uiState.themeColor) {
                    ThemeColor.MidnightBlue -> Color(0xFFFFD166)
                    ThemeColor.Dark -> Color(0xFF76D5D8)
                    else -> contentColor
                },
                fontSize = dateRowFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (uiState.themeColor) {
                            ThemeColor.MidnightBlue -> Color(0xFF132042)
                            ThemeColor.Dark -> Color(0xFF141414)
                            else -> contentColor.copy(alpha = 0.08f)
                        }
                    )
                    .then(
                        when (uiState.themeColor) {
                            ThemeColor.MidnightBlue -> Modifier.border(1.dp, Color(0xFF3B528A), RoundedCornerShape(8.dp))
                            ThemeColor.Dark -> Modifier.border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
                            else -> Modifier
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = hijriDate,
                    color = contentColor,
                    fontSize = dateRowFontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeShortcutButton(
    shortcut: HomeShortcut,
    contentColor: Color,
    onAction: (HomeUiAction) -> Unit,
) {
    val (iconRes, action) = when (shortcut) {
        HomeShortcut.Counter -> R.drawable.ic_tasbih to HomeUiAction.OnCounterLinkClick
        HomeShortcut.Silence -> R.drawable.outline_volume_off to HomeUiAction.OnSilenceShortcutClick
        HomeShortcut.ThemeToggle -> R.drawable.ic_theme_mode to HomeUiAction.OnThemeToggleShortcutClick
        HomeShortcut.NateegaSettings -> R.drawable.ic_nateega_card to HomeUiAction.OnNateegaShortcutClick
        HomeShortcut.Qibla -> R.drawable.compass_outline to HomeUiAction.OnQiblaShortcutClick
        HomeShortcut.MonthlyView -> R.drawable.calendar_month_outline to HomeUiAction.OnMonthlyViewClick
        HomeShortcut.Reminders -> R.drawable.alarm to HomeUiAction.OnReminderLinkClick
        HomeShortcut.UpcomingAlarms -> R.drawable.outline_calendar_month_24 to HomeUiAction.OnUpcomingAlarmsClick
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.1f))
            .border(1.dp, contentColor.copy(alpha = 0.28f), CircleShape)
            .clickable { onAction(action) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = shortcut.i18n(),
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

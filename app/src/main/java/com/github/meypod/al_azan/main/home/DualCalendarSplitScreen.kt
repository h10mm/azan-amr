package com.github.meypod.al_azan.main.home

import android.icu.text.DateFormat
import android.icu.text.DateFormatSymbols
import android.icu.util.ULocale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.IslamicCalendarEvents
import com.github.meypod.al_azan.core.domain.util.IslamicDayEvent
import com.github.meypod.al_azan.core.domain.util.IslamicEventType
import com.github.meypod.al_azan.core.domain.util.NateegaWisdomProvider
import com.github.meypod.al_azan.core.domain.util.addCalendarMonths
import com.github.meypod.al_azan.core.domain.util.calendarDayOf
import com.github.meypod.al_azan.core.domain.util.calendarMonth
import com.github.meypod.al_azan.core.domain.util.calendarMonthSpan
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.formatDateSpan
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.narrowWeekdayNames
import com.github.meypod.al_azan.core.domain.util.weekdaysFrom
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

@Composable
fun DualCalendarSplitScreen(
    homeState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // أنظمة التقويم الهجري والميلادي
    val lunarSystem = remember(homeState.arabicCalendar, homeState.arabicCalendarLocale, homeState.hijriDateAdjustment) {
        CalendarSystem(homeState.arabicCalendar, homeState.arabicCalendarLocale, homeState.hijriDateAdjustment)
    }
    val solarSystem = remember(homeState.calendar, homeState.locale) {
        CalendarSystem(homeState.calendar, homeState.locale)
    }

    // زر التبديل بين وضع الهجري كأساس أو الميلادي كأساس
    var isHijriPrimary by rememberSaveable { mutableStateOf(!homeState.swapHomeCalendars) }

    val primarySystem = if (isHijriPrimary) lunarSystem else solarSystem
    val secondarySystem = if (isHijriPrimary) solarSystem else lunarSystem

    val today = remember { Clock.System.now().toLocalDate() }
    val selectedDate = homeState.viewingInstant.toLocalDate()

    // الشهر المعروض حالياً في التقويم
    var shownEpochDay by rememberSaveable { mutableLongStateOf(selectedDate.toEpochDays()) }
    val shownDate = LocalDate.fromEpochDays(shownEpochDay)

    val month = remember(shownDate, primarySystem, homeState.numberingSystem) {
        calendarMonth(shownDate, primarySystem, homeState.numberingSystem)
    }

    // مزامنة الشهر المعروض عند اختيار يوم جديد من الخارج
    LaunchedEffect(selectedDate) {
        if (month.days.none { it.date == selectedDate }) {
            shownEpochDay = selectedDate.toEpochDays()
        }
    }

    // حالة تمدد التقويم للشهر الكامل أو انكماشه للأسبوع المحدد فقط
    var isCalendarExpanded by rememberSaveable { mutableStateOf(true) }
    val detailsScrollState = rememberScrollState()

    // رصد اتجاه سحب المستخدم (أعلى/أسفل) للتحكم في انكماش وتمدد التقويم بسلاسة ومرونة فائقة
    val collapseScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val dy = available.y
                    if ((dy < 0 && accumulatedDelta > 0) || (dy > 0 && accumulatedDelta < 0)) {
                        accumulatedDelta = 0f
                    }
                    accumulatedDelta += dy

                    if (accumulatedDelta < -20f && isCalendarExpanded) {
                        // المستخدم يسحب للأعلى -> انكماش التقويم للأسبوع الحالي لمنح مساحة كاملة للتفاصيل والصلوات
                        isCalendarExpanded = false
                        accumulatedDelta = 0f
                    } else if (accumulatedDelta > 20f && !isCalendarExpanded && detailsScrollState.value == 0) {
                        // المستخدم يسحب للأسفل من أعلى التفاصيل -> تمدد التقويم للشهر كاملاً
                        isCalendarExpanded = true
                        accumulatedDelta = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    // ألوان الشاشة المتناسقة مع السمات
    val isMidnight = homeState.themeColor == ThemeColor.MidnightBlue
    val isDark = homeState.themeColor == ThemeColor.Dark

    val bgColor = when {
        isMidnight -> Color(0xFF0A1128)
        isDark -> Color(0xFF000000)
        else -> MaterialTheme.colorScheme.background
    }

    val surfaceColor = when {
        isMidnight -> Color(0xFF111D3B)
        isDark -> Color(0xFF141414)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isMidnight -> Color(0xFF233660)
        isDark -> Color(0xFF262626)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val accentColor = when {
        isMidnight -> Color(0xFFFFD166)
        isDark -> Color(0xFF76D5D8)
        else -> Color(0xFF00585A)
    }

    val textPrimary = when {
        isMidnight || isDark -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val textMuted = when {
        isMidnight -> Color(0xFF94A3B8)
        isDark -> Color(0xFF888888)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. الشريط العلوي (رجوع + عنوان + زر اليوم)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back_button),
                        tint = textPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.dual_calendar_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }

            // زر اليوم للانتقال السريع لليوم الحالي
            Surface(
                onClick = {
                    onAction(HomeUiAction.OnShowTodayClick)
                    shownEpochDay = today.toEpochDays()
                },
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎯", fontSize = 11.sp)
                    Text(
                        text = stringResource(R.string.today),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        // 2. كبسولة التبديل بين التقويم الهجري والميلادي (Segmented Switcher)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceColor)
                .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // خيار الهجري
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isHijriPrimary) accentColor else Color.Transparent)
                    .clickable { isHijriPrimary = true }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🌙", fontSize = 13.sp)
                    Text(
                        text = stringResource(R.string.calendar_mode_hijri),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHijriPrimary) (if (isMidnight) Color(0xFF0A1128) else Color.White) else textMuted
                    )
                }
            }

            // خيار الميلادي
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (!isHijriPrimary) accentColor else Color.Transparent)
                    .clickable { isHijriPrimary = false }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("☀️", fontSize = 13.sp)
                    Text(
                        text = stringResource(R.string.calendar_mode_gregorian),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isHijriPrimary) (if (isMidnight) Color(0xFF0A1128) else Color.White) else textMuted
                    )
                }
            }
        }

        // 3. شريط التنقل بين الشهور (الشهر الأساسي والثانوي + أسهم التنقل)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    shownEpochDay = addCalendarMonths(shownDate, -1, primarySystem).toEpochDays()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.prev_month),
                    tint = textPrimary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${month.monthLabel} ${month.yearLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
                val secondarySpan = remember(shownDate, primarySystem, secondarySystem, homeState.numberingSystem) {
                    try {
                        formatDateSpan(calendarMonthSpan(shownDate, primarySystem), secondarySystem, DateFormat.YEAR_MONTH, homeState.numberingSystem)
                    } catch (e: Exception) {
                        ""
                    }
                }
                if (secondarySpan.isNotEmpty()) {
                    Text(
                        text = secondarySpan,
                        style = MaterialTheme.typography.labelSmall,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = {
                    shownEpochDay = addCalendarMonths(shownDate, 1, primarySystem).toEpochDays()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = stringResource(R.string.next_month),
                    tint = textPrimary
                )
            }
        }

        // 4. أسماء أيام الأسبوع
        val weekdays = remember(homeState.firstDayOfWeek) {
            weekdaysFrom(homeState.firstDayOfWeek)
        }
        val weekdayNames = remember(homeState.locale) {
            try {
                val symbols = DateFormatSymbols(ULocale(homeState.locale)).getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.SHORT)
                DayOfWeek.entries.associateWith { day ->
                    val icuIndex = when (day) {
                        DayOfWeek.SUNDAY -> 1
                        DayOfWeek.MONDAY -> 2
                        DayOfWeek.TUESDAY -> 3
                        DayOfWeek.WEDNESDAY -> 4
                        DayOfWeek.THURSDAY -> 5
                        DayOfWeek.FRIDAY -> 6
                        DayOfWeek.SATURDAY -> 7
                    }
                    symbols[icuIndex]
                }
            } catch (e: Exception) {
                narrowWeekdayNames(homeState.locale)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEach { dayOfWeek ->
                val isFriday = dayOfWeek == DayOfWeek.FRIDAY
                Text(
                    text = weekdayNames[dayOfWeek] ?: dayOfWeek.name.take(2),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isFriday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isFriday) Color(0xFF10B981) else textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. شبكة أيام الشهر (تدعم الانكماش للأسبوع الحالي عند السحب لأعلى والتمدد الكامل)
        val leadingBlanks = weekdays.indexOf(month.days.first().date.dayOfWeek).coerceAtLeast(0)
        val totalCells = leadingBlanks + month.days.size
        val weeks = (totalCells + 6) / 7

        // تحديد الأسبوع الذي يحتوي على اليوم المختار لعرضه وحده عند الانكماش
        val selectedIndexInDays = month.days.indexOfFirst { it.date == selectedDate }.coerceAtLeast(0)
        val selectedCellIndex = leadingBlanks + selectedIndexInDays
        val selectedWeekIndex = (selectedCellIndex / 7).coerceIn(0 until weeks)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            for (weekIndex in 0 until weeks) {
                val isWeekVisible = isCalendarExpanded || weekIndex == selectedWeekIndex
                AnimatedVisibility(
                    visible = isWeekVisible,
                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                    exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (colIndex in 0 until 7) {
                            val cellIndex = weekIndex * 7 + colIndex
                            val dayIndex = cellIndex - leadingBlanks
                            if (dayIndex in month.days.indices) {
                                val day = month.days[dayIndex]
                                val isDayToday = day.date == today
                                val isSelected = day.date == selectedDate
                                val isFriday = day.date.dayOfWeek == DayOfWeek.FRIDAY

                                val dayEvents = remember(day.date, lunarSystem) {
                                    IslamicCalendarEvents.getEventsForDate(day.date, lunarSystem)
                                }
                                val hasOccasion = dayEvents.any { it.type == IslamicEventType.Occasion }
                                val isWhiteDay = dayEvents.any { it.type == IslamicEventType.WhiteDay }
                                val isFastingDay = dayEvents.any {
                                    it.type == IslamicEventType.FastingFard ||
                                        it.type == IslamicEventType.FastingSunnah
                                }

                                val secondaryLabel = remember(day.date, secondarySystem, homeState.numberingSystem) {
                                    formatDate(day.date, secondarySystem, DateFormat.DAY, homeState.numberingSystem)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.05f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> accentColor
                                                isDayToday -> accentColor.copy(alpha = 0.18f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            when {
                                                isSelected -> Modifier
                                                isDayToday -> Modifier.border(1.5.dp, accentColor, RoundedCornerShape(10.dp))
                                                else -> Modifier
                                            }
                                        )
                                        .clickable {
                                            onAction(HomeUiAction.OnDatePicked(day.date, if (isHijriPrimary) 1 else 0))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.label,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected || isDayToday) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = when {
                                                isSelected -> if (isMidnight) Color(0xFF0A1128) else Color.White
                                                isFriday -> Color(0xFF10B981)
                                                else -> textPrimary
                                            },
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = secondaryLabel + if (isHijriPrimary) " م" else " هـ",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                isSelected -> if (isMidnight) Color(0xFF0A1128).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
                                                else -> textMuted
                                            },
                                            lineHeight = 10.sp
                                        )
                                        if (!isSelected && (hasOccasion || isWhiteDay || isFastingDay)) {
                                            Row(
                                                modifier = Modifier.padding(top = 1.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (hasOccasion) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFEAB308))
                                                    )
                                                }
                                                if (isWhiteDay) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFF59E0B))
                                                    )
                                                }
                                                if (isFastingDay && !isWhiteDay) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF10B981))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 6. مقبض الفصل التفاعلي (يدعم النقر والسحب لأعلى/أسفل للانكماش والتمدد)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isCalendarExpanded = !isCalendarExpanded }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -12f && isCalendarExpanded) {
                            isCalendarExpanded = false
                        } else if (dragAmount > 12f && !isCalendarExpanded) {
                            isCalendarExpanded = true
                        }
                    }
                }
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(borderColor)
                )
                Text(
                    text = if (isCalendarExpanded) "▲" else "▼",
                    fontSize = 9.sp,
                    color = textMuted.copy(alpha = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(borderColor)
                )
            }
        }

        // 7. النصف السفلي: تفاصيل اليوم المختار ومواقيت الصلاة والحكمة (تحت بعض)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(collapseScrollConnection)
                .verticalScroll(detailsScrollState)
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // كارت ملخص اليوم المختار
            val selectedWeekday = formatDate(selectedDate, solarSystem, DateFormat.WEEKDAY, homeState.numberingSystem)
            val selectedGregorian = formatDate(selectedDate, solarSystem, DateFormat.YEAR_MONTH_DAY, homeState.numberingSystem)
            val selectedHijri = formatDate(selectedDate, lunarSystem, DateFormat.YEAR_MONTH_DAY, homeState.numberingSystem)

            val selectedEvents = remember(selectedDate, lunarSystem) {
                IslamicCalendarEvents.getEventsForDate(selectedDate, lunarSystem)
            }
            val isSelectedToday = selectedDate == today
            val isSelectedFriday = selectedDate.dayOfWeek == DayOfWeek.FRIDAY

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "$selectedWeekday، $selectedGregorian",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = stringResource(R.string.cal_corresponding_date, selectedHijri),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = textMuted
                            )
                        }

                        if (isSelectedToday) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = stringResource(R.string.selected_day_current),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (selectedEvents.isNotEmpty() || isSelectedFriday) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSelectedFriday) {
                                EventBadgeChip(badge = "🕌", title = stringResource(R.string.selected_day_friday), color = Color(0xFF10B981))
                            }
                            selectedEvents.forEach { ev ->
                                val chipColor = when (ev.type) {
                                    IslamicEventType.Occasion -> Color(0xFFF59E0B)
                                    IslamicEventType.WhiteDay -> Color(0xFFD97706)
                                    IslamicEventType.FastingFard, IslamicEventType.FastingSunnah -> Color(0xFF10B981)
                                    IslamicEventType.FastingForbidden -> Color(0xFFEF4444)
                                }
                                EventBadgeChip(badge = ev.badge, title = ev.title, color = chipColor)
                            }
                        }
                    }

                    val mainEventWithDesc = selectedEvents.firstOrNull { !it.description.isNullOrBlank() }
                    if (mainEventWithDesc?.description != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mainEventWithDesc.badge, fontSize = 13.sp)
                                Text(
                                    text = mainEventWithDesc.description,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة مناسبات وأيام صيام الشهر المعروض
            MonthlyOccasionsCard(
                monthDays = month.days,
                lunarSystem = lunarSystem,
                primarySystem = primarySystem,
                numberingSystem = homeState.numberingSystem,
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textMuted = textMuted,
                accentColor = accentColor,
                onDateClick = { date ->
                    onAction(HomeUiAction.OnDatePicked(date, if (isHijriPrimary) 1 else 0))
                }
            )

            // شبكة مواقيت الصلاة لليوم المختار
            Text(
                text = stringResource(R.string.prayers_for_day),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )

            val mainPrayers = listOf(
                Prayer.Fajr, Prayer.Sunrise, Prayer.Dhuhr,
                Prayer.Asr, Prayer.Maghrib, Prayer.Isha
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mainPrayers.chunked(3).forEach { rowPrayers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowPrayers.forEach { prayer ->
                            val prayerInstant = homeState.shariaTimes?.forPrayer(prayer)
                            val formattedTime = if (prayerInstant != null) {
                                formatInstant(
                                    instant = prayerInstant,
                                    locale = homeState.locale,
                                    calendar = "gregorian",
                                    skeleton = if (homeState.is24Hour) "HH:mm" else "h:mm a",
                                    numberingSystem = homeState.numberingSystem
                                )
                            } else "--:--"

                            val isNextPrayer = selectedDate == today && homeState.nextShariaTime?.prayer == prayer

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isNextPrayer) accentColor else surfaceColor,
                                border = BorderStroke(1.dp, if (isNextPrayer) accentColor else borderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = prayer.i18n(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isNextPrayer) (if (isMidnight) Color(0xFF0A1128) else Color.White) else textMuted
                                    )
                                    Text(
                                        text = formattedTime,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isNextPrayer) (if (isMidnight) Color(0xFF0A1128) else Color.White) else textPrimary,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // كارت الحكمة لليوم المختار
            val wisdom = remember(homeState.viewingInstant) {
                NateegaWisdomProvider.getWisdomForDate(homeState.viewingInstant)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💡", fontSize = 16.sp)
                    Text(
                        text = wisdom,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun EventBadgeChip(
    badge: String,
    title: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(badge, fontSize = 11.sp)
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun MonthlyOccasionsCard(
    monthDays: List<com.github.meypod.al_azan.core.domain.util.CalendarMonthDay>,
    lunarSystem: CalendarSystem,
    primarySystem: CalendarSystem,
    numberingSystem: com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem,
    surfaceColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textMuted: Color,
    accentColor: Color,
    onDateClick: (LocalDate) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val monthEvents = remember(monthDays, lunarSystem) {
        monthDays.mapNotNull { day ->
            val evs = IslamicCalendarEvents.getEventsForDate(day.date, lunarSystem)
            // Keep major occasions, White Days, Ramadan/forbidden days, and special seasonal fasting;
            // exclude weekly Monday & Thursday fasting so the list stays focused and uncluttered.
            val highlighted = evs.filter {
                it.type == IslamicEventType.Occasion ||
                    it.type == IslamicEventType.WhiteDay ||
                    it.type == IslamicEventType.FastingForbidden ||
                    (it.type == IslamicEventType.FastingSunnah &&
                        !it.title.contains("الإثنين") &&
                        !it.title.contains("الخميس"))
            }
            if (highlighted.isNotEmpty()) day to highlighted else null
        }
    }

    if (monthEvents.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🗓️", fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.cal_monthly_occasions_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                    )
                }
                Text(
                    text = if (isExpanded) {
                        stringResource(R.string.cal_hide_list)
                    } else {
                        stringResource(R.string.cal_show_list, monthEvents.size)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    monthEvents.forEach { (day, evs) ->
                        val localizedDayDate = remember(day.date, primarySystem, numberingSystem) {
                            formatDate(
                                date = day.date,
                                system = primarySystem,
                                skeleton = "EEEEdMMMM",
                                numberingSystem = numberingSystem,
                            )
                        }
                        Surface(
                            onClick = { onDateClick(day.date) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    val firstBadge = evs.firstOrNull()?.badge ?: "🌙"
                                    Text(firstBadge, fontSize = 13.sp)
                                    Column {
                                        Text(
                                            text = evs.joinToString(" • ") { it.title },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = localizedDayDate,
                                            fontSize = 10.sp,
                                            color = textMuted,
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.cal_select_day),
                                    fontSize = 10.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isHijriWhiteDay(date: LocalDate, lunarSystem: CalendarSystem): Boolean {
    return try {
        val position = calendarDayOf(date, lunarSystem)
        position.day in 13..15
    } catch (e: Exception) {
        false
    }
}

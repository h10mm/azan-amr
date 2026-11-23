package com.github.meypod.al_azan.main.monthly

import android.icu.text.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.resolvedFirstDayOfWeek
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.DarkOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.DarkTertiary
import com.github.meypod.al_azan.core.presentation.DarkTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightTertiaryContainer
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.calendar.CalendarDatePickerDialog
import com.github.meypod.al_azan.core.presentation.components.calendar.CalendarPickerPrecision
import com.github.meypod.al_azan.core.presentation.components.calendar.rememberUserPickerCalendars
import com.github.meypod.al_azan.core.presentation.util.drawHorizontalScrollbar
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.dropShadow2
import kotlinx.datetime.LocalDate
import kotlin.math.abs

private val ROW_END_INSET = 4.dp

@Composable
fun MonthlyViewScreen(
    uiState: MonthlyViewUiState,
    onAction: (MonthlyViewUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var triggerScrollToToday by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var topBarVisible by remember { mutableStateOf(true) }

    // رصد اتجاه سحب المستخدم الحقيقي فقط (UserInput) لمنع أي حلقة ارتداد عند انكماش الشريط العلوي
    val collapseScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val dy = available.y
                    // عكس الاتجاه يصفر العداد التراكمي
                    if ((dy < 0 && accumulatedDelta > 0) || (dy > 0 && accumulatedDelta < 0)) {
                        accumulatedDelta = 0f
                    }
                    accumulatedDelta += dy

                    if (accumulatedDelta < -28f && topBarVisible) {
                        // المستخدم يسحب للأعلى لقراءة الأيام الأسفل -> إخفاء الشريط العلوي وبقاء شريط الصلوات
                        topBarVisible = false
                        accumulatedDelta = 0f
                    } else if (accumulatedDelta > 28f && !topBarVisible) {
                        // المستخدم يسحب للأسفل للعودة للأعلى -> إظهار الشريط العلوي
                        topBarVisible = true
                        accumulatedDelta = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    // إعداد نص الشهر المحدد بدون أيقونة التقويم
    val pickerCalendars = rememberUserPickerCalendars(uiState.settings, uiState.hijriDateAdjustment)
    val calendarIndex = uiState.calendarMode.pickerIndex.coerceIn(pickerCalendars.indices)
    val currentCalendar = pickerCalendars.getOrNull(calendarIndex) ?: pickerCalendars.firstOrNull()
    val formattedMonth = remember(uiState.anchorDate, currentCalendar, uiState.settings.numberingSystem) {
        uiState.anchorDate?.let { date ->
            if (currentCalendar != null) {
                formatDate(date, currentCalendar.system, DateFormat.YEAR_MONTH, uiState.settings.numberingSystem)
            } else {
                ""
            }
        } ?: ""
    }

    // نافذة اختيار الشهر عند النقر على اسم الشهر
    if (showDatePicker && uiState.anchorDate != null) {
        CalendarDatePickerDialog(
            initial = uiState.anchorDate,
            onConfirm = { picked, index ->
                showDatePicker = false
                onAction(MonthlyViewUiAction.OnDatePicked(picked, monthlyCalendarModeAt(index)))
            },
            onDismiss = { showDatePicker = false },
            calendars = pickerCalendars,
            weekLocale = uiState.settings.selectedLocale,
            numberingSystem = uiState.settings.numberingSystem,
            firstDayOfWeek = uiState.settings.resolvedFirstDayOfWeek(),
            initialCalendarIndex = calendarIndex,
            precision = CalendarPickerPrecision.Month,
        )
    }

    ScreenScaffold(
        title = "",
        onBackClick = {},
        showTopBar = false,
        navigationIcon = {},
        modifier = modifier,
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier.graphicsLayer { clip = false },
                visible = !uiState.isCurrentMonth,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 150),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(durationMillis = 200),
                ),
            ) {
                val buttonShape = MaterialTheme.shapes.extraLarge
                val dark = uiState.themeColor.isDark()
                val containerColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkTertiaryContainer else LightTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
                val contentColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkOnTertiaryContainer else LightOnTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        topBarVisible = true
                        onAction(MonthlyViewUiAction.OnShowThisMonthClick)
                    },
                    shape = buttonShape,
                    modifier = Modifier
                        .widthIn(min = 160.dp)
                        .dropShadow2(buttonShape),
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    Text(
                        stringResource(R.string.show_this_month),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        scrollable = false,
        contentPadding = PaddingValues(),
        verticalArrangement = Arrangement.Top,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(collapseScrollConnection),
        ) {
            // شريط علوي فائق النحافة ينكمش تلقائياً مع السحب
            AnimatedVisibility(
                visible = topBarVisible,
                enter = expandVertically(animationSpec = tween(durationMillis = 180)) + fadeIn(animationSpec = tween(durationMillis = 180)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 180)) + fadeOut(animationSpec = tween(durationMillis = 180)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // اسم الشاشة
                    Text(
                        text = stringResource(R.string.monthly_view_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    // أزرار التنقل مع اسم الشهر فقط (بدون أيقونة التقويم)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(
                            onClick = { onAction(MonthlyViewUiAction.OnPrevMonthClick) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.prev_month),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Text(
                            text = formattedMonth,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )

                        IconButton(
                            onClick = { onAction(MonthlyViewUiAction.OnNextMonthClick) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = stringResource(R.string.next_month),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // زر القائمة المنسدلة للخيارات والتكبير (⋮)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "خيارات العرض",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.fit_to_screen),
                                        fontWeight = if (zoomScale <= 1.02f) FontWeight.Bold else FontWeight.Normal,
                                        color = if (zoomScale <= 1.02f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    zoomScale = 1.0f
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.zoom_normal),
                                        fontWeight = if (zoomScale in 1.1f..1.25f) FontWeight.Bold else FontWeight.Normal,
                                        color = if (zoomScale in 1.1f..1.25f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    zoomScale = 1.15f
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.zoom_large),
                                        fontWeight = if (zoomScale >= 1.3f) FontWeight.Bold else FontWeight.Normal,
                                        color = if (zoomScale >= 1.3f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    zoomScale = 1.35f
                                    menuExpanded = false
                                },
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.scroll_to_today)) },
                                onClick = {
                                    triggerScrollToToday = true
                                    topBarVisible = true
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // جدول الصلوات مع شريط عناوين ثابت في القمة ودعم التكبير باليد
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.background,
            ) {
                MonthlyTable(
                    rows = uiState.rows,
                    themeColor = uiState.themeColor,
                    zoomScale = zoomScale,
                    onZoomScaleChange = { zoomScale = it },
                    lazyListState = lazyListState,
                    triggerScrollToToday = triggerScrollToToday,
                    onScrollToTodayHandled = { triggerScrollToToday = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MonthlyTable(
    rows: List<MonthlyDayRow>,
    themeColor: ThemeColor,
    zoomScale: Float,
    onZoomScaleChange: (Float) -> Unit,
    lazyListState: LazyListState,
    triggerScrollToToday: Boolean,
    onScrollToTodayHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hScroll = rememberScrollState()
    val currentZoomScale by rememberUpdatedState(zoomScale)
    val currentOnZoomChange by rememberUpdatedState(onZoomScaleChange)

    LaunchedEffect(rows, triggerScrollToToday) {
        val todayIndex = rows.indexOfFirst { it.isToday }
        if (todayIndex >= 0) {
            val targetIndex = (todayIndex - 2).coerceAtLeast(0)
            if (triggerScrollToToday) {
                lazyListState.animateScrollToItem(targetIndex)
                onScrollToTodayHandled()
            } else {
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }

    BoxWithConstraints(
        modifier
            .pointerInput(Unit) {
                // اعتراض حركة القرص بإصبعين في الطور الأولي (Initial Pass) قبل أن تسرقها عناصر التمرير الداخلي
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1.0f && !zoomChange.isNaN() && zoomChange > 0f) {
                                val updated = (currentZoomScale * zoomChange).coerceIn(0.85f, 1.6f)
                                if (abs(updated - currentZoomScale) > 0.01f) {
                                    currentOnZoomChange(updated)
                                }
                            }
                            // استهلاك الحدث عند وجود إصبعين فقط لمنع القفز أو التمرير العشوائي أثناء التكبير
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        val nextScale = if (currentZoomScale > 1.05f) 1.0f else 1.25f
                        currentOnZoomChange(nextScale)
                    },
                )
            }
            .drawHorizontalScrollbar(hScroll),
    ) {
        val rowWidth = maxWidth
        // مقاس الخلية الأساسي لملء الشاشة بالصلوات الـ 7، مضروباً بمقياس التكبير
        val fitCellWidth = (rowWidth - ROW_END_INSET) / 7
        val cellWidth = fitCellWidth * zoomScale

        Column(Modifier.fillMaxSize()) {
            // شريط الصلوات الثابت في القمة
            HeaderRow(cellWidth, rowWidth, ROW_END_INSET, hScroll, zoomScale)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp,
            )
            // قائمة الأيام باستخدام LazyColumn لضمان سرعة 60 إطار/ثانية أثناء التكبير والتمرير
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawVerticalScrollbar(lazyListState),
            ) {
                items(
                    items = rows,
                    key = { it.day },
                ) { row ->
                    DayRow(row, themeColor, cellWidth, rowWidth, ROW_END_INSET, hScroll, zoomScale)
                    HorizontalDivider(
                        color = if (row.isToday) todayAccentColor(themeColor).copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = if (row.isToday) 1.dp else 0.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    cellWidth: Dp,
    rowWidth: Dp,
    endInset: Dp,
    scrollState: ScrollState,
    zoomScale: Float,
) {
    val headerFontSize = (11.5f * zoomScale).sp
    val headerPaddingVertical = (8f * zoomScale).dp

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = headerPaddingVertical)
            .widthIn(min = rowWidth)
            .padding(end = endInset)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
            R.string.date_column,
            R.string.fajr,
            R.string.sunrise,
            R.string.dhuhr,
            R.string.asr,
            R.string.maghrib,
            R.string.isha,
        ).forEach { resId ->
            val colText = if (resId == R.string.sunrise) "الشروق" else stringResource(resId)
            Text(
                colText,
                modifier = Modifier.width(cellWidth),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = headerFontSize),
                fontWeight = FontWeight.Bold,
                color = if (resId == R.string.sunrise) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun todayAccentColor(themeColor: ThemeColor): Color =
    when (themeColor) {
        ThemeColor.ClassicLight -> MaterialTheme.colorScheme.primary
        ThemeColor.Dark, ThemeColor.MidnightBlue -> DarkTertiary
    }

@Composable
private fun DayRow(
    row: MonthlyDayRow,
    themeColor: ThemeColor,
    cellWidth: Dp,
    rowWidth: Dp,
    endInset: Dp,
    scrollState: ScrollState,
    zoomScale: Float,
) {
    val accent = todayAccentColor(themeColor)
    val todayLabel = stringResource(R.string.today)
    val dateColLabel = stringResource(R.string.date_column)
    val fajrLabel = stringResource(R.string.fajr)
    val sunriseLabel = stringResource(R.string.sunrise)
    val dhuhrLabel = stringResource(R.string.dhuhr)
    val asrLabel = stringResource(R.string.asr)
    val maghribLabel = stringResource(R.string.maghrib)
    val ishaLabel = stringResource(R.string.isha)

    val rowDescription = remember(row, todayLabel, dateColLabel, fajrLabel, sunriseLabel, dhuhrLabel, asrLabel, maghribLabel, ishaLabel) {
        val base = "$dateColLabel ${row.weekday} ${row.day}, $fajrLabel ${row.fajr}, $sunriseLabel ${row.sunrise}, $dhuhrLabel ${row.dhuhr}, $asrLabel ${row.asr}, $maghribLabel ${row.maghrib}, $ishaLabel ${row.isha}"
        if (row.isToday) "$todayLabel, $base" else base
    }

    val todayBg = when (themeColor) {
        ThemeColor.ClassicLight -> Color(0xFF007A7E).copy(alpha = 0.12f)
        ThemeColor.Dark -> Color(0xFF76D5D8).copy(alpha = 0.16f)
        ThemeColor.MidnightBlue -> Color(0xFFFFD166).copy(alpha = 0.16f)
    }

    val dayNumberFontSize = (12.5f * zoomScale).sp
    val weekdayFontSize = (9.0f * zoomScale).sp
    val prayerFontSize = (11.5f * zoomScale).sp
    val rowPaddingVertical = (5.5f * zoomScale).dp

    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (row.isToday) {
                    Modifier
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(todayBg)
                } else {
                    Modifier
                },
            )
            .horizontalScroll(scrollState)
            .padding(vertical = rowPaddingVertical)
            .widthIn(min = rowWidth)
            .padding(end = endInset)
            .semantics(mergeDescendants = true) { contentDescription = rowDescription },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // خلية اليوم والتاريخ معاً
        Column(
            modifier = Modifier.width(cellWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = row.day,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = dayNumberFontSize),
                fontWeight = if (row.isToday) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (row.isToday) accent else MaterialTheme.colorScheme.onSurface,
            )
            if (row.weekday.isNotBlank()) {
                Text(
                    text = row.weekday,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = weekdayFontSize),
                    color = if (row.isToday) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            if (row.eventBadge != null) {
                Text(
                    text = "${row.eventBadge} ${if (row.isWhiteDay) "البيض" else if (row.isFastingDay && (row.eventTitle?.contains("صيام") == true)) "صيام" else row.eventTitle?.take(6) ?: ""}".trim(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = (7.5f * zoomScale).sp),
                    fontWeight = FontWeight.Bold,
                    color = if (row.isWhiteDay) Color(0xFFD97706) else if (row.isFastingDay) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                )
            }
        }

        // أوقات الصلوات الست: الفجر، الشروق، الظهر، العصر، المغرب، العشاء
        val prayers = remember(row) {
            listOf(row.fajr, row.sunrise, row.dhuhr, row.asr, row.maghrib, row.isha)
        }
        prayers.forEachIndexed { i, timeText ->
            val isSunrise = i == 1
            Text(
                text = timeText,
                modifier = Modifier.width(cellWidth),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = prayerFontSize),
                fontWeight = if (row.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (row.isToday) {
                    accent
                } else if (isSunrise) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private fun previewRows() = (1..31).map {
    MonthlyDayRow(it.toString(), "السبت", "03:59", "05:15", "11:59", "15:20", "18:05", "19:30", isToday = it == 15)
}

@Preview(showBackground = true)
@Composable
private fun HeaderRowPreview() {
    AlAzanThemePreview {
        HeaderRow(64.dp, 320.dp, ROW_END_INSET, rememberScrollState(), 1.0f)
    }
}

@Preview(showBackground = true)
@Composable
private fun DayRowPreview() {
    AlAzanThemePreview {
        val scroll = rememberScrollState()
        Column {
            DayRow(
                MonthlyDayRow("15", "السبت", "03:59", "05:15", "13:00", "16:30", "20:15", "21:45", isToday = true),
                ThemeColor.Dark,
                64.dp,
                320.dp,
                ROW_END_INSET,
                scroll,
                1.0f,
            )
            DayRow(
                MonthlyDayRow("16", "الأحد", "04:00", "05:16", "13:00", "16:30", "20:16", "21:46"),
                ThemeColor.Dark,
                64.dp,
                320.dp,
                ROW_END_INSET,
                scroll,
                1.0f,
            )
        }
    }
}

@Preview(heightDp = 600)
@Composable
private fun MonthlyViewPreview() {
    AlAzanThemePreview {
        MonthlyViewScreen(
            uiState = MonthlyViewUiState(
                anchorDate = LocalDate(2026, 11, 3),
                rows = previewRows(),
            ),
            onAction = {},
        )
    }
}

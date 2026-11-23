package com.github.meypod.al_azan.main.quran

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.launch

private fun Int.toArabicDigits(): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return this.toString().map { if (it in '0'..'9') digits[it - '0'] else it }.joinToString("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahReaderScreen(
    surahNumber: Int = 18,
    onBackClick: () -> Unit = { NavigationController.navigateBack() },
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("quran_reader_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 1. Two Focused Themes: Warm Paper (ورق دافئ) vs Pure White (أبيض ناصع)
    var isWarmPaper by remember {
        mutableStateOf(prefs.getBoolean("is_warm_paper", true))
    }

    val bgColor = if (isWarmPaper) Color(0xFFFAF7EE) else Color(0xFFFFFFFF)
    val textColor = if (isWarmPaper) Color(0xFF262118) else Color(0xFF111827)
    val accentColor = if (isWarmPaper) Color(0xFFC59B27) else Color(0xFF00695C)
    val frameBorder = if (isWarmPaper) Color(0xFFDACFBA) else Color(0xFFE2E8F0)
    val surfaceBar = if (isWarmPaper) Color(0xFFF1EBDF) else Color(0xFFF8FAFC)

    // 2. Persistent Reading Bookmark (فاصل القراءة لحفظ موضع التوقف)
    val bookmarkKey = "bookmark_surah_$surahNumber"
    var bookmarkedAyah by remember {
        mutableIntStateOf(prefs.getInt(bookmarkKey, 0)) // 0 means no bookmark
    }

    // 3. Selection & Contextual Action Bar (تحديد الآية المباشر، النسخ، وحفظ الفاصل)
    var selectedAyahNum by remember { mutableIntStateOf(0) }

    // 4. Dedicated Surah & Font Preferences
    val surah = remember(surahNumber) { SurahLibrary.getSurah(surahNumber) }
    val amiriFont = remember { FontFamily(Font(R.font.amiri_regular)) }

    val initialFontSize = remember {
        try {
            prefs.getInt("font_size_sp", 24)
        } catch (e: Exception) {
            try {
                prefs.getFloat("font_size", 24f).toInt()
            } catch (e2: Exception) {
                24
            }
        }.coerceIn(16, 42)
    }
    var fontSizeSp by remember { mutableIntStateOf(initialFontSize) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    // Build the full continuous Uthmani text with interactive Ayah annotations & highlights (بدون أي إيموجي)
    val fullQuranText = remember(surah, amiriFont, fontSizeSp, isWarmPaper, bookmarkedAyah, selectedAyahNum) {
        buildAnnotatedString {
            surah.ayahs.forEach { ayah ->
                val isSelected = (ayah.numberInSurah == selectedAyahNum)
                val isBookmark = (ayah.numberInSurah == bookmarkedAyah)

                val ayahBgColor = when {
                    isSelected -> if (isWarmPaper) Color(0x66FFD54F) else Color(0x4480CBC4)
                    isBookmark -> if (isWarmPaper) Color(0x35D4AF37) else Color(0x2500897B)
                    else -> Color.Transparent
                }

                pushStringAnnotation(tag = "AYAH", annotation = "${ayah.numberInSurah}")

                withStyle(
                    SpanStyle(
                        fontFamily = amiriFont,
                        fontSize = fontSizeSp.sp,
                        color = when {
                            isSelected -> if (isWarmPaper) Color(0xFF795548) else Color(0xFF004D40)
                            isBookmark -> if (isWarmPaper) Color(0xFF8C4A00) else Color(0xFF004D40)
                            else -> textColor
                        },
                        background = ayahBgColor,
                        fontWeight = if (isSelected || isBookmark) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                    )
                ) {
                    append(ayah.text)
                }

                append(" ")
                withStyle(
                    SpanStyle(
                        fontFamily = amiriFont,
                        fontSize = (fontSizeSp * 0.75f).sp,
                        color = if (isSelected || isBookmark) Color(0xFFC59B27) else accentColor,
                        fontWeight = FontWeight.Bold,
                        background = ayahBgColor,
                    )
                ) {
                    append("﴿${ayah.numberInSurah.toArabicDigits()}﴾")
                }
                append(" ")

                pop()
            }
        }
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectedAyah = remember(selectedAyahNum, surah) {
        surah.ayahs.find { it.numberInSurah == selectedAyahNum }
    }

    // وظيفة الانتقال السلس للآية (العودة إلى فاصل القراءة)
    val scrollToAyah: (Int) -> Unit = { ayahNumber ->
        if (ayahNumber > 0) {
            selectedAyahNum = ayahNumber
            val layout = textLayoutResult
            if (layout != null) {
                val annotations = fullQuranText.getStringAnnotations("AYAH", 0, fullQuranText.length)
                val targetAnnotation = annotations.find { it.item.toIntOrNull() == ayahNumber }
                if (targetAnnotation != null) {
                    val lineIndex = layout.getLineForOffset(targetAnnotation.start)
                    val lineTop = layout.getLineTop(lineIndex)
                    val textItemIndex = if (bookmarkedAyah > 0) 3 else 2
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = textItemIndex,
                            scrollOffset = lineTop.toInt().coerceAtLeast(0)
                        )
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Sleek Compact Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Back Button + Surah Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back_button),
                            tint = textColor,
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = surah.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                    )
                }

                // Controls Row: Themes [📜 دافئ | ☀️ أبيض] + Zoom [A- | +A] + Jump to Bookmark + Bookmark Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Two Themes Directly Side-by-Side (ورق دافئ / أبيض ناصع)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceBar)
                            .border(1.dp, frameBorder, RoundedCornerShape(18.dp))
                            .padding(2.dp),
                    ) {
                        // Warm Paper Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isWarmPaper) Color(0xFFE8DFC8) else Color.Transparent)
                                .clickable {
                                    isWarmPaper = true
                                    prefs.edit().putBoolean("is_warm_paper", true).apply()
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.surah_paper_warm),
                                fontSize = 11.sp,
                                fontWeight = if (isWarmPaper) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isWarmPaper) Color(0xFF3E2723) else textColor.copy(alpha = 0.6f),
                            )
                        }

                        // Pure White Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!isWarmPaper) Color(0xFF00695C) else Color.Transparent)
                                .clickable {
                                    isWarmPaper = false
                                    prefs.edit().putBoolean("is_warm_paper", false).apply()
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.surah_paper_white),
                                fontSize = 11.sp,
                                fontWeight = if (!isWarmPaper) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (!isWarmPaper) Color.White else textColor.copy(alpha = 0.6f),
                            )
                        }
                    }

                    // Discrete Instant Zoom Buttons: [A- | 24 | A+] (Zero lag)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceBar)
                            .border(1.dp, frameBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                    ) {
                        IconButton(
                            onClick = {
                                if (fontSizeSp > 16) {
                                    fontSizeSp -= 2
                                    prefs.edit().putInt("font_size_sp", fontSizeSp).apply()
                                }
                            },
                            modifier = Modifier.size(28.dp),
                            enabled = fontSizeSp > 16,
                        ) {
                            Text(
                                text = "A-",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (fontSizeSp > 16) textColor else textColor.copy(alpha = 0.3f),
                            )
                        }

                        Text(
                            text = "$fontSizeSp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 3.dp),
                        )

                        IconButton(
                            onClick = {
                                if (fontSizeSp < 42) {
                                    fontSizeSp += 2
                                    prefs.edit().putInt("font_size_sp", fontSizeSp).apply()
                                }
                            },
                            modifier = Modifier.size(28.dp),
                            enabled = fontSizeSp < 42,
                        ) {
                            Text(
                                text = "A+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (fontSizeSp < 42) textColor else textColor.copy(alpha = 0.3f),
                            )
                        }
                    }

                    // Jump to Bookmark Button (زر العودة للفاصل السابق بجوار زر العلامة)
                    if (bookmarkedAyah > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isWarmPaper) Color(0xFFE8DFC8) else Color(0xFFE0F2F1),
                            border = BorderStroke(1.dp, Color(0xFFC59B27).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                scrollToAyah(bookmarkedAyah)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.surah_jumped_bookmark, bookmarkedAyah.toArabicDigits()),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = stringResource(R.string.surah_return_bookmark_cd),
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = stringResource(R.string.surah_ayah_short, bookmarkedAyah.toArabicDigits()),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                )
                            }
                        }
                    }

                    // Bookmark Status Icon in Top Bar
                    IconButton(
                        onClick = {
                            if (bookmarkedAyah > 0) {
                                scrollToAyah(bookmarkedAyah)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.surah_bookmark_at, bookmarkedAyah.toArabicDigits()),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.surah_bookmark_hint),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (bookmarkedAyah > 0) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
                            ),
                            contentDescription = stringResource(R.string.surah_return_bookmark_cd),
                            tint = if (bookmarkedAyah > 0) Color(0xFFC59B27) else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // 2. Quran Content with Smooth Quantized Pinch & Direct Ayah Tap
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomAccumulator *= zoom
                            if (zoomAccumulator > 1.25f) {
                                if (fontSizeSp < 42) {
                                    fontSizeSp += 2
                                    prefs.edit().putInt("font_size_sp", fontSizeSp).apply()
                                }
                                zoomAccumulator = 1f
                            } else if (zoomAccumulator < 0.8f) {
                                if (fontSizeSp > 16) {
                                    fontSizeSp -= 2
                                    prefs.edit().putInt("font_size_sp", fontSizeSp).apply()
                                }
                                zoomAccumulator = 1f
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 72.dp),
                ) {
                    // Active Bookmark Banner (شريط الفاصل المحفوظ أعلى الشاشة - نحيف ومدمج)
                    if (bookmarkedAyah > 0) {
                        item {
                            Spacer(Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isWarmPaper) Color(0xFFF1E6D0) else Color(0xFFE0F2F1))
                                    .border(1.dp, Color(0xFFC59B27).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        scrollToAyah(bookmarkedAyah)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_bookmark),
                                        contentDescription = null,
                                        tint = Color(0xFFC59B27),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.surah_bookmark_banner, bookmarkedAyah.toArabicDigits()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor,
                                    contentColor = Color.White,
                                    modifier = Modifier.clickable {
                                        scrollToAyah(bookmarkedAyah)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.surah_jumped_ayah, bookmarkedAyah.toArabicDigits()),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.surah_continue_here),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // Compact decorative frame header
                    item {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, frameBorder, RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(vertical = 5.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.surah_meta_ayahs, surah.revelationType, surah.ayahs.size.toArabicDigits()),
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = surah.name,
                                    fontFamily = amiriFont,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                )
                                Text(
                                    text = stringResource(R.string.surah_order_num, surah.number.toArabicDigits()),
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.7f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // Basmalah
                    item {
                        Text(
                            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                            fontFamily = amiriFont,
                            fontSize = (fontSizeSp * 1.05f).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                    }

                    // Full Continuous Ayahs with Direct Touch-to-Select
                    item {
                        Text(
                            text = fullQuranText,
                            textAlign = TextAlign.Justify,
                            lineHeight = (fontSizeSp * 2.05f).sp,
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(fullQuranText) {
                                    detectTapGestures { tapOffset ->
                                        textLayoutResult?.let { layout ->
                                            val offset = layout.getOffsetForPosition(tapOffset)
                                            val annotations = fullQuranText.getStringAnnotations(
                                                tag = "AYAH",
                                                start = offset,
                                                end = offset
                                            )
                                            val clickedAyahNum = annotations.firstOrNull()?.item?.toIntOrNull()
                                            if (clickedAyahNum != null) {
                                                selectedAyahNum = if (selectedAyahNum == clickedAyahNum) 0 else clickedAyahNum
                                            } else {
                                                selectedAyahNum = 0
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }

                // 3. Floating Contextual Action Bar on Ayah Selection (نسخ، حفظ فاصل، إلغاء)
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedAyah != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                ) {
                    selectedAyah?.let { ayah ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (isWarmPaper) Color(0xFF34281D) else Color(0xFF1E293B),
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, Color(0xFFC59B27)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Ayah Number Badge
                                Text(
                                    text = stringResource(R.string.surah_ayah_short, ayah.numberInSurah.toArabicDigits()),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F),
                                )

                                // Bookmark Button [ وقفت هنا / إلغاء الحفظ ]
                                val isCurrentlyBookmarked = (bookmarkedAyah == ayah.numberInSurah)
                                Button(
                                    onClick = {
                                        if (isCurrentlyBookmarked) {
                                            bookmarkedAyah = 0
                                            prefs.edit().remove(bookmarkKey).apply()
                                            Toast.makeText(context, context.getString(R.string.surah_bookmark_removed), Toast.LENGTH_SHORT).show()
                                        } else {
                                            bookmarkedAyah = ayah.numberInSurah
                                            prefs.edit().putInt(bookmarkKey, ayah.numberInSurah).apply()
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.surah_bookmark_saved, ayah.numberInSurah.toArabicDigits()),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrentlyBookmarked) Color(0xFFC59B27) else Color(0x33FFFFFF),
                                        contentColor = if (isCurrentlyBookmarked) Color(0xFF262118) else Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isCurrentlyBookmarked) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(if (isCurrentlyBookmarked) R.string.surah_remove_bookmark else R.string.surah_stopped_here),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // Return to previous saved stop point (الذهاب للمكان السابق دون الرجوع لأول الصفحة)
                                if (bookmarkedAyah > 0 && bookmarkedAyah != ayah.numberInSurah) {
                                    Button(
                                        onClick = {
                                            scrollToAyah(bookmarkedAyah)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.surah_jumped_bookmark, bookmarkedAyah.toArabicDigits()),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isWarmPaper) Color(0xFF4A3B2C) else Color(0xFF334155),
                                            contentColor = Color(0xFFFFD54F),
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward),
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color(0xFFFFD54F),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = stringResource(R.string.surah_prev_place, bookmarkedAyah.toArabicDigits()),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                // Copy Button [ 📋 نسخ ]
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val formattedAyah = "﴿${ayah.text}﴾ [سورة ${surah.name}: ${ayah.numberInSurah.toArabicDigits()}]"
                                        val clip = ClipData.newPlainText("آية من القرآن الكريم", formattedAyah)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.surah_copied_ayah, ayah.numberInSurah.toArabicDigits()),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isWarmPaper) Color(0xFFC59B27) else Color(0xFF00897B),
                                        contentColor = Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(34.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.content_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.surah_copy),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // Dismiss Button [ ✕ ]
                                IconButton(
                                    onClick = { selectedAyahNum = 0 },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_close_24),
                                        contentDescription = stringResource(R.string.close),
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Compact Bottom Status Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surfaceBar,
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(if (surah.number == 18) R.string.surah_kahf_virtue else R.string.surah_mulk_virtue),
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (bookmarkedAyah > 0) stringResource(R.string.surah_bookmark_at, bookmarkedAyah.toArabicDigits()) else stringResource(R.string.surah_footer_hint),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

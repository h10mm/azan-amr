package com.github.meypod.al_azan.main.azkar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.main.tasbih.TasbihHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AzkarReaderScreen(
    categoryId: String,
    initialDhikrId: String?,
    uiState: AzkarUiState,
    onToggleFavorite: (String) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onDecrementCount: (AzkarItem) -> Boolean,
    onResetDhikrCount: (AzkarItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isArabicLocale = remember(configuration) {
        configuration.locales[0]?.language == "ar"
    }
    val scope = rememberCoroutineScope()

    val category = remember(categoryId) { AzkarLibrary.getCategory(categoryId) }
    val items = category.items
    val initialPage = remember(categoryId, initialDhikrId) {
        if (initialDhikrId == null) 0
        else items.indexOfFirst { it.id == initialDhikrId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { items.size },
    )

    // Explicit BackHandler so Predictive Back / system back always exits cleanly
    BackHandler { onBackClick() }

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    val isFavorite = currentItem.id in uiState.favoriteIds
    val currentRemaining = uiState.remainingFor(currentItem)

    val completedItemsCount = items.count { uiState.remainingFor(it) == 0 }
    val categoryProgress by animateFloatAsState(
        targetValue = if (items.isNotEmpty()) {
            ((pagerState.currentPage + 1).toFloat() / items.size.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 250),
        label = "azkarCategoryProgress",
    )

    val handleCountTap: (AzkarItem, Int) -> Unit = { item, pageIndex ->
        val remainingBefore = uiState.remainingFor(item)
        if (remainingBefore > 0) {
            val justCompleted = onDecrementCount(item)
            TasbihHaptics.performFeedback(
                context = context,
                vibrationEnabled = true,
                soundEnabled = false,
                completedRound = justCompleted,
            )
            if (justCompleted && pageIndex < items.lastIndex) {
                scope.launch {
                    delay(280L)
                    if (pagerState.currentPage == pageIndex) {
                        pagerState.animateScrollToPage(pageIndex + 1)
                    }
                }
            }
        }
    }

    val copiedMessage = stringResource(R.string.azkar_copied_toast)
    val handleCopy: (AzkarItem) -> Unit = { item ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Dhikr", item.arabicText))
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                // Row 1: Back + Title + Order Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back_button),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(category.id.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(
                                R.string.azkar_dhikr_progress,
                                pagerState.currentPage + 1,
                                items.size,
                            ),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = { onToggleFavorite(currentItem.id) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
                            ),
                            contentDescription = stringResource(R.string.azkar_favorites_filter),
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Category Completion Progress Bar
                LinearProgressIndicator(
                    progress = { categoryProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                // Row 2: Compact Controls (A-, A+, Copy, Reset)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Font size controls (A- / A+)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompactControlPill(
                            text = "A-",
                            enabled = uiState.fontSizeSp > AzkarPreferences.MIN_FONT_SIZE_SP,
                            onClick = onDecreaseFontSize,
                        )
                        CompactControlPill(
                            text = "A+",
                            enabled = uiState.fontSizeSp < AzkarPreferences.MAX_FONT_SIZE_SP,
                            onClick = onIncreaseFontSize,
                        )
                    }

                    // Copy & Reset controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompactIconTextPill(
                            iconRes = R.drawable.content_copy,
                            text = stringResource(R.string.azkar_copy_dhikr),
                            onClick = { handleCopy(currentItem) },
                        )
                        CompactIconTextPill(
                            iconRes = R.drawable.ic_reset,
                            text = stringResource(R.string.azkar_reset_dhikr),
                            onClick = { onResetDhikrCount(currentItem) },
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Navigation & Interactive Counter Bar with full navigationBarsPadding
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Previous Button
                    val canGoPrev = pagerState.currentPage > 0
                    FilledTonalButton(
                        onClick = {
                            if (canGoPrev) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        enabled = canGoPrev,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.heightIn(min = 46.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.azkar_previous),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Central Interactive Count Button
                    val isDone = currentRemaining == 0
                    val countBg by animateColorAsState(
                        targetValue = if (isDone) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        label = "azkarCountBtnBg",
                    )
                    val countFg by animateColorAsState(
                        targetValue = if (isDone) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        label = "azkarCountBtnFg",
                    )

                    Button(
                        onClick = { handleCountTap(currentItem, pagerState.currentPage) },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = countBg,
                            contentColor = countFg,
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        if (isDone) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_check_24),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.azkar_completed),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text(
                                text = "${stringResource(R.string.azkar_remaining)}: $currentRemaining",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFeatureSettings = "tnum",
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    // Next Button
                    val canGoNext = pagerState.currentPage < items.lastIndex
                    FilledTonalButton(
                        onClick = {
                            if (canGoNext) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        enabled = canGoNext,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.heightIn(min = 46.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.azkar_next),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // HorizontalPager inset by 16.dp from screen edges so Predictive Back edge-swipes never conflict!
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { pageIndex ->
            val item = items[pageIndex]
            val remaining = uiState.remainingFor(item)
            val isCompleted = remaining == 0

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = if (isCompleted) 1.5.dp else 1.dp,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { handleCountTap(item, pageIndex) },
                    ),
            ) {
                // Vertically scrollable content inside card: cleanly separated from horizontal pager swipe,
                // and guarantees zero ellipsis truncation even at max font size (36.sp).
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Repeat target badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = if (item.repeatCount == 1) {
                                    stringResource(R.string.azkar_repeat_once)
                                } else {
                                    stringResource(R.string.azkar_repeat_count, item.repeatCount)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }

                        if (!isCompleted) {
                            Text(
                                text = stringResource(R.string.azkar_tap_to_count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }

                    // Original Arabic Dhikr Matn: ALWAYS RTL, Amiri Naskh font, generous lineHeight (1.75x)
                    // preventing tashkeel/diacritic overlap on Samsung, Xiaomi, Oppo, etc.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = item.arabicText,
                            style = TextStyle(
                                fontFamily = AzkarArabicFontFamily,
                                fontSize = uiState.fontSizeSp.sp,
                                lineHeight = (uiState.fontSizeSp * 1.75f).sp,
                                textAlign = TextAlign.Center,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                        )
                    }

                    // Translation / Meaning shown cleanly below Arabic text for non-Arabic app locales
                    if (!isArabicLocale && item.englishTranslation.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.azkar_translation_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = item.englishTranslation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Benefit / Virtue
                    if (item.benefit.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.azkar_benefit_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = item.benefit,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    // Hadith Source / Takrij
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "${stringResource(R.string.azkar_source_label)}:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Safe bottom spacing inside scrollable card
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CompactControlPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CompactIconTextPill(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

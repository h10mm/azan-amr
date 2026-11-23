package com.github.meypod.al_azan.main.azkar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.main.tasbih.TasbihSpeechController

internal val AzkarArabicFontFamily = FontFamily(
    Font(resId = R.font.amiri_regular, weight = FontWeight.Normal),
)

@Composable
fun AzkarIndexScreen(
    uiState: AzkarUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSetFavoritesOnly: (Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCategoryClick: (AzkarCategoryId) -> Unit,
    onDhikrClick: (AzkarCategoryId, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val trimmedQuery = uiState.searchQuery.trim()
    val normalizedQuery = remember(trimmedQuery) {
        TasbihSpeechController.normalizeArabic(trimmedQuery)
    }
    val lowerQuery = remember(trimmedQuery) { trimmedQuery.lowercase() }

    val matchingDhikrs = remember(trimmedQuery, uiState.showFavoritesOnly, uiState.favoriteIds) {
        val baseList = if (uiState.showFavoritesOnly) {
            AzkarLibrary.allItems.filter { it.id in uiState.favoriteIds }
        } else {
            AzkarLibrary.allItems
        }
        if (trimmedQuery.isEmpty()) {
            baseList
        } else {
            baseList.filter { item ->
                val catTitle = context.getString(item.category.titleRes)
                val normCat = TasbihSpeechController.normalizeArabic(catTitle)
                (normalizedQuery.isNotEmpty() && (
                    item.normalizedArabic.contains(normalizedQuery) ||
                        item.normalizedSource.contains(normalizedQuery) ||
                        normCat.contains(normalizedQuery)
                    )) ||
                    catTitle.lowercase().contains(lowerQuery) ||
                    item.englishTranslation.lowercase().contains(lowerQuery) ||
                    item.source.lowercase().contains(lowerQuery) ||
                    item.benefit.contains(trimmedQuery)
            }
        }
    }

    val isShowingDhikrList = trimmedQuery.isNotEmpty() || uiState.showFavoritesOnly

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                        text = stringResource(R.string.azkar_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.azkar_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            // Instant Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = stringResource(R.string.azkar_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_close_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Filter Chips (All / Favorites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !uiState.showFavoritesOnly,
                    onClick = { onSetFavoritesOnly(false) },
                    label = {
                        Text(
                            text = stringResource(R.string.azkar_all_filter),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                FilterChip(
                    selected = uiState.showFavoritesOnly,
                    onClick = { onSetFavoritesOnly(true) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(
                                if (uiState.showFavoritesOnly) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = {
                        val favCount = uiState.favoriteIds.size
                        val label = if (favCount > 0) {
                            "${stringResource(R.string.azkar_favorites_filter)} ($favCount)"
                        } else {
                            stringResource(R.string.azkar_favorites_filter)
                        }
                        Text(
                            text = label,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isShowingDhikrList) {
                    if (matchingDhikrs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (uiState.showFavoritesOnly && trimmedQuery.isEmpty()) {
                                        stringResource(R.string.azkar_no_favorites)
                                    } else {
                                        stringResource(R.string.azkar_no_results)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(matchingDhikrs, key = { it.id }) { dhikr ->
                            AzkarSearchResultCard(
                                item = dhikr,
                                isFavorite = dhikr.id in uiState.favoriteIds,
                                onToggleFavorite = { onToggleFavorite(dhikr.id) },
                                onClick = { onDhikrClick(dhikr.category, dhikr.id) },
                            )
                        }
                    }
                } else {
                    // Section 1: Quick Daily Azkar 2x2 Grid
                    item {
                        Text(
                            text = stringResource(R.string.azkar_daily_shortcuts),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }

                    val dailyRows = AzkarLibrary.dailyShortcutCategories.chunked(2)
                    items(dailyRows) { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowCats.forEach { category ->
                                DailyShortcutCard(
                                    category = category,
                                    onClick = { onCategoryClick(category.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowCats.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Section 2: All Categories
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.azkar_all_categories),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }

                    items(AzkarLibrary.categories, key = { it.id.id }) { category ->
                        AzkarCategoryRowCard(
                            category = category,
                            onClick = { onCategoryClick(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyShortcutCard(
    category: AzkarCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(category.id.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = stringResource(category.id.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.azkar_dhikr_count_format, category.items.size),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AzkarCategoryRowCard(
    category: AzkarCategory,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(category.id.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(category.id.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.azkar_dhikr_count_format, category.items.size),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.baseline_navigate_next_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AzkarSearchResultCard(
    item: AzkarItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = stringResource(item.category.titleRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.repeatCount == 1) {
                            stringResource(R.string.azkar_repeat_once)
                        } else {
                            stringResource(R.string.azkar_repeat_count, item.repeatCount)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border,
                            ),
                            contentDescription = stringResource(R.string.azkar_favorites_filter),
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Always render original Arabic text in RTL with Amiri font & comfortable line height
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = item.arabicText,
                    style = TextStyle(
                        fontFamily = AzkarArabicFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = item.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

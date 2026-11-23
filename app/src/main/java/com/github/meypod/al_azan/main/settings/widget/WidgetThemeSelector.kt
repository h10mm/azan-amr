package com.github.meypod.al_azan.main.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.core.domain.model.widget.DialThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.MosqueArchThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.NateegaThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SlimBarThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SmartPillThemeMode
import com.github.meypod.al_azan.core.domain.model.widget.SquareThemeMode

data class WidgetThemePillOption<T>(
    val value: T,
    val label: String,
    val previewColor: Color? = null,
    val isAuto: Boolean = false,
    val isTransparent: Boolean = false,
)

/**
 * Unified 1-Tap Segmented Pill Bar matching InterfaceSettingsScreen's theme selector design.
 */
@Composable
fun <T> UnifiedWidgetThemeSelector(
    title: String = "السمة والمظهر",
    options: List<WidgetThemePillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Primary row: up to 4 items (Auto / Light / Dark / Midnight Blue)
        val firstRow = if (options.size <= 4) options else options.take(4)
        val secondRow = if (options.size > 4) options.drop(4) else emptyList()

        ThemePillRow(
            items = firstRow,
            selected = selected,
            onSelect = onSelect,
        )

        if (secondRow.isNotEmpty()) {
            ThemePillRow(
                items = secondRow,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun <T> ThemePillRow(
    items: List<WidgetThemePillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp),
            )
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { option ->
            val isSelected = selected == option.value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onSelect(option.value) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val swatchModifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .let { mod ->
                            when {
                                option.isAuto -> mod.background(
                                    Brush.horizontalGradient(
                                        listOf(Color.White, Color(0xFF070D1E))
                                    )
                                )
                                option.isTransparent -> mod.background(
                                    Color.Transparent
                                )
                                option.previewColor != null -> mod.background(option.previewColor)
                                else -> mod.background(MaterialTheme.colorScheme.primary)
                            }
                        }
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                    Box(modifier = swatchModifier)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = option.label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun NateegaThemePillSelector(
    selected: NateegaThemeMode,
    onSelect: (NateegaThemeMode) -> Unit,
    title: String = "السمة والمظهر",
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = title,
        options = listOf(
            WidgetThemePillOption(NateegaThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(NateegaThemeMode.ClassicPaper, "فاتح", Color.White),
            WidgetThemePillOption(NateegaThemeMode.Amoled, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(NateegaThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun DialThemePillSelector(
    selected: DialThemeMode,
    onSelect: (DialThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = "السمة والمظهر",
        options = listOf(
            WidgetThemePillOption(DialThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(DialThemeMode.ClassicWhite, "فاتح", Color.White),
            WidgetThemePillOption(DialThemeMode.AmoledDark, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(DialThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
            WidgetThemePillOption(DialThemeMode.Transparent, "شفاف (بدون خلفية)", isTransparent = true),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun SquareThemePillSelector(
    selected: SquareThemeMode,
    onSelect: (SquareThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = "السمة والمظهر",
        options = listOf(
            WidgetThemePillOption(SquareThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(SquareThemeMode.ClassicWhite, "فاتح", Color.White),
            WidgetThemePillOption(SquareThemeMode.AmoledDark, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(SquareThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
            WidgetThemePillOption(SquareThemeMode.Transparent, "شفاف (بدون خلفية)", isTransparent = true),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun SlimBarThemePillSelector(
    selected: SlimBarThemeMode,
    onSelect: (SlimBarThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = "السمة والمظهر",
        options = listOf(
            WidgetThemePillOption(SlimBarThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(SlimBarThemeMode.ClassicWhite, "فاتح", Color.White),
            WidgetThemePillOption(SlimBarThemeMode.AmoledDark, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(SlimBarThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
            WidgetThemePillOption(SlimBarThemeMode.Transparent, "شفاف (بدون خلفية)", isTransparent = true),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun MosqueArchThemePillSelector(
    selected: MosqueArchThemeMode,
    onSelect: (MosqueArchThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = "السمة والمظهر",
        options = listOf(
            WidgetThemePillOption(MosqueArchThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(MosqueArchThemeMode.ClassicWhite, "فاتح", Color.White),
            WidgetThemePillOption(MosqueArchThemeMode.AmoledDark, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(MosqueArchThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
            WidgetThemePillOption(MosqueArchThemeMode.Transparent, "شفاف (بدون خلفية)", isTransparent = true),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun SmartPillThemePillSelector(
    selected: SmartPillThemeMode,
    onSelect: (SmartPillThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    UnifiedWidgetThemeSelector(
        title = "السمة والمظهر",
        options = listOf(
            WidgetThemePillOption(SmartPillThemeMode.MatchAppTheme, "تلقائي", isAuto = true),
            WidgetThemePillOption(SmartPillThemeMode.ClassicWhite, "فاتح", Color.White),
            WidgetThemePillOption(SmartPillThemeMode.AmoledDark, "داكن", Color(0xFF141414)),
            WidgetThemePillOption(SmartPillThemeMode.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
            WidgetThemePillOption(SmartPillThemeMode.Transparent, "شفاف (بدون خلفية)", isTransparent = true),
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
    )
}

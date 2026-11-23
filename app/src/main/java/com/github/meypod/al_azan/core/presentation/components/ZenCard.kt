package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.github.meypod.al_azan.R

/**
 * Wraps an LTR technical term or numeric percentage (e.g. "Exact Alarms", "Doze Mode", "100%")
 * using Android's BidiFormatter so parentheses and punctuation never overlap or invert inside RTL Arabic text.
 */
fun bidiLtr(text: String): String =
    BidiFormatter.getInstance(true).unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)

/**
 * Material Design 3 Standard Color Tokens backed exclusively by [MaterialTheme.colorScheme].
 * Automatically adapts to Light, Dark, and Midnight Blue themes with zero hardcoded colors.
 */
object ZenColors {
    val isDark: Boolean
        @Composable
        get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val Bg: Color
        @Composable
        get() = MaterialTheme.colorScheme.background

    val Card: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceContainerLow

    val CardSoft: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceContainer

    val Border: Color
        @Composable
        get() = MaterialTheme.colorScheme.outlineVariant

    val Primary: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    val PrimaryLight: Color
        @Composable
        get() = MaterialTheme.colorScheme.primaryContainer

    val PrimaryTint: Color
        @Composable
        get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)

    val PrimaryBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

    val Emerald: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    val EmeraldDark: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSecondaryContainer

    val EmeraldTint: Color
        @Composable
        get() = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)

    val EmeraldBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

    val Amber: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiary

    val AmberDark: Color
        @Composable
        get() = MaterialTheme.colorScheme.onTertiaryContainer

    val AmberTint: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)

    val AmberSoftBg: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)

    val AmberBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)

    val Text: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface

    val Muted: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val OnPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onPrimary
}

/**
 * Standard Material Design 3 card with 16.dp corners and 16.dp internal padding.
 */
@Composable
fun ZenCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    paddingValues: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = if (onClick != null) {
            modifier
                .heightIn(min = 48.dp)
                .clip(shape)
                .clickable(onClick = onClick)
        } else {
            modifier
        },
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            content = content,
        )
    }
}

/**
 * Standard Material Design 3 capsule badge / action chip using [MaterialTheme.typography.labelLarge].
 * Respects 48.dp minimum touch target when clickable.
 */
@Composable
fun ZenBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color = Color.Transparent,
    minWidth: Dp = 72.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    Surface(
        modifier = modifier
            .widthIn(min = minWidth)
            .let {
                if (onClick != null) {
                    it
                        .heightIn(min = 48.dp)
                        .clip(shape)
                        .clickable(onClick = onClick)
                } else {
                    it
                }
            },
        shape = shape,
        color = backgroundColor,
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Material Design 3 compact toggle switch with standard 48.dp accessible touch target.
 */
@Composable
fun ZenSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "zenSwitchTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "zenSwitchThumb",
    )
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 48.dp, height = 28.dp),
            shape = CircleShape,
            color = trackColor,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(thumbColor, CircleShape),
                )
            }
        }
    }
}

/**
 * Top Hero Header using standard Material 3 typography ([headlineSmall] + [bodyMedium]).
 */
@Composable
fun ZenHeroHeader(
    title: String,
    subtitle: String,
    iconEmoji: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (iconEmoji != null) {
            Text(
                text = iconEmoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Compact sticky top step indicator bar (height 44.dp <= 48.dp) with a single statusBarsPadding()
 * and solid MaterialTheme.colorScheme.background so all 3 themes apply cleanly.
 */
@Composable
fun ZenStepTopBar(
    currentStep: Int,
    totalSteps: Int = 4,
    showSkip: Boolean = true,
    skipLabel: String,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until totalSteps) {
                    val isActive = i == currentStep
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(250),
                        label = "stepDotWidth",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = dotWidth, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            ),
                    )
                }
            }

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (showSkip) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSkipClick),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = skipLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

/**
 * Bottom CTA Bar using Material 3 standard 48.dp touch targets, 12.dp button corners,
 * [MaterialTheme.typography.labelLarge], and RTL/LTR auto-mirrored arrow icons.
 */
@Composable
fun ZenBottomCtaBar(
    nextLabel: String,
    onNextClick: () -> Unit,
    showBack: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBackClick),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back_button),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onNextClick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = nextLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

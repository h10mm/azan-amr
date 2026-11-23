package com.github.meypod.al_azan.core.presentation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Material 3 shape tokens for the whole app.
 *
 * Sizes are intentionally rounded and progressive so related surfaces read as one family:
 * tags/badges ([extraSmall]) → chips/buttons ([small]) → menus/list items ([medium]) →
 * dialogs/containers ([large]) → bottom sheets/navigation surfaces ([extraLarge]).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Spacing and geometry tokens shared by composables, menus, dialogs and sheets.
 *
 * Provided once by [AlAzanTheme] through [LocalAppDimensions] so screens do not hard-code
 * their own magic numbers and stay visually consistent.
 */
@Immutable
data class AppDimensions(
    /** Inner padding for standard container cards. */
    val cardPadding: Dp = 16.dp,
    /** Minimum height of a tappable dropdown/menu row (meets the 48.dp touch target). */
    val menuItemHeight: Dp = 48.dp,
    /** Horizontal padding used by alert/confirmation dialogs. */
    val dialogHorizontalPadding: Dp = 20.dp,
    /** Corner radius for modal bottom sheets and similar navigation surfaces. */
    val bottomSheetCornerRadius: Dp = 28.dp,
) {
    companion object {
        val Default = AppDimensions()
    }
}

/**
 * Ambient [AppDimensions] token. Defaults to [AppDimensions.Default] so previews and any
 * composable rendered outside [AlAzanTheme] still get sensible geometry.
 */
val LocalAppDimensions = staticCompositionLocalOf { AppDimensions.Default }

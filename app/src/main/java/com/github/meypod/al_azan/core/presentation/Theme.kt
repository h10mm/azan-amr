package com.github.meypod.al_azan.core.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkSurfaceTint,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
    surfaceTint = LightSurfaceTint,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val LightHighContrastColorScheme = lightColorScheme(
    primary = LightHighContrastPrimary,
    onPrimary = LightHighContrastOnPrimary,
    primaryContainer = LightHighContrastPrimaryContainer,
    onPrimaryContainer = LightHighContrastOnPrimaryContainer,
    secondary = LightHighContrastSecondary,
    onSecondary = LightHighContrastOnSecondary,
    secondaryContainer = LightHighContrastSecondaryContainer,
    onSecondaryContainer = LightHighContrastOnSecondaryContainer,
    tertiary = LightHighContrastTertiary,
    onTertiary = LightHighContrastOnTertiary,
    tertiaryContainer = LightHighContrastTertiaryContainer,
    onTertiaryContainer = LightHighContrastOnTertiaryContainer,
    error = LightHighContrastError,
    onError = LightHighContrastOnError,
    errorContainer = LightHighContrastErrorContainer,
    onErrorContainer = LightHighContrastOnErrorContainer,
    background = LightHighContrastBackground,
    onBackground = LightHighContrastOnBackground,
    surface = LightHighContrastSurfaceContainer,
    onSurface = LightHighContrastOnSurface,
    surfaceVariant = LightHighContrastSurfaceVariant,
    onSurfaceVariant = LightHighContrastOnSurfaceVariant,
    outline = LightHighContrastOutline,
    outlineVariant = LightHighContrastOutlineVariant,
    scrim = LightHighContrastScrim,
    inverseSurface = LightHighContrastInverseSurface,
    inverseOnSurface = LightHighContrastInverseOnSurface,
    inversePrimary = LightHighContrastInversePrimary,
    surfaceTint = LightHighContrastSurfaceTint,
    surfaceContainerLowest = LightHighContrastSurfaceContainerLowest,
    surfaceContainerLow = LightHighContrastSurfaceContainerLow,
    surfaceContainer = LightHighContrastSurfaceContainer,
    surfaceContainerHigh = LightHighContrastSurfaceContainerHigh,
    surfaceContainerHighest = LightHighContrastSurfaceContainerHighest,
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = Color(0xFFFFD166), // أصفر كهرماني دافئ ومشرق - متوافق عالمياً مع عمى الألوان
    onPrimary = Color(0xFF001B3D),
    primaryContainer = Color(0xFF1E3566),
    onPrimaryContainer = Color(0xFFFFE8A3),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF132247),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE088),
    background = Color(0xFF070D1E), // كحلي عميق جداً فاحم ومريح
    onBackground = Color(0xFFF0F4F8),
    surface = Color(0xFF0D1730),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF0A1128), // لون خلفية الهيدر وحاوية الصلوات
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF2C3E6B),
    outlineVariant = Color(0xFF1B2A4A),
    surfaceContainerLowest = Color(0xFF050A18),
    surfaceContainerLow = Color(0xFF0F1B38),
    surfaceContainer = Color(0xFF152448),
    surfaceContainerHigh = Color(0xFF1C2E58),
    surfaceContainerHighest = Color(0xFF24396B),
)

@Composable
fun AlAzanTheme(
    themeColor: ThemeColor = ThemeColor.Default,
    displayScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeColor) {
        ThemeColor.ClassicLight -> false
        ThemeColor.Dark, ThemeColor.MidnightBlue -> true
    }

    val colorScheme = when (themeColor) {
        ThemeColor.ClassicLight -> LightColorScheme
        ThemeColor.MidnightBlue -> MidnightBlueColorScheme
        ThemeColor.Dark -> DarkColorScheme
    }

    val activity = LocalActivity.current
    if (activity != null) {
        SideEffect {
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val density = LocalDensity.current
    val scaledDensity = remember(density, displayScale) {
        Density(density.density * displayScale, density.fontScale)
    }

    val typography = when (LocalConfiguration.current.locales[0].language) {
        "fa" -> VazirmatnTypography
        "ar" -> NotoSansArabicTypography
        else -> Typography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalAppDimensions provides AppDimensions.Default,
            content = content,
        )
    }
}

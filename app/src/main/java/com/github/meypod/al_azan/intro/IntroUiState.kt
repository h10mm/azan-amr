package com.github.meypod.al_azan.intro

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.presentation.navigation.Route

/**
 * The four Minimal Zen onboarding screens, in order:
 *  1. Welcome & language (+ restore a backup),
 *  2. Location and astronomical calculation method together,
 *  3. Default Muezzin voice & Smart Spiritual Capsule,
 *  4. Readiness Shield & device-specific permissions / battery protection.
 */
private val introSteps = listOf(
    Route.Intro.LanguageSelection,
    Route.Intro.Location,
    Route.Intro.Adhan,
    Route.Intro.Troubleshoot,
)

@Immutable
data class IntroUiState(
    val route: Route = Route.Intro.LanguageSelection,
    val busy: Boolean = false,
    val restoring: Boolean = false,
    val showSkipDialog: Boolean = false,
    val appIntroDone: Boolean = false,
) {
    val totalSteps: Int
        get() = introSteps.size

    val step: Int
        get() {
            val indexOf = introSteps.indexOf(route)
            return if (indexOf < 0) 0 else indexOf
        }

    val nextRoute: Route?
        get() = introSteps.getOrNull(step + 1)

    val previousRoute: Route?
        get() = introSteps.getOrNull(step - 1)

    val isLastStep: Boolean
        get() {
            return step == introSteps.lastIndex
        }

    val progress: Float
        get() {
            if (introSteps.size <= 1) return 1f
            return (step.toFloat() / introSteps.lastIndex.toFloat()).coerceIn(0f, 1f)
        }
}

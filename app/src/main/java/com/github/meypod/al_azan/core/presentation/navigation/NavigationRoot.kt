package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.SnackbarController
import com.github.meypod.al_azan.core.presentation.feedback.ObserveScheduleFeedback
import com.github.meypod.al_azan.intro.IntroNavigation
import com.github.meypod.al_azan.main.MainNavigation

@Composable
fun NavigationRoot(
    appIntroDone: Boolean,
    startingRoute: Route?,
) {
    val rootBackStack =
        rememberNavBackStack(
            configuration = routeSavedStateConfiguration,
            if (appIntroDone) Route.Main else Route.Intro,
        )

    LaunchedEffect(appIntroDone) {
        if (!appIntroDone && rootBackStack.lastOrNull() != Route.Intro) {
            rootBackStack.clear()
            rootBackStack.add(Route.Intro)
        }
    }

    CompositionLocalProvider(
        LocalSnackbarController provides remember { SnackbarController(SnackbarHostState()) },
    ) {
        ObserveScheduleFeedback()
        NavDisplay(
            backStack = rootBackStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<Route.Intro> {
                        IntroNavigation(
                            onFinishIntro = {
                                rootBackStack.clear()
                                rootBackStack.add(Route.Main)
                            },
                        )
                    }

                    entry<Route.Main> {
                        MainNavigation(
                            startingRoute,
                        )
                    }
                },
        )
    }
}

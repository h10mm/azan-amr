package com.github.meypod.al_azan.intro

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.AppSnackbarHost
import com.github.meypod.al_azan.core.presentation.components.BlockingProgressDialog
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.TimedDangerDialog
import com.github.meypod.al_azan.core.presentation.components.ZenBottomCtaBar
import com.github.meypod.al_azan.core.presentation.components.ZenStepTopBar
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.navigateTo
import com.github.meypod.al_azan.core.presentation.navigation.rememberHorizontalSlideDirections
import com.github.meypod.al_azan.core.presentation.navigation.rootRedirectFallback
import com.github.meypod.al_azan.core.presentation.navigation.routeSavedStateConfiguration
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import com.github.meypod.al_azan.intro.adhan.IntroAdhanStep
import com.github.meypod.al_azan.intro.components.IntroTitle
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionScreen
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionViewModel
import com.github.meypod.al_azan.intro.location.IntroLocationStep
import com.github.meypod.al_azan.intro.location.IntroLocationViewModel
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupScreen
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsViewModel
import com.github.meypod.al_azan.main.settings.adhan.PrayerScheduleScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsViewModel
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsScreen
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsViewModel
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcScreen
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootViewModel
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsUiAction
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsViewModel

@Composable
fun IntroNavigation(onFinishIntro: () -> Unit) {
    val slideDirections = rememberHorizontalSlideDirections()

    val introBackstack =
        rememberNavBackStack(
            configuration = routeSavedStateConfiguration,
            Route.Intro.LanguageSelection,
        )

    val introViewModel = hiltViewModel<IntroViewModel>()
    val introUiState by introViewModel.uiState.collectAsStateWithLifecycle()

    BindBackStackWithController(
        currentRoute = introBackstack::lastOrNull,
        navigateTo = introBackstack::navigateTo,
        popBack = { introBackstack.removeAt(introBackstack.lastIndex) },
        canPopBack = { introBackstack.size > 1 },
        onRouteVisible = { (it as? Route)?.let { route -> introViewModel.onAction(IntroUiAction.OnRouteVisible(route)) } },
    )

    LaunchedEffect(introUiState.appIntroDone) {
        if (introUiState.appIntroDone) {
            onFinishIntro()
        }
    }

    NavDisplay(
        backStack = introBackstack,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.forwardEnter },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.forwardExit / 2 },
                )
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.backEnter / 2 },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.backExit },
                )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.backEnter / 2 },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.backExit },
                )
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider(fallback = rootRedirectFallback(introBackstack, Route.Intro.LanguageSelection)) {
                entry<Route.Intro.LanguageSelection> {
                    val viewModel = hiltViewModel<LanguageSelectionViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        LanguageSelectionScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            onIntroAction = introViewModel::onAction,
                            modifier = modifier,
                            restoring = introUiState.restoring,
                        )
                    }
                }
                entry<Route.Intro.RestoreBackup> {
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        RestoreBackupScreen(
                            onIntroAction = introViewModel::onAction,
                            modifier = modifier,
                            busy = introUiState.restoring,
                        )
                    }
                }
                entry<Route.Intro.Location> {
                    val locationViewModel = hiltViewModel<IntroLocationViewModel>()
                    val locationState by locationViewModel.uiState.collectAsStateWithLifecycle()
                    val calcViewModel = hiltViewModel<CalculationSettingsViewModel>()
                    val calcState by calcViewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        IntroLocationStep(
                            uiState = locationState,
                            calculationState = calcState,
                            onQueryChange = locationViewModel::onQueryChange,
                            onQuickCitySelected = locationViewModel::onQuickCitySelected,
                            onCitySelected = locationViewModel::onCitySelected,
                            onGpsLocation = locationViewModel::onGpsLocation,
                            onDialogConfirm = locationViewModel::onDialogConfirm,
                            onCalculationAction = calcViewModel::onAction,
                            getCountries = locationViewModel::getCountries,
                            getCities = locationViewModel::getCities,
                            modifier = modifier,
                        )
                    }
                }
                entry<Route.Intro.Calculation> {
                    val viewModel = hiltViewModel<CalculationSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        Column(
                            modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                        ) {
                            IntroTitle(R.string.calculation_title)
                            CalculationSettingsScreen(
                                uiState = uiState,
                                onAction = viewModel::onAction,
                                adjustmentsRoute = Route.Intro.Calculation.Adjustments,
                                advancedRoute = Route.Intro.Calculation.AdvancedCalculation,
                            )
                        }
                    }
                }
                entry<Route.Intro.Calculation.Adjustments> {
                    val viewModel = hiltViewModel<AdjustmentsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdjustmentsScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Calculation.AdvancedCalculation> {
                    val viewModel = hiltViewModel<AdvancedCalcViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdvancedCalcScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Adhan> {
                    val viewModel = hiltViewModel<AdhanSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val widgetVm = hiltViewModel<WidgetSettingsViewModel>()
                    val widgetState by widgetVm.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        IntroAdhanStep(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            capsuleEnabled = widgetState.nateegaConfig.showSpiritualPillNotification,
                            onCapsuleToggle = { enabled ->
                                widgetVm.onAction(WidgetSettingsUiAction.OnShowSpiritualPillNotificationToggle(enabled))
                            },
                            persistentNotifEnabled = widgetState.settings.showWidget,
                            onPersistentNotifToggle = { enabled ->
                                widgetVm.onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(enabled))
                            },
                            prayerScheduleRoute = { Route.Intro.Adhan.PrayerSchedule(it) },
                            modifier = modifier,
                        )
                    }
                }
                entry<Route.Intro.Adhan.PrayerSchedule> { key ->
                    val viewModel = hiltViewModel<AdhanSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    PrayerScheduleScreen(
                        prayer = key.prayer,
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Troubleshoot> {
                    val viewModel = hiltViewModel<TroubleshootViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val advancedViewModel = hiltViewModel<AdvancedTroubleshootViewModel>()
                    val advancedState by advancedViewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        TroubleshootScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            modifier = modifier,
                            advancedUiState = advancedState,
                            onAdvancedAction = advancedViewModel::onAction,
                            advancedRoute = Route.Intro.Troubleshoot.AdvancedTroubleshoot,
                            showAdvancedLink = false,
                            showIntroHeader = true,
                        )
                    }
                }
                entry<Route.Intro.Troubleshoot.AdvancedTroubleshoot> {
                    val viewModel = hiltViewModel<AdvancedTroubleshootViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdvancedTroubleshootScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
            },
    )
    if (introUiState.showSkipDialog) {
        TimedDangerDialog(
            title = stringResource(R.string.attention_title),
            text = stringResource(R.string.skip_dialog_body),
            confirmLabel = stringResource(R.string.skip),
            dismissLabel = stringResource(R.string.cancel),
            seconds = 3,
            onDismissRequest = { introViewModel.onAction(IntroUiAction.OnSkipDismiss) },
            onConfirm = { introViewModel.onAction(IntroUiAction.OnSkipConfirmed) },
        )
    }
    if (introUiState.restoring) {
        BlockingProgressDialog(message = stringResource(R.string.restoring_backup))
    }
    val snackbarController = LocalSnackbarController.current
    val resources = LocalResources.current
    LaunchedEffect(introViewModel.restoreErrors) {
        introViewModel.restoreErrors.collect {
            snackbarController.show(resources.getString(R.string.restore_failed))
        }
    }
    LaunchedEffect(introViewModel.locationRequired) {
        introViewModel.locationRequired.collect {
            snackbarController.show(resources.getString(R.string.intro_location_required))
        }
    }
}

@Composable
private fun IntroStepScaffold(
    uiState: IntroUiState,
    onAction: (IntroUiAction) -> Unit,
    scrollable: Boolean = true,
    floatingActionButton: @Composable (() -> Unit) = {},
    content: @Composable (Modifier) -> Unit,
) {
    val scrollState = rememberScrollState()

    val nextButtonLabel = when (uiState.route) {
        Route.Intro.LanguageSelection -> "ابدأ الإعداد السريع"
        Route.Intro.Location -> "التالي: المؤذن والكبسولة"
        Route.Intro.Adhan -> "التالي: درع الجاهزية"
        Route.Intro.Troubleshoot -> "دخول التطبيق الآن 🕌"
        else -> stringResource(if (uiState.isLastStep) R.string.finish_button else R.string.next_button)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ZenStepTopBar(
                currentStep = uiState.step,
                totalSteps = uiState.totalSteps,
                showSkip = !uiState.isLastStep,
                skipLabel = "تخطي الإعداد",
                onSkipClick = { onAction(IntroUiAction.OnSkipClick) },
                title = if (uiState.route == Route.Intro.Troubleshoot) stringResource(R.string.troubleshoot_title) else null,
            )
        },
        bottomBar = {
            ZenBottomCtaBar(
                nextLabel = nextButtonLabel,
                onNextClick = {
                    if (uiState.isLastStep) {
                        onAction(IntroUiAction.OnFinishClick)
                    } else {
                        onAction(IntroUiAction.OnNextClick)
                    }
                },
                showBack = uiState.step > 0,
                onBackClick = { onAction(IntroUiAction.OnBackClick) },
            )
        },
        snackbarHost = { AppSnackbarHost(LocalSnackbarController.current.hostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = floatingActionButton,
    ) { paddingValues ->
        val baseModifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)

        val contentModifier =
            if (scrollable) {
                baseModifier
                    .fadeScrollEdges(scrollState, Orientation.Vertical)
                    .drawVerticalScrollbar(scrollState, barColor = @Composable { MaterialTheme.colorScheme.primary })
                    .verticalScroll(scrollState)
            } else {
                baseModifier
            }.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)

        content(contentModifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroStepScaffoldPreview() {
    AlAzanTheme {
        IntroStepScaffold(
            uiState = IntroUiState(route = Route.Intro.LanguageSelection),
            onAction = {},
        ) {
            Column(it) { }
        }
    }
}

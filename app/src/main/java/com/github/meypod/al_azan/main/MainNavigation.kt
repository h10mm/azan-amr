package com.github.meypod.al_azan.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.ZenColors
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithController
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.navigateTo
import com.github.meypod.al_azan.core.presentation.navigation.rememberHorizontalSlideDirections
import com.github.meypod.al_azan.core.presentation.navigation.rootRedirectFallback
import com.github.meypod.al_azan.core.presentation.navigation.routeSavedStateConfiguration
import com.github.meypod.al_azan.main.about.AboutScreen
import com.github.meypod.al_azan.main.about.AboutViewModel
import com.github.meypod.al_azan.main.azkar.AzkarIndexScreen
import com.github.meypod.al_azan.main.azkar.AzkarReaderScreen
import com.github.meypod.al_azan.main.azkar.AzkarViewModel
import com.github.meypod.al_azan.main.tasbih.TasbihScreen
import com.github.meypod.al_azan.main.tasbih.TasbihViewModel
import com.github.meypod.al_azan.main.home.HomePermissionGate
import com.github.meypod.al_azan.main.home.HomeScreen
import com.github.meypod.al_azan.main.home.HomeViewModel
import com.github.meypod.al_azan.main.home.RamadanNoticeGate
import com.github.meypod.al_azan.main.location.LocationScreen
import com.github.meypod.al_azan.main.location.LocationViewModel
import com.github.meypod.al_azan.main.monthly.MonthlyViewScreen
import com.github.meypod.al_azan.main.monthly.MonthlyViewViewModel
import com.github.meypod.al_azan.main.qibla.QiblaCompassRoute
import com.github.meypod.al_azan.main.qibla.QiblaCompassViewModel
import com.github.meypod.al_azan.main.qibla.QiblaScreen
import com.github.meypod.al_azan.main.qibla.QiblaViewModel
import com.github.meypod.al_azan.main.reminder.ReminderScreen
import com.github.meypod.al_azan.main.reminder.ReminderViewModel
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsScreen
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsViewModel
import com.github.meypod.al_azan.main.settings.adhan.PrayerScheduleScreen
import com.github.meypod.al_azan.main.settings.adhan.ScheduleAndMuezzinScreen
import com.github.meypod.al_azan.main.settings.appearance.InterfaceSettingsScreen
import com.github.meypod.al_azan.main.settings.appearance.InterfaceSettingsViewModel
import com.github.meypod.al_azan.main.settings.backup.BackupRestoreScreen
import com.github.meypod.al_azan.main.settings.backup.BackupRestoreViewModel
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsViewModel
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsScreen
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsViewModel
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcScreen
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcViewModel
import com.github.meypod.al_azan.main.settings.developer.DeveloperScreen
import com.github.meypod.al_azan.main.settings.developer.DeveloperViewModel
import com.github.meypod.al_azan.main.settings.menu.SettingsMenuScreen
import com.github.meypod.al_azan.main.settings.menu.SettingsMenuViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootViewModel
import com.github.meypod.al_azan.main.settings.widget.CompactAppearanceScreen
import com.github.meypod.al_azan.main.settings.widget.CompactAppearanceViewModel
import com.github.meypod.al_azan.main.settings.widget.CompactPrayerAppearanceScreen
import com.github.meypod.al_azan.main.settings.widget.CompactPrayerAppearanceViewModel
import com.github.meypod.al_azan.main.settings.widget.NateegaAppearanceScreen
import com.github.meypod.al_azan.main.settings.widget.NateegaAppearanceViewModel
import com.github.meypod.al_azan.main.settings.widget.NotificationWidgetSettingsScreen
import com.github.meypod.al_azan.main.settings.widget.TableAppearanceScreen
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsScreen
import com.github.meypod.al_azan.main.settings.widget.WidgetSettingsViewModel
import com.github.meypod.al_azan.main.settings.widget.custom.CustomWidgetBuilderScreen
import com.github.meypod.al_azan.main.settings.widget.custom.CustomWidgetBuilderViewModel
import com.github.meypod.al_azan.main.silence.SilenceStatusScreen
import com.github.meypod.al_azan.main.silence.SilenceStatusViewModel
import com.github.meypod.al_azan.main.upcoming_alarms.UpcomingAlarmsScreen
import com.github.meypod.al_azan.main.upcoming_alarms.UpcomingAlarmsViewModel

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.github.meypod.al_azan.main.home.DualCalendarSplitScreen
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.main.home.HomeUiAction

@Composable
fun MainNavigation(
    startingRoute: Route?,
    modifier: Modifier = Modifier,
) {
    val slideDirections = rememberHorizontalSlideDirections()

    val mainBackstack =
        rememberNavBackStack(
            configuration = routeSavedStateConfiguration,
            *when {
                startingRoute == null || startingRoute == Route.Main.Home -> arrayOf<NavKey>(Route.Main.Home)
                else -> arrayOf<NavKey>(Route.Main.Home, startingRoute)
            },
        )

    BindBackStackWithController(
        currentRoute = { mainBackstack.lastOrNull() },
        navigateTo = { route -> mainBackstack.navigateTo(route) },
        popBack = { mainBackstack.removeAt(mainBackstack.lastIndex) },
        canPopBack = { mainBackstack.size > 1 },
    )

    val currentRoute = mainBackstack.lastOrNull() as? Route
    val isRoot = currentRoute == Route.Main.Home ||
        currentRoute == Route.Main.QiblaCompass ||
        currentRoute == Route.Main.Azkar ||
        currentRoute == Route.Main.Counter ||
        currentRoute == Route.Main.Settings

    // الاحتفاظ بنماذج العرض الأساسية للشريط السفلي جاهزة في الذاكرة لتنقل لحظي بدون أي تأخير
    val homeVm = hiltViewModel<HomeViewModel>()
    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
    val qiblaCompassVm = hiltViewModel<QiblaCompassViewModel>()
    val qiblaCompassState by qiblaCompassVm.uiState.collectAsStateWithLifecycle()
    val monthlyVm = hiltViewModel<MonthlyViewViewModel>()
    val monthlyState by monthlyVm.uiState.collectAsStateWithLifecycle()
    val settingsMenuVm = hiltViewModel<SettingsMenuViewModel>()
    val settingsMenuState by settingsMenuVm.uiState.collectAsStateWithLifecycle()
    val azkarVm = hiltViewModel<AzkarViewModel>()
    val azkarState by azkarVm.uiState.collectAsStateWithLifecycle()
    val tasbihVm = hiltViewModel<TasbihViewModel>()
    val tasbihState by tasbihVm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (isRoot) {
                NavigationBar {
                    val navigateRoot: (Route) -> Unit = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            Snapshot.withMutableSnapshot {
                                if (targetRoute == Route.Main.Home) {
                                    if (mainBackstack.size > 1) {
                                        mainBackstack.subList(1, mainBackstack.size).clear()
                                    }
                                } else if (mainBackstack.size == 1) {
                                    mainBackstack.add(targetRoute)
                                } else {
                                    mainBackstack[1] = targetRoute
                                    if (mainBackstack.size > 2) {
                                        mainBackstack.subList(2, mainBackstack.size).clear()
                                    }
                                }
                            }
                        }
                    }

                    NavigationBarItem(
                        selected = currentRoute == Route.Main.Home,
                        onClick = { navigateRoot(Route.Main.Home) },
                        icon = { Icon(painterResource(R.drawable.mosque), contentDescription = stringResource(R.string.home), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Main.QiblaCompass,
                        onClick = { navigateRoot(Route.Main.QiblaCompass) },
                        icon = { Icon(painterResource(R.drawable.compass_outline), contentDescription = stringResource(R.string.qibla), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.qibla)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Main.Azkar,
                        onClick = { navigateRoot(Route.Main.Azkar) },
                        icon = { Icon(painterResource(R.drawable.ic_azkar_book), contentDescription = stringResource(R.string.azkar_title), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.azkar_title)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Main.Counter,
                        onClick = { navigateRoot(Route.Main.Counter) },
                        icon = { Icon(painterResource(R.drawable.ic_tasbih), contentDescription = stringResource(R.string.nav_tasbih), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.nav_tasbih)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Main.Settings,
                        onClick = { navigateRoot(Route.Main.Settings) },
                        icon = { Icon(painterResource(R.drawable.settings), contentDescription = stringResource(R.string.settings), modifier = Modifier.size(24.dp)) },
                        label = { Text(stringResource(R.string.settings)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = mainBackstack,
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider(fallback = rootRedirectFallback(mainBackstack, Route.Main.Home)) {
                entry<Route.Main.Home> {
                    HomeScreen(homeState, homeVm::onAction)
                    HomePermissionGate(
                        isDontAskAgain = homeVm::isDontAskAgain,
                        onDontAskAgain = homeVm::onPermissionDontAskAgain,
                        onReschedule = homeVm::rescheduleAlarms,
                        onCleanup = homeVm::cleanupAlarms,
                        getCheck = homeVm::permissionCheck,
                    )
                    RamadanNoticeGate(
                        shouldShow = { homeVm.shouldShowRamadanNotice() },
                        onRemindNextYear = homeVm::onRamadanRemindNextYear,
                        onDontShowAgain = homeVm::onRamadanDontShowAgain,
                    )
                }
                entry<Route.Main.SilenceStatus> {
                    val vm = hiltViewModel<SilenceStatusViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    SilenceStatusScreen(s, vm::onAction)
                }
                entry<Route.Main.Location> {
                    val vm = hiltViewModel<LocationViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    LocationScreen(s, vm::onAction, getCountries = vm::getCountries, getCities = vm::getCities)
                }
                entry<Route.Main.Settings> {
                    SettingsMenuScreen(settingsMenuState, settingsMenuVm::onAction)
                }
                entry<Route.Main.UpcomingAlarms> {
                    val vm = hiltViewModel<UpcomingAlarmsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    UpcomingAlarmsScreen(s, vm::onAction)
                }
                entry<Route.Main.About> {
                    val vm = hiltViewModel<AboutViewModel>()
                    AboutScreen(vm::onAction)
                }
                entry<Route.Main.MonthlyView> {
                    MonthlyViewScreen(monthlyState, monthlyVm::onAction)
                }
                entry<Route.Main.CalendarView> {
                    DualCalendarSplitScreen(
                        homeState = homeState,
                        onAction = homeVm::onAction,
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.Reminder> {
                    val vm = hiltViewModel<ReminderViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    ReminderScreen(s, vm::onAction)
                }
                entry<Route.Main.Qibla> {
                    val vm = hiltViewModel<QiblaViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    QiblaScreen(s, vm::onAction)
                }
                entry<Route.Main.QiblaCompass> {
                    QiblaCompassRoute(qiblaCompassState, qiblaCompassVm.compassState, qiblaCompassVm.diagnostics, qiblaCompassVm::onAction)
                }
                entry<Route.Main.Counter> {
                    TasbihScreen(
                        uiState = tasbihState,
                        onAction = tasbihVm::onAction,
                        onIncrementWithOutcome = tasbihVm::incrementAndReport,
                        onCycleSensoryModeWithReport = tasbihVm::cycleSensoryModeAndReport,
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.Azkar> {
                    AzkarIndexScreen(
                        uiState = azkarState,
                        onSearchQueryChanged = azkarVm::onSearchQueryChanged,
                        onSetFavoritesOnly = azkarVm::onSetFavoritesOnly,
                        onToggleFavorite = azkarVm::toggleFavorite,
                        onCategoryClick = { categoryId ->
                            NavigationController.navigateTo(Route.Main.AzkarDetail(categoryId = categoryId.id))
                        },
                        onDhikrClick = { categoryId, dhikrId ->
                            NavigationController.navigateTo(
                                Route.Main.AzkarDetail(categoryId = categoryId.id, initialDhikrId = dhikrId),
                            )
                        },
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.AzkarDetail> { key ->
                    AzkarReaderScreen(
                        categoryId = key.categoryId,
                        initialDhikrId = key.initialDhikrId,
                        uiState = azkarState,
                        onToggleFavorite = azkarVm::toggleFavorite,
                        onIncreaseFontSize = azkarVm::increaseFontSize,
                        onDecreaseFontSize = azkarVm::decreaseFontSize,
                        onDecrementCount = azkarVm::decrementCount,
                        onResetDhikrCount = azkarVm::resetDhikrCount,
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.QuranKahf> {
                    com.github.meypod.al_azan.main.quran.SurahReaderScreen(
                        surahNumber = 18,
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.QuranMulk> {
                    com.github.meypod.al_azan.main.quran.SurahReaderScreen(
                        surahNumber = 67,
                        onBackClick = { NavigationController.navigateBack() },
                    )
                }
                entry<Route.Main.Settings.InterfaceSettings> {
                    val vm = hiltViewModel<InterfaceSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    InterfaceSettingsScreen(s, vm::onAction, events = vm.events)
                }
                entry<Route.Main.Settings.SoundAndNotifications> {
                    val vm = hiltViewModel<AdhanSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    AdhanSettingsScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.Calculations> {
                    val vm = hiltViewModel<CalculationSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    ScreenScaffold(
                        title = stringResource(R.string.calculation_title),
                        onBackClick = { NavigationController.navigateBack() },
                    ) {
                        CalculationSettingsScreen(s, vm::onAction)
                    }
                }
                entry<Route.Main.Settings.Troubleshoot> {
                    val vm = hiltViewModel<TroubleshootViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    val advancedVm = hiltViewModel<AdvancedTroubleshootViewModel>()
                    val advState by advancedVm.uiState.collectAsStateWithLifecycle()
                    ScreenScaffold(
                        title = stringResource(R.string.troubleshoot_title),
                        onBackClick = { NavigationController.navigateBack() },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        containerColor = ZenColors.Bg,
                        contentColor = ZenColors.Text,
                    ) {
                        TroubleshootScreen(
                            uiState = s,
                            onAction = vm::onAction,
                            advancedUiState = advState,
                            onAdvancedAction = advancedVm::onAction,
                        )
                    }
                }
                entry<Route.Main.Settings.WidgetSettings> {
                    val vm = hiltViewModel<WidgetSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    WidgetSettingsScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.NotificationWidget> {
                    val vm = hiltViewModel<WidgetSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    NotificationWidgetSettingsScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.WidgetSettings.TableAppearance> {
                    val vm = hiltViewModel<WidgetSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    TableAppearanceScreen(s, vm::onAction, events = vm.events)
                }
                entry<Route.Main.Settings.WidgetSettings.CompactAppearance> {
                    val vm = hiltViewModel<CompactAppearanceViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    CompactAppearanceScreen(
                        config = s,
                        onShapeChange = vm::onShapeChange,
                        onBackgroundChange = vm::onBackgroundChange,
                        onProgressColorChange = vm::onProgressColorChange,
                        onCountdownColorChange = vm::onCountdownColorChange,
                        onPrayerNameColorChange = vm::onPrayerNameColorChange,
                    )
                }
                entry<Route.Main.Settings.WidgetSettings.CustomBuilder> {
                    val vm = hiltViewModel<CustomWidgetBuilderViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    CustomWidgetBuilderScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.WidgetSettings.NateegaAppearance> {
                    val vm = hiltViewModel<NateegaAppearanceViewModel>()
                    val config by vm.uiState.collectAsStateWithLifecycle()
                    NateegaAppearanceScreen(
                        config = config,
                        onClockPositionChange = vm::onClockPositionChange,
                        onShowCopticDateToggle = vm::onShowCopticDateToggle,
                        onShowCountdownToggle = vm::onShowCountdownToggle,
                        onShowWisdomToggle = vm::onShowWisdomToggle,
                        onLanguageModeChange = vm::onLanguageModeChange,
                        onThemeModeChange = vm::onThemeModeChange,
                    )
                }
                entry<Route.Main.Settings.WidgetSettings.CompactPrayerAppearance> {
                    val vm = hiltViewModel<CompactPrayerAppearanceViewModel>()
                    val config by vm.uiState.collectAsStateWithLifecycle()
                    CompactPrayerAppearanceScreen(
                        config = config,
                        onDateDisplayModeChange = vm::onDateDisplayModeChange,
                        onDateOrderChange = vm::onDateOrderChange,
                        onThemeModeChange = vm::onThemeModeChange,
                        onShowCountdownToggle = vm::onShowCountdownToggle,
                        onShowWisdomToggle = vm::onShowWisdomToggle,
                    )
                }
                entry<Route.Main.Settings.BackupAndRestore> {
                    val vm = hiltViewModel<BackupRestoreViewModel>()
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    BackupRestoreScreen(
                        uiState = uiState,
                        onAction = vm::onAction,
                        events = vm.events,
                    )
                }
                entry<Route.Main.Settings.Developer> {
                    val vm = hiltViewModel<DeveloperViewModel>()
                    DeveloperScreen(vm::onAction)
                }
                entry<Route.Main.Settings.Calculations.Adjustments> {
                    val vm = hiltViewModel<AdjustmentsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    AdjustmentsScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.Calculations.AdvancedCalculation> {
                    val vm = hiltViewModel<AdvancedCalcViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    AdvancedCalcScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.Troubleshoot.AdvancedTroubleshoot> {
                    val vm = hiltViewModel<AdvancedTroubleshootViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    AdvancedTroubleshootScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin> {
                    val vm = hiltViewModel<AdhanSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    ScheduleAndMuezzinScreen(s, vm::onAction)
                }
                entry<Route.Main.Settings.SoundAndNotifications.PrayerSchedule> { key ->
                    val vm = hiltViewModel<AdhanSettingsViewModel>()
                    val s by vm.uiState.collectAsStateWithLifecycle()
                    PrayerScheduleScreen(key.prayer, s, vm::onAction)
                }
            },
        )
    }
}

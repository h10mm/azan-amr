package com.github.meypod.al_azan.main.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.HomeShortcut
import com.github.meypod.al_azan.core.domain.model.settings.i18n
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.DarkOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.DarkTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightOnTertiaryContainer
import com.github.meypod.al_azan.core.presentation.LightTertiaryContainer
import com.github.meypod.al_azan.core.presentation.util.dropShadow2
import com.github.meypod.al_azan.core.presentation.util.swipeNavigate
import com.github.meypod.al_azan.core.util.device.DeviceUtils
import com.github.meypod.al_azan.main.home.components.ConfigHintCard
import com.github.meypod.al_azan.main.home.components.HomeHeader
import com.github.meypod.al_azan.main.home.components.ShariaTimesBox
import com.github.meypod.al_azan.main.home.components.ShariaTimesBoxUiState
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

private val WIDE_LAYOUT_MIN_WIDTH = 480.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val wideLayout = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        windowWidth >= WIDE_LAYOUT_MIN_WIDTH

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(
                modifier = Modifier.graphicsLayer { clip = false },
                visible = DateComponents.from(uiState.viewingInstant) != DateComponents.from(uiState.currentInstant),
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 150),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it / 3 },
                    animationSpec = tween(durationMillis = 200),
                ),
            ) {
                val buttonShape = MaterialTheme.shapes.extraLarge
                val dark = uiState.themeColor.isDark()
                val containerColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkTertiaryContainer else LightTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
                val contentColor = if (uiState.themeColor.isClassic()) {
                    if (dark) DarkOnTertiaryContainer else LightOnTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
                ExtendedFloatingActionButton(
                    onClick = { onAction(HomeUiAction.OnShowTodayClick) },
                    shape = buttonShape,
                    modifier = Modifier
                        .widthIn(min = 160.dp)
                        .dropShadow2(buttonShape),
                    containerColor = containerColor,
                    contentColor = contentColor,
                ) {
                    Text(
                        stringResource(R.string.show_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val navigationModifier = Modifier
                .fillMaxSize()
                .swipeNavigate(
                    onNext = { onAction(HomeUiAction.OnNextDayClick) },
                    onPrev = { onAction(HomeUiAction.OnPrevDayClick) },
                )

            if (wideLayout) {
                Row(navigationModifier) {
                    // الجانب الأول في الوضع الأفقي: الساعة والتاريخ والعداد وأزرار الوصول السريع
                    HomeHeader(
                        uiState = uiState,
                        wideLayout = true,
                        onAction = onAction,
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                    )

                    // الجانب الثاني في الوضع الأفقي: حاوية الصلوات المقسمة
                    Column(
                        Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        if (uiState.location == null || !uiState.isCalculationConfigured || uiState.prayerTimesUnresolvable) {
                            ConfigHintCard(
                                missingLocation = uiState.location == null,
                                missingCalculation = !uiState.isCalculationConfigured,
                                onLocationClick = { onAction(HomeUiAction.OnLocationTextClick) },
                                onCalculationClick = { onAction(HomeUiAction.OnCalculationLinkClick) },
                                modifier = Modifier
                                    .padding(horizontal = dimensionResource(R.dimen.page_padding))
                                    .padding(bottom = dimensionResource(R.dimen.element_padding)),
                                unresolvableTimes = uiState.prayerTimesUnresolvable,
                            )
                        }
                        ShariaTimesBox(
                            ShariaTimesBoxUiState(
                                shariahTimes = uiState.shariaTimes,
                                locale = uiState.locale,
                                numberingSystem = uiState.numberingSystem,
                                is24Hours = uiState.is24Hour,
                                highlightedShariaTime = uiState.highlightedShariaTime,
                                hiddenPrayers = uiState.hiddenPrayers,
                                themeColor = uiState.themeColor,
                                skippedPrayers = uiState.skippedPrayers,
                                now = uiState.currentInstant,
                            ),
                            wideLayout = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            } else {
                Column(navigationModifier) {
                    // الهيدر العلوي في الوضع الرأسي
                    HomeHeader(
                        uiState = uiState,
                        wideLayout = false,
                        onAction = onAction,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // حاوية الصلوات في الوضع الرأسي
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        if (uiState.location == null || !uiState.isCalculationConfigured || uiState.prayerTimesUnresolvable) {
                            ConfigHintCard(
                                missingLocation = uiState.location == null,
                                missingCalculation = !uiState.isCalculationConfigured,
                                onLocationClick = { onAction(HomeUiAction.OnLocationTextClick) },
                                onCalculationClick = { onAction(HomeUiAction.OnCalculationLinkClick) },
                                modifier = Modifier
                                    .padding(horizontal = dimensionResource(R.dimen.page_padding))
                                    .padding(bottom = dimensionResource(R.dimen.element_padding)),
                                unresolvableTimes = uiState.prayerTimesUnresolvable,
                            )
                        }
                        ShariaTimesBox(
                            ShariaTimesBoxUiState(
                                shariahTimes = uiState.shariaTimes,
                                locale = uiState.locale,
                                numberingSystem = uiState.numberingSystem,
                                is24Hours = uiState.is24Hour,
                                highlightedShariaTime = uiState.highlightedShariaTime,
                                hiddenPrayers = uiState.hiddenPrayers,
                                themeColor = uiState.themeColor,
                                skippedPrayers = uiState.skippedPrayers,
                                now = uiState.currentInstant,
                            ),
                            wideLayout = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun HomeLoadedPreview() {
    AlAzanTheme {
        val location = StaticFavoriteLocation("foo", CalculationLocationDetail(0.0, 0.0, label = "Null Island"))
        val instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val viewing = instant.minus(1.toDuration(DurationUnit.DAYS))
        val getShariaTimesUseCase = GetShariaTimesUseCase()
        val shariahTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            midnightMethod = MidnightMethod.SunsetToFajr,
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        ).timesOrNull
        val nextShariaTime = GetNextShariaTimesUseCase(getShariaTimesUseCase)(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            midnightMethod = MidnightMethod.SunsetToFajr,
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        )
        HomeScreen(
            uiState = HomeUiState(
                currentInstant = instant,
                viewingInstant = viewing,
                calendar = "gregorian",
                location = location,
                shariaTimes = shariahTimes,
                nextShariaTime = nextShariaTime,
                highlightedShariaTime = nextShariaTime,
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun HomeInitialPreview() {
    AlAzanTheme {
        HomeScreen(
            uiState = HomeUiState(
                calendar = "gregorian",
            ),
            onAction = {},
        )
    }
}

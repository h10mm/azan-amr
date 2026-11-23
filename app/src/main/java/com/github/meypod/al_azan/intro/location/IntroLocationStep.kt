package com.github.meypod.al_azan.intro.location

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.canonicalParameters
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.util.CityMatch
import com.github.meypod.al_azan.core.domain.util.pickNameForLanguage
import com.github.meypod.al_azan.core.presentation.components.ZenBadge
import com.github.meypod.al_azan.core.presentation.components.ZenCard
import com.github.meypod.al_azan.core.presentation.components.ZenHeroHeader
import com.github.meypod.al_azan.core.presentation.components.bidiLtr
import com.github.meypod.al_azan.core.presentation.dialog.rememberLocationAccessHelperDialogs
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.main.location.LocationUiAction
import com.github.meypod.al_azan.main.location.components.NewLocationDialog
import com.github.meypod.al_azan.main.location.components.NewLocationDialogUiState
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsUiAction
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsUiState
import com.github.meypod.al_azan.main.settings.calculation.utils.i18n
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.delay

/** How long to wait for the OS to deliver a GPS fix before giving the button back (e.g. permission denied). */
private const val GPS_WAIT_TIMEOUT_MS = 20_000L

@Composable
fun IntroLocationStep(
    uiState: IntroLocationUiState,
    calculationState: CalculationSettingsUiState,
    onQueryChange: (String) -> Unit,
    onQuickCitySelected: (String, String) -> Unit,
    onCitySelected: (CityMatch, String) -> Unit,
    onGpsLocation: (CalculationLocationDetail, String) -> Unit,
    onDialogConfirm: (NewLocationDialogUiState) -> Unit,
    onCalculationAction: (CalculationSettingsUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
    modifier: Modifier = Modifier,
) {
    val language = LocalConfiguration.current.locales[0].language
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var waitingForFix by remember { mutableStateOf(false) }

    val triggerGps = rememberLocationAccessHelperDialogs(
        onLocation = { fix ->
            waitingForFix = false
            onGpsLocation(fix, language)
        },
    )
    LaunchedEffect(waitingForFix) {
        if (waitingForFix) {
            delay(GPS_WAIT_TIMEOUT_MS)
            waitingForFix = false
        }
    }

    if (showManualDialog) {
        NewLocationDialog(
            onAction = { action ->
                showManualDialog = false
                if (action is LocationUiAction.OnNewLocationConfirm) onDialogConfirm(action.state)
            },
            getCountries = getCountries,
            getCities = getCities,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ZenHeroHeader(
            title = "حدد موقعك لمواقيت الصلاة",
            subtitle = "يتم ضبط طريقة الحساب والزوايا الفلكية تلقائياً حسب بلدك",
        )

        // PRIMARY GPS BUTTON (Material 3 Primary Surface, 48.dp+ touch target)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    if (!waitingForFix && !uiState.resolvingGps) {
                        waitingForFix = true
                        triggerGps(true)
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (waitingForFix || uiState.resolvingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.outline_location_searching_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (waitingForFix || uiState.resolvingGps) {
                            "جارٍ تحديد موقعك عبر ${bidiLtr("GPS")}..."
                        } else {
                            "تحديد موقعي تلقائياً (${bidiLtr("GPS")})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "فحص الأذونات وتحديد خط الطول والعرض فوراً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                }
            }
        }

        // OR DIVIDER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "أو اختر مدينتك بالبحث أو القائمة",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }

        // UNIFIED SEARCH & MANUAL SELECTION CARD
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                placeholder = {
                    Text(
                        text = "🔍 اكتب اسم مدينتك أو مركزك...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    when {
                        uiState.searching -> CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        uiState.query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painterResource(R.drawable.baseline_close_24),
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )

            // Quick Suggested Cities Pills (One-tap selection)
            if (uiState.query.isEmpty() && uiState.results.isEmpty()) {
                Spacer(Modifier.height(12.dp))
                val activeDisplay = uiState.selectedLocation?.locationDetail?.toDisplayString().orEmpty()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val quickCities = listOf("القاهرة", "مكة المكرمة", "المدينة المنورة", "الرياض", "إسطنبول", "دبي", "عمان", "الرباط")
                    quickCities.forEach { city ->
                        val isCityActive = activeDisplay.contains(city)
                        Surface(
                            shape = CircleShape,
                            color = if (isCityActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isCityActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .clickable { onQuickCitySelected(city, language) },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    text = city,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCityActive) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.results.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    uiState.results.forEachIndexed { index, result ->
                        CityResultRow(
                            result = result,
                            language = language,
                            onClick = { onCitySelected(result.match, language) },
                        )
                        if (index != uiState.results.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            } else if (uiState.searched && !uiState.searching) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.intro_location_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // MANUAL SELECTION BUTTON (Standard 48.dp touch target, 12.dp corners)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showManualDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "📋 اختيار الدولة والمدينة يدوياً",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "▾",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ACTIVE LOCATION CONFIRMATION BADGE
        val selected = uiState.selectedLocation
        if (selected != null) {
            val loc = selected.locationDetail
            ZenCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                paddingValues = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✓ الموقع النشط: ${loc.toDisplayString()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = bidiLtr(loc.toCoordsString()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ZenBadge(
                        text = "معتمد",
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        textColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        minWidth = 64.dp,
                    )
                }
            }

            // ASTRONOMICAL CALCULATION METHOD CARD
            var calcExpanded by rememberSaveable { mutableStateOf(false) }
            var advancedAnglesExpanded by rememberSaveable { mutableStateOf(false) }
            val resources = LocalResources.current
            val currentMethod = calculationState.calculationParameters?.method
            val methodName = currentMethod?.i18n(resources) ?: stringResource(R.string.calculation_method)
            val fajr = calculationState.calculationParameters?.fajrAngle ?: 0.0
            val ishaAngle = calculationState.calculationParameters?.ishaAngle ?: 0.0
            val ishaInterval = calculationState.calculationParameters?.ishaInterval ?: 0

            ZenCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                borderWidth = 1.dp,
                paddingValues = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "📐 طريقة الحساب الفلكي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ZenBadge(
                        text = "ضبط تلقائي",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        minWidth = 76.dp,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = methodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(2.dp))

                val anglesText = if (ishaInterval > 0) {
                    "الفجر: ${bidiLtr("$fajr°")} | العشاء: $ishaInterval دقيقة | الظهر: زوال الشمس الدقيق"
                } else {
                    "الفجر: ${bidiLtr("$fajr°")} | العشاء: ${bidiLtr("$ishaAngle°")} | الظهر: زوال الشمس الدقيق"
                }
                Text(
                    text = anglesText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { calcExpanded = !calcExpanded },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (calcExpanded) "إغلاق قائمة الهيئات ▴" else "⚙️ تغيير أو تخصيص طريقة الحساب ▾",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                AnimatedVisibility(visible = calcExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "اختر الهيئة أو الطريقة المعتمدة في منطقتك:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val methods = remember {
                            CalculationMethod.entries.filter { it != CalculationMethod.OTHER }
                        }
                        methods.forEach { m ->
                            val isSelected = m == currentMethod
                            val params = m.canonicalParameters()
                            val mIsha = if (params.ishaInterval > 0) "${params.ishaInterval} د" else bidiLtr("${params.ishaAngle}°")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCalculationAction(CalculationSettingsUiAction.OnCalculationMethodChange(m))
                                    },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = m.i18n(resources),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        text = "الفجر: ${bidiLtr("${params.fajrAngle}°")} | العشاء: $mIsha",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { advancedAnglesExpanded = !advancedAnglesExpanded },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = if (advancedAnglesExpanded) {
                                        "إخفاء الإعدادات المتقدمة للزوايا ▴"
                                    } else {
                                        "تخصيص الزوايا والمذهب والتعديلات الدقيقة ▾"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        AnimatedVisibility(visible = advancedAnglesExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                CalculationSettingsScreen(
                                    uiState = calculationState,
                                    onAction = onCalculationAction,
                                    adjustmentsRoute = Route.Intro.Calculation.Adjustments,
                                    advancedRoute = Route.Intro.Calculation.AdvancedCalculation,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            ZenCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
            ) {
                Text(
                    text = stringResource(R.string.intro_location_none_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CityResultRow(
    result: IntroCityResult,
    language: String,
    onClick: () -> Unit,
) {
    val country = result.country
    val countryName = if (country != null) {
        pickNameForLanguage(country.names, country.name, language)
    } else {
        result.match.city.country
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = result.match.matchedName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = countryName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

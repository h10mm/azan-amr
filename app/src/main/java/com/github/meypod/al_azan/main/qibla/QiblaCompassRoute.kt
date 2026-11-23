package com.github.meypod.al_azan.main.qibla

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.CompassState
import com.github.meypod.al_azan.core.domain.repository.CompassDiagnostic
import com.github.meypod.al_azan.core.util.android.LocationUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Route for the Qibla Compass screen.
 * Handles lifecycle and sensor collection.
 */
@Composable
fun QiblaCompassRoute(
    uiState: QiblaCompassUiState,
    compassState: StateFlow<CompassState>,
    diagnostics: StateFlow<CompassDiagnostic?>,
    onAction: (QiblaCompassUiAction) -> Unit,
) {
    val heading = remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableStateOf(CompassAccuracy.UNRELIABLE) }
    var diagnosticData by remember { mutableStateOf<CompassDiagnostic?>(null) }
    var sensorSource by remember { mutableStateOf("None") }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(compassState, diagnostics, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                compassState.collect { state ->
                    heading.floatValue = state.currentReading.trueHeadingDegrees
                    accuracy = state.currentReading.accuracy
                    sensorSource = state.currentReading.sensorSource.name
                    isCalibrating = state.isCalibrating
                    calibrationProgress = state.calibrationProgress ?: 0f
                }
            }
            launch {
                diagnostics.collect { diag ->
                    diagnosticData = diag
                }
            }
        }
    }

    QiblaCompassScreen(
        uiState = uiState,
        accuracy = accuracy,
        diagnostic = diagnosticData,
        headingProvider = { heading.floatValue },
        sensorSource = sensorSource,
        isCalibrating = isCalibrating,
        calibrationProgress = calibrationProgress,
        onAction = onAction,
    )
}

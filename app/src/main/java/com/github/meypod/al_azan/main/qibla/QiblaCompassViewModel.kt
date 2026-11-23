package com.github.meypod.al_azan.main.qibla

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.toCoordinates
import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import com.github.meypod.al_azan.core.domain.model.compass.CompassState
import com.github.meypod.al_azan.core.domain.model.compass.QiblaCalculator
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CompassDiagnostic
import com.github.meypod.al_azan.core.domain.repository.CompassRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.android.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.meypod.adhan_kotlin.Qibla
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel for the Qibla Compass screen.
 *
 * Uses the selected city's coordinates from settings (or the last cached device location offline
 * without turning on GPS) to compute both Qibla bearing and GeomagneticField declination.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QiblaCompassViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    calculationSettingsRepository: CalculationSettingsRepository,
    favoriteLocationsRepository: FavoriteLocationsRepository,
    private val compassRepository: CompassRepository,
) : ViewModel() {

    /** A GPS location fetched explicitly on this screen; overrides settings/cached locations. */
    private val fetchedLocation = MutableStateFlow<CalculationLocationDetail?>(null)
    private val orientationLocked = MutableStateFlow(true)
    private val showDiagnostics = MutableStateFlow(false)

    private val cachedLocation: CalculationLocationDetail? by lazy {
        LocationUtils.getLastCachedLocation(context)?.let { loc ->
            CalculationLocationDetail(lat = loc.latitude, long = loc.longitude)
        }
    }

    private val settingsLocation: StateFlow<CalculationLocationDetail?> =
        combine(
            calculationSettingsRepository.data,
            favoriteLocationsRepository.data,
        ) { calcSettings, locations ->
            locations.firstOrNull { it.id == calcSettings.locationId }?.locationDetail
                ?: locations.firstOrNull()?.locationDetail
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Resolved location priority:
     * 1. Explicitly fetched GPS fix on this screen
     * 2. App's selected city (offline)
     * 3. Last cached device location in memory (offline, no GPS radio activation)
     */
    private val activeLocation: StateFlow<Pair<CalculationLocationDetail?, QiblaLocationLabel>> =
        combine(fetchedLocation, settingsLocation) { fetched, fromSettings ->
            when {
                fetched != null -> fetched to QiblaLocationLabel.Fetched(fetched)
                fromSettings != null -> fromSettings to QiblaLocationLabel.FromSettings
                cachedLocation != null -> cachedLocation!! to QiblaLocationLabel.Cached(cachedLocation!!)
                else -> null to QiblaLocationLabel.None
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null to QiblaLocationLabel.None)

    val uiState: StateFlow<QiblaCompassUiState> =
        combine(activeLocation, orientationLocked, showDiagnostics) { (location, label), locked, showDiag ->
            QiblaCompassUiState(
                qiblaDegrees = location?.let {
                    Qibla(it.toCoordinates()).direction.toFloat()
                },
                locationLabel = label,
                isOrientationLocked = locked,
                showDiagnostics = showDiag,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaCompassUiState())

    val diagnostics: StateFlow<CompassDiagnostic?> = compassRepository.diagnostics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Full compass state with Qibla information.
     * This is the primary state flow for the compass UI.
     * Collected by the screen with lifecycle awareness.
     */
    val compassState: StateFlow<CompassState> =
        activeLocation
            .map { (location, _) -> location }
            .distinctUntilChanged()
            .flatMapLatest { location ->
                val qiblaBearing = location?.let {
                    QiblaCalculator.calculateQiblaBearing(it.lat, it.long)
                }
                compassRepository.compassState(
                    latitude = location?.lat,
                    longitude = location?.long,
                    qiblaBearing = qiblaBearing
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompassState.UNAVAILABLE)

    /**
     * Legacy: Live compass readings, re-subscribed whenever the active location changes.
     * Collected by the screen with lifecycle awareness.
     */
    val readings: StateFlow<CompassReading?> =
        activeLocation
            .map { (location, _) -> location }
            .distinctUntilChanged()
            .flatMapLatest { location -> compassRepository.headings(location?.lat, location?.long) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onAction(action: QiblaCompassUiAction) {
        when (action) {
            QiblaCompassUiAction.OnToggleOrientationLock -> onToggleOrientationLock()
            is QiblaCompassUiAction.OnLocationFetched -> onLocationFetched(action)
            QiblaCompassUiAction.OnDiagnosticsClick -> showDiagnostics.value = true
            QiblaCompassUiAction.OnDismissDiagnostics -> showDiagnostics.value = false
        }
    }

    private fun onToggleOrientationLock() {
        orientationLocked.update { !it }
    }

    private fun onLocationFetched(action: QiblaCompassUiAction.OnLocationFetched) {
        fetchedLocation.value = action.detail
    }
    
    /**
     * Start compass calibration.
     */
    suspend fun startCalibration() {
        compassRepository.startCalibration()
    }
    
    /**
     * Stop compass calibration.
     */
    suspend fun stopCalibration() {
        compassRepository.stopCalibration()
    }
    
    /**
     * Check if compass is available.
     */
    suspend fun isCompassAvailable(): Boolean {
        return compassRepository.isCompassAvailable()
    }
    
    /**
     * Force sensor re-detection.
     */
    suspend fun redetectSensors() {
        compassRepository.redetectSensors()
    }
}

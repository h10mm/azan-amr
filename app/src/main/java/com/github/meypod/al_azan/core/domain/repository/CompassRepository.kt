package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import com.github.meypod.al_azan.core.domain.model.compass.CompassState
import com.github.meypod.al_azan.core.domain.model.compass.SensorSource
import kotlinx.coroutines.flow.Flow

/**
 * Interface for compass functionality.
 * Provides access to compass readings and state information.
 */
interface CompassRepository {

    /**
     * Emits diagnostic information about available sensors.
     */
    val diagnostics: Flow<CompassDiagnostic>

    /**
     * Emits complete compass state including Qibla information.
     * 
     * @param latitude User's latitude for declination calculation (optional)
     * @param longitude User's longitude for declination calculation (optional)
     * @param qiblaBearing Optional pre-calculated Qibla bearing (if known from other source)
     */
    fun compassState(
        latitude: Double? = null,
        longitude: Double? = null,
        qiblaBearing: Double? = null,
    ): Flow<CompassState>

    /**
     * Emits compass readings (legacy compatibility).
     * 
     * When [latitude]/[longitude] are provided, headings are corrected to true north
     * using the magnetic declination at that location; otherwise raw magnetic headings are emitted.
     */
    fun headings(
        latitude: Double? = null,
        longitude: Double? = null,
    ): Flow<CompassReading>

    /**
     * Force a re-detection of available sensors.
     * Useful when device configuration changes.
     */
    suspend fun redetectSensors()

    /**
     * Check if compass is available on this device.
     */
    suspend fun isCompassAvailable(): Boolean

    /**
     * Get current sensor source being used.
     */
    suspend fun getCurrentSensorSource(): SensorSource

    /**
     * Start calibration mode.
     * The compass will emit CALIBRATING state until calibration is complete.
     */
    suspend fun startCalibration()

    /**
     * Stop calibration mode.
     */
    suspend fun stopCalibration()
}

/**
 * Diagnostic information about compass sensors.
 */
data class CompassDiagnostic(
    val hasRotationVector: Boolean,
    val hasAccelerometer: Boolean,
    val hasMagnetometer: Boolean,
    val hasGyroscope: Boolean,
    val registeredRotationVector: Boolean,
    val registeredAccelerometer: Boolean,
    val registeredMagnetometer: Boolean,
    val registeredGyroscope: Boolean,
    val hasGeomagneticRotationVector: Boolean = false,
    val hasGameRotationVector: Boolean = false,
    val hasGravity: Boolean = false,
    val hasLinearAcceleration: Boolean = false,
    val hasOrientation: Boolean = false,
)

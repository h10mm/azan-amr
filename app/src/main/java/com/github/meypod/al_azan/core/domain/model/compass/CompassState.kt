package com.github.meypod.al_azan.core.domain.model.compass

import androidx.compose.runtime.Immutable

/**
 * Complete state of the compass system.
 * Contains all information needed for Qibla compass UI.
 *
 * @param currentReading The latest compass reading
 * @param qiblaBearing The bearing (azimuth) from current location to Kaaba in degrees (0-360)
 * @param relativeQiblaAngle The angle difference between current heading and Qibla direction (-180 to 180)
 * @param magneticDeclination Current magnetic declination in degrees (can be positive or negative)
 * @param sensorAvailability Information about which sensors are available on the device
 * @param isCalibrating Whether the compass is currently calibrating
 * @param calibrationProgress Calibration progress (0.0 to 1.0) if calibrating, null otherwise
 * @param errorMessage Error message to display to user, null if no error
 * @param warningMessage Warning message to display to user, null if no warning
 * @param lastUpdateTimestamp Timestamp of last successful reading
 */
@Immutable
data class CompassState(
    val currentReading: CompassReading,
    val qiblaBearing: Double? = null,
    val relativeQiblaAngle: Double? = null,
    val magneticDeclination: Float = 0f,
    val sensorAvailability: SensorAvailability = SensorAvailability(),
    val isCalibrating: Boolean = false,
    val calibrationProgress: Float? = null,
    val errorMessage: String? = null,
    val warningMessage: String? = null,
    val lastUpdateTimestamp: Long = 0L,
) {
    /**
     * Check if compass is available and providing usable readings
     */
    val isAvailable: Boolean
        get() = currentReading.isValid && sensorAvailability.hasAnyCompassSensor
    
    /**
     * Check if we have all information needed for Qibla calculation
     */
    val hasQiblaInformation: Boolean
        get() = qiblaBearing != null && relativeQiblaAngle != null && currentReading.isValid
    
    /**
     * Check if compass is facing Qibla direction (within threshold)
     */
    fun isFacingQibla(thresholdDegrees: Float = 5f): Boolean {
        if (relativeQiblaAngle == null) return false
        return kotlin.math.abs(relativeQiblaAngle) <= thresholdDegrees
    }
    
    /**
     * Get the deviation from Qibla in degrees (always positive, 0 to 180)
     */
    fun getQiblaDeviation(): Float {
        if (relativeQiblaAngle == null) return Float.POSITIVE_INFINITY
        return kotlin.math.abs(relativeQiblaAngle.toFloat())
    }
    
    /**
     * Create an unavailable state
     */
    companion object {
        val UNAVAILABLE = CompassState(
            currentReading = CompassReading.UNAVAILABLE,
            sensorAvailability = SensorAvailability(),
            errorMessage = "Compass unavailable",
        )
        
        val CALIBRATING = CompassState(
            currentReading = CompassReading.CALIBRATING,
            sensorAvailability = SensorAvailability(),
            isCalibrating = true,
            calibrationProgress = 0f,
        )
    }
}

/**
 * Information about sensor availability on the device
 */
@Immutable
data class SensorAvailability(
    val hasRotationVector: Boolean = false,
    val hasGeomagneticRotationVector: Boolean = false,
    val hasGameRotationVector: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasGravity: Boolean = false,
    val hasLinearAcceleration: Boolean = false,
    val hasOrientation: Boolean = false,
) {
    /**
     * Check if device has any sensor that can provide compass functionality
     */
    val hasAnyCompassSensor: Boolean
        get() = hasRotationVector || 
                hasGeomagneticRotationVector || 
                (hasAccelerometer && hasMagnetometer) ||
                hasOrientation
    
    /**
     * Check if device has high-quality compass sensors
     */
    val hasHighQualityCompass: Boolean
        get() = hasRotationVector || hasGeomagneticRotationVector
    
    /**
     * Get the best available sensor source
     */
    fun getBestSensorSource(): SensorSource {
        if (hasRotationVector) return SensorSource.ROTATION_VECTOR
        if (hasGeomagneticRotationVector) return SensorSource.GEOMAGNETIC_ROTATION_VECTOR
        if (hasAccelerometer && hasMagnetometer) return SensorSource.ACCELEROMETER_MAGNETOMETER
        if (hasOrientation) return SensorSource.ORIENTATION
        return SensorSource.NONE
    }
    
    /**
     * Get list of all available sensor sources in priority order
     */
    fun getAvailableSources(): List<SensorSource> {
        val sources = mutableListOf<SensorSource>()
        if (hasRotationVector) sources.add(SensorSource.ROTATION_VECTOR)
        if (hasGeomagneticRotationVector) sources.add(SensorSource.GEOMAGNETIC_ROTATION_VECTOR)
        if (hasAccelerometer && hasMagnetometer) sources.add(SensorSource.ACCELEROMETER_MAGNETOMETER)
        if (hasAccelerometer && hasMagnetometer && hasGyroscope) {
            sources.add(SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE)
        }
        if (hasOrientation) sources.add(SensorSource.ORIENTATION)
        if (hasGameRotationVector) sources.add(SensorSource.GAME_ROTATION_VECTOR)
        return sources
    }
    
    companion object {
        val EMPTY = SensorAvailability()
    }
}

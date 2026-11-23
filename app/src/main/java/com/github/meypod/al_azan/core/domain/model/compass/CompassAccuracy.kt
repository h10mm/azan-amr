package com.github.meypod.al_azan.core.domain.model.compass

import androidx.compose.runtime.Immutable

/**
 * Enhanced compass accuracy states with detailed calibration information.
 */
@Immutable
enum class CompassAccuracy {
    /**
     * Device has no suitable sensors for compass functionality.
     * Compass is completely unavailable.
     */
    NO_SENSOR,
    
    /**
     * Sensors are available but not yet calibrated or providing reliable data.
     * User should perform figure-8 calibration motion.
     */
    CALIBRATING,
    
    /**
     * Sensor data is unreliable, possibly due to magnetic interference.
     * User should move away from magnetic sources and recalibrate.
     */
    UNRELIABLE,
    
    /**
     * Low accuracy readings - significant drift or noise.
     * Compass may be usable but with reduced precision.
     */
    LOW,
    
    /**
     * Medium accuracy readings - moderate drift or noise.
     * Suitable for general navigation but may have some jitter.
     */
    MEDIUM,
    
    /**
     * High accuracy readings - stable and precise.
     * Best quality compass data.
     */
    HIGH,
    
    /**
     * Magnetic field interference detected.
     * User should move away from electronic devices, magnets, or metal structures.
     */
    MAGNETIC_INTERFERENCE,
    
    /**
     * Sensor is temporarily unavailable (e.g., disabled by system).
     * May become available again.
     */
    TEMPORARILY_UNAVAILABLE;
    
    /**
     * Check if compass is in a usable state (not unavailable or calibrating)
     */
    val isUsable: Boolean
        get() = this != NO_SENSOR && this != CALIBRATING && this != TEMPORARILY_UNAVAILABLE
    
    /**
     * Check if accuracy is sufficient for Qibla direction
     */
    val isSufficientForQibla: Boolean
        get() = this == HIGH || this == MEDIUM
    
    /**
     * Convert Android sensor status to CompassAccuracy
     */
    companion object {
        fun fromSensorStatus(status: Int): CompassAccuracy = when (status) {
            android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE -> UNRELIABLE
            android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW -> LOW
            android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> MEDIUM
            android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> HIGH
            else -> LOW
        }
        
        /**
         * Get worst accuracy from multiple sources
         */
        fun worstOf(vararg accuracies: CompassAccuracy): CompassAccuracy {
            return accuracies.minByOrNull { it.ordinal } ?: NO_SENSOR
        }
        
        /**
         * Get best accuracy from multiple sources
         */
        fun bestOf(vararg accuracies: CompassAccuracy): CompassAccuracy {
            return accuracies.maxByOrNull { it.ordinal } ?: NO_SENSOR
        }
    }
}

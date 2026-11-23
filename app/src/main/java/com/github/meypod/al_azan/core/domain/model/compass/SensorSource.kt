package com.github.meypod.al_azan.core.domain.model.compass

import androidx.compose.runtime.Immutable

/**
 * Represents the source of compass sensor data.
 * Used to track which sensor combination is currently providing orientation data.
 */
@Immutable
enum class SensorSource {
    /** No sensor available */
    NONE,
    
    /** Primary: TYPE_ROTATION_VECTOR sensor */
    ROTATION_VECTOR,
    
    /** Secondary: TYPE_GEOMAGNETIC_ROTATION_VECTOR sensor */
    GEOMAGNETIC_ROTATION_VECTOR,
    
    /** Tertiary: TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD fusion */
    ACCELEROMETER_MAGNETOMETER,
    
    /** Fallback: TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD + TYPE_GYROSCOPE */
    ACCELEROMETER_MAGNETOMETER_GYROSCOPE,
    
    /** Legacy: TYPE_ORIENTATION (deprecated, fallback only) */
    ORIENTATION,
    
    /** Game rotation vector (does NOT provide absolute magnetic north) */
    GAME_ROTATION_VECTOR,
    
    /** Only accelerometer available */
    ACCELEROMETER_ONLY,
    
    /** Only magnetometer available */
    MAGNETOMETER_ONLY,
    
    /** Simulated/fallback for testing */
    SIMULATED;
    
    /**
     * Check if this source provides absolute orientation (true north reference)
     */
    val providesAbsoluteOrientation: Boolean
        get() = when (this) {
            ROTATION_VECTOR -> true
            GEOMAGNETIC_ROTATION_VECTOR -> true
            ACCELEROMETER_MAGNETOMETER -> true
            ACCELEROMETER_MAGNETOMETER_GYROSCOPE -> true
            ORIENTATION -> true
            GAME_ROTATION_VECTOR -> false
            ACCELEROMETER_ONLY -> false
            MAGNETOMETER_ONLY -> false
            NONE -> false
            SIMULATED -> false
        }
    
    /**
     * Check if this source uses magnetometer data
     */
    val usesMagnetometer: Boolean
        get() = when (this) {
            ROTATION_VECTOR -> true
            GEOMAGNETIC_ROTATION_VECTOR -> true
            ACCELEROMETER_MAGNETOMETER -> true
            ACCELEROMETER_MAGNETOMETER_GYROSCOPE -> true
            ORIENTATION -> true
            GAME_ROTATION_VECTOR -> false
            ACCELEROMETER_ONLY -> false
            MAGNETOMETER_ONLY -> true
            NONE -> false
            SIMULATED -> false
        }
}

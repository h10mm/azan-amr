package com.github.meypod.al_azan.core.domain.model.compass

import androidx.compose.runtime.Immutable

/**
 * A comprehensive compass reading with all necessary information.
 *
 * @param magneticHeadingDegrees Raw magnetic heading (0-360 degrees), relative to magnetic north
 * @param trueHeadingDegrees Heading corrected for magnetic declination (0-360 degrees), relative to true north
 * @param accuracy Current accuracy state of the compass
 * @param sensorSource Which sensor or sensor combination provided this reading
 * @param timestamp Timestamp of this reading in nanoseconds (System.nanoTime())
 * @param isFiltered Whether this reading has been filtered/smoothed
 */
@Immutable
data class CompassReading(
    val magneticHeadingDegrees: Float,
    val trueHeadingDegrees: Float,
    val accuracy: CompassAccuracy,
    val sensorSource: SensorSource = SensorSource.NONE,
    val timestamp: Long = 0L,
    val isFiltered: Boolean = false,
) {
    /**
     * Legacy constructor for backward compatibility.
     * Assumes heading is already true heading (magnetic declination applied).
     */
    @Deprecated("Use primary constructor with explicit magnetic and true headings")
    constructor(
        headingDegrees: Float,
        accuracy: CompassAccuracy,
    ) : this(
        magneticHeadingDegrees = headingDegrees,
        trueHeadingDegrees = headingDegrees,
        accuracy = accuracy,
        sensorSource = SensorSource.NONE,
        timestamp = 0L,
        isFiltered = false,
    )
    
    /**
     * Check if this reading is valid and usable
     */
    val isValid: Boolean
        get() = accuracy.isUsable && 
                magneticHeadingDegrees.isFinite() && 
                trueHeadingDegrees.isFinite()
    
    /**
     * Create an unavailable/unreliable reading
     */
    companion object {
        val UNAVAILABLE = CompassReading(
            magneticHeadingDegrees = 0f,
            trueHeadingDegrees = 0f,
            accuracy = CompassAccuracy.NO_SENSOR,
            sensorSource = SensorSource.NONE,
        )
        
        val CALIBRATING = CompassReading(
            magneticHeadingDegrees = 0f,
            trueHeadingDegrees = 0f,
            accuracy = CompassAccuracy.CALIBRATING,
            sensorSource = SensorSource.NONE,
        )
    }
}

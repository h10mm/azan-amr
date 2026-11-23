package com.github.meypod.al_azan.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.QiblaCalculator
import com.github.meypod.al_azan.core.domain.model.compass.SensorAvailability
import com.github.meypod.al_azan.core.domain.model.compass.SensorSource
import kotlin.math.abs

/**
 * Standard priority-based sensor fusion coordinator:
 * 1. TYPE_ROTATION_VECTOR (hardware Kalman fusion)
 * 2. TYPE_GEOMAGNETIC_ROTATION_VECTOR (gyro-less fallback)
 * 3. TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD (legacy fallback via getRotationMatrix)
 * 4. TYPE_ORIENTATION (ultra-legacy fallback)
 */
class CompassSensorFusion(
    context: Context,
) {
    private val sensorManager = CompassSensorManager(context)
    private val eventProcessor = CompassSensorEventProcessor(context)

    private var currentSource: SensorSource = SensorSource.NONE
    private var desiredSource: SensorSource = SensorSource.NONE
    private var availableSources: List<SensorSource> = emptyList()

    private var currentMagneticHeading: Float = 0f
    private var currentTrueHeading: Float = 0f
    private var magneticDeclination: Float = 0f
    private var lastValidTimestamp: Long = 0L
    private var lastValidMagneticHeading: Float = 0f
    private var lastValidTrueHeading: Float = 0f

    private var isSwitchingSource: Boolean = false
    private var switchStartTimestamp: Long = 0L
    private var switchStartHeading: Float = 0f

    private var currentAccuracy: CompassAccuracy = CompassAccuracy.CALIBRATING
    private var sourceAccuracy: Map<SensorSource, CompassAccuracy> = emptyMap()
    private var hasMagneticInterference: Boolean = false

    fun initialize(availability: SensorAvailability) {
        availableSources = sensorManager.getAvailableSources(availability)
        desiredSource = sensorManager.getBestSensorSource(availability)
        currentSource = SensorSource.NONE
        currentAccuracy = CompassAccuracy.CALIBRATING
    }

    fun onAccuracyChanged(sensorType: Int, accuracy: Int) {
        eventProcessor.onAccuracyChanged(sensorType, accuracy)
        val source = getSensorSourceForType(sensorType)
        if (source != SensorSource.NONE) {
            sourceAccuracy = sourceAccuracy.toMutableMap().apply {
                put(source, eventProcessor.getCompassAccuracy(accuracy))
            }
        }
    }

    fun processEvent(
        event: SensorEvent,
        timestamp: Long,
    ): Result? {
        val sourceForEvent = getSensorSourceForType(event.sensor.type)

        if (sourceForEvent != SensorSource.NONE) {
            sourceAccuracy = sourceAccuracy.toMutableMap().apply {
                put(sourceForEvent, eventProcessor.getCompassAccuracy(event.accuracy))
            }
        }

        val azimuthRadians = eventProcessor.processEvent(event, sourceForEvent)
        hasMagneticInterference = eventProcessor.hasMagneticInterference()

        if (azimuthRadians == null) return null

        val magneticHeading = QiblaCalculator.normalizeAngleDegrees(
            Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        )
        val trueHeading = QiblaCalculator.normalizeAngleDegrees(
            magneticHeading + magneticDeclination
        )

        val effectiveSource = determineEffectiveSource(sourceForEvent, timestamp)
        if (effectiveSource != currentSource) {
            handleSourceSwitch(effectiveSource, magneticHeading, timestamp)
        }

        currentMagneticHeading = magneticHeading
        currentTrueHeading = trueHeading
        currentSource = effectiveSource
        lastValidTimestamp = timestamp
        lastValidMagneticHeading = magneticHeading
        lastValidTrueHeading = trueHeading
        currentAccuracy = determineCurrentAccuracy()

        return Result(
            magneticHeading = magneticHeading,
            trueHeading = trueHeading,
            source = effectiveSource,
            accuracy = currentAccuracy,
            timestamp = timestamp,
            hasMagneticInterference = hasMagneticInterference,
        )
    }

    fun setMagneticDeclination(declination: Float) {
        magneticDeclination = declination
    }

    fun getCurrentState(): State {
        return State(
            source = currentSource,
            desiredSource = desiredSource,
            availableSources = availableSources,
            magneticHeading = currentMagneticHeading,
            trueHeading = currentTrueHeading,
            accuracy = currentAccuracy,
            magneticDeclination = magneticDeclination,
            hasMagneticInterference = hasMagneticInterference,
            lastValidTimestamp = lastValidTimestamp,
            isSwitchingSource = isSwitchingSource,
        )
    }

    private fun getSensorSourceForType(sensorType: Int): SensorSource {
        return when (sensorType) {
            Sensor.TYPE_ROTATION_VECTOR -> SensorSource.ROTATION_VECTOR
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorSource.GEOMAGNETIC_ROTATION_VECTOR
            Sensor.TYPE_ACCELEROMETER -> SensorSource.ACCELEROMETER_ONLY
            Sensor.TYPE_MAGNETIC_FIELD -> SensorSource.MAGNETOMETER_ONLY
            @Suppress("DEPRECATION")
            Sensor.TYPE_ORIENTATION -> SensorSource.ORIENTATION
            else -> SensorSource.NONE
        }
    }

    private fun determineEffectiveSource(
        sourceForEvent: SensorSource,
        timestamp: Long,
    ): SensorSource {
        if (isSwitchingSource && timestamp - switchStartTimestamp < 2_000_000_000L) {
            return desiredSource
        }
        if (sourceForEvent == desiredSource) {
            return desiredSource
        }

        val desiredPriority = getSourcePriority(desiredSource)
        val eventPriority = getSourcePriority(sourceForEvent)
        if (eventPriority < desiredPriority) {
            return sourceForEvent
        }

        return if (currentSource != SensorSource.NONE && getSourcePriority(currentSource) < desiredPriority) {
            currentSource
        } else {
            desiredSource
        }
    }

    private fun handleSourceSwitch(
        newSource: SensorSource,
        newHeading: Float,
        timestamp: Long,
    ) {
        if (newSource == currentSource) return
        if (timestamp - switchStartTimestamp < 1_000_000_000L) return

        isSwitchingSource = true
        switchStartTimestamp = timestamp
        switchStartHeading = currentMagneticHeading

        val headingDiff = QiblaCalculator.normalizeAngle180(newHeading - currentMagneticHeading)
        if (abs(headingDiff) > 90f) {
            currentMagneticHeading = newHeading
        }

        currentSource = newSource
        desiredSource = newSource
        isSwitchingSource = false
    }

    private fun getSourcePriority(source: SensorSource): Int {
        return when (source) {
            SensorSource.ROTATION_VECTOR -> 0
            SensorSource.GEOMAGNETIC_ROTATION_VECTOR -> 1
            SensorSource.ACCELEROMETER_MAGNETOMETER -> 2
            SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE -> 3
            SensorSource.ORIENTATION -> 4
            SensorSource.GAME_ROTATION_VECTOR -> 5
            SensorSource.ACCELEROMETER_ONLY -> 6
            SensorSource.MAGNETOMETER_ONLY -> 7
            SensorSource.NONE -> 100
            SensorSource.SIMULATED -> 101
        }
    }

    private fun determineCurrentAccuracy(): CompassAccuracy {
        if (hasMagneticInterference) {
            return CompassAccuracy.MAGNETIC_INTERFERENCE
        }
        if (currentSource == SensorSource.NONE) {
            return CompassAccuracy.NO_SENSOR
        }

        val sourceAcc = sourceAccuracy[currentSource]
        val magAcc = if (eventProcessor.hasObservedMagnetometerAccuracy()) {
            eventProcessor.getCompassAccuracy(eventProcessor.getMagnetometerAccuracy())
        } else {
            null
        }

        return when {
            sourceAcc != null && magAcc != null -> CompassAccuracy.worstOf(sourceAcc, magAcc)
            sourceAcc != null -> sourceAcc
            magAcc != null -> magAcc
            else -> CompassAccuracy.MEDIUM
        }
    }

    fun isAvailable(): Boolean = currentSource != SensorSource.NONE

    fun hasValidHeading(): Boolean = lastValidTimestamp > 0

    fun getLastValidHeadings(): Pair<Float, Float> = lastValidMagneticHeading to lastValidTrueHeading

    fun reset() {
        currentSource = SensorSource.NONE
        desiredSource = SensorSource.NONE
        availableSources = emptyList()
        currentMagneticHeading = 0f
        currentTrueHeading = 0f
        magneticDeclination = 0f
        lastValidTimestamp = 0L
        lastValidMagneticHeading = 0f
        lastValidTrueHeading = 0f
        isSwitchingSource = false
        switchStartTimestamp = 0L
        switchStartHeading = 0f
        currentAccuracy = CompassAccuracy.CALIBRATING
        sourceAccuracy = emptyMap()
        hasMagneticInterference = false
        eventProcessor.reset()
    }

    data class Result(
        val magneticHeading: Float,
        val trueHeading: Float,
        val source: SensorSource,
        val accuracy: CompassAccuracy,
        val timestamp: Long,
        val hasMagneticInterference: Boolean,
    )

    data class State(
        val source: SensorSource,
        val desiredSource: SensorSource,
        val availableSources: List<SensorSource>,
        val magneticHeading: Float,
        val trueHeading: Float,
        val accuracy: CompassAccuracy,
        val magneticDeclination: Float,
        val hasMagneticInterference: Boolean,
        val lastValidTimestamp: Long,
        val isSwitchingSource: Boolean,
    )
}

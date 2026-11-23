package com.github.meypod.al_azan.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.SensorSource
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Clean standard sensor processor using Android hardware sensor fusion with 3D horizontal
 * vector projection (swing-twist tilt compensation).
 *
 * Hierarchy:
 * 1. [Sensor.TYPE_ROTATION_VECTOR] — direct hardware-fused quaternion/rotation matrix.
 * 2. [Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR] — automatic fallback for gyro-less devices.
 * 3. [Sensor.TYPE_ACCELEROMETER] + [Sensor.TYPE_MAGNETIC_FIELD] via [SensorManager.getRotationMatrix]
 *    — fallback for legacy devices lacking composite rotation vector sensors.
 * 4. [Sensor.TYPE_ORIENTATION] — last-resort legacy fallback.
 *
 * [Sensor.TYPE_MAGNETIC_FIELD] is also monitored passively to detect nearby magnetic/metallic
 * interference (< 20 μT or > 75 μT) and calibration status without altering the hardware rotation matrix.
 */
class CompassSensorEventProcessor(
    context: Context,
) {
    private val appContext = context.applicationContext

    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val rotationVec4 = FloatArray(4)

    // Smoothed vectors used only for Tier-2 (Accelerometer + Magnetometer) fallback
    private val accelVector = FloatArray(3)
    private val magVector = FloatArray(3)
    private var hasAccel = false
    private var hasMag = false

    // Passive ambient field monitoring for calibration & interference prompts
    private val lastMagneticFieldValues = FloatArray(3)
    private var magnetometerAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
    private var hasMagnetometerAccuracy = false

    private var lastRvTimestampNs = 0L
    private var lastGeoMagRvTimestampNs = 0L
    private var lastMagEmitTimestampNs = 0L

    fun processEvent(
        event: SensorEvent,
        activeSource: SensorSource,
    ): Float? {
        val nowNs = if (event.timestamp > 0L) event.timestamp else System.nanoTime()

        return when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> processRotationVector(event, nowNs)
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> processGeomagneticRotationVector(event, nowNs)
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event, nowNs)
            Sensor.TYPE_MAGNETIC_FIELD -> processMagneticField(event, nowNs)
            @Suppress("DEPRECATION")
            Sensor.TYPE_ORIENTATION -> processOrientation(event, nowNs)
            else -> null
        }
    }

    fun onAccuracyChanged(sensorType: Int, accuracy: Int) {
        if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerAccuracy = accuracy
            hasMagnetometerAccuracy = true
        }
    }

    private fun processRotationVector(event: SensorEvent, nowNs: Long): Float? {
        val matrix = toRotationMatrix(event.values) ?: return null
        val azimuthRad = extractAzimuthFromRotationMatrix(matrix) ?: return null
        lastRvTimestampNs = nowNs
        return azimuthRad
    }

    private fun processGeomagneticRotationVector(event: SensorEvent, nowNs: Long): Float? {
        val matrix = toRotationMatrix(event.values) ?: return null
        val azimuthRad = extractAzimuthFromRotationMatrix(matrix) ?: return null
        lastGeoMagRvTimestampNs = nowNs

        // Only emit as active fallback when hardware Rotation Vector is absent or stale
        return if (nowNs - lastRvTimestampNs > FALLBACK_STALE_NS) azimuthRad else null
    }

    private fun processAccelerometer(event: SensorEvent, nowNs: Long): Float? {
        val values = event.values
        if (values.size < 3 || !values.all { it.isFinite() }) return null
        val norm = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        if (norm < 1.0f) return null

        if (!hasAccel) {
            values.copyInto(accelVector, endIndex = 3)
            hasAccel = true
        } else {
            val alpha = FALLBACK_VECTOR_ALPHA
            accelVector[0] += alpha * (values[0] - accelVector[0])
            accelVector[1] += alpha * (values[1] - accelVector[1])
            accelVector[2] += alpha * (values[2] - accelVector[2])
        }

        return emitAccelMagFallbackIfNeeded(nowNs)
    }

    private fun processMagneticField(event: SensorEvent, nowNs: Long): Float? {
        val values = event.values
        if (values.size < 3 || !values.all { it.isFinite() }) return null
        if (values[0] == 0f && values[1] == 0f && values[2] == 0f) return null

        values.copyInto(lastMagneticFieldValues, endIndex = 3)
        magnetometerAccuracy = event.accuracy
        hasMagnetometerAccuracy = true

        if (!hasMag) {
            values.copyInto(magVector, endIndex = 3)
            hasMag = true
        } else {
            val alpha = FALLBACK_VECTOR_ALPHA
            magVector[0] += alpha * (values[0] - magVector[0])
            magVector[1] += alpha * (values[1] - magVector[1])
            magVector[2] += alpha * (values[2] - magVector[2])
        }

        return emitAccelMagFallbackIfNeeded(nowNs)
    }

    private fun emitAccelMagFallbackIfNeeded(nowNs: Long): Float? {
        if (!hasAccel || !hasMag) return null
        if (nowNs - lastRvTimestampNs <= FALLBACK_STALE_NS) return null
        if (nowNs - lastGeoMagRvTimestampNs <= FALLBACK_STALE_NS) return null
        if (nowNs - lastMagEmitTimestampNs < FALLBACK_MIN_EMIT_INTERVAL_NS) return null

        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelVector, magVector)) {
            return null
        }
        lastMagEmitTimestampNs = nowNs
        return extractAzimuthFromRotationMatrix(rotationMatrix)
    }

    private fun processOrientation(event: SensorEvent, nowNs: Long): Float? {
        val values = event.values
        if (values.size < 3 || !values.all { it.isFinite() }) return null
        if (nowNs - lastRvTimestampNs <= FALLBACK_STALE_NS) return null
        if (nowNs - lastGeoMagRvTimestampNs <= FALLBACK_STALE_NS) return null
        if (nowNs - lastMagEmitTimestampNs <= FALLBACK_STALE_NS) return null
        return Math.toRadians(values[0].toDouble()).toFloat()
    }

    private fun toRotationMatrix(values: FloatArray): FloatArray? {
        if (values.size < 3 || !values.all { it.isFinite() }) return null
        if (values[0] == 0f && values[1] == 0f && values[2] == 0f && (values.size < 4 || values[3] == 0f)) {
            return null
        }
        val count = minOf(values.size, 4)
        rotationVec4.fill(0f)
        for (i in 0 until count) rotationVec4[i] = values[i]

        return try {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVec4)
            rotationMatrix
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Projects the screen's +Y axis from the 3D world rotation matrix onto the Earth's horizontal
     * (East, North) plane using swing-twist parallel transport (tilt removal along shortest arc).
     *
     * Formula: h_Y = (y_E, y_N) - (y_U / (1 + z_U)) * (z_E, z_N)
     *
     * Mathematically continuous across all flat (0°), hand-tilted (45°), and vertical upright (90°+)
     * postures with zero Euler gimbal lock and zero conditional branches for posture switching.
     */
    private fun extractAzimuthFromRotationMatrix(matrix: FloatArray): Float? {
        val (axisX, axisY) = getDisplayRotationAxes()
        if (!SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remappedRotationMatrix)) {
            return null
        }

        val yE = remappedRotationMatrix[1]
        val yN = remappedRotationMatrix[4]
        val yU = remappedRotationMatrix[7]
        val zE = remappedRotationMatrix[2]
        val zN = remappedRotationMatrix[5]
        val zU = remappedRotationMatrix[8]

        val onePlusZu = 1f + zU
        val horizE: Float
        val horizN: Float
        if (onePlusZu > 0.05f) {
            val factor = yU / onePlusZu
            horizE = yE - factor * zE
            horizN = yN - factor * zN
        } else {
            // Only when device is inverted almost completely face-down (> ~162° tilt)
            horizE = yE
            horizN = yN
        }

        if (horizE * horizE + horizN * horizN < 1e-6f) {
            return null
        }
        return atan2(horizE.toDouble(), horizN.toDouble()).toFloat()
    }

    private fun getDisplayRotationAxes(): Pair<Int, Int> {
        return try {
            val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val rotation = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
            when (rotation) {
                Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
            }
        } catch (_: Exception) {
            SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
    }

    fun computeMagneticFieldStrength(values: FloatArray): Float {
        if (values.size < 3) return 0f
        return sqrt(
            (values[0] * values[0] + values[1] * values[1] + values[2] * values[2]).toDouble()
        ).toFloat()
    }

    /**
     * Detects nearby magnetic/metallic disturbance (Earth's surface field is ~22–67 μT;
     * readings outside 20–75 μT indicate nearby magnets, steel, or electronics).
     */
    fun hasMagneticInterference(): Boolean {
        if (!hasMag) return false
        val strength = computeMagneticFieldStrength(lastMagneticFieldValues)
        return strength < 20f || strength > 75f
    }

    fun getMagnetometerAccuracy(): Int = magnetometerAccuracy

    fun hasObservedMagnetometerAccuracy(): Boolean = hasMagnetometerAccuracy

    fun getCompassAccuracy(accuracy: Int): CompassAccuracy {
        return when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.UNRELIABLE
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
            else -> CompassAccuracy.LOW
        }
    }

    fun reset() {
        rotationMatrix.fill(0f)
        remappedRotationMatrix.fill(0f)
        rotationVec4.fill(0f)
        accelVector.fill(0f)
        magVector.fill(0f)
        lastMagneticFieldValues.fill(0f)
        hasAccel = false
        hasMag = false
        magnetometerAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
        hasMagnetometerAccuracy = false
        lastRvTimestampNs = 0L
        lastGeoMagRvTimestampNs = 0L
        lastMagEmitTimestampNs = 0L
    }

    private companion object {
        const val FALLBACK_STALE_NS = 250_000_000L
        const val FALLBACK_MIN_EMIT_INTERVAL_NS = 16_000_000L
        const val FALLBACK_VECTOR_ALPHA = 0.16f
    }
}

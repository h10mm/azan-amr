package com.github.meypod.al_azan.core.domain.model.compass

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Clean circular compass heading filter using unit-vector (cos/sin) EMA smoothing.
 * Handles 0°/360° wraparound seamlessly without deadband freezing or artificial damping.
 */
class CompassFilter(
    private val config: Config = Config(),
) {
    data class Config(
        val emaAlpha: Float = 0.32f,
        val movementThreshold: Float = 1.5f,
        val maxAngleJump: Float = 180.0f,
        val fastMovementAlpha: Float = 0.4f,
        val slowMovementAlpha: Float = 0.1f,
        val velocityTimeoutNs: Long = 1_000_000_000L,
    )

    private var filteredCos: Float? = null
    private var filteredSin: Float? = null
    private var lastOutputAngle: Float? = null
    private var lastRawValue: Float? = null
    private var lastTimestamp: Long = 0L
    private var lastVelocity: Float = 0f
    private var hasVelocitySample: Boolean = false
    private var lastValidTimestamp: Long = 0L

    fun filter(
        rawAngle: Float,
        timestamp: Long,
        isResetting: Boolean = false,
    ): Float {
        if (!rawAngle.isFinite()) {
            reset()
            return 0f
        }

        val normalizedAngle = normalize360(rawAngle)
        val rad = Math.toRadians(normalizedAngle.toDouble())
        val rawCos = cos(rad).toFloat()
        val rawSin = sin(rad).toFloat()

        if (isResetting || filteredCos == null || filteredSin == null || lastOutputAngle == null) {
            filteredCos = rawCos
            filteredSin = rawSin
            lastOutputAngle = normalizedAngle
            lastRawValue = normalizedAngle
            lastTimestamp = timestamp
            lastVelocity = 0f
            hasVelocitySample = false
            lastValidTimestamp = timestamp
            return normalizedAngle
        }

        val deltaTimeNs = timestamp - lastTimestamp
        if (deltaTimeNs > config.velocityTimeoutNs * 5) {
            filteredCos = rawCos
            filteredSin = rawSin
            lastOutputAngle = normalizedAngle
            lastRawValue = normalizedAngle
            lastTimestamp = timestamp
            lastVelocity = 0f
            hasVelocitySample = false
            lastValidTimestamp = timestamp
            return normalizedAngle
        }

        if (deltaTimeNs > 0L) {
            val deltaTimeSec = deltaTimeNs.toDouble() / 1_000_000_000.0
            val angleDiff = circularDifference(lastRawValue ?: normalizedAngle, normalizedAngle)
            val instantVelocity = (angleDiff / deltaTimeSec).toFloat()
            lastVelocity = if (!hasVelocitySample) {
                hasVelocitySample = true
                instantVelocity
            } else {
                lastVelocity * 0.7f + instantVelocity * 0.3f
            }
        }

        val previousOutput = lastOutputAngle!!
        val rawJump = abs(circularDifference(previousOutput, normalizedAngle))
        if (rawJump > config.maxAngleJump) {
            lastTimestamp = timestamp
            return previousOutput
        }

        val alpha = config.emaAlpha.coerceIn(0.01f, 1f)
        val newCos = filteredCos!! + alpha * (rawCos - filteredCos!!)
        val newSin = filteredSin!! + alpha * (rawSin - filteredSin!!)
        val norm = sqrt((newCos * newCos + newSin * newSin).toDouble()).toFloat()
        if (norm > 1e-4f) {
            filteredCos = newCos / norm
            filteredSin = newSin / norm
        } else {
            filteredCos = rawCos
            filteredSin = rawSin
        }

        val finalAngle = normalize360(
            Math.toDegrees(atan2(filteredSin!!.toDouble(), filteredCos!!.toDouble())).toFloat()
        )

        lastOutputAngle = finalAngle
        lastRawValue = normalizedAngle
        lastTimestamp = timestamp
        lastValidTimestamp = timestamp
        return finalAngle
    }

    fun reset() {
        filteredCos = null
        filteredSin = null
        lastOutputAngle = null
        lastRawValue = null
        lastTimestamp = 0L
        lastVelocity = 0f
        hasVelocitySample = false
        lastValidTimestamp = 0L
    }

    private fun normalize360(angle: Float): Float = ((angle % 360f) + 360f) % 360f

    private fun circularDifference(a: Float, b: Float): Float {
        val na = normalize360(a)
        val nb = normalize360(b)
        return ((nb - na + 540f) % 360f) - 180f
    }

    fun circularLerp(a: Float, b: Float, t: Float): Float {
        val clampedT = t.coerceIn(0f, 1f)
        val diff = circularDifference(a, b)
        return normalize360(a + diff * clampedT)
    }

    fun lowPassFilter(previous: Float, current: Float, alpha: Float): Float {
        return circularLerp(previous, current, alpha.coerceIn(0f, 1f))
    }

    fun getVelocity(): Float = lastVelocity

    fun getTimeSinceLastReading(timestamp: Long): Long {
        return if (lastValidTimestamp > 0) timestamp - lastValidTimestamp else Long.MAX_VALUE
    }

    fun hasValidState(): Boolean = lastOutputAngle != null && lastOutputAngle!!.isFinite()
}

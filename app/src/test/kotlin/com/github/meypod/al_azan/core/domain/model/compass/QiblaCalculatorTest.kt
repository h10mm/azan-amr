package com.github.meypod.al_azan.core.domain.model.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for QiblaCalculator.
 * Tests great-circle bearing calculations and angle normalization.
 */
class QiblaCalculatorTest {
    
    @Test
    fun `Kaaba coordinates are correct`() {
        assertEquals(21.422487, QiblaCalculator.getKaabaLatitude(), 0.000001)
        assertEquals(39.826206, QiblaCalculator.getKaabaLongitude(), 0.000001)
    }
    
    @Test
    fun `Calculate Qibla bearing from Mecca returns null`() {
        // At Kaaba, there's no direction to Kaaba
        val bearing = QiblaCalculator.calculateQiblaBearing(21.422487, 39.826206)
        assertNull(bearing)
    }
    
    @Test
    fun `Calculate Qibla bearing from known locations`() {
        // Test with known values
        // New York: approximately 58.5 degrees
        val nyBearing = QiblaCalculator.calculateQiblaBearing(40.7128, -74.0060)
        assertNotNull(nyBearing)
        assertTrue("New York bearing should be around 58.5", abs(nyBearing!! - 58.5) < 1.0)
        
        // London: approximately 117.5 degrees
        val londonBearing = QiblaCalculator.calculateQiblaBearing(51.5074, -0.1278)
        assertNotNull(londonBearing)
        assertTrue("London bearing should be around 117.5", abs(londonBearing!! - 117.5) < 1.0)
        
        // Tokyo: approximately 293 degrees
        val tokyoBearing = QiblaCalculator.calculateQiblaBearing(35.6762, 139.6503)
        assertNotNull(tokyoBearing)
        assertTrue("Tokyo bearing should be around 293", abs(tokyoBearing!! - 293.0) < 1.0)
    }
    
    @Test
    fun `Calculate Qibla bearing from invalid coordinates returns null`() {
        // Invalid latitude
        assertNull(QiblaCalculator.calculateQiblaBearing(100.0, 39.826206))
        assertNull(QiblaCalculator.calculateQiblaBearing(-100.0, 39.826206))
        
        // Invalid longitude
        assertNull(QiblaCalculator.calculateQiblaBearing(21.422487, 200.0))
        assertNull(QiblaCalculator.calculateQiblaBearing(21.422487, -200.0))
        
        // NaN values
        assertNull(QiblaCalculator.calculateQiblaBearing(Double.NaN, 39.826206))
        assertNull(QiblaCalculator.calculateQiblaBearing(21.422487, Double.NaN))
        assertNull(QiblaCalculator.calculateQiblaBearing(Double.POSITIVE_INFINITY, 39.826206))
    }
    
    @Test
    fun `Normalize angle to 0-360 degrees`() {
        // Positive angles
        assertEquals(45.0, QiblaCalculator.normalizeAngleDegrees(45.0), 0.001)
        assertEquals(360.0, QiblaCalculator.normalizeAngleDegrees(360.0), 0.001)
        assertEquals(360.0, QiblaCalculator.normalizeAngleDegrees(720.0), 0.001)
        
        // Negative angles
        assertEquals(359.0, QiblaCalculator.normalizeAngleDegrees(-1.0), 0.001)
        assertEquals(350.0, QiblaCalculator.normalizeAngleDegrees(-10.0), 0.001)
        assertEquals(180.0, QiblaCalculator.normalizeAngleDegrees(-180.0), 0.001)
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(-360.0), 0.001)
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(-720.0), 0.001)
        
        // Large positive angles
        assertEquals(90.0, QiblaCalculator.normalizeAngleDegrees(450.0), 0.001)
        assertEquals(180.0, QiblaCalculator.normalizeAngleDegrees(540.0), 0.001)
    }
    
    @Test
    fun `Normalize angle to 0-360 degrees (Float)`() {
        assertEquals(45f, QiblaCalculator.normalizeAngleDegrees(45f), 0.001f)
        assertEquals(359f, QiblaCalculator.normalizeAngleDegrees(-1f), 0.001f)
        assertEquals(90f, QiblaCalculator.normalizeAngleDegrees(450f), 0.001f)
    }
    
    @Test
    fun `Normalize angle to -180 to 180 degrees`() {
        // Positive angles
        assertEquals(45.0, QiblaCalculator.normalizeAngle180(45.0), 0.001)
        assertEquals(180.0, QiblaCalculator.normalizeAngle180(180.0), 0.001)
        
        // Angles > 180
        assertEquals(-179.0, QiblaCalculator.normalizeAngle180(181.0), 0.001)
        assertEquals(-90.0, QiblaCalculator.normalizeAngle180(270.0), 0.001)
        assertEquals(-1.0, QiblaCalculator.normalizeAngle180(359.0), 0.001)
        
        // Negative angles
        assertEquals(-45.0, QiblaCalculator.normalizeAngle180(-45.0), 0.001)
        assertEquals(-180.0, QiblaCalculator.normalizeAngle180(-180.0), 0.001)
        
        // Angles < -180
        assertEquals(179.0, QiblaCalculator.normalizeAngle180(-181.0), 0.001)
        assertEquals(90.0, QiblaCalculator.normalizeAngle180(-270.0), 0.001)
    }
    
    @Test
    fun `Normalize angle to -180 to 180 degrees (Float)`() {
        assertEquals(45f, QiblaCalculator.normalizeAngle180(45f), 0.001f)
        assertEquals(-179f, QiblaCalculator.normalizeAngle180(181f), 0.001f)
        assertEquals(-45f, QiblaCalculator.normalizeAngle180(-45f), 0.001f)
    }
    
    @Test
    fun `Calculate relative Qibla angle`() {
        // Facing exactly Qibla
        val facingQibla = QiblaCalculator.calculateRelativeQiblaAngle(180.0, 180f)
        assertEquals(0.0, facingQibla, 0.001)
        
        // Facing away from Qibla
        val facingAway = QiblaCalculator.calculateRelativeQiblaAngle(180.0, 0f)
        assertEquals(180.0, facingAway, 0.001)
        
        // Turn right to face Qibla
        val turnRight = QiblaCalculator.calculateRelativeQiblaAngle(180.0, 90f)
        assertEquals(90.0, turnRight, 0.001)
        
        // Turn left to face Qibla
        val turnLeft = QiblaCalculator.calculateRelativeQiblaAngle(180.0, 270f)
        assertEquals(-90.0, turnLeft, 0.001)
        
        // Test with angles crossing 0
        val crossZero = QiblaCalculator.calculateRelativeQiblaAngle(10.0, 350f)
        assertEquals(-20.0, crossZero, 0.001)
        
        val crossZero2 = QiblaCalculator.calculateRelativeQiblaAngle(350.0, 10f)
        assertEquals(20.0, crossZero2, 0.001)
    }
    
    @Test
    fun `Calculate distance to Kaaba`() {
        // Distance from Mecca should be 0
        val meccaDistance = QiblaCalculator.calculateDistanceToKaaba(21.422487, 39.826206)
        assertNotNull(meccaDistance)
        assertTrue("Distance from Mecca should be ~0", meccaDistance!! < 0.1)
        
        // Distance from New York (approximately 10,800 km)
        val nyDistance = QiblaCalculator.calculateDistanceToKaaba(40.7128, -74.0060)
        assertNotNull(nyDistance)
        assertTrue("Distance from NY should be around 10,800 km", abs(nyDistance!! - 10800) < 500)
        
        // Distance from London (approximately 4,500 km)
        val londonDistance = QiblaCalculator.calculateDistanceToKaaba(51.5074, -0.1278)
        assertNotNull(londonDistance)
        assertTrue("Distance from London should be around 4,500 km", abs(londonDistance!! - 4500) < 500)
    }
    
    @Test
    fun `Calculate distance from invalid coordinates returns null`() {
        assertNull(QiblaCalculator.calculateDistanceToKaaba(100.0, 39.826206))
        assertNull(QiblaCalculator.calculateDistanceToKaaba(21.422487, 200.0))
    }
    
    @Test
    fun `Angle normalization handles edge cases`() {
        // Very large angles
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(720.0), 0.001)
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(360.0 * 1000), 0.001)
        
        // Very negative angles
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(-720.0), 0.001)
        assertEquals(0.0, QiblaCalculator.normalizeAngleDegrees(-360.0 * 1000), 0.001)
    }
    
    @Test
    fun `Circular angle transitions are smooth`() {
        // Test 359 to 0 to 1 transition (should not rotate backwards through 180)
        val angle1 = 359.0
        val angle2 = 0.0
        val angle3 = 1.0
        
        val diff1 = QiblaCalculator.normalizeAngle180(angle2 - angle1)
        val diff2 = QiblaCalculator.normalizeAngle180(angle3 - angle2)
        
        // Both differences should be positive and small
        assertTrue("359 to 0 should be +1 degree", abs(diff1 - 1.0) < 0.001)
        assertTrue("0 to 1 should be +1 degree", abs(diff2 - 1.0) < 0.001)
    }
}

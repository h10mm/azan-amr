package com.github.meypod.al_azan.core.domain.model.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SensorSource enum.
 * Tests sensor source properties and availability.
 */
class SensorSourceTest {
    
    @Test
    fun `ROTATION_VECTOR provides absolute orientation`() {
        assertTrue(SensorSource.ROTATION_VECTOR.providesAbsoluteOrientation)
    }
    
    @Test
    fun `GEOMAGNETIC_ROTATION_VECTOR provides absolute orientation`() {
        assertTrue(SensorSource.GEOMAGNETIC_ROTATION_VECTOR.providesAbsoluteOrientation)
    }
    
    @Test
    fun `ACCELEROMETER_MAGNETOMETER provides absolute orientation`() {
        assertTrue(SensorSource.ACCELEROMETER_MAGNETOMETER.providesAbsoluteOrientation)
    }
    
    @Test
    fun `ORIENTATION provides absolute orientation`() {
        assertTrue(SensorSource.ORIENTATION.providesAbsoluteOrientation)
    }
    
    @Test
    fun `GAME_ROTATION_VECTOR does NOT provide absolute orientation`() {
        assertFalse(SensorSource.GAME_ROTATION_VECTOR.providesAbsoluteOrientation)
    }
    
    @Test
    fun `ACCELEROMETER_ONLY does NOT provide absolute orientation`() {
        assertFalse(SensorSource.ACCELEROMETER_ONLY.providesAbsoluteOrientation)
    }
    
    @Test
    fun `MAGNETOMETER_ONLY does NOT provide absolute orientation`() {
        assertFalse(SensorSource.MAGNETOMETER_ONLY.providesAbsoluteOrientation)
    }
    
    @Test
    fun `NONE does NOT provide absolute orientation`() {
        assertFalse(SensorSource.NONE.providesAbsoluteOrientation)
    }
    
    @Test
    fun `ROTATION_VECTOR uses magnetometer`() {
        assertTrue(SensorSource.ROTATION_VECTOR.usesMagnetometer)
    }
    
    @Test
    fun `GEOMAGNETIC_ROTATION_VECTOR uses magnetometer`() {
        assertTrue(SensorSource.GEOMAGNETIC_ROTATION_VECTOR.usesMagnetometer)
    }
    
    @Test
    fun `ACCELEROMETER_MAGNETOMETER uses magnetometer`() {
        assertTrue(SensorSource.ACCELEROMETER_MAGNETOMETER.usesMagnetometer)
    }
    
    @Test
    fun `ORIENTATION uses magnetometer`() {
        assertTrue(SensorSource.ORIENTATION.usesMagnetometer)
    }
    
    @Test
    fun `GAME_ROTATION_VECTOR does NOT use magnetometer`() {
        assertFalse(SensorSource.GAME_ROTATION_VECTOR.usesMagnetometer)
    }
    
    @Test
    fun `ACCELEROMETER_ONLY does NOT use magnetometer`() {
        assertFalse(SensorSource.ACCELEROMETER_ONLY.usesMagnetometer)
    }
    
    @Test
    fun `MAGNETOMETER_ONLY uses magnetometer`() {
        assertTrue(SensorSource.MAGNETOMETER_ONLY.usesMagnetometer)
    }
    
    @Test
    fun `NONE does NOT use magnetometer`() {
        assertFalse(SensorSource.NONE.usesMagnetometer)
    }
    
    @Test
    fun `SensorSource has correct count`() {
        // Verify all expected sensor sources are present
        val expectedSources = listOf(
            SensorSource.NONE,
            SensorSource.ROTATION_VECTOR,
            SensorSource.GEOMAGNETIC_ROTATION_VECTOR,
            SensorSource.ACCELEROMETER_MAGNETOMETER,
            SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE,
            SensorSource.ORIENTATION,
            SensorSource.GAME_ROTATION_VECTOR,
            SensorSource.ACCELEROMETER_ONLY,
            SensorSource.MAGNETOMETER_ONLY,
            SensorSource.SIMULATED,
        )
        
        assertEquals(expectedSources.size, SensorSource.entries.size)
        
        // Verify all expected sources are present
        expectedSources.forEach { source ->
            assertTrue("SensorSource should contain $source", SensorSource.entries.contains(source))
        }
    }
    
    @Test
    fun `Priority order is correct`() {
        // Lower ordinal = higher priority
        assertTrue(SensorSource.ROTATION_VECTOR.ordinal < SensorSource.GEOMAGNETIC_ROTATION_VECTOR.ordinal)
        assertTrue(SensorSource.GEOMAGNETIC_ROTATION_VECTOR.ordinal < SensorSource.ACCELEROMETER_MAGNETOMETER.ordinal)
        assertTrue(SensorSource.ACCELEROMETER_MAGNETOMETER.ordinal < SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE.ordinal)
        assertTrue(SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE.ordinal < SensorSource.ORIENTATION.ordinal)
        assertTrue(SensorSource.ORIENTATION.ordinal < SensorSource.GAME_ROTATION_VECTOR.ordinal)
    }
}

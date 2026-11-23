package com.github.meypod.al_azan.core.domain.model.compass

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for CompassAccuracy enum.
 * Tests accuracy states and conversions.
 */
class CompassAccuracyTest {
    
    @Test
    fun `HIGH accuracy is usable`() {
        assertTrue(CompassAccuracy.HIGH.isUsable)
    }
    
    @Test
    fun `MEDIUM accuracy is usable`() {
        assertTrue(CompassAccuracy.MEDIUM.isUsable)
    }
    
    @Test
    fun `LOW accuracy is usable`() {
        assertTrue(CompassAccuracy.LOW.isUsable)
    }
    
    @Test
    fun `UNRELIABLE accuracy is usable`() {
        assertTrue(CompassAccuracy.UNRELIABLE.isUsable)
    }
    
    @Test
    fun `NO_SENSOR accuracy is NOT usable`() {
        assertFalse(CompassAccuracy.NO_SENSOR.isUsable)
    }
    
    @Test
    fun `CALIBRATING accuracy is NOT usable`() {
        assertFalse(CompassAccuracy.CALIBRATING.isUsable)
    }
    
    @Test
    fun `TEMPORARILY_UNAVAILABLE accuracy is NOT usable`() {
        assertFalse(CompassAccuracy.TEMPORARILY_UNAVAILABLE.isUsable)
    }
    
    @Test
    fun `MAGNETIC_INTERFERENCE accuracy is usable`() {
        assertTrue(CompassAccuracy.MAGNETIC_INTERFERENCE.isUsable)
    }
    
    @Test
    fun `HIGH accuracy is sufficient for Qibla`() {
        assertTrue(CompassAccuracy.HIGH.isSufficientForQibla)
    }
    
    @Test
    fun `MEDIUM accuracy is sufficient for Qibla`() {
        assertTrue(CompassAccuracy.MEDIUM.isSufficientForQibla)
    }
    
    @Test
    fun `LOW accuracy is NOT sufficient for Qibla`() {
        assertFalse(CompassAccuracy.LOW.isSufficientForQibla)
    }
    
    @Test
    fun `UNRELIABLE accuracy is NOT sufficient for Qibla`() {
        assertFalse(CompassAccuracy.UNRELIABLE.isSufficientForQibla)
    }
    
    @Test
    fun `NO_SENSOR accuracy is NOT sufficient for Qibla`() {
        assertFalse(CompassAccuracy.NO_SENSOR.isSufficientForQibla)
    }
    
    @Test
    fun `Convert SENSOR_STATUS_ACCURACY_HIGH to HIGH`() {
        val accuracy = CompassAccuracy.fromSensorStatus(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
        assertEquals(CompassAccuracy.HIGH, accuracy)
    }
    
    @Test
    fun `Convert SENSOR_STATUS_ACCURACY_MEDIUM to MEDIUM`() {
        val accuracy = CompassAccuracy.fromSensorStatus(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
        assertEquals(CompassAccuracy.MEDIUM, accuracy)
    }
    
    @Test
    fun `Convert SENSOR_STATUS_ACCURACY_LOW to LOW`() {
        val accuracy = CompassAccuracy.fromSensorStatus(SensorManager.SENSOR_STATUS_ACCURACY_LOW)
        assertEquals(CompassAccuracy.LOW, accuracy)
    }
    
    @Test
    fun `Convert SENSOR_STATUS_UNRELIABLE to UNRELIABLE`() {
        val accuracy = CompassAccuracy.fromSensorStatus(SensorManager.SENSOR_STATUS_UNRELIABLE)
        assertEquals(CompassAccuracy.UNRELIABLE, accuracy)
    }
    
    @Test
    fun `Convert unknown status to LOW`() {
        val accuracy = CompassAccuracy.fromSensorStatus(999)
        assertEquals(CompassAccuracy.LOW, accuracy)
    }
    
    @Test
    fun `Worst of multiple accuracies`() {
        val worst = CompassAccuracy.worstOf(
            CompassAccuracy.HIGH,
            CompassAccuracy.MEDIUM,
            CompassAccuracy.LOW
        )
        assertEquals(CompassAccuracy.LOW, worst)
    }
    
    @Test
    fun `Best of multiple accuracies`() {
        val best = CompassAccuracy.bestOf(
            CompassAccuracy.HIGH,
            CompassAccuracy.MEDIUM,
            CompassAccuracy.LOW
        )
        assertEquals(CompassAccuracy.HIGH, best)
    }
    
    @Test
    fun `Worst of includes NO_SENSOR`() {
        val worst = CompassAccuracy.worstOf(
            CompassAccuracy.HIGH,
            CompassAccuracy.NO_SENSOR,
            CompassAccuracy.MEDIUM
        )
        assertEquals(CompassAccuracy.NO_SENSOR, worst)
    }
    
    @Test
    fun `Best of includes NO_SENSOR`() {
        val best = CompassAccuracy.bestOf(
            CompassAccuracy.NO_SENSOR,
            CompassAccuracy.NO_SENSOR
        )
        assertEquals(CompassAccuracy.NO_SENSOR, best)
    }
    
    @Test
    fun `All accuracy values are present`() {
        val expectedAccuracies = listOf(
            CompassAccuracy.NO_SENSOR,
            CompassAccuracy.CALIBRATING,
            CompassAccuracy.UNRELIABLE,
            CompassAccuracy.LOW,
            CompassAccuracy.MEDIUM,
            CompassAccuracy.HIGH,
            CompassAccuracy.MAGNETIC_INTERFERENCE,
            CompassAccuracy.TEMPORARILY_UNAVAILABLE,
        )
        
        assertEquals(expectedAccuracies.size, CompassAccuracy.entries.size)
        
        expectedAccuracies.forEach { accuracy ->
            assertTrue("CompassAccuracy should contain $accuracy", 
                CompassAccuracy.entries.contains(accuracy))
        }
    }
}

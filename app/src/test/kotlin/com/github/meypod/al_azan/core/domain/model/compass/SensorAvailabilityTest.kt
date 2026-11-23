package com.github.meypod.al_azan.core.domain.model.compass

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SensorAvailability.
 * Tests sensor availability detection and source selection.
 */
class SensorAvailabilityTest {
    
    @Test
    fun `Empty availability has no sensors`() {
        val availability = SensorAvailability.EMPTY
        
        assertFalse(availability.hasRotationVector)
        assertFalse(availability.hasAccelerometer)
        assertFalse(availability.hasMagnetometer)
        assertFalse(availability.hasGyroscope)
        assertFalse(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with rotation vector has compass`() {
        val availability = SensorAvailability(hasRotationVector = true)
        
        assertTrue(availability.hasRotationVector)
        assertTrue(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with geomagnetic rotation vector has compass`() {
        val availability = SensorAvailability(hasGeomagneticRotationVector = true)
        
        assertTrue(availability.hasGeomagneticRotationVector)
        assertTrue(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with accelerometer and magnetometer has compass`() {
        val availability = SensorAvailability(
            hasAccelerometer = true,
            hasMagnetometer = true
        )
        
        assertTrue(availability.hasAccelerometer)
        assertTrue(availability.hasMagnetometer)
        assertTrue(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with only accelerometer does NOT have compass`() {
        val availability = SensorAvailability(hasAccelerometer = true)
        
        assertTrue(availability.hasAccelerometer)
        assertFalse(availability.hasMagnetometer)
        assertFalse(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with only magnetometer does NOT have compass`() {
        val availability = SensorAvailability(hasMagnetometer = true)
        
        assertFalse(availability.hasAccelerometer)
        assertTrue(availability.hasMagnetometer)
        assertFalse(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `Availability with orientation has compass`() {
        val availability = SensorAvailability(hasOrientation = true)
        
        assertTrue(availability.hasOrientation)
        assertTrue(availability.hasAnyCompassSensor)
    }
    
    @Test
    fun `High quality compass sensors`() {
        val availabilityWithRotation = SensorAvailability(hasRotationVector = true)
        assertTrue(availabilityWithRotation.hasHighQualityCompass)
        
        val availabilityWithGeomagnetic = SensorAvailability(hasGeomagneticRotationVector = true)
        assertTrue(availabilityWithGeomagnetic.hasHighQualityCompass)
        
        val availabilityWithAccelMag = SensorAvailability(
            hasAccelerometer = true,
            hasMagnetometer = true
        )
        assertFalse(availabilityWithAccelMag.hasHighQualityCompass)
    }
    
    @Test
    fun `Best sensor source selection`() {
        // Rotation vector is best
        val availability1 = SensorAvailability(hasRotationVector = true)
        assertEquals(SensorSource.ROTATION_VECTOR, availability1.getBestSensorSource())
        
        // Geomagnetic rotation vector is second best
        val availability2 = SensorAvailability(hasGeomagneticRotationVector = true)
        assertEquals(SensorSource.GEOMAGNETIC_ROTATION_VECTOR, availability2.getBestSensorSource())
        
        // Accelerometer + magnetometer is third
        val availability3 = SensorAvailability(
            hasAccelerometer = true,
            hasMagnetometer = true
        )
        assertEquals(SensorSource.ACCELEROMETER_MAGNETOMETER, availability3.getBestSensorSource())
        
        // Orientation is fourth
        val availability4 = SensorAvailability(hasOrientation = true)
        assertEquals(SensorSource.ORIENTATION, availability4.getBestSensorSource())
        
        // No sensors returns NONE
        val availability5 = SensorAvailability.EMPTY
        assertEquals(SensorSource.NONE, availability5.getBestSensorSource())
    }
    
    @Test
    fun `Get available sources`() {
        val availability = SensorAvailability(
            hasRotationVector = true,
            hasGeomagneticRotationVector = true,
            hasAccelerometer = true,
            hasMagnetometer = true,
            hasOrientation = true
        )
        
        val sources = availability.getAvailableSources()
        
        assertTrue(sources.contains(SensorSource.ROTATION_VECTOR))
        assertTrue(sources.contains(SensorSource.GEOMAGNETIC_ROTATION_VECTOR))
        assertTrue(sources.contains(SensorSource.ACCELEROMETER_MAGNETOMETER))
        assertTrue(sources.contains(SensorSource.ORIENTATION))
        assertFalse(sources.contains(SensorSource.NONE))
        
        // Check order (should be in priority order)
        assertTrue(sources.indexOf(SensorSource.ROTATION_VECTOR) < 
                   sources.indexOf(SensorSource.GEOMAGNETIC_ROTATION_VECTOR))
    }
    
    @Test
    fun `Available sources with only basic sensors`() {
        val availability = SensorAvailability(
            hasAccelerometer = true,
            hasMagnetometer = true
        )
        
        val sources = availability.getAvailableSources()
        
        assertTrue(sources.contains(SensorSource.ACCELEROMETER_MAGNETOMETER))
        assertFalse(sources.contains(SensorSource.ROTATION_VECTOR))
        assertEquals(1, sources.size)
    }
    
    @Test
    fun `All sensor types are checked`() {
        val allSensors = listOf(
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ORIENTATION,
        )
        
        // Verify SensorAvailability has properties for all relevant sensors
        val availability = SensorAvailability(
            hasRotationVector = true,
            hasGeomagneticRotationVector = true,
            hasGameRotationVector = true,
            hasAccelerometer = true,
            hasMagnetometer = true,
            hasGyroscope = true,
            hasGravity = true,
            hasLinearAcceleration = true,
            hasOrientation = true,
        )
        
        assertNotNull(availability)
    }
}

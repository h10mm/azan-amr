package com.github.meypod.al_azan.core.domain.model.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for CompassFilter.
 * Tests EMA filtering, circular interpolation, and adaptive filtering.
 */
class CompassFilterTest {
    
    private lateinit var filter: CompassFilter
    
    @Before
    fun setup() {
        filter = CompassFilter(CompassFilter.Config(
            emaAlpha = 0.2f,
            movementThreshold = 1.0f,
            maxAngleJump = 180.0f,
            fastMovementAlpha = 0.4f,
            slowMovementAlpha = 0.1f,
        ))
    }
    
    @Test
    fun `First reading is passed through unchanged`() {
        val result = filter.filter(100f, 0L, isResetting = false)
        assertEquals(100f, result, 0.001f)
    }
    
    @Test
    fun `Filter applies EMA smoothing`() {
        // First reading
        filter.filter(0f, 0L)
        
        // Second reading - should be smoothed
        val result = filter.filter(90f, 1000000L)
        
        // With alpha=0.2, result should be 0 + 0.2 * 90 = 18
        // But since we're using circular interpolation, we need to check it's between 0 and 90
        assertTrue("Filtered value should be between 0 and 90", result in 0f..90f)
        assertNotEquals("Filtered value should not equal input", 90f, result, 0.001f)
    }
    
    @Test
    fun `Circular transition from 359 to 0 is smooth`() {
        // Set initial state
        filter.filter(359f, 0L)
        
        // Next reading is 0 (should be treated as +1 degree, not -359)
        val result = filter.filter(0f, 1000000L)
        
        // Result should be close to 359 + a small amount
        assertTrue("Should be close to 359", result > 358f && result <= 360f)
        
        // Next reading is 1
        val result2 = filter.filter(1f, 2000000L)
        
        // Should continue smoothly
        assertTrue("Should be close to 0 or 1", result2 >= 0f && result2 <= 2f)
    }
    
    @Test
    fun `Circular transition from 0 to 359 is smooth backwards`() {
        // Set initial state
        filter.filter(0f, 0L)
        
        // Next reading is 359 (should be treated as -1 degree, not +359)
        val result = filter.filter(359f, 1000000L)
        
        // Result should be close to 0 - a small amount, normalized to 359.x
        assertTrue("Should be close to 359", result >= 358f && result < 360f)
    }
    
    @Test
    fun `Outlier detection rejects large jumps`() {
        filter.filter(0f, 0L)
        
        // This should be rejected as an outlier
        val result = filter.filter(200f, 1000000L)
        
        // Should still return last valid value (0), not the outlier
        // Note: This depends on the maxAngleJump configuration
        // With maxAngleJump=180, 200 should still be processed
        // Let's adjust the test
        filter = CompassFilter(CompassFilter.Config(
            emaAlpha = 0.2f,
            movementThreshold = 1.0f,
            maxAngleJump = 90.0f, // Smaller threshold
            fastMovementAlpha = 0.4f,
            slowMovementAlpha = 0.1f,
        ))
        
        filter.reset()
        filter.filter(0f, 0L)
        
        // Jump of 100 degrees should be rejected
        val result2 = filter.filter(100f, 1000000L)
        
        // Should return 0 (last valid), not 100
        assertEquals("Outlier should be rejected", 0f, result2, 0.001f)
    }
    
    @Test
    fun `Filter reset clears state`() {
        filter.filter(100f, 0L)
        filter.reset()
        
        val result = filter.filter(50f, 1000000L)
        
        // After reset, first reading should pass through
        assertEquals(50f, result, 0.001f)
    }
    
    @Test
    fun `Circular lerp works correctly`() {
        // Normal interpolation
        val result1 = filter.circularLerp(0f, 90f, 0.5f)
        assertEquals(45f, result1, 0.001f)
        
        // Interpolate from 0 to 359 (should go backwards through 359, not forwards through 180)
        val result2 = filter.circularLerp(0f, 359f, 0.5f)
        // Should be 359.5 or 0.5 depending on direction
        // With circular lerp, it should take the shortest path
        assertTrue("Should be near 0 or 359", result2 in 358f..1f)
        
        // Interpolate from 10 to 350 (should go forward through 350, not backward through 190)
        val result3 = filter.circularLerp(10f, 350f, 0.5f)
        // Shortest path is backwards: 10 -> 0 -> 359 -> 358 -> ... -> 350
        // At 0.5, we should be at 355 (which is -5, the midpoint)
        assertEquals("Midpoint of 10 and 350 going shortest path should be 355", 
            355f, result3, 0.001f)
    }
    
    @Test
    fun `Low pass filter works`() {
        filter.filter(0f, 0L)
        
        val result = filter.lowPassFilter(0f, 90f, 0.5f)
        assertEquals(45f, result, 0.001f)
    }
    
    @Test
    fun `Low pass filter with circular angles`() {
        filter.filter(0f, 0L)
        
        val result = filter.lowPassFilter(0f, 359f, 0.5f)
        // Midpoint should be 359.5 or 0.5
        assertTrue("Should be near 0 or 359", result in 358f..1f)
    }
    
    @Test
    fun `Filter handles NaN input`() {
        filter.filter(100f, 0L)
        
        val result = filter.filter(Float.NaN, 1000000L)
        
        // Should return 0 (reset state)
        assertEquals(0f, result, 0.001f)
    }
    
    @Test
    fun `Filter handles infinite input`() {
        filter.filter(100f, 0L)
        
        val result = filter.filter(Float.POSITIVE_INFINITY, 1000000L)
        
        // Should return 0 (reset state)
        assertEquals(0f, result, 0.001f)
    }
    
    @Test
    fun `Has valid state after first reading`() {
        assertTrue(!filter.hasValidState())
        
        filter.filter(100f, 0L)
        
        assertTrue(filter.hasValidState())
    }
    
    @Test
    fun `Time since last reading`() {
        filter.filter(100f, 0L)
        
        val timeSince = filter.getTimeSinceLastReading(1000000000L) // 1 second in ns
        
        assertEquals(1000000000L, timeSince)
    }
    
    @Test
    fun `Velocity tracking`() {
        filter.filter(0f, 0L)
        filter.filter(10f, 1000000000L) // 1 second later, 10 degrees
        
        val velocity = filter.getVelocity()
        
        assertTrue("Velocity should be around 10 degrees per second", abs(velocity - 10f) < 1f)
    }
    
    @Test
    fun `359 to 0 to 1 transition is smooth`() {
        // This is the critical test for compass circular behavior
        filter = CompassFilter(CompassFilter.Config(
            emaAlpha = 0.5f,
            movementThreshold = 1.0f,
            maxAngleJump = 180.0f,
            fastMovementAlpha = 0.5f,
            slowMovementAlpha = 0.5f,
        ))
        
        val result1 = filter.filter(359f, 0L)
        val result2 = filter.filter(0f, 1000000L)
        val result3 = filter.filter(1f, 2000000L)
        
        // All results should be close to each other (within 2 degrees)
        // This ensures the needle doesn't jump 180 degrees
        assertTrue("359 to 0 should be smooth", abs(result2 - result1) < 2f)
        assertTrue("0 to 1 should be smooth", abs(result3 - result2) < 2f)
    }
    
    @Test
    fun `0 to 359 to 358 transition is smooth backwards`() {
        filter = CompassFilter(CompassFilter.Config(
            emaAlpha = 0.5f,
            movementThreshold = 1.0f,
            maxAngleJump = 180.0f,
            fastMovementAlpha = 0.5f,
            slowMovementAlpha = 0.5f,
        ))
        
        val result1 = filter.filter(0f, 0L)
        val result2 = filter.filter(359f, 1000000L)
        val result3 = filter.filter(358f, 2000000L)
        
        // All results should be close to each other
        assertTrue("0 to 359 should be smooth", abs(result2 - result1) < 2f)
        assertTrue("359 to 358 should be smooth", abs(result3 - result2) < 2f)
    }
}

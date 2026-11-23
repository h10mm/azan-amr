package com.github.meypod.al_azan.core.domain.model.compass

import android.hardware.GeomagneticField
import java.util.Date

/**
 * Provides magnetic declination calculations with caching.
 * 
 * Magnetic declination (or variation) is the angle between magnetic north
 * and true north at a given location. It varies by location and time.
 * 
 * This provider uses Android's GeomagneticField class and caches results
 * to avoid unnecessary recalculations.
 */
class GeomagneticDeclinationProvider {
    
    private data class CacheEntry(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val declination: Float,
    )
    
    private var cache: CacheEntry? = null
    private var lastCacheHit: Long = 0
    
    /**
     * Cache validity threshold in milliseconds.
     * If location changes by more than 1km or time changes by more than 1 hour, recalculate.
     */
    private companion object {
        private const val CACHE_TIME_THRESHOLD_MS = 60 * 60 * 1000L // 1 hour
        private const val CACHE_DISTANCE_THRESHOLD_KM = 1.0 // 1 kilometer
    }
    
    /**
     * Get magnetic declination for a given location.
     * 
     * @param latitude Latitude in degrees
     * @param longitude Longitude in degrees
     * @param altitude Altitude in meters (defaults to 0)
     * @param timestamp Timestamp in milliseconds (defaults to current time)
     * @return Magnetic declination in degrees (positive = east, negative = west), or 0 if invalid
     */
    fun getDeclination(
        latitude: Double,
        longitude: Double,
        altitude: Float = 0f,
        timestamp: Long = System.currentTimeMillis(),
    ): Float {
        // Validate coordinates
        if (!isValidCoordinate(latitude) || !isValidCoordinate(longitude)) {
            return 0f
        }
        
        // Check cache
        val cached = cache
        if (cached != null) {
            val timeDiff = kotlin.math.abs(timestamp - cached.timestamp)
            val distanceKm = calculateDistanceKm(
                cached.latitude, cached.longitude, latitude, longitude
            )
            
            // Use cache if within thresholds
            if (timeDiff < CACHE_TIME_THRESHOLD_MS && distanceKm < CACHE_DISTANCE_THRESHOLD_KM) {
                lastCacheHit = System.currentTimeMillis()
                return cached.declination
            }
        }
        
        // Calculate new declination
        val declination = try {
            val geoField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitude,
                timestamp,
            )
            geoField.declination
        } catch (e: Exception) {
            // Fallback to 0 if calculation fails
            0f
        }
        
        // Update cache
        cache = CacheEntry(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            declination = declination,
        )
        
        return declination
    }
    
    /**
     * Get declination with automatic timestamp
     */
    fun getDeclination(
        latitude: Double,
        longitude: Double,
        altitude: Float = 0f,
    ): Float = getDeclination(latitude, longitude, altitude, Date().time)
    
    /**
     * Get declination using current system time
     */
    fun getDeclination(latitude: Double, longitude: Double): Float {
        return getDeclination(latitude, longitude, 0f, System.currentTimeMillis())
    }
    
    /**
     * Clear the declination cache
     */
    fun clearCache() {
        cache = null
    }
    
    /**
     * Check if declination is currently cached
     */
    fun hasCachedDeclination(): Boolean = cache != null
    
    /**
     * Get cache statistics for debugging
     */
    fun getCacheStats(): String {
        return "Cache: ${if (cache != null) "valid" else "empty"}, " +
               "Last hit: ${System.currentTimeMillis() - lastCacheHit}ms ago"
    }
    
    /**
     * Calculate approximate distance between two coordinates in kilometers.
     * Used for cache invalidation.
     */
    private fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        // Simple approximation using equirectangular projection
        // Accurate enough for cache invalidation purposes
        val x = (lon2 - lon1) * kotlin.math.cos((lat1 + lat2) / 2 * Math.PI / 180)
        val y = lat2 - lat1
        val distance = kotlin.math.sqrt(x * x + y * y) * 111.32 // 111.32 km per degree
        return kotlin.math.abs(distance)
    }
    
    /**
     * Validate coordinate
     */
    private fun isValidCoordinate(value: Double): Boolean {
        return value.isFinite() && value >= -180.0 && value <= 180.0
    }
}

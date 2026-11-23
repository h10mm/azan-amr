package com.github.meypod.al_azan.core.domain.model.compass

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates Qibla direction (bearing to Kaaba) using great-circle navigation.
 * 
 * The Kaaba is located at:
 * - Latitude: 21.422487°N
 * - Longitude: 39.826206°E
 * 
 * This implementation uses the spherical law of cosines for accurate bearing calculation.
 */
object QiblaCalculator {
    
    /**
     * Kaaba coordinates in Mecca, Saudi Arabia
     */
    private const val KAABA_LATITUDE = 21.422487
    private const val KAABA_LONGITUDE = 39.826206
    
    /**
     * Calculate the initial bearing (forward azimuth) from user's location to Kaaba.
     * 
     * @param userLatitude User's latitude in degrees
     * @param userLongitude User's longitude in degrees
     * @return Bearing in degrees (0-360) from true north, or null if coordinates are invalid
     */
    fun calculateQiblaBearing(
        userLatitude: Double,
        userLongitude: Double,
    ): Double? {
        // Validate coordinates
        if (!isValidLatitude(userLatitude) || !isValidLongitude(userLongitude)) {
            return null
        }
        
        // Check if user is at or very near Kaaba
        if (isAtKaaba(userLatitude, userLongitude)) {
            return null // No direction needed
        }
        
        // Convert all coordinates to radians
        val lat1 = Math.toRadians(userLatitude)
        val lon1 = Math.toRadians(userLongitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val lon2 = Math.toRadians(KAABA_LONGITUDE)
        
        // Calculate difference in longitude
        val deltaLon = lon2 - lon1
        
        // Calculate bearing using spherical trigonometry
        // Formula: θ = atan2(sin(Δlon) * cos(lat2), cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(Δlon))
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        
        val bearingRadians = atan2(y, x)
        
        // Convert to degrees and normalize to 0-360
        var bearingDegrees = Math.toDegrees(bearingRadians)
        bearingDegrees = (bearingDegrees + 360.0) % 360.0
        
        return bearingDegrees
    }
    
    /**
     * Calculate the relative angle between current heading and Qibla direction.
     * 
     * @param qiblaBearing Qibla bearing in degrees (0-360)
     * @param currentHeading Current device heading in degrees (0-360)
     * @return Relative angle in degrees (-180 to 180), where:
     *         - Positive = turn right (clockwise)
     *         - Negative = turn left (counter-clockwise)
     *         - 0 = facing Qibla
     */
    fun calculateRelativeQiblaAngle(
        qiblaBearing: Double,
        currentHeading: Float,
    ): Double {
        // Normalize both angles to 0-360
        val normalizedQibla = (qiblaBearing + 360.0) % 360.0
        val normalizedHeading = (currentHeading.toDouble() + 360.0) % 360.0
        
        // Calculate difference
        var difference = normalizedQibla - normalizedHeading
        
        // Normalize difference to -180 to 180 range
        // This ensures we get the shortest path
        difference = (difference + 180.0) % 360.0 - 180.0
        
        return difference
    }
    
    /**
     * Calculate distance to Kaaba in kilometers (great-circle distance).
     * 
     * @param userLatitude User's latitude in degrees
     * @param userLongitude User's longitude in degrees
     * @return Distance in kilometers, or null if coordinates are invalid
     */
    fun calculateDistanceToKaaba(
        userLatitude: Double,
        userLongitude: Double,
    ): Double? {
        if (!isValidLatitude(userLatitude) || !isValidLongitude(userLongitude)) {
            return null
        }
        
        // Earth's radius in kilometers
        val earthRadius = 6371.0
        
        // Convert to radians
        val lat1 = Math.toRadians(userLatitude)
        val lon1 = Math.toRadians(userLongitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val lon2 = Math.toRadians(KAABA_LONGITUDE)
        
        // Haversine formula
        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1
        
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) + 
                cos(lat1) * cos(lat2) * 
                sin(deltaLon / 2) * sin(deltaLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Check if coordinates are valid
     */
    private fun isValidLatitude(latitude: Double): Boolean {
        return latitude >= -90.0 && latitude <= 90.0 && latitude.isFinite()
    }
    
    private fun isValidLongitude(longitude: Double): Boolean {
        return longitude >= -180.0 && longitude <= 180.0 && longitude.isFinite()
    }
    
    /**
     * Check if user is at or very near Kaaba (within 100 meters)
     */
    private fun isAtKaaba(latitude: Double, longitude: Double): Boolean {
        val distance = calculateDistanceToKaaba(latitude, longitude)
        return distance != null && distance < 0.1 // 100 meters
    }
    
    /**
     * Get Kaaba coordinates
     */
    fun getKaabaLatitude(): Double = KAABA_LATITUDE
    fun getKaabaLongitude(): Double = KAABA_LONGITUDE
    
    /**
     * Normalize an angle to 0-360 degrees range
     */
    fun normalizeAngleDegrees(angle: Double): Double {
        return (angle + 360.0) % 360.0
    }
    
    /**
     * Normalize an angle to 0-360 degrees range (Float version)
     */
    fun normalizeAngleDegrees(angle: Float): Float {
        return (angle + 360f) % 360f
    }
    
    /**
     * Normalize an angle to -180 to 180 degrees range
     */
    fun normalizeAngle180(angle: Double): Double {
        var normalized = angle % 360.0
        if (normalized > 180.0) {
            normalized -= 360.0
        } else if (normalized < -180.0) {
            normalized += 360.0
        }
        return normalized
    }
    
    /**
     * Normalize an angle to -180 to 180 degrees range (Float version)
     */
    fun normalizeAngle180(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized > 180f) {
            normalized -= 360f
        } else if (normalized < -180f) {
            normalized += 360f
        }
        return normalized
    }
}

package com.github.meypod.al_azan.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.github.meypod.al_azan.core.domain.model.compass.SensorAvailability
import com.github.meypod.al_azan.core.domain.model.compass.SensorSource

/**
 * Manages sensor detection, registration, and cleanup for the compass system.
 * 
 * This class provides:
 * - Dynamic sensor availability detection
 * - Intelligent sensor registration with priority-based selection
 * - Safe handling of sensor failures
 * - Automatic cleanup of registered listeners
 * - Thread-safe operations
 */
class CompassSensorManager(
    context: Context,
) {
    
    private val appContext = context.applicationContext
    private val sensorManager: SensorManager by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: throw IllegalStateException("SensorManager service not available")
    }
    
    private val registeredListeners = mutableMapOf<SensorEventListener, MutableSet<Sensor>>() //noban
    private val sensorToListener = mutableMapOf<Sensor, MutableSet<SensorEventListener>>() //noban
    
    /**
     * Detect all available compass-relevant sensors on the device.
     * 
     * @return SensorAvailability object containing availability of all relevant sensors
     */
    fun detectSensorAvailability(): SensorAvailability {
        val sensorTypes = listOf(
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
        
        var hasRotationVector = false
        var hasGeomagneticRotationVector = false
        var hasGameRotationVector = false
        var hasAccelerometer = false
        var hasMagnetometer = false
        var hasGyroscope = false
        var hasGravity = false
        var hasLinearAcceleration = false
        var hasOrientation = false
        
        sensorTypes.forEach { sensorType ->
            val sensor = sensorManager.getDefaultSensor(sensorType)
            when (sensorType) {
                Sensor.TYPE_ROTATION_VECTOR -> hasRotationVector = sensor != null
                Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> hasGeomagneticRotationVector = sensor != null
                Sensor.TYPE_GAME_ROTATION_VECTOR -> hasGameRotationVector = sensor != null
                Sensor.TYPE_ACCELEROMETER -> hasAccelerometer = sensor != null
                Sensor.TYPE_MAGNETIC_FIELD -> hasMagnetometer = sensor != null
                Sensor.TYPE_GYROSCOPE -> hasGyroscope = sensor != null
                Sensor.TYPE_GRAVITY -> hasGravity = sensor != null
                Sensor.TYPE_LINEAR_ACCELERATION -> hasLinearAcceleration = sensor != null
                Sensor.TYPE_ORIENTATION -> hasOrientation = sensor != null
            }
        }
        
        return SensorAvailability(
            hasRotationVector = hasRotationVector,
            hasGeomagneticRotationVector = hasGeomagneticRotationVector,
            hasGameRotationVector = hasGameRotationVector,
            hasAccelerometer = hasAccelerometer,
            hasMagnetometer = hasMagnetometer,
            hasGyroscope = hasGyroscope,
            hasGravity = hasGravity,
            hasLinearAcceleration = hasLinearAcceleration,
            hasOrientation = hasOrientation,
        )
    }
    
    /**
     * Get the best available sensor source based on current availability.
     * 
     * @param availability Current sensor availability
     * @return Best SensorSource, or NONE if no compass sensors available
     */
    fun getBestSensorSource(availability: SensorAvailability): SensorSource {
        return when {
            availability.hasRotationVector -> SensorSource.ROTATION_VECTOR
            availability.hasGeomagneticRotationVector -> SensorSource.GEOMAGNETIC_ROTATION_VECTOR
            availability.hasAccelerometer && availability.hasMagnetometer && availability.hasGyroscope ->
                SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE
            availability.hasAccelerometer && availability.hasMagnetometer ->
                SensorSource.ACCELEROMETER_MAGNETOMETER
            availability.hasOrientation -> SensorSource.ORIENTATION
            availability.hasGameRotationVector -> SensorSource.GAME_ROTATION_VECTOR
            else -> SensorSource.NONE
        }
    }
    
    /**
     * Register a sensor listener for a specific sensor type.
     * 
     * @param listener The listener to register
     * @param sensorType The type of sensor to listen to
     * @param samplingRateUs Sampling rate in microseconds (use SensorManager constants)
     * @return true if registration succeeded, false otherwise
     */
    fun registerListener(
        listener: SensorEventListener,
        sensorType: Int,
        samplingRateUs: Int = SensorManager.SENSOR_DELAY_GAME,
    ): Boolean {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            
            return false
        }
        
        // Check if already registered
        val listenerSensors = registeredListeners[listener] ?: mutableSetOf()
        if (listenerSensors.contains(sensor)) {
            
            return true
        }
        
        // Register the listener
        val result = try {
            sensorManager.registerListener(listener, sensor, samplingRateUs)
        } catch (e: Exception) {
            
            false
        }
        
        if (result) {
            listenerSensors.add(sensor)
            registeredListeners[listener] = listenerSensors
            
            val sensorListeners = sensorToListener[sensor] ?: mutableSetOf()
            sensorListeners.add(listener)
            sensorToListener[sensor] = sensorListeners
            
            
        }
        
        return result
    }
    
    /**
     * Register listeners for multiple sensor types at once.
     * 
     * @param listener The listener to register
     * @param sensorTypes List of sensor types to listen to
     * @param samplingRateUs Sampling rate in microseconds
     * @return Map of sensor type to registration success
     */
    fun registerListeners(
        listener: SensorEventListener,
        sensorTypes: List<Int>,
        samplingRateUs: Int = SensorManager.SENSOR_DELAY_GAME,
    ): Map<Int, Boolean> {
        val results = mutableMapOf<Int, Boolean>()
        sensorTypes.forEach { sensorType ->
            results[sensorType] = registerListener(listener, sensorType, samplingRateUs)
        }
        return results
    }
    
    /**
     * Register sensors based on a desired sensor source.
     * Automatically selects the appropriate sensor types.
     * 
     * @param listener The listener to register
     * @param source The desired sensor source
     * @param availability Current sensor availability
     * @param samplingRateUs Sampling rate in microseconds
     * @return true if all required sensors were registered successfully
     */
    fun registerForSource(
        listener: SensorEventListener,
        source: SensorSource,
        availability: SensorAvailability,
        samplingRateUs: Int = SensorManager.SENSOR_DELAY_GAME,
    ): Boolean {
        val requiredSensors = getSensorTypesForSource(source, availability)
        if (requiredSensors.isEmpty()) {
            
            return false
        }
        
        val results = registerListeners(listener, requiredSensors, samplingRateUs)
        val allSuccessful = results.values.all { it }
        
        if (allSuccessful) {
            
        } else {
            
        }
        
        return allSuccessful
    }
    
    /**
     * Unregister a sensor listener from all sensors.
     * 
     * @param listener The listener to unregister
     */
    fun unregisterListener(listener: SensorEventListener) {
        val sensors = registeredListeners[listener] ?: return
        
        sensors.forEach { sensor ->
            try {
                sensorManager.unregisterListener(listener, sensor)
                
                // Remove from sensorToListener map
                val sensorListeners = sensorToListener[sensor] ?: return@forEach
                sensorListeners.remove(listener)
                if (sensorListeners.isEmpty()) {
                    sensorToListener.remove(sensor)
                }
                
                
            } catch (e: Exception) {
                
            }
        }
        
        registeredListeners.remove(listener)
    }
    
    /**
     * Unregister all listeners from a specific sensor.
     * 
     * @param sensorType The sensor type to unregister from
     */
    fun unregisterListenersForSensor(sensorType: Int) {
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return
        val listeners = sensorToListener[sensor] ?: return
        
        listeners.forEach { listener ->
            try {
                sensorManager.unregisterListener(listener, sensor)
                
            } catch (e: Exception) {
                
            }
        }
        
        sensorToListener.remove(sensor)
        
        // Also remove from registeredListeners
        listeners.forEach { listener ->
            val listenerSensors = registeredListeners[listener] ?: return@forEach
            listenerSensors.remove(sensor)
            if (listenerSensors.isEmpty()) {
                registeredListeners.remove(listener)
            }
        }
    }
    
    /**
     * Unregister all listeners from all sensors.
     */
    fun unregisterAllListeners() {
        registeredListeners.keys.forEach { listener ->
            unregisterListener(listener)
        }
        registeredListeners.clear()
        sensorToListener.clear()
        
    }
    
    /**
     * Get the sensor types needed for a specific sensor source.
     * 
     * @param source The desired sensor source
     * @param availability Current sensor availability
     * @return List of sensor types to register
     */
    fun getSensorTypesForSource(
        source: SensorSource,
        availability: SensorAvailability,
    ): List<Int> {
        return when (source) {
            SensorSource.ROTATION_VECTOR -> 
                if (availability.hasRotationVector) listOf(Sensor.TYPE_ROTATION_VECTOR) else emptyList()
            
            SensorSource.GEOMAGNETIC_ROTATION_VECTOR -> 
                if (availability.hasGeomagneticRotationVector) 
                    listOf(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) 
                else emptyList()
            
            SensorSource.ACCELEROMETER_MAGNETOMETER -> 
                if (availability.hasAccelerometer && availability.hasMagnetometer) 
                    listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD) 
                else emptyList()
            
            SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE -> 
                if (availability.hasAccelerometer && availability.hasMagnetometer && availability.hasGyroscope) 
                    listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_GYROSCOPE) 
                else emptyList()
            
            SensorSource.ORIENTATION -> 
                if (availability.hasOrientation) listOf(Sensor.TYPE_ORIENTATION) else emptyList()
            
            SensorSource.GAME_ROTATION_VECTOR -> 
                if (availability.hasGameRotationVector) listOf(Sensor.TYPE_GAME_ROTATION_VECTOR) else emptyList()
            
            SensorSource.ACCELEROMETER_ONLY -> 
                if (availability.hasAccelerometer) listOf(Sensor.TYPE_ACCELEROMETER) else emptyList()
            
            SensorSource.MAGNETOMETER_ONLY -> 
                if (availability.hasMagnetometer) listOf(Sensor.TYPE_MAGNETIC_FIELD) else emptyList()
            
            SensorSource.NONE -> emptyList()
            SensorSource.SIMULATED -> emptyList()
        }
    }
    
    /**
     * Check if a specific sensor source is available given current sensor availability.
     * 
     * @param source The sensor source to check
     * @param availability Current sensor availability
     * @return true if the source is available
     */
    fun isSourceAvailable(
        source: SensorSource,
        availability: SensorAvailability,
    ): Boolean {
        return getSensorTypesForSource(source, availability).isNotEmpty()
    }
    
    /**
     * Get all available sensor sources in priority order.
     * 
     * @param availability Current sensor availability
     * @return List of available sensor sources, ordered by priority
     */
    fun getAvailableSources(availability: SensorAvailability): List<SensorSource> {
        val allSources = SensorSource.entries.filter { it != SensorSource.NONE && it != SensorSource.SIMULATED }
        return allSources.filter { source ->
            isSourceAvailable(source, availability)
        }.sortedBy { it.ordinal }
    }
    
    /**
     * Check if any compass sensors are currently registered.
     */
    fun hasRegisteredListeners(): Boolean = registeredListeners.isNotEmpty()
    
    /**
     * Get currently registered sensor types.
     */
    fun getRegisteredSensorTypes(): Set<Int> {
        return registeredListeners.values.flatMap { it.map { sensor -> sensor.type } }.toSet()
    }
    
    /**
     * Get sensor object for a given type.
     */
    fun getSensor(sensorType: Int): Sensor? = sensorManager.getDefaultSensor(sensorType)
    
    /**
     * Flush the sensor manager (for testing).
     * Note: This may not work on all devices.
     */
    fun flush(listener: SensorEventListener) {
        try {
            sensorManager.flush(listener)
        } catch (e: Exception) {
            
        }
    }
    
    /**
     * Check if sensor manager is available.
     */
    fun isAvailable(): Boolean = appContext.getSystemService(Context.SENSOR_SERVICE) != null
}

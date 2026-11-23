package com.github.meypod.al_azan.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.CompassFilter
import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import com.github.meypod.al_azan.core.domain.model.compass.CompassState
import com.github.meypod.al_azan.core.domain.model.compass.GeomagneticDeclinationProvider
import com.github.meypod.al_azan.core.domain.model.compass.QiblaCalculator
import com.github.meypod.al_azan.core.domain.model.compass.SensorAvailability
import com.github.meypod.al_azan.core.domain.model.compass.SensorSource
import com.github.meypod.al_azan.core.domain.repository.CompassDiagnostic
import com.github.meypod.al_azan.core.domain.repository.CompassRepository
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CompassRepositoryImpl(
    context: Context,
) : CompassRepository {

    private val appContext = context.applicationContext

    private val compassSensorManager = CompassSensorManager(context)
    private val sensorFusion = CompassSensorFusion(context)
    private val declinationProvider = GeomagneticDeclinationProvider()

    private val headingFilter = CompassFilter(
        CompassFilter.Config(
            emaAlpha = 0.35f,
            maxAngleJump = 180.0f,
        )
    )

    private val _sensorAvailability = MutableStateFlow(SensorAvailability.EMPTY)
    private val _isCalibrating = MutableStateFlow(false)

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentDeclination: Float = 0f

    init {
        detectSensorAvailability()
    }

    private fun detectSensorAvailability() {
        _sensorAvailability.value = compassSensorManager.detectSensorAvailability()
        sensorFusion.initialize(_sensorAvailability.value)
    }

    override val diagnostics: Flow<CompassDiagnostic> = flow {
        val androidSensorManager = getAndroidSensorManager()
        val rotationVector = androidSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = androidSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val geomagneticRot = androidSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val gameRot = androidSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        val gravity = androidSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val linearAccel = androidSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        @Suppress("DEPRECATION")
        val orientation = androidSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        emit(
            CompassDiagnostic(
                hasRotationVector = rotationVector != null,
                hasAccelerometer = accelerometer != null,
                hasMagnetometer = magnetometer != null,
                hasGyroscope = gyroscope != null,
                registeredRotationVector = rotationVector != null,
                registeredAccelerometer = accelerometer != null,
                registeredMagnetometer = magnetometer != null,
                registeredGyroscope = gyroscope != null,
                hasGeomagneticRotationVector = geomagneticRot != null,
                hasGameRotationVector = gameRot != null,
                hasGravity = gravity != null,
                hasLinearAcceleration = linearAccel != null,
                hasOrientation = orientation != null,
            )
        )
    }.conflate()

    override fun compassState(
        latitude: Double?,
        longitude: Double?,
        qiblaBearing: Double?,
    ): Flow<CompassState> = callbackFlow {
        val locationChanged = latitude != currentLatitude || longitude != currentLongitude
        currentLatitude = latitude
        currentLongitude = longitude

        if (locationChanged) {
            updateDeclination()
        }

        sensorFusion.reset()
        detectSensorAvailability()
        sensorFusion.setMagneticDeclination(currentDeclination)
        headingFilter.reset()

        val androidSensorManager = getAndroidSensorManager()
        val scope = this

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                emitSensorUpdate(event, scope, qiblaBearing)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor != null) {
                    sensorFusion.onAccuracyChanged(sensor.type, accuracy)
                }
            }
        }

        val registeredTypes = registerSensors(androidSensorManager, listener)
        trySend(createInitialState(qiblaBearing))

        awaitClose {
            registeredTypes.forEach { sensorType ->
                androidSensorManager.getDefaultSensor(sensorType)?.let { sensor ->
                    try {
                        androidSensorManager.unregisterListener(listener, sensor)
                    } catch (_: Exception) {
                    }
                }
            }
            try {
                androidSensorManager.unregisterListener(listener)
            } catch (_: Exception) {
            }
        }
    }.conflate()

    override fun headings(
        latitude: Double?,
        longitude: Double?,
    ): Flow<CompassReading> = compassState(latitude, longitude).map { state ->
        CompassReading(
            magneticHeadingDegrees = state.currentReading.magneticHeadingDegrees,
            trueHeadingDegrees = state.currentReading.trueHeadingDegrees,
            accuracy = state.currentReading.accuracy,
            sensorSource = state.currentReading.sensorSource,
            timestamp = state.lastUpdateTimestamp,
            isFiltered = state.currentReading.isFiltered,
        )
    }.conflate()

    override suspend fun redetectSensors() {
        detectSensorAvailability()
    }

    override suspend fun isCompassAvailable(): Boolean {
        return _sensorAvailability.value.hasAnyCompassSensor
    }

    override suspend fun getCurrentSensorSource(): SensorSource {
        return sensorFusion.getCurrentState().source
    }

    override suspend fun startCalibration() {
        _isCalibrating.value = true
    }

    override suspend fun stopCalibration() {
        _isCalibrating.value = false
    }

    private fun getAndroidSensorManager(): SensorManager {
        return appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private fun emitSensorUpdate(
        event: SensorEvent,
        scope: ProducerScope<CompassState>,
        qiblaBearing: Double?,
    ) {
        val timestamp = System.nanoTime()
        val fusionResult = sensorFusion.processEvent(event, timestamp) ?: return

        val filteredTrueHeading = headingFilter.filter(
            rawAngle = fusionResult.trueHeading,
            timestamp = timestamp,
            isResetting = false,
        )

        val calculatedQiblaBearing = qiblaBearing ?: calculateQiblaBearing()
        val relativeQiblaAngle = calculatedQiblaBearing?.let { bearing ->
            QiblaCalculator.calculateRelativeQiblaAngle(bearing, filteredTrueHeading)
        }

        val isCalibrating = _isCalibrating.value || fusionResult.accuracy == CompassAccuracy.CALIBRATING
        val hasInterference = fusionResult.hasMagneticInterference

        val warningMessage = when {
            hasInterference -> "Magnetic interference detected. Move away from electronic devices."
            fusionResult.accuracy == CompassAccuracy.LOW ||
                fusionResult.accuracy == CompassAccuracy.UNRELIABLE ->
                "Compass accuracy is low. Move device in a figure-8 pattern."
            else -> null
        }

        val reading = CompassReading(
            magneticHeadingDegrees = fusionResult.magneticHeading,
            trueHeadingDegrees = filteredTrueHeading,
            accuracy = fusionResult.accuracy,
            sensorSource = fusionResult.source,
            timestamp = timestamp,
            isFiltered = true,
        )

        val state = CompassState(
            currentReading = reading,
            qiblaBearing = calculatedQiblaBearing,
            relativeQiblaAngle = relativeQiblaAngle,
            magneticDeclination = currentDeclination,
            sensorAvailability = _sensorAvailability.value,
            isCalibrating = isCalibrating,
            calibrationProgress = null,
            errorMessage = if (fusionResult.source == SensorSource.NONE) {
                "No compass sensors available on this device."
            } else {
                null
            },
            warningMessage = warningMessage,
            lastUpdateTimestamp = timestamp,
        )

        scope.trySend(state)
    }

    private fun createInitialState(qiblaBearing: Double?): CompassState {
        val calculatedQiblaBearing = qiblaBearing ?: calculateQiblaBearing()
        val relativeQiblaAngle = if (calculatedQiblaBearing != null && sensorFusion.hasValidHeading()) {
            val headings = sensorFusion.getLastValidHeadings()
            QiblaCalculator.calculateRelativeQiblaAngle(calculatedQiblaBearing, headings.second)
        } else {
            null
        }

        val fusionState = sensorFusion.getCurrentState()
        val compassReading = if (sensorFusion.hasValidHeading()) {
            val headings = sensorFusion.getLastValidHeadings()
            CompassReading(
                magneticHeadingDegrees = headings.first,
                trueHeadingDegrees = headings.second,
                accuracy = fusionState.accuracy,
                sensorSource = fusionState.source,
                timestamp = fusionState.lastValidTimestamp,
                isFiltered = false,
            )
        } else {
            CompassReading.UNAVAILABLE
        }

        return CompassState(
            currentReading = compassReading,
            qiblaBearing = calculatedQiblaBearing,
            relativeQiblaAngle = relativeQiblaAngle,
            magneticDeclination = currentDeclination,
            sensorAvailability = _sensorAvailability.value,
            isCalibrating = _isCalibrating.value,
            calibrationProgress = null,
            errorMessage = if (!_sensorAvailability.value.hasAnyCompassSensor) {
                "Compass unavailable"
            } else {
                null
            },
            warningMessage = null,
            lastUpdateTimestamp = System.nanoTime(),
        )
    }

    private fun calculateQiblaBearing(): Double? {
        val lat = currentLatitude
        val lon = currentLongitude
        return if (lat != null && lon != null) {
            QiblaCalculator.calculateQiblaBearing(lat, lon)
        } else {
            null
        }
    }

    private fun updateDeclination() {
        val lat = currentLatitude
        val lon = currentLongitude
        currentDeclination = if (lat != null && lon != null) {
            declinationProvider.getDeclination(lat, lon)
        } else {
            0f
        }
    }

    /**
     * Registers only the clean primary sensor tier plus passive magnetometer monitoring (at UI rate
     * when Rotation Vector is primary) so we detect accuracy/interference without fighting the hardware hub.
     */
    private fun registerSensors(
        androidSensorManager: SensorManager,
        listener: SensorEventListener,
    ): List<Int> {
        val availability = _sensorAvailability.value
        val source = sensorFusion.getCurrentState().desiredSource

        val primaryRequests: List<Pair<Int, Int>> = when (source) {
            SensorSource.ROTATION_VECTOR -> buildList {
                add(Sensor.TYPE_ROTATION_VECTOR to SensorManager.SENSOR_DELAY_GAME)
                if (availability.hasMagnetometer) {
                    add(Sensor.TYPE_MAGNETIC_FIELD to SensorManager.SENSOR_DELAY_UI)
                }
            }
            SensorSource.GEOMAGNETIC_ROTATION_VECTOR -> buildList {
                add(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to SensorManager.SENSOR_DELAY_GAME)
                if (availability.hasMagnetometer) {
                    add(Sensor.TYPE_MAGNETIC_FIELD to SensorManager.SENSOR_DELAY_UI)
                }
            }
            SensorSource.ACCELEROMETER_MAGNETOMETER,
            SensorSource.ACCELEROMETER_MAGNETOMETER_GYROSCOPE -> listOf(
                Sensor.TYPE_ACCELEROMETER to SensorManager.SENSOR_DELAY_GAME,
                Sensor.TYPE_MAGNETIC_FIELD to SensorManager.SENSOR_DELAY_GAME,
            )
            SensorSource.ORIENTATION -> @Suppress("DEPRECATION") listOf(
                Sensor.TYPE_ORIENTATION to SensorManager.SENSOR_DELAY_GAME,
            )
            else -> getFallbackSensors(availability).map { it to SensorManager.SENSOR_DELAY_GAME }
        }

        val registered = mutableListOf<Int>()
        primaryRequests.distinctBy { it.first }.forEach { (sensorType, rate) ->
            val sensor = androidSensorManager.getDefaultSensor(sensorType)
            if (sensor != null && androidSensorManager.registerListener(listener, sensor, rate)) {
                registered.add(sensorType)
            }
        }
        return registered
    }

    private fun getFallbackSensors(availability: SensorAvailability): List<Int> {
        return when {
            availability.hasRotationVector -> listOf(Sensor.TYPE_ROTATION_VECTOR)
            availability.hasGeomagneticRotationVector -> listOf(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
            availability.hasAccelerometer && availability.hasMagnetometer -> listOf(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_MAGNETIC_FIELD,
            )
            availability.hasOrientation -> @Suppress("DEPRECATION") listOf(Sensor.TYPE_ORIENTATION)
            else -> emptyList()
        }
    }
}

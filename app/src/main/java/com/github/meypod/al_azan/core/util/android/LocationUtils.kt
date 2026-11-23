package com.github.meypod.al_azan.core.util.android

import android.Manifest.permission
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object LocationUtils {

    fun hasFineLocationPermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(
            context,
            permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocationPermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(
            context,
            permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(context: Context): Boolean =
        hasFineLocationPermission(context) || hasCoarseLocationPermission(context)

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = getLocationManager(context)
        return if (locationManager != null) {
            LocationManagerCompat.isLocationEnabled(locationManager)
        } else {
            false
        }
    }

    fun getLastCachedLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val hasFine = hasFineLocationPermission(context)
        val locationManager = getLocationManager(context) ?: return null
        val availableProviders = runCatching { locationManager.allProviders }.getOrDefault(emptyList())
        val candidateProviders = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
            if (hasFine) {
                add(LocationManager.GPS_PROVIDER)
            }
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }.filter { it in availableProviders }

        return candidateProviders.mapNotNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: Exception) {
                null
            }
        }.filter { isValidCoordinates(it) }
            .maxByOrNull { it.time }
    }

    /**
     * Dual Location Fetch:
     * 1. Immediately reads [getLastCachedLocation] and delivers it via [onImmediateLastKnown]
     *    for an instant response with zero wait.
     * 2. Simultaneously requests a fresh live fix from enabled providers (Fused / GPS / Network)
     *    with a [timeoutMillis] timeout (default 10 seconds).
     * 3. Returns the fresh fix if obtained, or falls back to the immediate last-known fix if
     *    available, or a failure if neither could be obtained.
     */
    suspend fun fetchLocationDual(
        context: Context,
        timeoutMillis: Long = 10_000L,
        onImmediateLastKnown: (Location) -> Unit = {},
    ): Result<Location> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        val lastKnown = withContext(Dispatchers.IO) { getLastCachedLocation(context) }
        if (lastKnown != null) {
            withContext(Dispatchers.Main.immediate) {
                onImmediateLastKnown(lastKnown)
            }
        }

        val fresh = requestFreshLocation(context, timeoutMillis)
        return when {
            fresh != null -> Result.success(fresh)
            lastKnown != null -> Result.success(lastKnown)
            else -> Result.failure(Exception("Location unavailable"))
        }
    }

    suspend fun requestCurrentLocation(
        context: Context,
        timeoutMillis: Long = 10_000L,
        // When true, skip the cached last-known fix and force a fresh one-shot fix. Used by explicit
        // "refresh" taps so re-pressing the button actually re-queries GPS instead of no-op'ing on
        // an identical cached value.
        forceFresh: Boolean = false,
    ): Result<Location> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }

        return withContext(Dispatchers.IO) {
            if (!forceFresh) {
                val lastKnown = getLastCachedLocation(context)
                if (lastKnown != null && isLocationValid(lastKnown)) {
                    return@withContext Result.success(lastKnown)
                }
            }

            val fresh = requestFreshLocation(context, timeoutMillis)
            if (fresh != null) {
                Result.success(fresh)
            } else {
                val fallback = getLastCachedLocation(context)
                if (fallback != null && !forceFresh) {
                    Result.success(fallback)
                } else {
                    Result.failure(Exception("Location unavailable"))
                }
            }
        }
    }

    private suspend fun requestFreshLocation(
        context: Context,
        timeoutMillis: Long,
    ): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        val locationManager = getLocationManager(context) ?: return@withContext null
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return@withContext null

        val hasFine = hasFineLocationPermission(context)
        val availableProviders = runCatching { locationManager.allProviders }.getOrDefault(emptyList())
        fun isProviderReady(provider: String): Boolean =
            provider in availableProviders && runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)

        val hasFused = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderReady(LocationManager.FUSED_PROVIDER)
        val hasGps = hasFine && isProviderReady(LocationManager.GPS_PROVIDER)
        val hasNetwork = isProviderReady(LocationManager.NETWORK_PROVIDER)

        val providerFlows = buildList<Flow<Location>> {
            if (hasFused) {
                add(createSingleLocationFlow(locationManager, LocationManager.FUSED_PROVIDER))
            }
            if (hasGps) {
                add(createSingleLocationFlow(locationManager, LocationManager.GPS_PROVIDER))
            }
            if (hasNetwork) {
                val networkFlow = createSingleLocationFlow(locationManager, LocationManager.NETWORK_PROVIDER)
                if (hasFused || hasGps) {
                    // Brief head start for Fused/GPS precision, while ensuring fast indoor resolution.
                    add(
                        flow {
                            delay(1_500L)
                            emitAll(networkFlow)
                        },
                    )
                } else {
                    add(networkFlow)
                }
            }
        }

        if (providerFlows.isEmpty()) return@withContext null

        withTimeoutOrNull(timeoutMillis) {
            merge(*providerFlows.toTypedArray()).firstOrNull()
        }
    }

    private fun isValidCoordinates(location: Location): Boolean =
        !(location.latitude == 0.0 && location.longitude == 0.0) &&
            !location.latitude.isNaN() && !location.longitude.isNaN()

    private fun isLocationValid(location: Location): Boolean {
        val now = System.currentTimeMillis()
        val age = (now - location.time).toDuration(DurationUnit.MILLISECONDS)

        // If location is older than 15 minutes or accuracy is terrible (> 500m), ignore it
        return isValidCoordinates(location) &&
            age < 15.toDuration(DurationUnit.MINUTES) &&
            location.accuracy > 0 &&
            location.accuracy < 500f
    }

    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private fun createSingleLocationFlow(
        locationManager: LocationManager,
        providerName: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Flow<Location> =
        callbackFlow {
            val signal = CancellationSignal()

            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    providerName,
                    signal,
                    dispatcher.asExecutor(),
                ) { location ->
                    if (location != null && isValidCoordinates(location)) {
                        trySend(location)
                    }
                    // Close cleanly without throwing so sibling providers in merge(...) keep running.
                    close()
                }
            } catch (_: SecurityException) {
                close()
            } catch (_: IllegalArgumentException) {
                close()
            } catch (_: Exception) {
                close()
            }

            awaitClose {
                signal.cancel()
            }
        }

    private fun getLocationManager(context: Context): LocationManager? =
        context.getSystemService(LOCATION_SERVICE) as LocationManager?
}

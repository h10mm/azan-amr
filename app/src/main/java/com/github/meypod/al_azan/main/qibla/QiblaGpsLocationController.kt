package com.github.meypod.al_azan.main.qibla

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.CtaDialog
import com.github.meypod.al_azan.core.util.android.LocationUtils
import kotlinx.coroutines.launch

@Stable
class QiblaGpsController(
    val isFetching: Boolean,
    val requestGpsLocation: () -> Unit,
)

/**
 * Isolated GPS permission + Dual Location Fetch controller for the Qibla screen.
 *
 * Strictly handles runtime location permissions, location-service (GPS) availability checks,
 * and fast dual location fetching (instant lastKnown + fresh fix with 10s timeout), delivering
 * only numeric (lat, lng) coordinates to the existing Qibla & GeomagneticField pipeline.
 * If GPS fails, is denied, or is cancelled, the compass continues smoothly on the saved city.
 */
@Composable
fun rememberQiblaGpsController(
    onCoordinatesFetched: (lat: Double, lng: Double) -> Unit,
): QiblaGpsController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnCoordinatesFetched by rememberUpdatedState(onCoordinatesFetched)

    var isFetching by remember { mutableStateOf(false) }
    var showLocationDisabledDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val startDualLocationFetch: () -> Unit = remember(context, scope) {
        {
            if (!isFetching) {
                scope.launch {
                    isFetching = true
                    var deliveredAnyFix = false
                    try {
                        val result = LocationUtils.fetchLocationDual(
                            context = context,
                            timeoutMillis = 10_000L,
                            onImmediateLastKnown = { lastKnown ->
                                deliveredAnyFix = true
                                latestOnCoordinatesFetched(lastKnown.latitude, lastKnown.longitude)
                            },
                        )
                        val resolved = result.getOrNull()
                        if (resolved != null) {
                            deliveredAnyFix = true
                            latestOnCoordinatesFetched(resolved.latitude, resolved.longitude)
                            Toast.makeText(
                                context,
                                R.string.qibla_gps_updated,
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else if (!deliveredAnyFix) {
                            Toast.makeText(
                                context,
                                R.string.qibla_gps_fetch_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    } finally {
                        isFetching = false
                    }
                }
            }
        }
    }

    val proceedAfterPermissionGranted: () -> Unit = remember(context, startDualLocationFetch) {
        {
            if (!LocationUtils.isLocationEnabled(context)) {
                showLocationDisabledDialog = true
            } else {
                startDualLocationFetch()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted || LocationUtils.hasLocationPermission(context)) {
            showPermanentlyDeniedDialog = false
            proceedAfterPermissionGranted()
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )

            if (permanentlyDenied) {
                showPermanentlyDeniedDialog = true
            } else {
                Toast.makeText(
                    context,
                    R.string.qibla_gps_permission_denied_brief,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (LocationUtils.isLocationEnabled(context)) {
            showLocationDisabledDialog = false
            if (LocationUtils.hasLocationPermission(context)) {
                startDualLocationFetch()
            }
        }
    }

    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (LocationUtils.hasLocationPermission(context)) {
            showPermanentlyDeniedDialog = false
            proceedAfterPermissionGranted()
        }
    }

    val requestGpsLocation: () -> Unit = remember(context, proceedAfterPermissionGranted, permissionLauncher) {
        {
            if (!isFetching) {
                if (LocationUtils.hasLocationPermission(context)) {
                    proceedAfterPermissionGranted()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }
        }
    }

    if (showLocationDisabledDialog) {
        CtaDialog(
            title = stringResource(R.string.location_service_required_title),
            text = stringResource(R.string.qibla_gps_service_disabled),
            confirmLabel = stringResource(R.string.open_settings_label),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                showLocationDisabledDialog = false
                try {
                    locationSettingsLauncher.launch(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                }
            },
            onDismissRequest = {
                showLocationDisabledDialog = false
            },
        )
    }

    if (showPermanentlyDeniedDialog) {
        CtaDialog(
            title = stringResource(R.string.permission_denied),
            text = stringResource(R.string.qibla_gps_permission_permanently_denied),
            confirmLabel = stringResource(R.string.open_settings_label),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                showPermanentlyDeniedDialog = false
                try {
                    appSettingsLauncher.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                    )
                    Toast.makeText(context, R.string.open_permissions_guidance, Toast.LENGTH_LONG).show()
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_LONG).show()
                }
            },
            onDismissRequest = {
                showPermanentlyDeniedDialog = false
            },
        )
    }

    return remember(isFetching, requestGpsLocation) {
        QiblaGpsController(
            isFetching = isFetching,
            requestGpsLocation = requestGpsLocation,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

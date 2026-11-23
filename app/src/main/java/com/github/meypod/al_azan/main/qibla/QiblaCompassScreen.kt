package com.github.meypod.al_azan.main.qibla

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.repository.CompassDiagnostic
import com.github.meypod.al_azan.core.presentation.DarkSuccess
import com.github.meypod.al_azan.core.presentation.DarkWarning
import com.github.meypod.al_azan.core.presentation.LightSuccess
import com.github.meypod.al_azan.core.presentation.LightWarning
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.util.android.LocationUtils
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

enum class CompassDisplayMode {
    CLASSIC_DIAL,
    DIRECT_POINTER,
}

private fun normalize360(deg: Float): Float = ((deg % 360f) + 360f) % 360f

private fun shortestAngleDelta(from: Float, to: Float): Float {
    val a = normalize360(from)
    val b = normalize360(to)
    return ((b - a + 540f) % 360f) - 180f
}

/**
 * محرك حركة نابض فيزيائي مثبّط (Critically Damped Spring) يعمل بسرعة شاشة الهاتف
 * يمنع أي ارتعاش أو قفزات ويجعل البوصلة ناعمة كالبوصلة السائلة.
 */
@Composable
private fun rememberFluidAngle(targetProvider: () -> Float): Float {
    var position by remember { mutableFloatStateOf(normalize360(targetProvider())) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var velocity = 0f
        val omega = 12.0f // استجابة فيزيائية سريعة وسلسة بدون اهتزاز
        var lastNanos = withFrameNanos { it }

        while (true) {
            val nowNanos = withFrameNanos { it }
            val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastNanos = nowNanos

            val target = normalize360(targetProvider())
            if (!initialized && target != 0f) {
                position = target
                velocity = 0f
                initialized = true
                continue
            }

            val displacement = shortestAngleDelta(position, target)

            if (abs(displacement) < 0.03f && abs(velocity) < 0.08f) {
                position = target
                velocity = 0f
            } else {
                // معادلة النابض المثبّط حرجاً بدون أي ارتداد زائد
                val expFactor = exp(-omega * dt)
                val newVelocity = (velocity - omega * omega * displacement * dt) * expFactor
                val newDisplacement = (displacement + (velocity + omega * displacement) * dt) * expFactor
                position = normalize360(target - newDisplacement)
                velocity = newVelocity
            }
        }
    }

    return position
}

@Composable
fun QiblaCompassScreen(
    uiState: QiblaCompassUiState,
    accuracy: CompassAccuracy,
    diagnostic: CompassDiagnostic?,
    headingProvider: () -> Float,
    sensorSource: String,
    isCalibrating: Boolean,
    calibrationProgress: Float,
    onAction: (QiblaCompassUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LockOrientationEffect(uiState.isOrientationLocked)

    val gpsController = rememberQiblaGpsController(
        onCoordinatesFetched = { lat, lng ->
            onAction(
                QiblaCompassUiAction.OnLocationFetched(
                    CalculationLocationDetail(lat = lat, long = lng),
                ),
            )
        },
    )

    var displayMode by rememberSaveable { mutableStateOf(CompassDisplayMode.DIRECT_POINTER) }

    if (uiState.showDiagnostics) {
        CompassDiagnosticDialog(
            diagnostic = diagnostic,
            sensorSource = sensorSource,
            onDismiss = { onAction(QiblaCompassUiAction.OnDismissDiagnostics) },
        )
    }

    ScreenScaffold(
        title = stringResource(R.string.qibla_compass),
        onBackClick = { NavigationController.navigateBack() },
        scrollable = false,
        modifier = modifier,
        actions = {
            CompassIconButton(
                icon = R.drawable.info_variant_outline,
                contentDescription = "Diagnostics",
                onClick = { onAction(QiblaCompassUiAction.OnDiagnosticsClick) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.page_padding)),
        ) {
            // شريط التحكم العلوي: زر التبديل + قفل الشاشة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = {
                        displayMode = if (displayMode == CompassDisplayMode.CLASSIC_DIAL) {
                            CompassDisplayMode.DIRECT_POINTER
                        } else {
                            CompassDisplayMode.CLASSIC_DIAL
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.compass_outline),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = if (displayMode == CompassDisplayMode.CLASSIC_DIAL) {
                                "المؤشر المباشر"
                            } else {
                                "قرص البوصلة"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                CompassIconButton(
                    icon = if (uiState.isOrientationLocked) {
                        R.drawable.outline_screen_lock_rotation_24
                    } else {
                        R.drawable.outline_screen_rotation_24
                    },
                    contentDescription = stringResource(
                        if (uiState.isOrientationLocked) {
                            R.string.qibla_unlock_orientation
                        } else {
                            R.string.qibla_lock_orientation
                        },
                    ),
                    active = uiState.isOrientationLocked,
                    onClick = { onAction(QiblaCompassUiAction.OnToggleOrientationLock) },
                )
            }

            // مساحة البوصلة الرئيسية (تشغل معظم الشاشة)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val compassDiameter = minOf(maxWidth * 0.94f, maxHeight * 0.96f)

                if (displayMode == CompassDisplayMode.CLASSIC_DIAL) {
                    ClassicCompassDial(
                        headingProvider = headingProvider,
                        qiblaDegrees = uiState.qiblaDegrees,
                        modifier = Modifier.size(compassDiameter),
                    )
                } else {
                    DirectQiblaPointer(
                        headingProvider = headingProvider,
                        qiblaDegrees = uiState.qiblaDegrees,
                        modifier = Modifier.size(compassDiameter),
                    )
                }
            }

            // تنبيه لطيف لحركة المعايرة (رقم 8) يظهر فقط عند انخفاض دقة الحساس أو رصد تشويش معدني قريب
            GentleCalibrationBanner(accuracy = accuracy)

            // بطاقة معلومات مدمجة وصغيرة في الأسفل حتى لا تأخذ مساحة البوصلة
            CompactInfoCard(
                uiState = uiState,
                accuracy = accuracy,
                isFetchingLocation = gpsController.isFetching,
                onRefreshLocation = gpsController.requestGpsLocation,
            )
        }
    }
}

@Composable
private fun GentleCalibrationBanner(accuracy: CompassAccuracy) {
    val needsCalibration = accuracy == CompassAccuracy.LOW ||
        accuracy == CompassAccuracy.UNRELIABLE ||
        accuracy == CompassAccuracy.CALIBRATING ||
        accuracy == CompassAccuracy.MAGNETIC_INTERFERENCE

    AnimatedVisibility(
        visible = needsCalibration,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        val isInterference = accuracy == CompassAccuracy.MAGNETIC_INTERFERENCE
        val containerColor = if (isInterference) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        val contentColor = if (isInterference) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FigureEightAnimationIcon(
                    tint = contentColor,
                    modifier = Modifier.size(width = 36.dp, height = 20.dp),
                )
                Text(
                    text = stringResource(
                        if (isInterference) R.string.qibla_interference_hint else R.string.qibla_calibration_hint
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FigureEightAnimationIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "figure8")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "figure8Phase",
    )

    Canvas(modifier = modifier) {
        val rx = size.width * 0.42f
        val ry = size.height * 0.36f
        val cx = size.width / 2f
        val cy = size.height / 2f

        val path = Path()
        val steps = 48
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2.0 * PI).toFloat()
            val x = cx + rx * sin(t)
            val y = cy + ry * sin(2f * t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = tint.copy(alpha = 0.45f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        val dotX = cx + rx * sin(phase)
        val dotY = cy + ry * sin(2f * phase)
        drawCircle(
            color = tint,
            radius = 3.2.dp.toPx(),
            center = Offset(dotX, dotY),
        )
    }
}

@Composable
private fun ClassicCompassDial(
    headingProvider: () -> Float,
    qiblaDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val baseImage = if (isDark) R.drawable.compass_base_dark else R.drawable.compass_base_light

    val smoothHeading = rememberFluidAngle(headingProvider)

    val isAligned by remember(qiblaDegrees) {
        derivedStateOf {
            if (qiblaDegrees == null) {
                false
            } else {
                abs(shortestAngleDelta(smoothHeading, qiblaDegrees)) <= 3f
            }
        }
    }

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isAligned) {
        if (isAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        )
        Image(
            painter = painterResource(baseImage),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -smoothHeading },
        )
        if (qiblaDegrees != null) {
            Image(
                painter = painterResource(R.drawable.qibla_indicator),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = if (isAligned) ColorFilter.tint(Color(0xFF00C853)) else null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = -smoothHeading + qiblaDegrees },
            )
        }

        Text(
            text = "${normalize360(smoothHeading).roundToInt()}°",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DirectQiblaPointer(
    headingProvider: () -> Float,
    qiblaDegrees: Float?,
    modifier: Modifier = Modifier,
) {
    val qiblaTarget = qiblaDegrees ?: 136f
    val smoothHeading = rememberFluidAngle(headingProvider)
    val relativeAngle = shortestAngleDelta(smoothHeading, qiblaTarget)

    val isAligned = abs(relativeAngle) <= 3.5f

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isAligned) {
        if (isAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val circleColor = if (isAligned) Color(0xFF00C853) else MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val outerDiameter = minOf(maxWidth, maxHeight) * 0.96f
        val innerDiameter = outerDiameter * 0.84f

        Box(
            modifier = Modifier.size(outerDiameter),
            contentAlignment = Alignment.Center,
        ) {
            // مؤشر السهم الخارجي الكبير
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = relativeAngle },
                contentAlignment = Alignment.TopCenter,
            ) {
                Canvas(modifier = Modifier.size(outerDiameter * 0.14f)) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = path, color = circleColor)
                }
            }

            // الحلقة الدائرية الكبيرة ومؤشر الكعبة
            Box(
                modifier = Modifier
                    .size(innerDiameter)
                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .border(width = 8.dp, color = circleColor, shape = CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.qibla_indicator),
                        contentDescription = "Kaaba",
                        modifier = Modifier.size(innerDiameter * 0.38f),
                        colorFilter = if (isAligned) ColorFilter.tint(Color(0xFF00C853)) else null,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAligned) {
                            "أنت تواجه القبلة الآن"
                        } else {
                            "${abs(relativeAngle).roundToInt()}° ${if (relativeAngle > 0) "يميناً" else "يساراً"}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isAligned) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    uiState: QiblaCompassUiState,
    accuracy: CompassAccuracy,
    isFetchingLocation: Boolean,
    onRefreshLocation: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // الصف الأول: الموقع وزر تحديث GPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.qibla_location_label) + " ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = uiState.locationLabel.text(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    onClick = onRefreshLocation,
                    enabled = !isFetchingLocation,
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.outline_location_searching_24),
                                contentDescription = stringResource(R.string.qibla_refresh_location),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // الصف الثاني: زاوية القبلة + دقة البوصلة في سطر واحد مدمج
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                uiState.qiblaDegrees?.let { qibla ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.qibla_label) + " ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.qibla_degrees_from_north, qibla.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.qibla_accuracy_label) + " ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(accuracy.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accuracy.color(),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun CompassDiagnosticDialog(
    diagnostic: CompassDiagnostic?,
    sensorSource: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Sensor Diagnostics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Active Source: $sensorSource",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider()
                if (diagnostic == null) {
                    Text("Loading diagnostics...")
                } else {
                    DiagnosticItem("Rotation Vector", diagnostic.hasRotationVector, diagnostic.registeredRotationVector)
                    DiagnosticItem("Accelerometer", diagnostic.hasAccelerometer, diagnostic.registeredAccelerometer)
                    DiagnosticItem("Magnetometer", diagnostic.hasMagnetometer, diagnostic.registeredMagnetometer)
                    DiagnosticItem("Gyroscope", diagnostic.hasGyroscope, diagnostic.registeredGyroscope)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}

@Composable
fun DiagnosticItem(name: String, hasSensor: Boolean, registered: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, fontWeight = FontWeight.Medium)
        val statusText = when {
            !hasSensor -> "Missing"
            !registered -> "Failed"
            else -> "Active"
        }
        val statusColor = when {
            !hasSensor -> MaterialTheme.colorScheme.error
            !registered -> MaterialTheme.colorScheme.error
            else -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) DarkSuccess else LightSuccess
        }
        Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompassIconButton(
    icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    loading: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        shape = MaterialTheme.shapes.medium,
        color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shadowElevation = 2.dp,
        modifier = modifier.size(CompassButtonSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(CompassIconSize),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Icon(painterResource(icon), contentDescription)
            }
        }
    }
}

private val CompassButtonSize = 44.dp
private val CompassIconSize = 22.dp

@Composable
private fun QiblaLocationLabel.text(): String =
    when (this) {
        QiblaLocationLabel.None -> stringResource(R.string.qibla_no_location)
        QiblaLocationLabel.FromSettings -> stringResource(R.string.qibla_location_from_settings)
        is QiblaLocationLabel.Fetched -> String.format(
            Locale.US,
            "%.2f, %.2f",
            detail.lat,
            detail.long,
        )
        is QiblaLocationLabel.Cached -> String.format(
            Locale.US,
            "%.2f, %.2f",
            detail.lat,
            detail.long,
        )
    }

private fun CompassAccuracy.labelRes(): Int =
    when (this) {
        CompassAccuracy.HIGH -> R.string.compass_accuracy_high
        CompassAccuracy.MEDIUM -> R.string.compass_accuracy_medium
        CompassAccuracy.LOW -> R.string.compass_accuracy_low
        CompassAccuracy.UNRELIABLE -> R.string.compass_accuracy_unreliable
        CompassAccuracy.NO_SENSOR -> R.string.compass_sensor_unavailable
        CompassAccuracy.CALIBRATING -> R.string.compass_calibrating
        CompassAccuracy.MAGNETIC_INTERFERENCE -> R.string.compass_magnetic_interference
        CompassAccuracy.TEMPORARILY_UNAVAILABLE -> R.string.compass_temporarily_unavailable
    }

@Composable
private fun CompassAccuracy.color(): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        CompassAccuracy.HIGH -> if (isDark) DarkSuccess else LightSuccess
        CompassAccuracy.MEDIUM -> if (isDark) DarkWarning else LightWarning
        CompassAccuracy.LOW, CompassAccuracy.UNRELIABLE, CompassAccuracy.NO_SENSOR,
        CompassAccuracy.TEMPORARILY_UNAVAILABLE ->
            MaterialTheme.colorScheme.error
        CompassAccuracy.CALIBRATING -> MaterialTheme.colorScheme.primary
        CompassAccuracy.MAGNETIC_INTERFERENCE -> MaterialTheme.colorScheme.error
    }
}

@Composable
private fun LockOrientationEffect(locked: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    DisposableEffect(locked) {
        activity.requestedOrientation = if (locked) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

package com.github.meypod.al_azan.main.tasbih

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.bidiLtr
import kotlinx.coroutines.delay

private val AmiriFontFamily = FontFamily(
    Font(resId = R.font.amiri_regular, weight = FontWeight.Normal),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    uiState: TasbihUiState,
    onAction: (TasbihUiAction) -> Unit,
    onIncrementWithOutcome: () -> TasbihIncrementOutcome,
    onCycleSensoryModeWithReport: () -> SensoryFeedbackMode,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentState by rememberUpdatedState(uiState)
    val latestIncrement by rememberUpdatedState(onIncrementWithOutcome)

    var pulseTrigger by remember { mutableIntStateOf(0) }
    var targetScale by remember { mutableFloatStateOf(1f) }
    var hasTapped by rememberSaveable { mutableStateOf(false) }

    val animatedCircleScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "tasbihCircleScale",
    )

    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger > 0) {
            targetScale = 0.94f
            delay(75L)
            targetScale = 1f
        }
    }

    val performCount: () -> Unit = remember {
        {
            hasTapped = true
            val outcome = latestIncrement()
            val tasbih = currentState.tasbih
            TasbihHaptics.performFeedback(
                context = context,
                vibrationEnabled = tasbih.vibrationEnabled,
                soundEnabled = tasbih.soundEnabled,
                completedRound = outcome.completedRound,
            )
            pulseTrigger++
        }
    }

    // Hardware Volume Up / Down keys counting (respected if volumeKeysEnabled)
    val volumeKeysEnabled = uiState.tasbih.volumeKeysEnabled
    DisposableEffect(volumeKeysEnabled) {
        if (volumeKeysEnabled) {
            TasbihVolumeKeyBus.onVolumeKeyIncrement = performCount
        } else {
            TasbihVolumeKeyBus.onVolumeKeyIncrement = null
        }
        onDispose {
            TasbihVolumeKeyBus.onVolumeKeyIncrement = null
        }
    }

    // Continuous Arabic Speech Recognizer
    val speechController = remember(context) {
        TasbihSpeechController(
            context = context,
            getCurrentDhikrText = { currentState.tasbih.activeDhikrText },
            onCountDetected = { times ->
                repeat(times.coerceIn(1, 5)) { performCount() }
            },
            onListeningStateChanged = { listening ->
                onAction(TasbihUiAction.OnVoiceListeningChanged(listening))
            },
            onErrorMessage = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
        )
    }

    DisposableEffect(speechController) {
        onDispose { speechController.release() }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            speechController.start()
        } else {
            Toast.makeText(context, context.getString(R.string.tasbih_mic_permission_toast), Toast.LENGTH_SHORT).show()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) {
            FloatingTasbihService.start(context)
            Toast.makeText(context, context.getString(R.string.tasbih_floating_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    val handleVoiceClick: () -> Unit = {
        if (uiState.isVoiceListening) {
            speechController.stop()
        } else {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                speechController.start()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val handleFloatingClick: () -> Unit = {
        if (uiState.isFloatingRunning) {
            FloatingTasbihService.stop(context)
            Toast.makeText(context, context.getString(R.string.tasbih_floating_disabled), Toast.LENGTH_SHORT).show()
        } else if (Settings.canDrawOverlays(context)) {
            FloatingTasbihService.start(context)
            Toast.makeText(context, context.getString(R.string.tasbih_floating_enabled), Toast.LENGTH_SHORT).show()
        } else {
            onAction(TasbihUiAction.OnOpenOverlayPermissionDialog)
        }
    }

    val handleSensoryClick: () -> Unit = {
        val newMode = onCycleSensoryModeWithReport()
        val message = when (newMode) {
            SensoryFeedbackMode.VIBRATE_ONLY -> context.getString(R.string.tasbih_feedback_vibrate)
            SensoryFeedbackMode.SOUND_AND_VIBRATE -> context.getString(R.string.tasbih_feedback_sound_vibrate)
            SensoryFeedbackMode.SILENT -> context.getString(R.string.tasbih_feedback_silent)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val tasbih = uiState.tasbih
    val animatedProgress by animateFloatAsState(
        targetValue = tasbih.progressFraction,
        animationSpec = tween(durationMillis = 260),
        label = "tasbihProgress",
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TasbihTopBarClean(
                sensoryMode = tasbih.sensoryMode,
                isFloatingRunning = uiState.isFloatingRunning,
                isVoiceListening = uiState.isVoiceListening,
                onBackClick = onBackClick,
                onVoiceClick = handleVoiceClick,
                onSensoryClick = handleSensoryClick,
                onFloatingClick = handleFloatingClick,
                onSettingsClick = { onAction(TasbihUiAction.OnOpenSettingsSheet) },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            val isNarrowScreen = maxWidth < 360.dp
            val sidePadding = if (isNarrowScreen) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sidePadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. CENTERED DHIKR AREA (No Card/Box, authentic Amiri font)
                CenteredDhikrHeader(
                    activeDhikrText = tasbih.activeDhikrText,
                    onOpenDhikrPicker = { onAction(TasbihUiAction.OnOpenDhikrPicker) },
                )

                // 2. INDEPENDENT TARGET ROW (Order: 33, 100, ∞, Custom ✎)
                IndependentTargetRow(
                    activeTargetType = tasbih.activeTargetType,
                    isSequentialMode = tasbih.isSequentialMode,
                    savedCustomTarget = tasbih.savedCustomTarget,
                    onSelectTargetType = { onAction(TasbihUiAction.OnSelectTargetType(it)) },
                    onSaveCustomTarget = { onAction(TasbihUiAction.OnSaveCustomTarget(it)) },
                )

                // 3. CENTRAL ONE-HANDED TOUCH CARD (Ring exactly centered horizontally & vertically)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = performCount,
                        ),
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        // Responsive ring size: adapts to screen size without distortion
                        val maxDimension = minOf(maxWidth * 0.72f, maxHeight * 0.65f)
                        val ringSize = maxDimension.coerceIn(180.dp, 240.dp)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)

                        // Top: Sequential stages indicator (if enabled)
                        if (tasbih.isSequentialMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp),
                            ) {
                                SequentialStagesIndicator(activeIndex = tasbih.sequentialStepIndex)
                            }
                        }

                        // Center: Large Progress Ring & Count - EXACTLY in the Center
                        Box(
                            modifier = Modifier
                                .size(ringSize)
                                .align(Alignment.Center)
                                .scale(animatedCircleScale),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 12.dp.toPx()
                                val inset = strokeWidth / 2f
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(inset, inset)

                                // Track circle
                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                )

                                // Progress arc
                                val sweep = if (tasbih.activeTarget > 0) {
                                    (animatedProgress * 360f).coerceIn(0f, 360f)
                                } else {
                                    if (tasbih.activeCount > 0) 360f else 0f
                                }
                                if (sweep > 0f) {
                                    drawArc(
                                        color = primaryColor,
                                        startAngle = -90f,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                    )
                                }
                            }

                            // Center Count and Ratio Capsule
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = tasbih.activeCount.toString(),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 60.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )

                                Spacer(Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                ) {
                                    val ratioText = if (tasbih.activeTarget > 0) {
                                        bidiLtr("${tasbih.activeTarget} / ${tasbih.activeCount}")
                                    } else {
                                        "∞"
                                    }
                                    Text(
                                        text = ratioText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        // Bottom: Tap Hint anchored at the bottom of the card
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !hasTapped && tasbih.activeCount == 0,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Text(
                                    text = stringResource(R.string.tasbih_tap_anywhere),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // 4. BOTTOM ANCHOR BAR (شريط ارتكاز سفلي هادئ بارتفاع 48.dp مع قائمة التصفير المنسدلة)
                BottomAnchorBar(
                    totalCount = tasbih.totalCount,
                    cycleCount = tasbih.cycleCount,
                    onResetCurrent = { onAction(TasbihUiAction.OnConfirmResetCurrent) },
                    onResetAll = { onAction(TasbihUiAction.OnConfirmResetAll) },
                )
            }
        }
    }

    // DIALOG 1: Reset Confirmation AlertDialog
    if (uiState.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onAction(TasbihUiAction.OnDismissResetDialog) },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_reset_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tasbih_reset_confirm_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAction(TasbihUiAction.OnConfirmResetCurrent) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_reset_current),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { onAction(TasbihUiAction.OnDismissResetDialog) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(
                        onClick = { onAction(TasbihUiAction.OnConfirmResetAll) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tasbih_reset_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )
    }

    // DIALOG 2: Custom Target Input Dialog
    if (uiState.showCustomTargetDialog) {
        var inputTargetText by rememberSaveable {
            mutableStateOf(tasbih.savedCustomTarget?.toString() ?: "300")
        }
        AlertDialog(
            onDismissRequest = { onAction(TasbihUiAction.OnDismissCustomTargetDialog) },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_custom_target_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.tasbih_custom_target_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = inputTargetText,
                        onValueChange = { input ->
                            inputTargetText = input.filter { it.isDigit() }.take(6)
                        },
                        label = { Text(stringResource(R.string.tasbih_target_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputTargetText.toIntOrNull()?.coerceAtLeast(1) ?: 33
                        onAction(TasbihUiAction.OnSaveCustomTarget(parsed))
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_save_activate),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(TasbihUiAction.OnDismissCustomTargetDialog) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )
    }

    // DIALOG 3: Dhikr Picker Dialog (with inline "إضافة ذكر جديد (+)" and delete)
    if (uiState.showDhikrPickerSheet) {
        var isAddingDhikr by rememberSaveable { mutableStateOf(false) }
        var newDhikrText by rememberSaveable { mutableStateOf("") }
        var newDhikrTarget by rememberSaveable { mutableStateOf("33") }

        AlertDialog(
            onDismissRequest = {
                isAddingDhikr = false
                onAction(TasbihUiAction.OnDismissDhikrPicker)
            },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_choose_dhikr),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // List of Dhikrs
                    tasbih.items.forEach { item ->
                        val isSelected = item.id == tasbih.selectedId && !tasbih.isSequentialMode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAction(TasbihUiAction.OnSelectItem(item.id))
                                    onAction(TasbihUiAction.OnDismissDhikrPicker)
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (item.isCustom) {
                                    IconButton(
                                        onClick = { onAction(TasbihUiAction.OnDeleteCustomItem(item.id)) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.delete),
                                            contentDescription = stringResource(R.string.tasbih_delete_dhikr),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Add Custom Dhikr Button / Form
                    if (!isAddingDhikr) {
                        Button(
                            onClick = { isAddingDhikr = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_edit_24),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tasbih_add_new_dhikr),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.tasbih_add_custom_dhikr),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedTextField(
                                value = newDhikrText,
                                onValueChange = { newDhikrText = it },
                                label = { Text(stringResource(R.string.tasbih_dhikr_text_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = newDhikrTarget,
                                    onValueChange = { newDhikrTarget = it.filter { ch -> ch.isDigit() }.take(6) },
                                    label = { Text(stringResource(R.string.tasbih_target_label)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                Button(
                                    onClick = {
                                        val trimmed = newDhikrText.trim()
                                        if (trimmed.isNotEmpty()) {
                                            val targetNum = newDhikrTarget.toIntOrNull() ?: 33
                                            onAction(TasbihUiAction.OnAddCustomItem(trimmed, targetNum))
                                            newDhikrText = ""
                                            newDhikrTarget = "33"
                                            isAddingDhikr = false
                                        }
                                    },
                                    enabled = newDhikrText.isNotBlank(),
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(text = stringResource(R.string.tasbih_save), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isAddingDhikr = false
                        onAction(TasbihUiAction.OnDismissDhikrPicker)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.close), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }

    // MODAL BOTTOM SHEET: Settings & Stats (Image 2 style - compact without add-dhikr)
    if (uiState.showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(TasbihUiAction.OnDismissSettingsSheet) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            TasbihSettingsAndStatsSheet(
                tasbih = tasbih,
                onClose = { onAction(TasbihUiAction.OnDismissSettingsSheet) },
                onResetTotal = { onAction(TasbihUiAction.OnResetTotal) },
                onToggleSequential = { onAction(TasbihUiAction.OnSetSequentialMode(it)) },
                onToggleVolumeKeys = { onAction(TasbihUiAction.OnSetVolumeKeys(it)) },
            )
        }
    }

    // DIALOG 4: Floating Bubble Overlay Permission Dialog
    if (uiState.showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { onAction(TasbihUiAction.OnDismissOverlayPermissionDialog) },
            title = {
                Text(
                    text = stringResource(R.string.tasbih_overlay_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tasbih_overlay_msg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAction(TasbihUiAction.OnDismissOverlayPermissionDialog)
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri(),
                            )
                            overlayPermissionLauncher.launch(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_open_permission),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(TasbihUiAction.OnDismissOverlayPermissionDialog) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_later),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )
    }
}

/**
 * Top App Bar clean:
 * Start (Right in RTL): Back Button (48.dp) + Voice Counting Capsule ("🎤 تسبيح بالصوت")
 * End (Left in RTL): 3 action icon buttons (Sensory Feedback 3-state, Floating Bubble, Settings Gear).
 */
@Composable
private fun TasbihTopBarClean(
    sensoryMode: SensoryFeedbackMode,
    isFloatingRunning: Boolean,
    isVoiceListening: Boolean,
    onBackClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onSensoryClick: () -> Unit,
    onFloatingClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Start (Right in RTL): Back Button + Voice Counting Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Compact Capsule: "🎤 تسبيح بالصوت"
            val voiceBgColor by animateColorAsState(
                targetValue = if (isVoiceListening) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                label = "voiceBgColor",
            )
            val voiceFgColor by animateColorAsState(
                targetValue = if (isVoiceListening) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                label = "voiceFgColor",
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = voiceBgColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isVoiceListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
                ),
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onVoiceClick),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = stringResource(R.string.tasbih_voice_btn),
                        tint = voiceFgColor,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = stringResource(R.string.tasbih_voice_btn),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        ),
                        color = voiceFgColor,
                    )
                }
            }
        }

        // End (Left in RTL): 3 Action Buttons (Sensory, Floating Bubble, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Sensory Mode
            val sensoryIconRes = when (sensoryMode) {
                SensoryFeedbackMode.VIBRATE_ONLY -> R.drawable.ic_vibration
                SensoryFeedbackMode.SOUND_AND_VIBRATE -> R.drawable.outline_volume_up_24
                SensoryFeedbackMode.SILENT -> R.drawable.outline_volume_off
            }
            TopSquareActionButton(
                iconRes = sensoryIconRes,
                contentDescription = stringResource(R.string.tasbih_sensory_cd),
                active = sensoryMode != SensoryFeedbackMode.SILENT,
                onClick = onSensoryClick,
            )

            // 2. Floating Bubble
            TopSquareActionButton(
                iconRes = R.drawable.ic_floating_bubble,
                contentDescription = stringResource(R.string.tasbih_floating_cd),
                active = isFloatingRunning,
                onClick = onFloatingClick,
            )

            // 3. Settings
            TopSquareActionButton(
                iconRes = R.drawable.settings,
                contentDescription = stringResource(R.string.tasbih_settings_stats),
                active = false,
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun TopSquareActionButton(
    iconRes: Int,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "squareActionContainer",
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "squareActionTint",
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Centered Dhikr Area without Box / Card:
 * - Dhikr text in authentic Amiri spiritual font, centered on the screen.
 * - For long dhikrs: no right padding, adaptive font size down to 19.sp, no ellipsis truncation.
 * - Circular chevron button placed at far left (Alignment.CenterEnd in RTL).
 */
@Composable
private fun CenteredDhikrHeader(
    activeDhikrText: String,
    onOpenDhikrPicker: () -> Unit,
) {
    val fontSize = remember(activeDhikrText) {
        when {
            activeDhikrText.length <= 15 -> 28.sp
            activeDhikrText.length <= 25 -> 23.sp
            else -> 19.sp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Centered Dhikr text taking full available width up to the left chevron button
        Text(
            text = activeDhikrText,
            style = TextStyle(
                fontFamily = AmiriFontFamily,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize.value * 1.35).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 42.dp) // Leave room only for the circular button on far left
                .clickable(onClick = onOpenDhikrPicker),
        )

        // Circular chevron button at far left (CenterEnd in RTL)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .clickable(onClick = onOpenDhikrPicker),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = stringResource(R.string.tasbih_choose_dhikr),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Independent Target Row: 4 Equal Circles (38.dp each)
 * Fixed Order from Right to Left: (٣٣ ، ١٠٠ ، ∞ ، تخصيص ✎)
 * Strictly ONE circle highlighted at a time!
 */
@Composable
private fun IndependentTargetRow(
    activeTargetType: ActiveTargetType,
    isSequentialMode: Boolean,
    savedCustomTarget: Int?,
    onSelectTargetType: (ActiveTargetType) -> Unit,
    onSaveCustomTarget: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circle 1 (Far Right in RTL): 33 (٣٣)
        TargetCircle(
            text = "٣٣",
            selected = activeTargetType == ActiveTargetType.TARGET_33 && !isSequentialMode,
            onClick = { onSelectTargetType(ActiveTargetType.TARGET_33) },
            size = 38.dp,
            fontSize = 14.sp,
        )

        // Circle 2 (Second in RTL): 100 (١٠٠)
        TargetCircle(
            text = "١٠٠",
            selected = activeTargetType == ActiveTargetType.TARGET_100 && !isSequentialMode,
            onClick = { onSelectTargetType(ActiveTargetType.TARGET_100) },
            size = 38.dp,
            fontSize = 14.sp,
        )

        // Circle 3 (Third in RTL): Infinity (∞)
        TargetCircle(
            text = "∞",
            selected = activeTargetType == ActiveTargetType.INFINITY && !isSequentialMode,
            onClick = { onSelectTargetType(ActiveTargetType.INFINITY) },
            size = 38.dp,
            fontSize = 15.sp,
        )

        // Circle 4 (Far Left in RTL): Custom Target (تخصيص ✎)
        CustomTargetCircle(
            savedTarget = savedCustomTarget,
            selected = activeTargetType == ActiveTargetType.CUSTOM && !isSequentialMode,
            onSelect = { onSelectTargetType(ActiveTargetType.CUSTOM) },
            onSaveCustomTarget = onSaveCustomTarget,
            size = 38.dp,
        )
    }
}

@Composable
private fun TargetCircle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    size: Dp = 38.dp,
    fontSize: TextUnit = 14.sp,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "targetCircleBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "targetCircleFg",
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CustomTargetCircle(
    savedTarget: Int?,
    selected: Boolean,
    onSelect: () -> Unit,
    onSaveCustomTarget: (Int) -> Unit,
    size: Dp = 38.dp,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var inputText by remember(savedTarget) {
        mutableStateOf(savedTarget?.toString() ?: "")
    }

    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "customTargetBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "customTargetFg",
    )

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable {
                    if (savedTarget != null) {
                        onSelect()
                    }
                    isMenuOpen = true
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (savedTarget != null) {
                    Text(
                        text = bidiLtr(savedTarget.toString()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = contentColor,
                        maxLines = 1,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tasbih_custom_btn),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = contentColor,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(1.dp))
                        Icon(
                            painter = painterResource(R.drawable.outline_edit_24),
                            contentDescription = stringResource(R.string.tasbih_custom_btn),
                            tint = contentColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(9.dp),
                        )
                    }
                }
            }
        }

        // DropdownMenu anchored right below the circle (single row: input + save button)
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            inputText = newValue
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.tasbih_target_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .width(76.dp)
                        .height(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 6.dp),
                )

                Button(
                    onClick = {
                        val num = inputText.toIntOrNull()
                        if (num != null && num > 0) {
                            onSaveCustomTarget(num)
                            isMenuOpen = false
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_save),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

/**
 * Bottom Anchor Bar:
 * SurfaceContainerLow with height 48.dp, displaying from Right to Left:
 * - "المجموع الكلي: [totalCount]" on the right (start in RTL)
 * - "الدورة: [cycleCount]" in the middle
 * - "↺ تصفير" on the left (end in RTL) with compact DropdownMenu (2 options: current, all)
 */
@Composable
private fun BottomAnchorBar(
    totalCount: Long,
    cycleCount: Int,
    onResetCurrent: () -> Unit,
    onResetAll: () -> Unit,
) {
    var isResetMenuOpen by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Right in RTL: "المجموع الكلي: [totalCount]"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.tasbih_total_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = bidiLtr(totalCount.toString()),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Middle in RTL: "الدورة: [cycleCount]"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_repeat),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = stringResource(R.string.tasbih_cycle_label, bidiLtr(cycleCount.toString())),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Left in RTL: "↺ تصفير" with DropdownMenu
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isResetMenuOpen = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reset),
                            contentDescription = stringResource(R.string.tasbih_reset_btn),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.tasbih_reset_btn),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                DropdownMenu(
                    expanded = isResetMenuOpen,
                    onDismissRequest = { isResetMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.tasbih_reset_current_menu),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_reset),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            isResetMenuOpen = false
                            onResetCurrent()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.tasbih_reset_total_menu),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            isResetMenuOpen = false
                            onResetAll()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SequentialStagesIndicator(activeIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DEFAULT_SEQUENTIAL_WIRD.forEachIndexed { index, stage ->
            val isActive = index == activeIndex
            val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            val fg = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

            Surface(
                shape = CircleShape,
                color = bg,
            ) {
                Text(
                    text = "${stage.text} (${bidiLtr(stage.target.toString())})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/**
 * Settings & Stats Modal Bottom Sheet:
 * Short & compact with ONLY:
 * - Title "الإعدادات والإحصائيات" with Close (×) button
 * - Total count card with "تصفير الإجمالي" button
 * - Sequential Wird mode toggle
 * - Volume buttons count toggle
 */
@Composable
private fun TasbihSettingsAndStatsSheet(
    tasbih: TasbihState,
    onClose: () -> Unit,
    onResetTotal: () -> Unit,
    onToggleSequential: (Boolean) -> Unit,
    onToggleVolumeKeys: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Sheet Header: Close Button + Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_close_24),
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }

            Text(
                text = stringResource(R.string.tasbih_settings_stats),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // 1. STATS CARD: Total Counts & Reset Total
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Reset Total Button
                Button(
                    onClick = onResetTotal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_reset_total_menu),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Total Info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.tasbih_total_all_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = bidiLtr(tasbih.totalCount.toString()),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // 2. SEQUENTIAL WIRD CARD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSequential(!tasbih.isSequentialMode) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(
                    checked = tasbih.isSequentialMode,
                    onCheckedChange = onToggleSequential,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tasbih_wird_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.tasbih_wird_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 3. HARDWARE VOLUME BUTTONS COUNT CARD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleVolumeKeys(!tasbih.volumeKeysEnabled) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(
                    checked = tasbih.volumeKeysEnabled,
                    onCheckedChange = onToggleVolumeKeys,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tasbih_volume_keys_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.tasbih_volume_keys_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

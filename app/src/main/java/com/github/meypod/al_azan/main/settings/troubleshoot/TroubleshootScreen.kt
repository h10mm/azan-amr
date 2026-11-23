package com.github.meypod.al_azan.main.settings.troubleshoot

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.ZenBadge
import com.github.meypod.al_azan.core.presentation.components.ZenCard
import com.github.meypod.al_azan.core.presentation.components.ZenSwitch
import com.github.meypod.al_azan.core.presentation.components.bidiLtr
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.isSchedulingPermissionGranted
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.util.android.IntentUtils
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootUiAction
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootUiState
import java.util.Locale

enum class DeviceFamily(
    val displayName: String,
    val exactMatches: Set<String>,
    val cardTitle: String,
    val cardSubtitle: String,
    val whatToDoSteps: List<String>,
    val actionButtonLabel: String,
) {
    PIXEL(
        displayName = bidiLtr("Google Pixel"),
        exactMatches = setOf("google"),
        cardTitle = "⚡ منبه ${bidiLtr("Pixel")} والشحن المتكيف (${bidiLtr("Pixel 7/8/9")})",
        cardSubtitle = "إخفاء أيقونة ⏰ الدائمة وحماية ميزة الشحن المتكيف ليلاً",
        whatToDoSteps = listOf(
            "1️⃣ الوضع الافتراضي (مطفأ) هو الأكثر موثوقية لرنين الأذان في موعده.",
            "2️⃣ فعّل هذا المفتاح فقط إذا كانت أيقونة المنبه ⏰ في شريط الحالة تزعجك أو تتعارض مع الشحن المتكيف ليلاً.",
        ),
        actionButtonLabel = "تبديل وضع منبه ${bidiLtr("Pixel")}",
    ),
    XIAOMI(
        displayName = bidiLtr("Xiaomi / POCO / Redmi"),
        exactMatches = setOf("xiaomi", "redmi", "poco", "blackshark"),
        cardTitle = "🚀 التشغيل التلقائي (${bidiLtr("Xiaomi Autostart")})",
        cardSubtitle = "مطلوب في ${bidiLtr("HyperOS / MIUI")} لمنع نوم الأذان بعد إعادة التشغيل",
        whatToDoSteps = listOf(
            "1️⃣ اضغط على الزر بالأسفل لفتح إعدادات «البدء التلقائي» (${bidiLtr("Autostart")}) ثم فعّل المفتاح أمام تطبيق الأذان.",
            "2️⃣ ادخل إلى «موفر البطارية» واختر «لا توجد قيود» لضمان عدم إغلاق الأذان في الخلفية.",
        ),
        actionButtonLabel = "فتح إعدادات البدء التلقائي (${bidiLtr("Xiaomi / POCO")})",
    ),
    SAMSUNG(
        displayName = bidiLtr("Samsung Galaxy"),
        exactMatches = setOf("samsung"),
        cardTitle = "🛡️ تطبيقات لا تنام أبداً (${bidiLtr("Samsung One UI")})",
        cardSubtitle = "استثناء التطبيق من قائمة «السكون العميق» بعد عدة أيام",
        whatToDoSteps = listOf(
            "1️⃣ اضغط لفتح «العناية بالجهاز / البطارية» ثم اختر «حدود الاستخدام في الخلفية».",
            "2️⃣ ادخل إلى قائمة «تطبيقات لا تنام أبداً» وأضف تطبيق الأذان إليها.",
            "3️⃣ من معلومات التطبيق ثم البطارية، اختر «غير مقيد».",
        ),
        actionButtonLabel = "فتح إعدادات البطارية والسكون (${bidiLtr("Samsung")})",
    ),
    OPPO(
        displayName = bidiLtr("OPPO / Realme / OnePlus"),
        exactMatches = setOf("oppo", "realme", "oneplus", "coloros"),
        cardTitle = "🚀 إدارة التشغيل بالخلفية (${bidiLtr("ColorOS / Realme UI")})",
        cardSubtitle = "إعفاء التطبيق من إيقاف التشغيل عند قفل الشاشة",
        whatToDoSteps = listOf(
            "1️⃣ اضغط لفتح إعدادات التطبيق ثم ادخل إلى «استهلاك البطارية».",
            "2️⃣ فعّل خيار «السماح بالنشاط في الخلفية» وخيار «التشغيل التلقائي».",
        ),
        actionButtonLabel = "فتح إعدادات الخلفية (${bidiLtr("OPPO / Realme / OnePlus")})",
    ),
    HUAWEI(
        displayName = bidiLtr("Huawei / Honor"),
        exactMatches = setOf("huawei", "honor"),
        cardTitle = "🔋 التطبيقات المحمية (${bidiLtr("Huawei EMUI / MagicOS")})",
        cardSubtitle = "حماية التطبيق من الإغلاق القسري عند قفل الشاشة في هواوي وهونر",
        whatToDoSteps = listOf(
            "1️⃣ اضغط لفتح شاشة «تشغيل التطبيقات» وابحث عن تطبيق الأذان.",
            "2️⃣ أوقف «الإدارة التلقائية» وفعّل الخيارات الثلاثة: التشغيل التلقائي + الثانوي + العمل في الخلفية.",
        ),
        actionButtonLabel = "فتح إعدادات تشغيل التطبيقات (${bidiLtr("Huawei / Honor")})",
    ),
    VIVO(
        displayName = bidiLtr("Vivo / iQOO"),
        exactMatches = setOf("vivo", "iqoo"),
        cardTitle = "🔋 التشغيل في الخلفية (${bidiLtr("FuntouchOS / OriginOS")})",
        cardSubtitle = "منع تجميد منبهات الأذان أثناء قفل الشاشة",
        whatToDoSteps = listOf(
            "1️⃣ اضغط لفتح إعدادات البطارية للتطبيق واختر «استهلاك طاقة مرتفع في الخلفية».",
            "2️⃣ فعّل «التشغيل التلقائي» (${bidiLtr("Autostart")}) من إعدادات أذونات التطبيق.",
        ),
        actionButtonLabel = "فتح إعدادات البطارية والخلفية (${bidiLtr("Vivo / iQOO")})",
    ),
    NOKIA(
        displayName = bidiLtr("Nokia / HMD"),
        exactMatches = setOf("nokia", "hmd", "hmd global"),
        cardTitle = "🔋 حماية التشغيل في الخلفية (${bidiLtr("Nokia / HMD")})",
        cardSubtitle = "استثناء التطبيق من موفر الطاقة في هواتف نوكيا لضمان دقة الأذان",
        whatToDoSteps = listOf(
            "1️⃣ اضغط لفتح إعدادات البطارية أو تفاصيل التطبيق واختر «غير مقيد».",
            "2️⃣ تأكد من إيقاف قيود «مراقب الخلفية» أمام تطبيق الأذان.",
        ),
        actionButtonLabel = "فتح إعدادات البطارية (${bidiLtr("Nokia")})",
    ),
    GENERIC(
        displayName = "هاتف عام / أندرويد قياسي",
        exactMatches = emptySet(),
        cardTitle = "✓ نظام أندرويد القياسي (${bidiLtr("Standard Android")})",
        cardSubtitle = "هاتفك لا يفرض قيوداً إضافية معقدة خارج معايير أندرويد",
        whatToDoSteps = listOf(
            "1️⃣ تفعيل الأذونات الأساسية بالأعلى كافٍ بنسبة ${bidiLtr("100%")} لعمل الأذان في الثانية الصفر.",
            "2️⃣ يمكنك مراجعة صفحة معلومات التطبيق للتأكد من أن البطارية في وضع «غير مقيد».",
        ),
        actionButtonLabel = "فتح صفحة تفاصيل التطبيق في النظام",
    );

    companion object {
        /**
         * Accurately identifies the device family from [Build.MANUFACTURER] and [Build.BRAND]
         * using exact and whole-word token matching so OEM screens never appear on the wrong device.
         */
        fun detect(): DeviceFamily {
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT).trim()
            val brand = Build.BRAND.orEmpty().lowercase(Locale.ROOT).trim()
            val tokens = buildSet {
                if (manufacturer.isNotEmpty()) {
                    add(manufacturer)
                    manufacturer.split(" ", "-", "_", "/").filter { it.isNotEmpty() }.forEach(::add)
                }
                if (brand.isNotEmpty()) {
                    add(brand)
                    brand.split(" ", "-", "_", "/").filter { it.isNotEmpty() }.forEach(::add)
                }
            }
            return entries.firstOrNull { family ->
                family.exactMatches.any { key -> key in tokens }
            } ?: GENERIC
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun TroubleshootScreen(
    uiState: TroubleshootUiState,
    onAction: (TroubleshootUiAction) -> Unit,
    modifier: Modifier = Modifier,
    advancedUiState: AdvancedTroubleshootUiState? = null,
    onAdvancedAction: ((AdvancedTroubleshootUiAction) -> Unit)? = null,
    advancedRoute: Route = Route.Main.Settings.Troubleshoot.AdvancedTroubleshoot,
    showAdvancedLink: Boolean = true,
    showIntroHeader: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    var refreshKey by remember { mutableIntStateOf(0) }

    var autoSequenceActive by rememberSaveable { mutableStateOf(false) }
    var autoSequenceNextIndex by rememberSaveable { mutableIntStateOf(0) }
    var activeStepInFlight by rememberSaveable { mutableIntStateOf(-1) }

    fun isAtomicStepGranted(stepIndex: Int): Boolean =
        when (stepIndex) {
            0 -> isSchedulingPermissionGranted(context, SchedulingPermission.Notification)
            1 -> Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                isSchedulingPermissionGranted(context, SchedulingPermission.FullScreenIntent)
            2 -> isSchedulingPermissionGranted(context, SchedulingPermission.ExactAlarm)
            3 -> uiState.appIsAllowedToKeepRunning ||
                isSchedulingPermissionGranted(context, SchedulingPermission.BatteryOptimization)
            4 -> uiState.dndAccessGranted ||
                isSchedulingPermissionGranted(context, SchedulingPermission.DndAccess)
            5 -> isSchedulingPermissionGranted(context, SchedulingPermission.PhoneState)
            else -> true
        }

    val launchAtomicStepRef = remember { mutableStateOf<(Int) -> Unit>({}) }

    fun continueAutoSequence(fromIndex: Int) {
        val nextUngranted = (fromIndex..5).firstOrNull { !isAtomicStepGranted(it) }
        if (nextUngranted == null) {
            autoSequenceActive = false
            autoSequenceNextIndex = 0
            activeStepInFlight = -1
        } else {
            autoSequenceActive = true
            autoSequenceNextIndex = nextUngranted + 1
            activeStepInFlight = nextUngranted
            launchAtomicStepRef.value(nextUngranted)
        }
    }

    fun onStepReturned(stepIndex: Int) {
        refreshKey++
        onAction(TroubleshootUiAction.OnLifecycleChanged)
        if (activeStepInFlight == stepIndex) {
            activeStepInFlight = -1
            if (autoSequenceActive) {
                continueAutoSequence(autoSequenceNextIndex)
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted && !autoSequenceActive && activity != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                openAppNotificationSettings(context)
            }
            onStepReturned(0)
        },
    )

    val fullScreenIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onStepReturned(1) },
    )

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onStepReturned(2) },
    )

    val ignoreBatteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onStepReturned(3) },
    )

    val dndAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onStepReturned(4) },
    )

    val phoneStateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted && !autoSequenceActive && activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)
            ) {
                openAppDetailsSettings(context, showGuidanceToast = true)
            }
            onStepReturned(5)
        },
    )

    launchAtomicStepRef.value = { stepIndex ->
        when (stepIndex) {
            0 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onStepReturned(0)
                }
            }
            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    safeLaunch(context) {
                        fullScreenIntentLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                "package:${context.packageName}".toUri(),
                            ),
                        )
                    }
                } else {
                    onStepReturned(1)
                }
            }
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    safeLaunch(context) {
                        exactAlarmLauncher.launch(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                "package:${context.packageName}".toUri(),
                            ),
                        )
                    }
                } else {
                    onStepReturned(2)
                }
            }
            3 -> {
                safeLaunch(context) {
                    val intent = if (uiState.isTelevision) {
                        PowerManagerUtils.tvEnergySettingsIntent(context)
                    } else {
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            "package:${context.packageName}".toUri(),
                        )
                    }
                    ignoreBatteryLauncher.launch(intent)
                }
            }
            4 -> {
                safeLaunch(context) {
                    dndAccessLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
            }
            5 -> {
                phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
        }
    }

    // Re-check all 5 permissions immediately whenever the user returns from system Settings (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
                onAction(TroubleshootUiAction.OnLifecycleChanged)
                val inFlight = activeStepInFlight
                if (autoSequenceActive && inFlight in 1..4) {
                    onStepReturned(inFlight)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            refreshKey++
            onAction(TroubleshootUiAction.OnLifecycleChanged)
        }
    }

    val notifGranted = remember(refreshKey) {
        isSchedulingPermissionGranted(context, SchedulingPermission.Notification) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                isSchedulingPermissionGranted(context, SchedulingPermission.FullScreenIntent))
    }
    val exactAlarmGranted = remember(refreshKey) {
        isSchedulingPermissionGranted(context, SchedulingPermission.ExactAlarm)
    }
    val batteryExemptGranted = remember(refreshKey, uiState.appIsAllowedToKeepRunning) {
        uiState.appIsAllowedToKeepRunning ||
            isSchedulingPermissionGranted(context, SchedulingPermission.BatteryOptimization)
    }
    val dndGranted = remember(refreshKey, uiState.dndAccessGranted) {
        uiState.dndAccessGranted ||
            isSchedulingPermissionGranted(context, SchedulingPermission.DndAccess)
    }
    val phoneStateGranted = remember(refreshKey) {
        isSchedulingPermissionGranted(context, SchedulingPermission.PhoneState)
    }

    val totalPermissionsCount = 5
    val grantedCount = listOf(
        notifGranted,
        exactAlarmGranted,
        batteryExemptGranted,
        dndGranted,
        phoneStateGranted,
    ).count { it }
    val missingCount = totalPermissionsCount - grantedCount
    val readinessPercentage = (grantedCount * 100) / totalPermissionsCount
    val allGranted = grantedCount == totalPermissionsCount

    val detectedFamily = remember { DeviceFamily.detect() }
    var selectedFamily by rememberSaveable { mutableStateOf(detectedFamily) }
    var showDevicePicker by rememberSaveable { mutableStateOf(false) }

    var oemCardAcknowledged by rememberSaveable(selectedFamily) { mutableStateOf(false) }
    val isDeviceSwitchOn = if (selectedFamily == DeviceFamily.PIXEL) {
        advancedUiState?.useDifferentAlarmType == true
    } else {
        oemCardAcknowledged
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Readiness Progress Meter + One-Tap Sequential Activation Button
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
            borderColor = if (allGranted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
            shape = RoundedCornerShape(16.dp),
            paddingValues = PaddingValues(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp),
                ) {
                    CircularProgressIndicator(
                        progress = { readinessPercentage / 100f },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        text = bidiLtr("$readinessPercentage%"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allGranted) {
                            "🎉 جاهزية كاملة ${bidiLtr("100%")} للأذان والصلاة!"
                        } else {
                            "مطلوب تفعيل $missingCount أذونات متبقية"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (allGranted) {
                            "جميع الأذونات الأساسية الخمسة مفعّلة بنجاح"
                        } else {
                            "اضغط الزر بالأسفل ليفتح لك النظام الأذونات المتبقية واحداً تلو الآخر تلقائياً."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!allGranted) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { continueAutoSequence(0) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "⚡ تفعيل جميع الأذونات تلقائياً بالترتيب",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // SECTION 1: THE 5 CORE PERMISSIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⭐ الأذونات الأساسية:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            ZenBadge(
                text = "$grantedCount من $totalPermissionsCount",
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                minWidth = 68.dp,
            )
        }

        // 1. NOTIFICATIONS & LOCK SCREEN
        PillarZenCard(
            title = "1. 🔔 إشعار الأذان وإضاءة شاشة القفل",
            subtitle = "لإطلاق صوت الأذان وفتح شاشة الإيقاف فوق القفل",
            isGranted = notifGranted,
            onClick = {
                autoSequenceActive = false
                when {
                    !isAtomicStepGranted(0) -> {
                        activeStepInFlight = 0
                        launchAtomicStepRef.value(0)
                    }
                    !isAtomicStepGranted(1) -> {
                        activeStepInFlight = 1
                        launchAtomicStepRef.value(1)
                    }
                    else -> openAppNotificationSettings(context)
                }
            },
        )

        // 2. EXACT ALARMS
        PillarZenCard(
            title = "2. ⏱️ المنبهات الدقيقة (${bidiLtr("Exact Alarms")})",
            subtitle = "لإطلاق الأذان في الثانية الصفر بالضبط دون أي تأخير",
            isGranted = exactAlarmGranted,
            onClick = {
                autoSequenceActive = false
                if (!exactAlarmGranted) {
                    activeStepInFlight = 2
                    launchAtomicStepRef.value(2)
                } else {
                    openAppDetailsSettings(context, showGuidanceToast = false)
                }
            },
        )

        // 3. BATTERY OPTIMIZATION
        PillarZenCard(
            title = "3. 🔋 استثناء قيود البطارية (${bidiLtr("Doze Mode")})",
            subtitle = "لمنع نظام أندرويد من تجميد التطبيق وقت صلاة الفجر",
            isGranted = batteryExemptGranted,
            onClick = {
                autoSequenceActive = false
                if (!batteryExemptGranted) {
                    activeStepInFlight = 3
                    launchAtomicStepRef.value(3)
                } else {
                    onAction(TroubleshootUiAction.OnAppIsAllowedToKeepRunningClick(activity))
                }
            },
        )

        // 4. DO NOT DISTURB OVERRIDE
        PillarZenCard(
            title = "4. 🌙 تخطي الوضع الصامت / عدم الإزعاج",
            subtitle = "لسماع صوت أذان الفجر حتى لو كان الهاتف صامتاً أثناء نومك",
            isGranted = dndGranted,
            onClick = {
                autoSequenceActive = false
                activeStepInFlight = 4
                launchAtomicStepRef.value(4)
            },
        )

        // 5. PHONE CALL STATE (READ_PHONE_STATE)
        PillarZenCard(
            title = "5. 📞 إيقاف الأذان عند المكالمات",
            subtitle = "لإيقاف صوت الأذان تلقائياً عند ورود مكالمة هاتفية",
            isGranted = phoneStateGranted,
            onClick = {
                autoSequenceActive = false
                if (!phoneStateGranted) {
                    activeStepInFlight = 5
                    launchAtomicStepRef.value(5)
                } else {
                    openAppDetailsSettings(context, showGuidanceToast = false)
                }
            },
        )

        // SECTION 2: DEVICE-SPECIFIC OPTION (ONLY MATCHES ACTUAL DEVICE BY DEFAULT + MANUAL PICKER)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📱 نوع الهاتف المكتشف:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = selectedFamily.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.width(8.dp))

                ZenBadge(
                    text = if (showDevicePicker) "إغلاق ▴" else "تغيير ▾",
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    textColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    minWidth = 76.dp,
                    onClick = { showDevicePicker = !showDevicePicker },
                )
            }

            // EXPANDED DEVICE SELECTION PILLS
            AnimatedVisibility(visible = showDevicePicker) {
                ZenCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                    paddingValues = PaddingValues(16.dp),
                ) {
                    Text(
                        text = "اختر نوع هاتفك أو نظامك يدوياً:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DeviceFamily.entries.forEach { family ->
                            val isSelected = family == selectedFamily
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedFamily = family
                                        showDevicePicker = false
                                    },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = family.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DEVICE-SPECIFIC ACTION CARD WITH LAYERED TRY-CATCH & GUARANTEED APP DETAILS FALLBACK
            val triggerDeviceAction: () -> Unit = {
                when (selectedFamily) {
                    DeviceFamily.PIXEL -> {
                        if (advancedUiState != null && onAdvancedAction != null) {
                            onAdvancedAction(
                                AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle(!advancedUiState.useDifferentAlarmType),
                            )
                        } else if (showAdvancedLink) {
                            onAction(TroubleshootUiAction.OnAdvancedSettingsClick(advancedRoute))
                        }
                    }
                    DeviceFamily.GENERIC -> {
                        oemCardAcknowledged = true
                        openAppDetailsSettings(context, showGuidanceToast = true)
                    }
                    else -> {
                        oemCardAcknowledged = true
                        openOemSettingsWithGuaranteedFallback(context = context, family = selectedFamily)
                    }
                }
            }

            ZenCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (isDeviceSwitchOn) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                borderColor = if (isDeviceSwitchOn) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                borderWidth = if (isDeviceSwitchOn) 1.5.dp else 1.dp,
                paddingValues = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedFamily.cardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = selectedFamily.cardSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ZenSwitch(
                        checked = isDeviceSwitchOn,
                        onCheckedChange = { checked ->
                            if (selectedFamily == DeviceFamily.PIXEL) {
                                if (advancedUiState != null && onAdvancedAction != null) {
                                    onAdvancedAction(AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle(checked))
                                }
                            } else {
                                oemCardAcknowledged = checked
                                if (checked) {
                                    triggerDeviceAction()
                                }
                            }
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Clear Step-by-Step "What to do on this phone" instruction box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "📋 ماذا يجب أن تفعل في هاتفك (${selectedFamily.displayName})؟",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        selectedFamily.whatToDoSteps.forEach { stepText ->
                            Text(
                                text = stepText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (isDeviceSwitchOn) {
                        "✓ خيار مفعّل: تم تطبيق إعداد الحماية الخاص بهاتفك."
                    } else {
                        "⚪ غير مفعّل افتراضياً (انقر على الزر بالأسفل أو المفتاح لتطبيقه)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDeviceSwitchOn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                Spacer(Modifier.height(12.dp))

                // Standard 48.dp+ Full-Width Action Button with RTL/LTR Auto-Mirrored Arrow Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDeviceSwitchOn) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isDeviceSwitchOn) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { triggerDeviceAction() },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedFamily.actionButtonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDeviceSwitchOn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            tint = if (isDeviceSwitchOn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarZenCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    ZenCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = if (isGranted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        shape = RoundedCornerShape(16.dp),
        paddingValues = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            ZenBadge(
                text = if (isGranted) "مفعّل ✓" else "تفعيل",
                backgroundColor = if (isGranted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                textColor = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                minWidth = 76.dp,
            )
        }
    }
}

/**
 * Returns the OEM-specific candidate intents strictly matching [family], so one vendor's settings
 * are never accidentally opened on a different manufacturer's device.
 */
private fun candidateOemIntentsFor(family: DeviceFamily): List<Intent> =
    when (family) {
        DeviceFamily.XIAOMI -> listOf(
            oemIntent("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            oemIntent("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"),
        )
        DeviceFamily.SAMSUNG -> listOf(
            oemIntent("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
            oemIntent("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            oemIntent("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        )
        DeviceFamily.OPPO -> listOf(
            oemIntent("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            oemIntent("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            oemIntent("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            oemIntent("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
            oemIntent("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
        )
        DeviceFamily.HUAWEI -> listOf(
            oemIntent("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            oemIntent("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            oemIntent("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        )
        DeviceFamily.VIVO -> listOf(
            oemIntent("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            oemIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            oemIntent("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        )
        DeviceFamily.NOKIA -> listOf(
            oemIntent("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"),
        )
        DeviceFamily.PIXEL, DeviceFamily.GENERIC -> emptyList()
    }

private fun oemIntent(
    pkg: String,
    cls: String,
): Intent = Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * Layered try-catch launcher with [Intent.FLAG_ACTIVITY_NEW_TASK]:
 * 1. Tries each manufacturer-specific Autostart / Battery settings activity for [family].
 * 2. If none resolves or any throws SecurityException / ActivityNotFoundException, falls back
 *    to [Settings.ACTION_APPLICATION_DETAILS_SETTINGS] with a helpful Toast explaining what to do.
 */
private fun openOemSettingsWithGuaranteedFallback(
    context: Context,
    family: DeviceFamily,
) {
    for (intent in candidateOemIntentsFor(family)) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (IntentUtils.isAvailableOnDevice(context, intent)) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
            // Try the next OEM intent or proceed to the guaranteed App Details fallback
        }
    }
    openAppDetailsSettings(context, showGuidanceToast = true)
}

private fun openAppDetailsSettings(
    context: Context,
    showGuidanceToast: Boolean = false,
) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            ("package:" + context.packageName).toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        if (showGuidanceToast) {
            Toast.makeText(
                context,
                "تم فتح تفاصيل التطبيق؛ يُرجى تفعيل «البدء التلقائي» أو اختيار «غير مقيد» من إعدادات البطارية.",
                Toast.LENGTH_LONG,
            ).show()
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    safeLaunch(context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/** Runs [launch], swallowing [ActivityNotFoundException] and [SecurityException] with a toast instead of crashing. */
private fun safeLaunch(
    context: Context,
    launch: () -> Unit,
) {
    try {
        launch()
    } catch (_: Exception) {
        Toast.makeText(context, R.string.open_settings_failed, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun BatterSaverCTA(
    isAllowed: Boolean,
    onButtonClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (isAllowed) {
                OutlinedButton(
                    onClick = onButtonClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                    ) {
                        Text(
                            text = stringResource(R.string.app_is_allowed_to_keep_running),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Icon(painterResource(R.drawable.baseline_check_24), contentDescription = null)
                    }
                }
            } else {
                PrimaryButton(onClick = onButtonClick) {
                    Text(
                        text = stringResource(R.string.allow_app_to_keep_running),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun DndAccessCard(
    granted: Boolean,
    onButtonClick: () -> Unit,
) {
    ZenCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingLabel(stringResource(R.string.dnd_access_card_title))
            DndStatusBadge(granted)
        }
        Text(
            text = stringResource(R.string.dnd_access_card_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (granted) {
                OutlinedButton(
                    onClick = onButtonClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dnd_access_card_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                PrimaryButton(onButtonClick) {
                    Text(
                        text = stringResource(R.string.dnd_access_card_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DndStatusBadge(granted: Boolean) {
    val color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
    ) {
        Icon(
            painterResource(if (granted) R.drawable.baseline_check_24 else R.drawable.baseline_close_24),
            contentDescription = null,
            tint = color,
        )
        Text(
            stringResource(if (granted) R.string.dnd_access_status_granted else R.string.dnd_access_status_missing),
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun AutostartCard(onButtonClick: () -> Unit) {
    ZenCard(modifier = Modifier.fillMaxWidth()) {
        SettingLabel(stringResource(R.string.autostart_card_title))
        Text(
            text = stringResource(R.string.autostart_card_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PrimaryButton(onButtonClick) {
                Text(
                    text = stringResource(R.string.open_autostart_settings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun PowerManagerCard(
    @StringRes samsungHintRes: Int? = null,
    onButtonClick: () -> Unit,
) {
    ZenCard(modifier = Modifier.fillMaxWidth()) {
        SettingLabel(stringResource(R.string.power_manager_card_title))
        Text(
            text = stringResource(R.string.power_manager_card_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (samsungHintRes != null) {
            Text(
                text = stringResource(samsungHintRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PrimaryButton(onButtonClick) {
                Text(
                    text = stringResource(R.string.open_power_manager_settings),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TroubleshootPreview() {
    AlAzanTheme {
        TroubleshootScreen(
            uiState = TroubleshootUiState(true, PowerManagerUtils.PowerManagerInfo("XIAOMI", "foo", "foo", "foo")),
            onAction = {},
        )
    }
}

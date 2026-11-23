package com.github.meypod.al_azan.intro.components

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.dialog.PermissionStep
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.isSchedulingPermissionGranted
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest

private data class PermissionRow(
    val permission: SchedulingPermission,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

/** What the adhan needs from Android to be delivered exactly on time, in the order they are asked. */
private fun adhanPermissionRows(): List<PermissionRow> =
    buildList {
        add(PermissionRow(SchedulingPermission.Notification, R.string.intro_permission_notifications_title, R.string.intro_permission_notifications_body))
        add(PermissionRow(SchedulingPermission.ExactAlarm, R.string.intro_permission_exact_alarm_title, R.string.intro_permission_exact_alarm_body))
        // Full-screen intents are only gated from Android 14; before that the permission is always on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(PermissionRow(SchedulingPermission.FullScreenIntent, R.string.intro_permission_fullscreen_title, R.string.intro_permission_fullscreen_body))
        }
    }

/**
 * Plain-language checklist of the permissions the (already default-enabled) adhan relies on, each with a live
 * "granted / needed" badge, and one button that walks through the missing ones using the app's standard
 * permission flow ([rememberSchedulingPermissionRequest]) — the same flow the Settings toggles use, so
 * nothing about how permissions are requested or remembered changes.
 *
 * Purely informative and non-blocking: finishing onboarding never depends on the answers here, and Home
 * re-checks the same permissions on load.
 */
@Composable
fun IntroPermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rows = remember { adhanPermissionRows() }
    // Bumped when we come back from a system settings screen, so the badges re-read the real state.
    var refresh by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val request = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { refresh++ },
    )

    // Re-read whenever `refresh` is bumped (resume from settings / flow completion).
    val granted = remember(refresh) { rows.associate { it.permission to isSchedulingPermissionGranted(context, it.permission) } }
    val allGranted = granted.values.all { it }

    ACard(modifier.fillMaxWidth()) { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingLabel(stringResource(R.string.intro_permissions_title))
            rows.forEach { row ->
                PermissionStatusRow(
                    title = stringResource(row.title),
                    body = stringResource(row.body),
                    granted = granted.getValue(row.permission),
                )
            }
            if (allGranted) {
                Text(
                    text = stringResource(R.string.intro_permissions_all_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    PrimaryButton(
                        onClick = {
                            val wanted = rows.map { it.permission }.toSet()
                            request(SchedulingPermissionSteps.adhan.filter { step: PermissionStep -> step.permission in wanted })
                        },
                    ) {
                        Text(stringResource(R.string.intro_permissions_grant_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    body: String,
    granted: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(if (granted) R.drawable.baseline_check_24 else R.drawable.baseline_warning_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(if (granted) R.string.intro_permission_granted else R.string.intro_permission_required),
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

package com.github.meypod.al_azan.intro.adhan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.DefaultAdhanEntryId
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.presentation.components.ZenBadge
import com.github.meypod.al_azan.core.presentation.components.ZenCard
import com.github.meypod.al_azan.core.presentation.components.ZenHeroHeader
import com.github.meypod.al_azan.core.presentation.components.ZenSwitch
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsUiAction
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsUiState
import com.github.meypod.al_azan.main.settings.adhan.ScheduleAndMuezzinContent

@Composable
fun IntroAdhanStep(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    capsuleEnabled: Boolean,
    onCapsuleToggle: (Boolean) -> Unit,
    persistentNotifEnabled: Boolean = false,
    onPersistentNotifToggle: (Boolean) -> Unit = {},
    prayerScheduleRoute: (Prayer) -> Route,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val defaultEntries = remember { getDefaultAdhanEntries() }
    val selectedId = uiState.settings.selectedAdhanEntries[AdhanKey.Default]?.id
        ?: DefaultAdhanEntryId.TakbeerOnly.key
    var showFullSchedule by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            onAction(AdhanSettingsUiAction.OnStopPreview)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ZenHeroHeader(
            title = "اختر المؤذن والكبسولة الشرعية",
            subtitle = "خصص تنبيهاتك اليومية وسنن الصيام في ثوانٍ معدودة",
        )

        // CARD 1: Default Muezzin Voice
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            Text(
                text = "🔊 صوت الأذان الافتراضي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                defaultEntries.forEach { entry ->
                    val isSelected = selectedId == entry.id
                    val isPlaying = uiState.playingId == entry.id
                    val title = stringResource(entry.labelResId)
                    val subtitle = when (entry.id) {
                        DefaultAdhanEntryId.TakbeerOnly.key -> "تكبير قصير وهادئ (الافتراضي)"
                        DefaultAdhanEntryId.MasjidAnNabawi.key -> "أذان كامل خاشع من المسجد النبوي"
                        DefaultAdhanEntryId.AbdulBasitAbduSamad.key -> "أذان كامل — تسجيل كلاسيكي مميز"
                        DefaultAdhanEntryId.RaghebMustafaGhalwash.key -> "أذان كامل — أذان مصري خاشع"
                        DefaultAdhanEntryId.MoazenZade.key -> "أذان كامل — تراثي بمقام البياتي"
                        else -> resources.getString(entry.labelResId)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onAction(AdhanSettingsUiAction.OnGlobalMuezzinSelect(entry))
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Play / Preview / Check circle button
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            onAction(AdhanSettingsUiAction.OnGlobalMuezzinSelect(entry))
                                            if (isPlaying) {
                                                onAction(AdhanSettingsUiAction.OnStopPreview)
                                            } else {
                                                onAction(AdhanSettingsUiAction.OnPreviewAudio(entry))
                                            }
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = when {
                                                isPlaying -> "■"
                                                isSelected -> "✓"
                                                else -> "▶"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }

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
                            }

                            Spacer(Modifier.width(8.dp))

                            // Explicit Preview Pill (48.dp touch target via clickable ZenBadge)
                            ZenBadge(
                                text = if (isPlaying) "إيقاف ■" else "معاينة ▶",
                                backgroundColor = if (isPlaying) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                textColor = if (isPlaying) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                borderColor = if (isPlaying) {
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                minWidth = 76.dp,
                                onClick = {
                                    onAction(AdhanSettingsUiAction.OnGlobalMuezzinSelect(entry))
                                    if (isPlaying) {
                                        onAction(AdhanSettingsUiAction.OnStopPreview)
                                    } else {
                                        onAction(AdhanSettingsUiAction.OnPreviewAudio(entry))
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showFullSchedule = !showFullSchedule },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                ) {
                    Text(
                        text = if (showFullSchedule) {
                            "إخفاء تخصيص جدول الصلوات ▴"
                        } else {
                            "⚙️ تخصيص جدول الصلوات أو اختيار صوت من الهاتف ▾"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            AnimatedVisibility(visible = showFullSchedule) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScheduleAndMuezzinContent(
                        uiState = uiState,
                        onAction = onAction,
                        prayerScheduleRoute = prayerScheduleRoute,
                    )
                }
            }
        }

        // CARD 2: Smart Spiritual Capsule (Material 3 standard surfaces)
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✨ الكبسولة الشرعية الذكية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "تذكير بسنن الصيام (الإثنين والخميس والأيام البيض)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ZenSwitch(
                    checked = capsuleEnabled,
                    onCheckedChange = onCapsuleToggle,
                )
            }

            AnimatedVisibility(visible = capsuleEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "غداً الخميس سنة الصيام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "«تُعرض الأعمال يوم الإثنين والخميس...»",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = "صِيَام",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }

        // CARD 3: Persistent Prayer Times Notification Quick Toggle
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(com.github.meypod.al_azan.R.string.nws_enable_prayer_notif),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(com.github.meypod.al_azan.R.string.nws_enable_prayer_notif_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ZenSwitch(
                    checked = persistentNotifEnabled,
                    onCheckedChange = onPersistentNotifToggle,
                )
            }
        }
    }
}

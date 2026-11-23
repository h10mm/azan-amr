package com.github.meypod.al_azan.intro.languageselection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.core.domain.model.settings.SupportedLocales
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ZenCard
import com.github.meypod.al_azan.intro.IntroUiAction

private val BACKUP_MIME_TYPES = arrayOf("application/json", "application/octet-stream", "*/*")

@Composable
fun LanguageSelectionScreen(
    uiState: LanguageSelectionUiState,
    onAction: (LanguageSelectionUiAction) -> Unit,
    onIntroAction: (IntroUiAction) -> Unit,
    modifier: Modifier = Modifier,
    restoring: Boolean = false,
) {
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onIntroAction(IntroUiAction.OnRestoreBackup(it)) } }

    var showMoreLanguages by rememberSaveable { mutableStateOf(false) }

    val selectedLanguage = remember(uiState.selectedLocale) {
        SupportedLocales.firstOrNull { it.value == uiState.selectedLocale } ?: SupportedLocales.first()
    }
    val isOtherLanguage = uiState.selectedLocale != "ar" && uiState.selectedLocale != "en"

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Compact single-line screen title
        Text(
            text = "إعداد تطبيق الأذان",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Language Selection Card
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            Text(
                text = "🌐 اختر لغة التطبيق",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguageQuickButton(
                    label = "العربية",
                    selected = uiState.selectedLocale == "ar",
                    onClick = {
                        showMoreLanguages = false
                        onAction(LanguageSelectionUiAction.OnLanguageSelected("ar"))
                    },
                    modifier = Modifier.weight(1f),
                )
                LanguageQuickButton(
                    label = "English",
                    selected = uiState.selectedLocale == "en",
                    onClick = {
                        showMoreLanguages = false
                        onAction(LanguageSelectionUiAction.OnLanguageSelected("en"))
                    },
                    modifier = Modifier.weight(1f),
                )
                LanguageQuickButton(
                    label = if (isOtherLanguage) "${selectedLanguage.label} ▾" else "المزيد ▾",
                    selected = isOtherLanguage || showMoreLanguages,
                    onClick = { showMoreLanguages = !showMoreLanguages },
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedVisibility(visible = showMoreLanguages) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "جميع اللغات المدعومة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SupportedLocales.forEach { locale ->
                            val isSelected = locale.value == uiState.selectedLocale
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
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
                                        onAction(LanguageSelectionUiAction.OnLanguageSelected(locale.value))
                                        showMoreLanguages = false
                                    },
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Text(
                                        text = locale.label,
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
        }

        // Theme Selection Card (applies Light / Dark / Midnight Blue immediately across all onboarding screens)
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(16.dp),
        ) {
            Text(
                text = "🎨 اختر مظهر التطبيق",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val themeItems = listOf(
                    Triple(ThemeColor.ClassicLight, "فاتح", Color(0xFFF6FAFA)),
                    Triple(ThemeColor.Dark, "داكن", Color(0xFF141414)),
                    Triple(ThemeColor.MidnightBlue, "أزرق كحلي", Color(0xFF070D1E)),
                )

                themeItems.forEach { (theme, label, previewColor) ->
                    val isSelected = uiState.themeColor == theme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onAction(LanguageSelectionUiAction.OnThemeSelected(theme)) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(previewColor)
                                    .border(
                                        1.dp,
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        CircleShape,
                                    ),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Restore Backup Card
        ZenCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            paddingValues = PaddingValues(16.dp),
            onClick = {
                if (!restoring) {
                    restoreLauncher.launch(BACKUP_MIME_TYPES)
                }
            },
        ) {
            Text(
                text = "☁️ هل لديك نسخة احتياطية سابقة؟ اضغط هنا لاستعادتها",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LanguageQuickButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSelectionScreenPreview() {
    AlAzanTheme {
        LanguageSelectionScreen(
            uiState = LanguageSelectionUiState(selectedLocale = "ar"),
            onAction = {},
            onIntroAction = {},
        )
    }
}

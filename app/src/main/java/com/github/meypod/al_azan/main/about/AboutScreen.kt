package com.github.meypod.al_azan.main.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun AboutScreen(
    onAction: (AboutUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOriginalInfoDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.about),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        actions = {
            IconButton(onClick = { showOriginalInfoDialog = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = stringResource(R.string.dedication_info_tooltip),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // 1. الصورة الكريمة داخل إطار دائري باللون الزمردي الملكي
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(96.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_amr_portrait),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(CircleShape),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. اسم التطبيق
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // شارة الصدقة الجارية
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.dedication_badge),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. بطاقة الإهداء والدعاء الصافية الموحدة
                MainDedicationCard()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showOriginalInfoDialog) {
        Dialog(onDismissRequest = { showOriginalInfoDialog = false }) {
            OriginalDetailsDialog(onDismiss = { showOriginalInfoDialog = false })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainDedicationCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isArabic = LocalConfiguration.current.locales[0].language == "ar"
    val amiriFont = remember { FontFamily(Font(R.font.amiri_regular)) }
    val activeFont = if (isArabic) amiriFont else null

    val basmala = stringResource(R.string.dedication_basmala)
    val opening = stringResource(R.string.dedication_opening)
    val request = stringResource(R.string.dedication_request)
    val name = stringResource(R.string.dedication_name)
    val status = stringResource(R.string.dedication_status)
    val dua = stringResource(R.string.dedication_dua)
    val closing = stringResource(R.string.dedication_closing)
    val copiedToastText = stringResource(R.string.dedication_copied_toast)
    val shareTitle = stringResource(R.string.dedication_share)

    // الألوان: لون التمييز الزمردي الملكي للبسملة واسم الوالد، واللون الموحد الفحمي/اللؤلؤي لسائر النص
    val emeraldAccent = MaterialTheme.colorScheme.primary
    val unifiedTextColor = MaterialTheme.colorScheme.onSurface
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. البسملة (مميزة بلون الزمرد الملكي وبحجم جليل 25sp)
            Text(
                text = basmala,
                fontFamily = activeFont,
                fontSize = if (isArabic) 25.sp else 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 1.8.em,
                color = emeraldAccent,
                textAlign = TextAlign.Center,
            )

            // 2. المقدمة (اللون الموحد + الحجم الموحد 21sp)
            Text(
                text = opening,
                fontFamily = activeFont,
                fontSize = if (isArabic) 21.sp else 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 1.85.em,
                color = unifiedTextColor,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.65f),
                thickness = 0.6.dp,
                color = dividerColor,
            )

            // 3. قسم اسم الوالد (الطلب موحد 21sp، والاسم مهيب 36sp بالزمرد الملكي، والترحم 21sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = request,
                    fontFamily = activeFont,
                    fontSize = if (isArabic) 21.sp else 15.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 1.65.em,
                    color = unifiedTextColor,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = name,
                    fontFamily = activeFont,
                    fontSize = if (isArabic) 36.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 1.3.em,
                    color = emeraldAccent,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = status,
                    fontFamily = activeFont,
                    fontSize = if (isArabic) 21.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 1.5.em,
                    color = emeraldAccent,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.65f),
                thickness = 0.6.dp,
                color = dividerColor,
            )

            // 4. متن الدعاء (اللون الموحد + الحجم الموحد 21sp)
            Text(
                text = dua,
                fontFamily = activeFont,
                fontSize = if (isArabic) 21.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 1.95.em,
                color = unifiedTextColor,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.65f),
                thickness = 0.6.dp,
                color = dividerColor,
            )

            // 5. الخاتمة (اللون الموحد + الحجم الموحد 21sp - مساوية للدعاء تماماً دون تصغير)
            Text(
                text = closing,
                fontFamily = activeFont,
                fontSize = if (isArabic) 21.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 1.85.em,
                color = unifiedTextColor,
                textAlign = TextAlign.Center,
            )

            // 6. زرا النسخ والمشاركة بترتيب مرن يلتف تلقائياً
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = "$name ($status)\n$dua"
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Dua", clipText))
                        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .widthIn(min = 135.dp)
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.content_copy),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dedication_copy_dua),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }

                Button(
                    onClick = {
                        val shareMessage = buildString {
                            appendLine(basmala)
                            appendLine()
                            appendLine(opening)
                            appendLine()
                            appendLine(request)
                            appendLine("$name — $status")
                            appendLine()
                            appendLine(dua)
                            appendLine()
                            append(closing)
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .widthIn(min = 135.dp)
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shareTitle,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun OriginalDetailsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            HorizontalDivider(thickness = 0.5.dp)

            Column {
                Text(
                    text = stringResource(R.string.version),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = versionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(thickness = 0.5.dp)

            val isArabic = LocalConfiguration.current.locales[0].language == "ar"
            val originalRepoUrl = "https://github.com/meypod/al-azan-compose/"
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isArabic) "المصدر الأصلي وحفظ الحقوق" else "Original Source & Attribution",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (isArabic) {
                        "تم تطوير هذا التطبيق وبناء إضافاته اعتماداً على المشروع مفتوح المصدر Al-Azan Compose، مع حفظ كامل الحقوق والتقدير لمطوريه الأصليين، جزاهم الله خيراً."
                    } else {
                        "This app is developed and extended based on the open-source Al-Azan Compose project, with full credit and attribution to its original authors."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    onClick = {
                        runCatching {
                            val browseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(originalRepoUrl))
                            context.startActivity(browseIntent)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "github.com/meypod/al-azan-compose",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "↗",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            Column {
                Text(
                    text = stringResource(R.string.license),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.about_credits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.close))
            }
        }
    }
}

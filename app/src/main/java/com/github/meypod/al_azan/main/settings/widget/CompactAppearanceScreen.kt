package com.github.meypod.al_azan.main.settings.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.meypod.al_azan.core.domain.model.widget.NateegaWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerBackground
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerShape
import com.github.meypod.al_azan.core.domain.model.widget.WidgetColorOption
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun CompactAppearanceScreen(
    config: NateegaWidgetConfig,
    onShapeChange: (NextPrayerShape) -> Unit,
    onBackgroundChange: (NextPrayerBackground) -> Unit,
    onProgressColorChange: (WidgetColorOption) -> Unit,
    onCountdownColorChange: (WidgetColorOption) -> Unit,
    onPrayerNameColorChange: (WidgetColorOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = "مظهر ودجت العداد التنازلي",
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        // 1. Live Interactive Preview Card at Top
        LiveNextPrayerPreview(config = config)

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Shape Selection: Circle with Ring vs Square
        SectionHeader(title = "شكل الودجت")
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ShapeOptionCard(
                        title = "دائري",
                        subtitle = "حلقة تقدم محيطية حول الدائرة",
                        isSelected = config.nextPrayerShape == NextPrayerShape.Circle,
                        onClick = { onShapeChange(NextPrayerShape.Circle) },
                        modifier = Modifier.weight(1f),
                    )
                    ShapeOptionCard(
                        title = "مربع",
                        subtitle = "بطاقة منحنية مع شريط أفقي",
                        isSelected = config.nextPrayerShape == NextPrayerShape.Square,
                        onClick = { onShapeChange(NextPrayerShape.Square) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Background Selection: Transparent vs Dark
        SectionHeader(title = "خلفية الودجت")
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ShapeOptionCard(
                        title = "شفافة",
                        subtitle = "تظهر خلفية الشاشة (بدون قرص داكن)",
                        isSelected = config.nextPrayerBackground == NextPrayerBackground.Transparent,
                        onClick = { onBackgroundChange(NextPrayerBackground.Transparent) },
                        modifier = Modifier.weight(1f),
                    )
                    ShapeOptionCard(
                        title = "داكنة",
                        subtitle = "قرص داكن أنيق بدون فراغات",
                        isSelected = config.nextPrayerBackground == NextPrayerBackground.Dark,
                        onClick = { onBackgroundChange(NextPrayerBackground.Dark) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Progress Line / Ring Color
        SectionHeader(title = "لون الخط المتناقص (شريط وحلقة التقدم)")
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
            ) {
                Text(
                    text = "اختر لون حلقة التقدم المحيطية بالدائرة أو شريط التقدم بالمربع:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ColorSelectorRow(
                    options = WidgetColorOption.entries,
                    selectedOption = config.nextPrayerProgressColor,
                    onSelect = onProgressColorChange,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Countdown Timer Color
        SectionHeader(title = "لون العداد المتناقص (الوقت المتبقي)")
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
            ) {
                Text(
                    text = "اختر لون أرقام العداد التنازلي التفاعلي:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ColorSelectorRow(
                    options = listOf(
                        WidgetColorOption.Gold,
                        WidgetColorOption.Emerald,
                        WidgetColorOption.White,
                        WidgetColorOption.Cyan,
                    ),
                    selectedOption = config.nextPrayerCountdownColor,
                    onSelect = onCountdownColorChange,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Prayer Name Color
        SectionHeader(title = "لون اسم الصلاة")
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
            ) {
                Text(
                    text = "اختر لون خط اسم الصلاة القادمة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ColorSelectorRow(
                    options = listOf(
                        WidgetColorOption.White,
                        WidgetColorOption.Emerald,
                        WidgetColorOption.Gold,
                        WidgetColorOption.Cyan,
                        WidgetColorOption.Blue,
                    ),
                    selectedOption = config.nextPrayerNameColor,
                    onSelect = onPrayerNameColorChange,
                )
            }
        }
    }
}

@Composable
private fun LiveNextPrayerPreview(
    config: NateegaWidgetConfig,
    modifier: Modifier = Modifier,
) {
    val progressColor = Color(config.nextPrayerProgressColor.colorHex)
    val countdownColor = Color(config.nextPrayerCountdownColor.colorHex)
    val prayerNameColor = Color(config.nextPrayerNameColor.colorHex)

    ACard(modifier = modifier.fillMaxWidth()) { cardPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "معاينة حية للودجت",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center,
            ) {
                val isTransparent = config.nextPrayerBackground == NextPrayerBackground.Transparent

                if (config.nextPrayerShape == NextPrayerShape.Circle) {
                    // Circular shape with outer ring
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 5.dp.toPx()
                            // Background track
                            drawCircle(
                                color = if (isTransparent) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                style = Stroke(width = strokeWidth),
                            )
                            // Progress arc
                            drawArc(
                                color = progressColor,
                                startAngle = -90f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isTransparent) 156.dp else 158.dp)
                                .clip(CircleShape)
                                .background(if (isTransparent) Color.Transparent else Color(0xFF1E293B))
                                .then(
                                    if (!isTransparent) Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 6.dp),
                            ) {
                                Text(
                                    text = "الفجر",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = prayerNameColor,
                                )
                                Text(
                                    text = "04:50",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9999.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "02:34:18",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = countdownColor,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Square shape with bottom progress bar
                    Box(
                        modifier = Modifier
                            .size(165.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(if (isTransparent) Color.Transparent else Color(0xFF1E293B))
                            .border(
                                1.dp,
                                if (isTransparent) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(26.dp)
                            )
                            .padding(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 4.dp),
                            ) {
                                Text(
                                    text = "الفجر",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = prayerNameColor,
                                )
                                Text(
                                    text = "04:50",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9999.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "02:34:18",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = countdownColor,
                                    )
                                }
                            }
                            // Bottom Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(progressColor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
        label = "borderColor",
    )
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        label = "bgColor",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun ColorSelectorRow(
    options: List<WidgetColorOption>,
    selectedOption: WidgetColorOption,
    onSelect: (WidgetColorOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            val color = Color(option.colorHex)
            val isSelected = option == selectedOption
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (color == Color.White) Color.Black else Color.White),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

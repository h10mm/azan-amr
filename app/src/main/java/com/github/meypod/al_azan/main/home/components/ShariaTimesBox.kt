package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlin.time.Clock

@Composable
fun ShariaTimesBox(
    state: ShariaTimesBoxUiState,
    modifier: Modifier = Modifier,
    wideLayout: Boolean = false,
) {
    val chunks = remember(state.hiddenPrayers, wideLayout) {
        val visible = SHARIA_TIMES_IN_ORDER.filter { it !in state.hiddenPrayers }
        if (wideLayout && visible.size > 5) {
            // في الوضع الأفقي نقسم الصلوات لعمودين متوازيين متزنين فقط لمنع التداخل والضغط الأفقي
            val half = (visible.size + 1) / 2
            visible.chunked(half)
        } else {
            listOf(visible)
        }
    }

    if (chunks.size > 1) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            chunks.forEach { chunk ->
                key(chunk.first().name) {
                    ShariaTimesCard(state, chunk, Modifier.weight(1f))
                }
            }
        }
    } else {
        ShariaTimesCard(state, chunks.first(), modifier.fillMaxWidth())
    }
}

@Composable
private fun ShariaTimesCard(
    state: ShariaTimesBoxUiState,
    prayers: List<Prayer>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val classic = state.themeColor.isClassic()
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)

    val patternBackgroundColor = when (state.themeColor) {
        ThemeColor.ClassicLight -> Color(0xFFE9F1F1)
        ThemeColor.MidnightBlue -> Color(0xFF0A1128)
        ThemeColor.Dark -> Color(0xFF000000) // سواد حقيقي كامل لخلفية الصلوات (True AMOLED Black)
    }

    // تحديد الصلاة التي عليها الدور فوراً (الصلاة القادمة التي يعمل لها العداد حالياً)
    val isViewingToday = remember(state.shariahTimes, state.now) {
        state.shariahTimes?.forDate == DateComponents.from(state.now)
    }
    val prayerToHighlight = remember(state.shariahTimes, state.now, state.highlightedShariaTime, isViewingToday, prayers) {
        val upcomingToday = prayers.firstOrNull { prayer ->
            val inst = state.shariahTimes?.forPrayer(prayer)
            inst != null && inst > state.now
        }
        when {
            isViewingToday -> state.highlightedShariaTime?.prayer ?: upcomingToday ?: prayers.firstOrNull()
            state.shariahTimes != null && state.highlightedShariaTime != null &&
                state.shariahTimes.forDate == state.highlightedShariaTime.forDate -> state.highlightedShariaTime.prayer
            else -> null
        }
    }

    // امتداد الحاوية بنفس نقش ولون الهيدر العلوي لتبدو ككتلة واحدة تماماً
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .patternedBackground(
                pattern = patternImage,
                backgroundColor = patternBackgroundColor,
                patternAlpha = 0.03f,
            )
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        SubcomposeLayout { constraints ->
            val sampleHeight = subcompose(SlotId.Sample) {
                ShariaTimeRow(sampleRowState(state), compact = false)
            }.first().measure(Constraints(maxWidth = constraints.maxWidth))
                .height

            val rowCount = prayers.size
            val needed = rowCount * sampleHeight
            val compact = needed > constraints.maxHeight

            val content = subcompose(SlotId.Content) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .fadeScrollEdges(scrollState, Orientation.Vertical)
                        .drawVerticalScrollbar(scrollState)
                        .then(if (compact) Modifier.verticalScroll(scrollState) else Modifier),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (index in prayers.indices) {
                        val prayer = prayers[index]
                        key(prayer.name) {
                            val instant = state.shariahTimes?.forPrayer(prayer)
                            val isThisPrayerNext = (prayerToHighlight != null && prayer == prayerToHighlight)

                            ShariaTimeRow(
                                ShariaTimeRowUiState(
                                    prayer,
                                    instant,
                                    state.locale,
                                    state.numberingSystem,
                                    state.is24Hours,
                                    highlightState = if (isThisPrayerNext) {
                                        HighlightState.Highlighted
                                    } else {
                                        getHighlightState(
                                            prayer,
                                            state.shariahTimes,
                                            state.highlightedShariaTime,
                                        )
                                    },
                                    state.themeColor,
                                    skipped = prayer in state.skippedPrayers &&
                                        instant != null && instant > state.now,
                                ),
                                compact = compact,
                            )
                        }
                    }
                }
            }.first().measure(constraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                content.place(0, 0)
            }
        }
    }
}

private enum class SlotId { Sample, Content }

private fun sampleRowState(state: ShariaTimesBoxUiState) =
    ShariaTimeRowUiState(
        SHARIA_TIMES_IN_ORDER.first(),
        state.shariahTimes?.forPrayer(SHARIA_TIMES_IN_ORDER.first()),
        state.locale,
        state.numberingSystem,
        state.is24Hours,
        HighlightState.AfterHighlight,
        state.themeColor,
    )

internal fun getHighlightState(
    prayer: Prayer,
    shariaTimes: ShariaTimes?,
    highlightedShariaTime: ShariaTimeDetails?,
): HighlightState {
    if (shariaTimes != null && highlightedShariaTime != null) {
        if (shariaTimes.forDate == highlightedShariaTime.forDate) {
            val pos = (prayer.ordinal - highlightedShariaTime.prayer.ordinal)
            return when {
                pos < 0 -> HighlightState.BeforeHighlight
                pos == 0 -> HighlightState.Highlighted
                else -> HighlightState.AfterHighlight
            }
        } else if (highlightedShariaTime.forInstant > shariaTimes.forInstant) {
            return HighlightState.BeforeHighlight
        }
    }
    return HighlightState.AfterHighlight
}

@Composable
private fun PreviewContent(themeColor: ThemeColor = ThemeColor.Default) {
    Scaffold {
        Column(
            Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            val instant = Clock.System.now()
            val dateComponents = DateComponents.from(instant)
            ShariaTimesBox(
                ShariaTimesBoxUiState(
                    ShariaTimes(
                        instant,
                        dateComponents,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                    ),
                    highlightedShariaTime = ShariaTimeDetails(
                        forInstant = instant,
                        forDate = dateComponents,
                        prayer = Prayer.Asr,
                        prayerTime = instant,
                        notify = false,
                        sound = false,
                    ),
                    themeColor = themeColor,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ShariaTimesBoxPreview() {
    AlAzanTheme {
        PreviewContent()
    }
}

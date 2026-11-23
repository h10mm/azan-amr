package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.LOREM_IMPSUM_SHORT

@Composable
fun ACard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    compact: Boolean = false,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    cardPadding: PaddingValues = if (compact) {
        PaddingValues(
            vertical = dimensionResource(R.dimen.card_padding_vc),
            horizontal = dimensionResource(R.dimen.card_padding_hc),
        )
    } else {
        PaddingValues(16.dp)
    },
    content: @Composable (cardPadding: PaddingValues) -> Unit,
) {
    Surface(
        modifier,
        color = color,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        shape = RoundedCornerShape(16.dp),
        border = border,
    ) {
        content(cardPadding)
    }
}

@Composable
fun ACard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    compact: Boolean = false,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    cardPadding: PaddingValues = if (compact) {
        PaddingValues(
            vertical = dimensionResource(R.dimen.card_padding_vc),
            horizontal = dimensionResource(R.dimen.card_padding_hc),
        )
    } else {
        PaddingValues(16.dp)
    },
    content: @Composable (cardPadding: PaddingValues) -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        color = color,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        shape = RoundedCornerShape(16.dp),
        border = border,
    ) {
        content(cardPadding)
    }
}

@Preview
@Composable
private fun CardPreview() {
    AlAzanTheme {
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                Text(LOREM_IMPSUM_SHORT)
            }
        }
    }
}

@Preview
@Composable
private fun CardOnBackgroundPreview() {
    AlAzanTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                ACard { cardPadding ->
                    Column(Modifier.padding(cardPadding)) {
                        Text(LOREM_IMPSUM_SHORT)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CardOnBackgroundClassicLightPreview() {
    AlAzanTheme(themeColor = ThemeColor.ClassicLight) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                ACard { cardPadding ->
                    Column(Modifier.padding(cardPadding)) {
                        Text(LOREM_IMPSUM_SHORT)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CardOnBackgroundMidnightBluePreview() {
    AlAzanTheme(themeColor = ThemeColor.MidnightBlue) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                ACard { cardPadding ->
                    Column(Modifier.padding(cardPadding)) {
                        Text(LOREM_IMPSUM_SHORT)
                    }
                }
            }
        }
    }
}

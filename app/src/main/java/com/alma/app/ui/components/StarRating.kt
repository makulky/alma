package com.alma.app.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alma.app.R
import com.alma.app.domain.MAX_RATING_HALVES
import com.alma.app.domain.MIN_RATING_HALVES
import com.alma.app.domain.STAR_COUNT
import com.alma.app.domain.formatRating
import com.alma.app.domain.ratingFromPosition
import kotlin.math.roundToInt

/** Estrellas de solo lectura. */
@Composable
fun StarRatingDisplay(halves: Int?, modifier: Modifier = Modifier, starSize: Dp = 16.dp) {
    val description = ratingDescription(halves)
    Stars(
        halves = halves,
        starSize = starSize,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    )
}

/**
 * Selector de puntuación con medias estrellas: admite toques y arrastre, y con TalkBack funciona
 * como un control ajustable en pasos de 0,5.
 */
@Composable
fun StarRatingInput(
    halves: Int?,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 44.dp,
) {
    val currentHalves by rememberUpdatedState(halves)
    val currentOnChange by rememberUpdatedState(onChange)
    val layoutDirection = LocalLayoutDirection.current
    val description = stringResource(R.string.detail_rating)
    val state = ratingDescription(halves)

    fun select(newHalves: Int) {
        if (newHalves != currentHalves) currentOnChange(newHalves)
    }

    Stars(
        halves = halves,
        starSize = starSize,
        modifier = modifier
            .pointerInput(layoutDirection) {
                detectTapGestures { offset ->
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - offset.x else offset.x
                    select(ratingFromPosition(x, size.width.toFloat()))
                }
            }
            .pointerInput(layoutDirection) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val position = change.position.x
                    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - position else position
                    select(ratingFromPosition(x, size.width.toFloat()))
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = state
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = (halves ?: MIN_RATING_HALVES) / 2f,
                    range = MIN_RATING_HALVES / 2f..MAX_RATING_HALVES / 2f,
                    steps = MAX_RATING_HALVES - MIN_RATING_HALVES - 1,
                )
                setProgress { target ->
                    select((target * 2).roundToInt().coerceIn(MIN_RATING_HALVES, MAX_RATING_HALVES))
                    true
                }
            },
    )
}

@Composable
private fun ratingDescription(halves: Int?): String =
    if (halves == null) {
        stringResource(R.string.rating_none)
    } else {
        stringResource(R.string.rating_value, formatRating(halves))
    }

@Composable
private fun Stars(halves: Int?, starSize: Dp, modifier: Modifier = Modifier) {
    val value = halves ?: 0
    Row(modifier) {
        repeat(STAR_COUNT) { index ->
            val icon = when {
                value >= (index + 1) * 2 -> Icons.Filled.Star
                value == index * 2 + 1 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

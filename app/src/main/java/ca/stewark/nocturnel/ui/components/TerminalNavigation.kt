package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.theme.TerminalTheme

@Composable
fun TerminalNavigation(
    selected: NocturneLDestination,
    onSelected: (NocturneLDestination) -> Unit,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = TerminalTheme.palette
    val transition = rememberInfiniteTransition(label = "active navigation")
    val pulse = if (effectsEnabled) {
        transition.animateFloat(
            initialValue = .62f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "active navigation pulse",
        ).value
    } else 1f
    Column(modifier.fillMaxWidth().testTag(if (effectsEnabled) "animated-navigation" else "static-navigation")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TerminalDimensions.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NocturneLDestination.entries.forEach { destination ->
                BracketButton(
                    label = destination.label,
                    onClick = { onSelected(destination) },
                    selected = destination == selected,
                    modifier = Modifier
                        .widthIn(min = TerminalDimensions.minimumTouchTarget)
                        .graphicsLayer { alpha = if (destination == selected) pulse else 1f },
                    textStyle = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.sp,
                    ),
                    horizontalPadding = 0.dp,
                    contentAlignment = Alignment.CenterStart,
                )
            }
        }
        Canvas(Modifier.fillMaxWidth()) {
            var x = 0f
            while (x < size.width) {
                drawLine(palette.textSecondary, Offset(x, 0f), Offset((x + 6.dp.toPx()).coerceAtMost(size.width), 0f), 1.dp.toPx())
                x += 10.dp.toPx()
            }
        }
    }
}

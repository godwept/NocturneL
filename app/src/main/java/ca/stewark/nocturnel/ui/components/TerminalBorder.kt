package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.Dp
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.theme.TerminalPalette
import ca.stewark.nocturnel.ui.theme.TerminalTheme

internal data class TerminalBorderStyle(
    val glowColor: Color,
    val glowAlpha: Float,
)

internal fun terminalBorderStyle(
    palette: TerminalPalette,
    effectsEnabled: Boolean,
    emphasized: Boolean,
): TerminalBorderStyle = TerminalBorderStyle(
    glowColor = palette.glowColor,
    glowAlpha = if (effectsEnabled) {
        palette.glowStrength * if (emphasized) 1f else .5f
    } else {
        0f
    },
)

@Composable
fun Modifier.terminalBorder(
    color: Color,
    emphasized: Boolean = false,
    width: Dp = TerminalDimensions.border,
): Modifier {
    val style = terminalBorderStyle(TerminalTheme.palette, TerminalTheme.effectsEnabled, emphasized)
    return this
        .drawWithCache {
            onDrawBehind {
                if (style.glowAlpha > 0f) {
                    listOf(6f to style.glowAlpha * .5f, 3f to style.glowAlpha).forEach { (stroke, alpha) ->
                        drawRect(
                            color = style.glowColor.copy(alpha = alpha),
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(
                                (size.width - stroke).coerceAtLeast(0f),
                                (size.height - stroke).coerceAtLeast(0f),
                            ),
                            style = Stroke(stroke),
                        )
                    }
                }
            }
        }
        .border(width, color)
}

package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.theme.TerminalTheme

@Composable
fun BracketButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentDescription: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val palette = TerminalTheme.palette
    val color = when {
        !enabled -> palette.textSecondary.copy(alpha = .5f)
        selected -> palette.selection
        else -> palette.textPrimary
    }
    Box(
        modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = TerminalDimensions.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "[ ${label.uppercase()} ]",
            color = color,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BracketIconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    BracketButton(glyph, onClick, modifier, enabled, selected, contentDescription)
}

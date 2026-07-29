package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun TerminalToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
            .toggleable(checked, enabled, Role.Checkbox) { onCheckedChange(it) }
            .padding(horizontal = TerminalDimensions.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (checked) "[X]" else "[ ]", color = MaterialTheme.colorScheme.primary)
        Text(label.uppercase(), modifier = Modifier.padding(start = TerminalDimensions.xs))
    }
}

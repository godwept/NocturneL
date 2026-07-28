package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TerminalFrame(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text("[ $title ]", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}

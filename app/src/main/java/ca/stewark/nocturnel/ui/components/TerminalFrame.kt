package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TerminalFrame(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text("+--[ $title ]${"-".repeat(32)}+", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
        Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) { content() }
        Text("+${"-".repeat(46)}+", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
    }
}

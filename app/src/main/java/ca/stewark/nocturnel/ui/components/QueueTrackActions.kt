package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QueueTrackActions(
    title: String,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        BracketIconButton("NXT", "Play $title next", onPlayNext)
        BracketIconButton("+Q", "Add $title to queue", onAddToQueue)
    }
}

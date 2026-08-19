package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QueueTrackActions(
    title: String,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    playCount: Long? = null,
    favorite: Boolean? = null,
    onToggleFavorite: () -> Unit = {},
) {
    Row(modifier) {
        playCount?.let { Text("$it×", color = MaterialTheme.colorScheme.secondary) }
        favorite?.let { FavoriteToggle(title, it, onToggleFavorite) }
        BracketIconButton("+Q", "Add $title to queue", onAddToQueue)
    }
}

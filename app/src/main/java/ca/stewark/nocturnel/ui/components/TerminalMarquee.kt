package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TerminalMarquee(text: String, effectsEnabled: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = if (effectsEnabled) modifier.basicMarquee(iterations = Int.MAX_VALUE) else modifier,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

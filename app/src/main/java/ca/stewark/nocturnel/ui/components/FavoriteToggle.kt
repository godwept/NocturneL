package ca.stewark.nocturnel.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FavoriteToggle(
    title: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BracketIconButton(
        glyph = "FAV",
        contentDescription = if (selected) "Remove $title from favorites" else "Add $title to favorites",
        onClick = onToggle,
        modifier = modifier,
        selected = selected,
    )
}

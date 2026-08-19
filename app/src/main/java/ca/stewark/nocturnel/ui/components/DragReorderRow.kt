package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ca.stewark.nocturnel.ui.theme.AlertAmber
import ca.stewark.nocturnel.ui.theme.TerminalBlackAlt
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

internal fun Modifier.dragReorderRow(
    isDragging: Boolean,
    translationY: Float,
    position: Int,
    itemCount: Int,
    testTag: String,
): Modifier = this
    .zIndex(if (isDragging) 1f else 0f)
    .graphicsLayer {
        this.translationY = translationY
        scaleX = if (isDragging) 1.02f else 1f
        scaleY = if (isDragging) 1.02f else 1f
        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
    }
    .then(
        if (isDragging) Modifier.background(TerminalBlackAlt).border(TerminalDimensions.border, AlertAmber)
        else Modifier,
    )
    .testTag(testTag)
    .semantics {
        if (isDragging) stateDescription = "Dragging, position ${position + 1} of $itemCount"
    }

@Composable
internal fun DragReorderHandle(
    title: String,
    dragKey: String,
    testTag: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val actions = buildList {
        if (canMoveUp) add(CustomAccessibilityAction("Move $title up") { onMoveUp(); true })
        if (canMoveDown) add(CustomAccessibilityAction("Move $title down") { onMoveDown(); true })
    }
    BracketIconButton(
        "::",
        "Reorder $title",
        {},
        Modifier.testTag(testTag)
            .semantics { customActions = actions }
            .pointerInput(dragKey) {
                detectVerticalDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                )
            },
    )
}

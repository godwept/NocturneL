package ca.stewark.nocturnel.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragReorderStateTest {
    @Test fun `session captures identity and previews moves across any distance`() {
        val session = beginDragReorder(listOf("a", "b", "c", "d"), "b")!!

        assertEquals(1, session.startIndex)
        assertEquals(1, session.targetIndex)
        assertEquals(listOf("a", "b", "c", "d"), session.startingOrder)
        assertEquals(listOf("a", "c", "d", "b"), session.moveTo(3).previewOrder)
        assertEquals(listOf("b", "a", "c", "d"), session.moveTo(0).previewOrder)
        assertEquals(listOf("a", "b", "c", "d"), session.startingOrder)
    }

    @Test fun `unknown key cannot begin and destinations clamp`() {
        assertNull(beginDragReorder(listOf("a", "b"), "missing"))
        val session = beginDragReorder(listOf("a", "b", "c"), "b")!!
        assertEquals(listOf("b", "a", "c"), session.moveTo(-10).previewOrder)
        assertEquals(listOf("a", "c", "b"), session.moveTo(10).previewOrder)
    }

    @Test fun `target follows crossed visible row midpoints`() {
        val bounds = listOf(
            DragReorderItemBounds("a", 0, 0f, 100f),
            DragReorderItemBounds("b", 1, 100f, 200f),
            DragReorderItemBounds("c", 2, 200f, 300f),
            DragReorderItemBounds("d", 3, 300f, 400f),
        )

        assertEquals(1, dragReorderTargetIndex("b", 175f, bounds, 1))
        assertEquals(2, dragReorderTargetIndex("b", 251f, bounds, 1))
        assertEquals(3, dragReorderTargetIndex("b", 399f, bounds, 1))
        assertEquals(0, dragReorderTargetIndex("d", 1f, bounds, 3))
        assertEquals(0, dragReorderTargetIndex("b", -20f, bounds, 1))
        assertEquals(3, dragReorderTargetIndex("b", 450f, bounds, 1))
    }

    @Test fun `target ignores dragged row and keeps target without neighbors`() {
        assertEquals(
            2,
            dragReorderTargetIndex(
                draggedKey = "dragged",
                draggedCenterY = 999f,
                visibleItems = listOf(DragReorderItemBounds("dragged", 2, 0f, 10f)),
                currentTargetIndex = 2,
            ),
        )
    }

    @Test fun `edge scroll velocity is directional proportional and capped`() {
        assertEquals(0f, dragReorderEdgeVelocity(500f, 0f, 1000f, 64f, 900f))
        assertEquals(-450f, dragReorderEdgeVelocity(32f, 0f, 1000f, 64f, 900f))
        assertEquals(450f, dragReorderEdgeVelocity(968f, 0f, 1000f, 64f, 900f))
        assertEquals(-900f, dragReorderEdgeVelocity(-100f, 0f, 1000f, 64f, 900f))
        assertEquals(900f, dragReorderEdgeVelocity(1100f, 0f, 1000f, 64f, 900f))
    }

    @Test fun `compatibility requires unchanged order and dragged key`() {
        val session = beginDragReorder(listOf("a", "b", "c"), "b")!!
        assertTrue(session.isCompatible(listOf("a", "b", "c")))
        assertFalse(session.isCompatible(listOf("a", "c")))
        assertFalse(session.isCompatible(listOf("b", "a", "c")))
    }

    @Test fun `commit exists only when destination changed`() {
        val session = beginDragReorder(listOf("a", "b", "c"), "b")!!
        assertNull(session.commitOrNull())
        assertEquals(DragReorderCommit("b", 2), session.moveTo(2).commitOrNull())
    }
}

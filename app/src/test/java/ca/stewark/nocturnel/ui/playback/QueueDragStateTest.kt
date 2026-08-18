package ca.stewark.nocturnel.ui.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueDragStateTest {
    @Test fun `session captures identity and previews moves across any distance`() {
        val session = beginQueueDrag(listOf("a", "b", "c", "d"), "b", "current")!!

        assertEquals(1, session.startIndex)
        assertEquals(1, session.targetIndex)
        assertEquals(listOf("a", "b", "c", "d"), session.startingOrder)
        assertEquals("current", session.expectedCurrentOccurrenceId)
        assertEquals(listOf("a", "c", "d", "b"), session.moveTo(3).previewOrder)
        assertEquals(listOf("b", "a", "c", "d"), session.moveTo(0).previewOrder)
        assertEquals(listOf("a", "b", "c", "d"), session.startingOrder)
    }

    @Test fun `unknown occurrence cannot begin and destinations clamp`() {
        assertNull(beginQueueDrag(listOf("a", "b"), "missing", "current"))
        val session = beginQueueDrag(listOf("a", "b", "c"), "b", null)!!
        assertEquals(listOf("b", "a", "c"), session.moveTo(-10).previewOrder)
        assertEquals(listOf("a", "c", "b"), session.moveTo(10).previewOrder)
    }

    @Test fun `target follows crossed visible row midpoints`() {
        val bounds = listOf(
            QueueDragItemBounds("a", 0, 0f, 100f),
            QueueDragItemBounds("b", 1, 100f, 200f),
            QueueDragItemBounds("c", 2, 200f, 300f),
            QueueDragItemBounds("d", 3, 300f, 400f),
        )

        assertEquals(1, queueDragTargetIndex("b", 175f, bounds, 1))
        assertEquals(2, queueDragTargetIndex("b", 251f, bounds, 1))
        assertEquals(3, queueDragTargetIndex("b", 399f, bounds, 1))
        assertEquals(0, queueDragTargetIndex("d", 1f, bounds, 3))
        assertEquals(0, queueDragTargetIndex("b", -20f, bounds, 1))
        assertEquals(3, queueDragTargetIndex("b", 450f, bounds, 1))
    }

    @Test fun `target ignores dragged row bounds and keeps target without neighbors`() {
        assertEquals(
            2,
            queueDragTargetIndex(
                draggedOccurrenceId = "dragged",
                draggedCenterY = 999f,
                visibleItems = listOf(QueueDragItemBounds("dragged", 2, 0f, 10f)),
                currentTargetIndex = 2,
            ),
        )
    }

    @Test fun `edge scroll velocity is directional proportional and capped`() {
        assertEquals(0f, queueDragEdgeVelocity(500f, 0f, 1000f, 64f, 900f))
        assertEquals(-450f, queueDragEdgeVelocity(32f, 0f, 1000f, 64f, 900f))
        assertEquals(450f, queueDragEdgeVelocity(968f, 0f, 1000f, 64f, 900f))
        assertEquals(-900f, queueDragEdgeVelocity(-100f, 0f, 1000f, 64f, 900f))
        assertEquals(900f, queueDragEdgeVelocity(1100f, 0f, 1000f, 64f, 900f))
    }

    @Test fun `compatibility requires unchanged queue and current occurrence`() {
        val session = beginQueueDrag(listOf("a", "b", "c"), "b", "current")!!
        assertTrue(session.isCompatible(listOf("a", "b", "c"), "current"))
        assertFalse(session.isCompatible(listOf("a", "c"), "current"))
        assertFalse(session.isCompatible(listOf("b", "a", "c"), "current"))
        assertFalse(session.isCompatible(listOf("a", "b", "c"), "next"))
    }

    @Test fun `commit exists only when destination changed`() {
        val session = beginQueueDrag(listOf("a", "b", "c"), "b", "current")!!
        assertNull(session.commitOrNull())
        assertEquals(QueueDragCommit("b", 2, "current"), session.moveTo(2).commitOrNull())
    }
}

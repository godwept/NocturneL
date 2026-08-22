package ca.stewark.nocturnel.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverFlowSelectionTest {
    @Test fun nearestCoverUsesDistanceToViewportCenter() {
        val items = listOf(
            CoverFlowItemBounds(0, -50, 100),
            CoverFlowItemBounds(1, 50, 100),
            CoverFlowItemBounds(2, 150, 100),
        )

        assertEquals(1, nearestCoverIndex(0, 200, items))
        assertEquals(0, nearestCoverIndex(0, 100, items))
        assertEquals(2, nearestCoverIndex(100, 300, items))
    }

    @Test fun nearestCoverUsesLowerIndexForTiesAndNullForNoItems() {
        val tied = listOf(
            CoverFlowItemBounds(4, 0, 100),
            CoverFlowItemBounds(5, 100, 100),
        )

        assertEquals(4, nearestCoverIndex(0, 200, tied))
        assertNull(nearestCoverIndex(0, 200, emptyList()))
    }

    @Test fun reconciliationPreservesAlbumIdentityAcrossReordering() {
        assertEquals(
            CoverFlowSelection("b", 2),
            reconcileCoverFlowSelection("b", 1, listOf("c", "a", "b")),
        )
    }

    @Test fun reconciliationUsesAndClampsPreviousIndexWhenAlbumDisappears() {
        assertEquals(
            CoverFlowSelection("c", 1),
            reconcileCoverFlowSelection("b", 1, listOf("a", "c", "d")),
        )
        assertEquals(
            CoverFlowSelection("c", 2),
            reconcileCoverFlowSelection("removed", 9, listOf("a", "b", "c")),
        )
    }

    @Test fun reconciliationHandlesInitialAndEmptyCollections() {
        assertEquals(
            CoverFlowSelection("a", 0),
            reconcileCoverFlowSelection(null, null, listOf("a", "b")),
        )
        assertEquals(
            CoverFlowSelection(null, null),
            reconcileCoverFlowSelection("a", 0, emptyList()),
        )
    }
}

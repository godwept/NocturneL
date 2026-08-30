package ca.stewark.nocturnel.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverFlowLayoutTest {
    @Test fun coverSizeUsesWidthHeightAndMaximumConstraints() {
        assertEquals(336f, coverFlowCoverSize(400f, 600f), 0.01f)
        assertEquals(340f, coverFlowCoverSize(412f, 600f), 0.01f)
        assertEquals(240f, coverFlowCoverSize(412f, 240f), 0.01f)
        assertEquals(0f, coverFlowCoverSize(0f, 600f), 0.01f)
        assertEquals(0f, coverFlowCoverSize(400f, -1f), 0.01f)
    }

    @Test fun overlapStrideExposesApprovedNeighborFraction() {
        listOf(340f, 268.8f).forEach { coverSize ->
            val neighborWidth = coverSize * COVER_FLOW_NEIGHBOR_SCALE
            val stride = coverFlowItemStride(coverSize)
            val exposedWidth = stride + neighborWidth / 2f - coverSize / 2f
            val exposedFraction = exposedWidth / neighborWidth

            assertEquals(COVER_FLOW_NEIGHBOR_EXPOSURE, exposedFraction, 0.001f)
            assertTrue(stride > 0f)
            assertTrue(stride < coverSize)
            assertEquals(stride - coverSize, coverFlowItemSpacing(coverSize), 0.001f)
        }
    }

    @Test fun visualStateEmphasizesCenterAndDimsNeighbors() {
        val center = coverFlowVisualState(0f)
        val neighbor = coverFlowVisualState(1f)

        assertEquals(1f, center.scale, 0.001f)
        assertEquals(1f, center.alpha, 0.001f)
        assertTrue(center.stackingOrder > neighbor.stackingOrder)
        assertTrue(center.interactive)
        assertEquals(COVER_FLOW_NEIGHBOR_SCALE, neighbor.scale, 0.001f)
        assertEquals(COVER_FLOW_NEIGHBOR_ALPHA, neighbor.alpha, 0.001f)
        assertTrue(neighbor.interactive)
    }

    @Test fun visualStateInterpolatesDuringDragAndHidesDistantItems() {
        val halfway = coverFlowVisualState(0.5f)
        val mirrored = coverFlowVisualState(-0.5f)
        val distant = coverFlowVisualState(2f)

        assertEquals(0.88f, halfway.scale, 0.001f)
        assertEquals(0.75f, halfway.alpha, 0.001f)
        assertEquals(halfway.scale, mirrored.scale, 0.001f)
        assertEquals(halfway.alpha, mirrored.alpha, 0.001f)
        assertEquals(0f, distant.alpha, 0.001f)
        assertFalse(distant.interactive)
    }

    @Test fun distanceFromCenterUsesTheOverlappingItemStride() {
        assertEquals(0f, coverFlowDistanceFromCenter(0, 400, 100, 200, 80f), 0.001f)
        assertEquals(-1f, coverFlowDistanceFromCenter(0, 400, 20, 200, 80f), 0.001f)
        assertEquals(1f, coverFlowDistanceFromCenter(0, 400, 180, 200, 80f), 0.001f)
        assertEquals(0f, coverFlowDistanceFromCenter(0, 400, 100, 200, 0f), 0.001f)
    }
}

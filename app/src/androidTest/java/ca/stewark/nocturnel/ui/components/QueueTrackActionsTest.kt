package ca.stewark.nocturnel.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QueueTrackActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actionsHaveDistinctAccessibleCallbacks() {
        var next = 0
        var append = 0
        compose.setContent { NocturneLTheme { QueueTrackActions("Carrier", { next++ }, { append++ }) } }
        compose.onNodeWithContentDescription("Play Carrier next").performClick()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        assertEquals(1, next)
        assertEquals(1, append)
    }
}

package ca.stewark.nocturnel.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QueueTrackActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exposesOnlyTheAppendQueueAction() {
        var appended = 0
        compose.setContent { NocturneLTheme { QueueTrackActions("Carrier", { appended++ }) } }

        compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
        compose.onNodeWithText("[ NXT ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        assertEquals(1, appended)
    }
}

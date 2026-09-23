package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ImmersiveSystemBarsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun restoresThePreviousSystemBarVisibilityWhenDisposed() {
        var immersive by mutableStateOf(false)
        compose.setContent { if (immersive) ImmersiveSystemBars() }
        val decor = compose.activity.window.decorView
        val statusType = WindowInsetsCompat.Type.statusBars()
        val navigationType = WindowInsetsCompat.Type.navigationBars()
        val captionType = WindowInsetsCompat.Type.captionBar()
        val initial = ViewCompat.getRootWindowInsets(decor)!!
        val initialStatus = initial.isVisible(statusType)
        val initialNavigation = initial.isVisible(navigationType)
        val initialCaption = initial.isVisible(captionType)

        compose.runOnIdle { immersive = true }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.systemBars()) == false
        }
        assertFalse(ViewCompat.getRootWindowInsets(decor)!!.isVisible(statusType))
        assertFalse(ViewCompat.getRootWindowInsets(decor)!!.isVisible(navigationType))

        compose.runOnIdle { immersive = false }
        compose.waitUntil(5_000) {
            val current = ViewCompat.getRootWindowInsets(decor)
            current != null && current.isVisible(statusType) == initialStatus &&
                current.isVisible(navigationType) == initialNavigation &&
                current.isVisible(captionType) == initialCaption
        }
        val restored = ViewCompat.getRootWindowInsets(decor)!!
        assertEquals(initialStatus, restored.isVisible(statusType))
        assertEquals(initialNavigation, restored.isVisible(navigationType))
        assertEquals(initialCaption, restored.isVisible(captionType))
    }
}

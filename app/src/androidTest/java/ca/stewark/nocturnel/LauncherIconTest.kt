package ca.stewark.nocturnel

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconTest {
    @Test fun applicationUsesAdaptiveLauncherIcon() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotEquals(0, context.applicationInfo.icon)
        assertTrue(context.getDrawable(context.applicationInfo.icon) is AdaptiveIconDrawable)
    }
}

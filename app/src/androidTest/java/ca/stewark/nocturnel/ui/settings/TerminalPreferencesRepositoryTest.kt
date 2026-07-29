package ca.stewark.nocturnel.ui.settings

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPreferencesRepositoryTest {
    @Test fun preferenceDefaultsOnAndPersistsOff() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-preferences-test"
        context.getSharedPreferences(name, 0).edit().clear().commit()
        assertTrue(TerminalPreferencesRepository(context, name).effectsEnabled.value)
        TerminalPreferencesRepository(context, name).setEffectsEnabled(false)
        assertFalse(TerminalPreferencesRepository(context, name).effectsEnabled.value)
    }
}

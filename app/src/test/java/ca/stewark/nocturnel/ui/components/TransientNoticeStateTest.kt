package ca.stewark.nocturnel.ui.components

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransientNoticeStateTest {
    @Test fun successNoticeExpiresAfterFiveSeconds() = runTest {
        val notices = TransientNoticeState(this, timeoutMillis = 5_000)
        notices.info("Done")
        advanceTimeBy(5_000)
        runCurrent()
        assertNull(notices.current)
    }

    @Test fun replacementSuccessRestartsTheTimer() = runTest {
        val notices = TransientNoticeState(this, timeoutMillis = 5_000)
        notices.info("First")
        advanceTimeBy(4_000)
        notices.info("Second")
        advanceTimeBy(1_500)
        runCurrent()
        assertEquals("Second", notices.current?.text)
    }

    @Test fun persistentNoticesDoNotExpire() = runTest {
        val notices = TransientNoticeState(this, timeoutMillis = 5_000)
        notices.persistent("Cancelled", NoticeSeverity.WARNING)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals("Cancelled", notices.current?.text)
    }
}

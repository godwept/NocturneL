package ca.stewark.nocturnel.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AppNotice(val text: String, val severity: NoticeSeverity, val transient: Boolean)

class TransientNoticeState(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = 5_000,
) {
    var current: AppNotice? by mutableStateOf(null)
        private set
    private var expiryJob: Job? = null

    fun info(text: String) = publish(AppNotice(text, NoticeSeverity.INFO, transient = true))

    fun persistent(text: String, severity: NoticeSeverity = NoticeSeverity.ERROR) =
        publish(AppNotice(text, severity, transient = false))

    fun publish(notice: AppNotice) {
        expiryJob?.cancel()
        current = notice
        if (notice.transient) {
            expiryJob = scope.launch {
                delay(timeoutMillis)
                if (current == notice) current = null
            }
        }
    }
}

package se.gustavkarlsson.chefgpt.snackbar

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_DURATION = 5.seconds
private const val DEFAULT_DISMISS_TEXT = "OK"

// A snackbar to display: its [text], whether it represents an [isError] (styled distinctly),
// the [dismissText] shown on its dismiss button, and how long it stays before being
// automatically dismissed. Errors default to staying until dismissed.
data class SnackbarMessage(
    val text: String,
    val isError: Boolean,
    val dismissText: String,
    val duration: Duration,
)

/**
 * The single app-wide source of snackbar messages. Any screen or background job can
 * [show] a message; a root [SnackbarMessageHost] renders it, so a message survives the
 * sender (a ViewModel) being cleared.
 */
class SnackbarManager {
    private val channel = Channel<SnackbarMessage>(Channel.UNLIMITED)
    val messages: Flow<SnackbarMessage> = channel.receiveAsFlow()

    fun show(message: SnackbarMessage) {
        channel.trySend(message)
    }

    fun show(
        text: String,
        isError: Boolean = false,
        dismissText: String = DEFAULT_DISMISS_TEXT,
        duration: Duration = if (isError) Duration.INFINITE else DEFAULT_DURATION,
    ) {
        show(SnackbarMessage(text, isError, dismissText, duration))
    }
}

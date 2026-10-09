package se.gustavkarlsson.chefgpt.snackbar

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val DEFAULT_DURATION = 5.seconds
private const val DEFAULT_DISMISS_TEXT = "OK"

// How long a scheduled message may wait in the queue before being dropped unshown.
internal val QUEUE_EXPIRY = 30.seconds

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
 * sender (a ViewModel) being cleared. A message that has queued behind a showing
 * snackbar for longer than [QUEUE_EXPIRY] is dropped without being shown.
 */
class SnackbarManager(
    private val timeSource: TimeSource,
) {
    private data class ScheduledMessage(
        val message: SnackbarMessage,
        val scheduledAt: TimeMark,
    )

    private val channel = Channel<ScheduledMessage>(Channel.UNLIMITED)

    // Expired messages are filtered out here, in the manager, so they never reach the UI.
    val messages: Flow<SnackbarMessage> =
        channel
            .receiveAsFlow()
            .filter { it.scheduledAt.elapsedNow() <= QUEUE_EXPIRY }
            .map { it.message }

    fun show(message: SnackbarMessage) {
        channel.trySend(ScheduledMessage(message, timeSource.markNow()))
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

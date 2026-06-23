package se.gustavkarlsson.chefgpt.snackbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

// Binds a ViewModel's [messages] flow to a [SnackbarHostState], showing each message for its
// configured duration. Pass the returned state to [SnackbarMessageHost] in a Scaffold's
// `snackbarHost` slot so messages display in the standard Material way.
@Composable
fun rememberSnackbarHostState(messages: Flow<SnackbarMessage>): SnackbarHostState {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(messages) {
        messages.collect { message ->
            // The snackbar itself stays Indefinite; the timeout drives auto-dismissal so any
            // Duration is honored, not just Material's fixed Short/Long presets. An infinite
            // duration never times out, leaving the dismiss button as the only way to close it.
            withTimeoutOrNull(message.duration) {
                hostState.showSnackbar(
                    MessageSnackbarVisuals(message.text, message.dismissText, message.isError),
                )
            }
        }
    }
    return hostState
}

// Renders [hostState], styling error messages with the theme's error colors. Use in place of
// the default `SnackbarHost` in a Scaffold's `snackbarHost` slot.
@Composable
fun SnackbarMessageHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState, modifier) { data ->
        val isError = (data.visuals as? MessageSnackbarVisuals)?.isError == true
        Snackbar(
            snackbarData = data,
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else SnackbarDefaults.color,
            contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else SnackbarDefaults.contentColor,
        )
    }
}

private class MessageSnackbarVisuals(
    override val message: String,
    // Rendered as the snackbar's action button; tapping it dismisses the snackbar.
    override val actionLabel: String,
    val isError: Boolean,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false

    // Auto-dismissal is handled by the caller's timeout, not Material's duration presets.
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

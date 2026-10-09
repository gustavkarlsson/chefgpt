package se.gustavkarlsson.chefgpt.updates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Set when the server answers 410 Gone: the API versions this app speaks have been
 * retired. App() watches it and replaces the whole back stack with the update-required screen.
 */
class UpdateRequiredNotifier {
    val updateRequired: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun notifyUpdateRequired() {
        updateRequired.value = true
    }
}

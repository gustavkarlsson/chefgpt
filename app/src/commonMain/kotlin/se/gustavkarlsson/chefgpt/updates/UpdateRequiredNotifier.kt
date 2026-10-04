package se.gustavkarlsson.chefgpt.updates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Set when the server answers 410 Gone: the API versions this app speaks have been
 * retired. App() watches it and replaces the whole back stack with the update-required screen.
 */
class UpdateRequiredNotifier {
    private val _updateRequired = MutableStateFlow(false)
    val updateRequired: StateFlow<Boolean> = _updateRequired.asStateFlow()

    fun notifyUpdateRequired() {
        _updateRequired.value = true
    }
}

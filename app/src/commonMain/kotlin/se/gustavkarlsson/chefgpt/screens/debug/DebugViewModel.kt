package se.gustavkarlsson.chefgpt.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.SERVER_BASE_URL
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.navigation.Navigator

class DebugViewModel(
    private val settings: Settings,
    private val navigator: Navigator,
) : ViewModel() {
    private val innerState = MutableStateFlow(State(baseUrl = SERVER_BASE_URL))

    val uiState: StateFlow<UiState> =
        innerState
            .map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), innerState.value.toUiState())

    private fun State.toUiState(): UiState =
        UiState(
            items =
                listOf(
                    UiDebugItem.TextField(
                        title = "Server base URL",
                        value = baseUrl,
                        onValueChange = ::updateBaseUrl,
                    ),
                ),
            onClickBack = navigator::pop,
        )

    init {
        viewModelScope.launch {
            innerState.update { it.copy(baseUrl = settings.getBaseUrl()) }
        }
    }

    private fun updateBaseUrl(value: String) {
        innerState.update { it.copy(baseUrl = value) }
        viewModelScope.launch { settings.setBaseUrl(value) }
    }
}

private data class State(
    val baseUrl: String,
)

data class UiState(
    val items: List<UiDebugItem>,
    val onClickBack: () -> Unit,
)

sealed interface UiDebugItem {
    val title: String

    data class TextField(
        override val title: String,
        val value: String,
        val onValueChange: (String) -> Unit,
        val placeholder: String? = null,
    ) : UiDebugItem

    data class Toggle(
        override val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : UiDebugItem

    data class Button(
        override val title: String,
        val label: String,
        val onClick: () -> Unit,
    ) : UiDebugItem
}

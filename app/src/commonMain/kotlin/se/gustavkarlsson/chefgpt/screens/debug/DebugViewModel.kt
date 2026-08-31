package se.gustavkarlsson.chefgpt.screens.debug

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.BASE_URL_HINT
import se.gustavkarlsson.chefgpt.SERVER_BASE_URL
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel

class DebugViewModel(
    private val settings: Settings,
    private val navigator: Navigator,
) : StateViewModel<State, UiState>() {
    override fun createInitialState() = State(baseUrl = SERVER_BASE_URL)

    override fun State.toUiState(): UiState =
        UiState(
            items =
                buildList {
                    add(
                        UiDebugItem.Labeled.TextField(
                            title = "Server base URL",
                            value = baseUrl,
                            onValueChange = ::updateBaseUrl,
                            placeholder = null,
                        ),
                    )
                    BASE_URL_HINT?.let { hint ->
                        add(UiDebugItem.Note(text = hint))
                    }
                },
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

data class State(
    val baseUrl: String,
)

data class UiState(
    val items: List<UiDebugItem>,
    val onClickBack: () -> Unit,
)

sealed interface UiDebugItem {
    data class Note(
        val text: String,
    ) : UiDebugItem

    sealed interface Labeled : UiDebugItem {
        val title: String

        data class TextField(
            override val title: String,
            val value: String,
            val onValueChange: (String) -> Unit,
            val placeholder: String?,
        ) : Labeled

        data class Toggle(
            override val title: String,
            val checked: Boolean,
            val onCheckedChange: (Boolean) -> Unit,
        ) : Labeled

        data class Button(
            override val title: String,
            val label: String,
            val onClick: () -> Unit,
        ) : Labeled
    }
}

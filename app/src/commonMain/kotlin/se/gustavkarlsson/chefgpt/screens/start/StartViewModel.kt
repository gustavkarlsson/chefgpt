package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.gustavkarlsson.chefgpt.LoginRepository

class StartViewModel(
    private val loginRepository: LoginRepository,
) : ViewModel() {
    private data class State(
        val loggedIn: Boolean = false,
        val userName: String? = null,
    )

    sealed interface ViewState {
        data class LoggedOut(
            val onClickLogin: () -> Unit,
        ) : ViewState

        data class LoggedIn(
            val userName: String,
            val onClickStartChatting: () -> Unit,
        ) : ViewState
    }

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
        val credentials = loginRepository.load()
        if (credentials != null) {
            innerState.value = State(loggedIn = true, userName = credentials.username)
        }
    }

    private fun State.toViewState(): ViewState =
        if (loggedIn && userName != null) {
            ViewState.LoggedIn(
                userName = userName,
                onClickStartChatting = {
                    // TODO Navigate to chat
                },
            )
        } else {
            ViewState.LoggedOut(
                onClickLogin = {
                    // TODO Implement login
                },
            )
        }
}

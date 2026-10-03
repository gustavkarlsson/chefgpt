package se.gustavkarlsson.chefgpt.screens.login

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.debug.DebugScreen
import se.gustavkarlsson.chefgpt.screens.loading.LoadingScreen
import se.gustavkarlsson.chefgpt.sessions.RegisterError
import se.gustavkarlsson.chefgpt.sessions.UserCredentials
import se.gustavkarlsson.chefgpt.sessions.usecases.LogIn
import se.gustavkarlsson.chefgpt.sessions.usecases.Register
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

private val log = Logger.withTag("${LoginViewModel::class.simpleName}")

class LoginViewModel(
    private val register: Register,
    private val logIn: LogIn,
    private val showSnackbar: ShowSnackbar,
    private val navigator: Navigator,
) : StateViewModel<State, UiState>() {
    override fun createInitialState() =
        State(
            username = "",
            password = "",
            authenticating = false,
        )

    override fun State.toUiState(): UiState =
        UiState(
            username = username,
            password = password,
            onUsernameChange = ::updateUsername,
            onPasswordChange = ::updatePassword,
            onClickRegister = if (canAuthenticate) ::registerUser else null,
            onClickLogin = if (canAuthenticate) ::logInUser else null,
            onClickDebug = ::openDebug,
        )

    private val State.canAuthenticate: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !authenticating

    private fun updateUsername(username: String) {
        innerState.update { it.copy(username = username) }
    }

    private fun updatePassword(password: String) {
        innerState.update { it.copy(password = password) }
    }

    private fun openDebug() {
        navigator.push(DebugScreen())
    }

    private fun registerUser() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.username
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                register(state.inputCredentials)
                    .onOk {
                        log.i { "Registered user '$username'" }
                        navigator.replaceAll(LoadingScreen())
                    }.onErr { error ->
                        when (error) {
                            is RegisterError.ServerError -> {
                                log.i { "Registration failed for '$username': ${error.error}" }
                                showSnackbar("Registration failed", isError = true)
                            }

                            RegisterError.StorageFailed -> {
                                log.e { "Registration succeeded but failed to save session for '$username'" }
                                showSnackbar("Couldn't save your session", isError = true)
                            }
                        }
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }

    private fun logInUser() {
        val state = innerState.value
        if (state.authenticating) return
        val username = state.username
        innerState.update { it.copy(authenticating = true) }
        viewModelScope.launch {
            try {
                logIn(state.inputCredentials)
                    .onOk {
                        log.i { "Logged in as '$username'" }
                        navigator.replaceAll(LoadingScreen())
                    }.onErr {
                        log.i { "Login failed for '$username': $it" }
                        showSnackbar("Login failed", isError = true)
                    }
            } finally {
                innerState.update { it.copy(authenticating = false) }
            }
        }
    }
}

data class State(
    val username: String,
    val password: String,
    val authenticating: Boolean,
) {
    val inputCredentials: UserCredentials
        get() = UserCredentials(username, password)
}

data class UiState(
    val username: String,
    val password: String,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onClickRegister: (() -> Unit)?,
    val onClickLogin: (() -> Unit)?,
    val onClickDebug: () -> Unit,
)

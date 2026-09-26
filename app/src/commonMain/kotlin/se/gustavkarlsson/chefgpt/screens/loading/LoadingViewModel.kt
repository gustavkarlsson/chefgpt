package se.gustavkarlsson.chefgpt.screens.loading

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.facts.usecases.GetFacts
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.home.HomeScreen
import se.gustavkarlsson.chefgpt.screens.login.LoginScreen
import se.gustavkarlsson.chefgpt.screens.onboarding.OnboardingScreen
import se.gustavkarlsson.chefgpt.sessions.usecases.GetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.usecases.LogOut
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("${LoadingViewModel::class.simpleName}")

class LoadingViewModel(
    private val getCurrentSession: GetCurrentSession,
    private val getFacts: GetFacts,
    private val logOut: LogOut,
    private val showSnackbar: ShowSnackbar,
    private val navigator: Navigator,
) : StateViewModel<State, UiState>() {
    private val resolveJob = atomic<Job?>(null)

    override fun createInitialState() = State(failed = false)

    override fun State.toUiState(): UiState =
        if (!failed) {
            UiState.Loading
        } else {
            UiState.Failed(
                onClickRetry = ::retry,
                onClickLogout = ::logout,
            )
        }

    init {
        startResolve()
    }

    private fun retry() {
        innerState.update { it.copy(failed = false) }
        startResolve()
    }

    private fun logout() {
        viewModelScope.launch {
            logOut()
            navigator.replaceAll(LoginScreen())
        }
    }

    private fun startResolve() {
        val job = viewModelScope.launch { resolve() }
        resolveJob.getAndSet(job)?.cancel()
    }

    private suspend fun resolve() {
        val (credentials, _) = getCurrentSession()
        if (credentials == null) {
            navigator.replaceAll(LoginScreen())
            return
        }
        var attempts = 0
        while (true) {
            attempts++
            val (facts, error) = getFacts(credentials.sessionId)
            when {
                facts != null -> {
                    val screen =
                        if (facts.hasUnknown) {
                            OnboardingScreen()
                        } else {
                            HomeScreen()
                        }
                    navigator.replaceAll(screen)
                    return
                }

                error is ClientError.Http && error.status == HttpStatusCode.Unauthorized -> {
                    log.i { "Session invalid, logging out" }
                    showSnackbar("You were logged out", isError = true)
                    logOut()
                    navigator.replaceAll(LoginScreen())
                    return
                }

                attempts >= MAX_ATTEMPTS -> {
                    log.w { "Gave up loading facts after $attempts attempts: $error" }
                    innerState.update { it.copy(failed = true) }
                    return
                }

                else -> {
                    log.w { "Failed to load facts (attempt $attempts): $error" }
                    delay(RETRY_DELAY)
                }
            }
        }
    }

    private companion object {
        val RETRY_DELAY = 5.seconds
        const val MAX_ATTEMPTS = 2
    }
}

data class State(
    val failed: Boolean,
)

sealed interface UiState {
    data object Loading : UiState

    data class Failed(
        val onClickRetry: () -> Unit,
        val onClickLogout: () -> Unit,
    ) : UiState
}

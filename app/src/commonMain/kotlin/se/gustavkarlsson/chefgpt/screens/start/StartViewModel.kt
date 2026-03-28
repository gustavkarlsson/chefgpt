package se.gustavkarlsson.chefgpt.screens.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StartViewModel : ViewModel() {
    private data class State(
        val dummy: Unit = Unit,
    )

    data class ViewState(
        val dummy: Unit,
    )

    private val innerState = MutableStateFlow(State())

    val viewState: StateFlow<ViewState> =
        innerState
            .map { it.toViewState() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, innerState.value.toViewState())

    init {
    }

    private fun State.toViewState(): ViewState = ViewState(Unit)
}

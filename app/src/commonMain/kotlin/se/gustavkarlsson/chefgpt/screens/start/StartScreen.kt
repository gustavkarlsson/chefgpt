package se.gustavkarlsson.chefgpt.screens.start

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel.ViewState

@Composable
fun StartScreen() {
    val viewModel = viewModel { StartViewModel() }
    val viewState by viewModel.viewState.collectAsState()
    Content(viewState)
}

@Composable
private fun Content(viewState: ViewState) {
}

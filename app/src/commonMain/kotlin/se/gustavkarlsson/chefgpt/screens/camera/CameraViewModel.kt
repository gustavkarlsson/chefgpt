package se.gustavkarlsson.chefgpt.screens.camera

import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.completeResult
import se.gustavkarlsson.chefgpt.screens.StateViewModel

class CameraViewModel(
    private val navigator: Navigator,
    @InjectedParam private val screen: CameraScreen,
) : StateViewModel<State, UiState>() {
    override fun createInitialState() = State(permissionDenied = false)

    override fun State.toUiState(): UiState =
        UiState(
            permissionDenied = permissionDenied,
            onPhotoCaptured = ::publish,
            onPermissionDenied = ::denyPermission,
            onCancelled = navigator::pop,
        )

    private fun publish(path: Path) {
        navigator.completeResult(screen, CapturedPhoto(path))
        navigator.pop()
    }

    private fun denyPermission() {
        innerState.update { it.copy(permissionDenied = true) }
    }
}

data class State(
    val permissionDenied: Boolean,
)

data class UiState(
    val permissionDenied: Boolean,
    val onPhotoCaptured: (Path) -> Unit,
    val onPermissionDenied: () -> Unit,
    val onCancelled: () -> Unit,
)

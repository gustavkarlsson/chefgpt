package se.gustavkarlsson.chefgpt.screens.camera

import kotlinx.coroutines.flow.update
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel

class CameraViewModel(
    private val navigator: Navigator,
    private val photoResults: PhotoCaptureCoordinator,
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
        photoResults.publish(path)
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

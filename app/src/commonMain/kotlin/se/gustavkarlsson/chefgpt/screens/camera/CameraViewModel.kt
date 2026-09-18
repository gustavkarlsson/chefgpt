package se.gustavkarlsson.chefgpt.screens.camera

import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.navigation.completeResult
import se.gustavkarlsson.chefgpt.screens.StateViewModel

class CameraViewModel(
    private val navigator: Navigator,
    @InjectedParam private val screen: CameraScreen,
    private val fileSystem: FileSystem,
) : StateViewModel<State, UiState>() {
    override fun createInitialState() = State.UsingCamera

    override fun State.toUiState(): UiState =
        when (this) {
            State.UsingCamera -> {
                UiState.UsingCamera(
                    onClickClose = navigator::pop,
                    onPhotoTaken = ::displayPhoto,
                    onPermissionDenied = ::setPermissionDenied,
                    onError = ::handleError,
                )
            }

            is State.DisplayingPhoto -> {
                UiState.DisplayingPhoto(
                    onClickClose = navigator::pop,
                    photoPath = fileSystem.resolve(photo).toString(),
                    onClickAccept = ::returnImage,
                    onClickRetry = ::reset,
                )
            }

            is State.PermissionDenied -> {
                UiState.PermissionDenied(
                    onClickOpenPermissions =
                        if (fixable) {
                            {} // FIXME open permission dialog
                        } else {
                            null
                        },
                    onClickClose = navigator::pop,
                )
            }
        }

    private fun displayPhoto(photo: Path) {
        innerState.value = State.DisplayingPhoto(photo)
    }

    private fun setPermissionDenied(fixable: Boolean) {
        innerState.value = State.PermissionDenied(fixable)
    }

    private fun handleError() {
        // TODO Show snackbar
        navigator.pop()
    }

    private fun returnImage(photo: Path) {
        navigator.completeResult(screen, CapturedPhoto(photo))
        navigator.pop()
    }

    private fun reset(photo: Path) {
        fileSystem.delete(photo)
        innerState.value = State.UsingCamera
    }
}

sealed interface State {
    data object UsingCamera : State

    data class DisplayingPhoto(
        val photo: Path,
    ) : State

    data class PermissionDenied(
        val fixable: Boolean,
    ) : State
}

sealed interface UiState {
    data class UsingCamera(
        val onClickClose: () -> Unit,
        val onPhotoTaken: (Path) -> Unit,
        val onPermissionDenied: (fixable: Boolean) -> Unit,
        val onError: () -> Unit,
    ) : UiState

    data class DisplayingPhoto(
        val onClickClose: () -> Unit,
        val photoPath: String,
        val onClickAccept: (Path) -> Unit,
        val onClickRetry: (Path) -> Unit,
    ) : UiState

    data class PermissionDenied(
        val onClickOpenPermissions: (() -> Unit)?,
        val onClickClose: () -> Unit,
    ) : UiState
}

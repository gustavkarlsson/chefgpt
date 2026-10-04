package se.gustavkarlsson.chefgpt.screens.recipescan

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.IoOrDefault
import se.gustavkarlsson.chefgpt.files.usecases.DeleteFile
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.screens.photos.CameraAction
import se.gustavkarlsson.chefgpt.screens.photos.PickerAction
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

class RecipeScanSheetViewModel(
    private val navigator: Navigator,
    private val scanRecipes: ScanRecipes,
    private val showSnackbar: ShowSnackbar,
    private val deleteFile: DeleteFile,
    private val deviceConfig: DeviceConfig,
    @InjectedParam screen: RecipeScanSheet,
) : StateViewModel<RecipeScanSheetState, RecipeScanSheetUiState>() {
    private val sessionId: SessionId = screen.sessionId

    // Whether a scan has been handed to [scanRecipes]; if so, the files are the job's
    // to read, so onCleared must not delete them.
    private var scanStarted = false

    override fun createInitialState() =
        RecipeScanSheetState(
            photos = emptyList(),
            discardTarget = null,
        )

    override fun RecipeScanSheetState.toUiState(): RecipeScanSheetUiState =
        RecipeScanSheetUiState(
            photos = photos,
            discardTarget = discardTarget,
            camera =
                if (deviceConfig.supportsCamera) {
                    CameraAction(
                        onPhotoTaken = ::onPhotoCaptured,
                        onError = ::onCaptureError,
                    )
                } else {
                    null
                },
            picker =
                if (deviceConfig.supportsFilePicker) {
                    PickerAction(
                        onPhotosPicked = ::onPhotosPicked,
                        onError = ::onPickerError,
                    )
                } else {
                    null
                },
            onClickPhoto = ::requestDiscard,
            onClickConfirm = if (photos.isEmpty()) null else ::confirm,
            onDiscardConfirmed = ::discard,
            onDiscardDismissed = ::dismissDiscard,
        )

    override fun onCleared() {
        if (scanStarted) return
        for (photo in innerState.value.photos) {
            deleteFile(photo)
        }
    }

    private fun onPhotoCaptured(photo: Path) {
        innerState.update { it.copy(photos = it.photos + photo) }
    }

    private fun onPhotosPicked(photos: List<Path>) {
        innerState.update { it.copy(photos = (it.photos + photos).distinct()) }
    }

    private fun onCaptureError() {
        showSnackbar("Could not take a photo", isError = true)
    }

    private fun onPickerError() {
        showSnackbar("Could not pick photos", isError = true)
    }

    private fun requestDiscard(photo: Path) {
        innerState.update { it.copy(discardTarget = photo) }
    }

    private fun dismissDiscard() {
        innerState.update { it.copy(discardTarget = null) }
    }

    private fun discard() {
        val photo = innerState.value.discardTarget ?: return
        innerState.update { it.copy(discardTarget = null, photos = it.photos - photo) }
        viewModelScope.launch {
            withContext(Dispatchers.IoOrDefault) { deleteFile(photo) }
        }
    }

    private fun confirm() {
        scanStarted = true
        scanRecipes(sessionId, innerState.value.photos)
        navigator.pop()
    }
}

data class RecipeScanSheetState(
    val photos: List<Path>,
    val discardTarget: Path?,
)

data class RecipeScanSheetUiState(
    val photos: List<Path>,
    val discardTarget: Path?,
    val camera: CameraAction?,
    val picker: PickerAction?,
    val onClickPhoto: (Path) -> Unit,
    val onClickConfirm: (() -> Unit)?,
    val onDiscardConfirmed: () -> Unit,
    val onDiscardDismissed: () -> Unit,
)

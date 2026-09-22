package se.gustavkarlsson.chefgpt.screens.recipescan

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import org.koin.core.annotation.InjectedParam
import se.gustavkarlsson.chefgpt.IoOrDefault
import se.gustavkarlsson.chefgpt.jobs.ScanRecipes
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.StateViewModel
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager

class RecipeScanSheetViewModel(
    private val navigator: Navigator,
    private val scanRecipes: ScanRecipes,
    private val snackbarManager: SnackbarManager,
    private val fileSystem: FileSystem,
    @InjectedParam screen: RecipeScanSheet,
) : StateViewModel<RecipeScanSheetState, RecipeScanSheetUiState>() {
    private val sessionId: SessionId = screen.sessionId

    // Whether the current capture is the auto-opened first one. A cancelled first
    // capture dismisses the sheet; later cancels just stay.
    private var initialCapture = true

    // Whether a scan has been handed to [scanRecipes]; if so, the files are the job's
    // to read, so onCleared must not delete them.
    private var scanStarted = false

    override fun createInitialState() =
        RecipeScanSheetState(
            photos = emptyList(),
            capturing = true,
            discardTarget = null,
        )

    override fun RecipeScanSheetState.toUiState(): RecipeScanSheetUiState =
        RecipeScanSheetUiState(
            photos = photos,
            capturing = capturing,
            discardTarget = discardTarget,
            onClickAddPhoto = ::addPhoto,
            onClickPhoto = ::requestDiscard,
            onClickConfirm = if (photos.isEmpty()) null else ::confirm,
            onPhotoCaptured = ::onPhotoCaptured,
            onCaptureCancelled = ::onCaptureCancelled,
            onCaptureError = ::onCaptureError,
            onDiscardConfirmed = ::discard,
            onDiscardDismissed = ::dismissDiscard,
        )

    override fun onCleared() {
        if (scanStarted) return
        for (photo in innerState.value.photos) {
            try {
                fileSystem.delete(photo, mustExist = false)
            } catch (_: Exception) {
                // Best-effort cleanup; the cache dir is reclaimed by the OS anyway.
            }
        }
    }

    private fun addPhoto() {
        innerState.update { it.copy(capturing = true) }
    }

    private fun onPhotoCaptured(photo: Path) {
        initialCapture = false
        innerState.update { it.copy(capturing = false, photos = it.photos + photo) }
    }

    private fun onCaptureCancelled() {
        val wasInitial = initialCapture
        initialCapture = false
        innerState.update { it.copy(capturing = false) }
        if (wasInitial) navigator.pop()
    }

    private fun onCaptureError() {
        initialCapture = false
        innerState.update { it.copy(capturing = false) }
        snackbarManager.show("Could not take a photo", isError = true)
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
        deleteFile(photo)
    }

    private fun confirm() {
        scanStarted = true
        scanRecipes.scan(sessionId, innerState.value.photos)
        navigator.pop()
    }

    private fun deleteFile(photo: Path) {
        viewModelScope.launch {
            withContext(Dispatchers.IoOrDefault) {
                try {
                    fileSystem.delete(photo, mustExist = false)
                } catch (_: Exception) {
                    // Best-effort cleanup
                }
            }
        }
    }
}

data class RecipeScanSheetState(
    val photos: List<Path>,
    val capturing: Boolean,
    val discardTarget: Path?,
)

data class RecipeScanSheetUiState(
    val photos: List<Path>,
    val capturing: Boolean,
    val discardTarget: Path?,
    val onClickAddPhoto: () -> Unit,
    val onClickPhoto: (Path) -> Unit,
    val onClickConfirm: (() -> Unit)?,
    val onPhotoCaptured: (Path) -> Unit,
    val onCaptureCancelled: () -> Unit,
    val onCaptureError: () -> Unit,
    val onDiscardConfirmed: () -> Unit,
    val onDiscardDismissed: () -> Unit,
)

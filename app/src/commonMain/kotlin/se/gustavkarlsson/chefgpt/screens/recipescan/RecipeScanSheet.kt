package se.gustavkarlsson.chefgpt.screens.recipescan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.navigation.Screen.Id
import se.gustavkarlsson.chefgpt.screens.photos.PhotoCollectionSheetContent
import se.gustavkarlsson.chefgpt.sessions.SessionId

@Serializable
@SerialName("recipe-scan-sheet")
data class RecipeScanSheet(
    val sessionId: SessionId,
    override val id: Id = Id.new(),
) : Screen.BottomSheet {
    @Composable
    override fun Content() {
        val viewModel = koinViewModel<RecipeScanSheetViewModel> { parametersOf(this) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Content(uiState)
    }
}

@Composable
private fun Content(
    uiState: RecipeScanSheetUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = "Scan recipes", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Take photos of one or more recipes. I'll save each one I find.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        PhotoCollectionSheetContent(
            photos = uiState.photos,
            camera = uiState.camera,
            picker = uiState.picker,
            onClickPhoto = uiState.onClickPhoto,
            onClickConfirm = uiState.onClickConfirm,
            confirmLabel = "Confirm",
            autoOpenCamera = uiState.autoOpenCamera,
            autoOpenPicker = uiState.autoOpenPicker,
            onAutoOpenCancelled = uiState.onAutoOpenCancelled,
        )
    }

    if (uiState.discardTarget != null) {
        AlertDialog(
            onDismissRequest = uiState.onDiscardDismissed,
            title = { Text("Discard photo?") },
            text = { Text("This photo won't be added to the scan.") },
            confirmButton = {
                TextButton(onClick = uiState.onDiscardConfirmed) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = uiState.onDiscardDismissed) { Text("Keep") }
            },
        )
    }
}

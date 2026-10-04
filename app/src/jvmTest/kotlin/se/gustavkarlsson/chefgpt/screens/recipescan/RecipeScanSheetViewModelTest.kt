package se.gustavkarlsson.chefgpt.screens.recipescan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.io.files.Path
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.Platform
import se.gustavkarlsson.chefgpt.files.usecases.DeleteFile
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeScanSheetViewModelTest {
    @Test
    fun `offers only the camera tile when only the camera is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = false)

                val uiState = currentUiState(setup)

                assertNotNull(uiState.camera)
                assertNull(uiState.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `offers only the picker tile when only the picker is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)

                val uiState = currentUiState(setup)

                assertNull(uiState.camera)
                assertNotNull(uiState.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `offers both tiles when both sources are supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = true)

                val uiState = currentUiState(setup)

                assertNotNull(uiState.camera)
                assertNotNull(uiState.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `adds picked photos to the collection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)
                val picker = assertNotNull(currentUiState(setup).picker)

                picker.onPhotosPicked(listOf(Path("/photos/a.jpg"), Path("/photos/b.jpg")))

                assertEquals(
                    listOf(Path("/photos/a.jpg"), Path("/photos/b.jpg")),
                    currentUiState(setup).photos,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `adds a captured photo to the collection`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = false)
                val camera = assertNotNull(currentUiState(setup).camera)

                camera.onPhotoTaken(Path("/photos/a.jpg"))

                assertEquals(listOf(Path("/photos/a.jpg")), currentUiState(setup).photos)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `disables confirm when no photos are collected`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)

                val uiState = currentUiState(setup)

                assertNull(uiState.onClickConfirm)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `starts the scan with the collected photos and closes the sheet`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)
                val photo = Path("/photos/a.jpg")
                assertNotNull(currentUiState(setup).picker).onPhotosPicked(listOf(photo))
                val confirm = assertNotNull(currentUiState(setup).onClickConfirm)

                confirm()

                val scannedPhotos = setup.scanned.single()
                assertEquals(listOf(photo), scannedPhotos)
                assertEquals(emptyList(), setup.navigator.backStack.value)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `removes the photo from the collection when discarding`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)
                val photo = Path("/photos/a.jpg")
                assertNotNull(currentUiState(setup).picker).onPhotosPicked(listOf(photo))
                currentUiState(setup).onClickPhoto(photo)
                val discard = currentUiState(setup).onDiscardConfirmed

                discard()

                val uiState = currentUiState(setup)
                assertEquals(emptyList(), uiState.photos)
                assertNull(uiState.discardTarget)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `auto-opens the camera when only the camera is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = false)

                val uiState = currentUiState(setup)

                assertTrue(uiState.autoOpenCamera)
                assertFalse(uiState.autoOpenPicker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `auto-opens the picker when only the picker is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = false, supportsFilePicker = true)

                val uiState = currentUiState(setup)

                assertTrue(uiState.autoOpenPicker)
                assertFalse(uiState.autoOpenCamera)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `auto-opens nothing when both sources are supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = true)

                val uiState = currentUiState(setup)

                assertFalse(uiState.autoOpenCamera)
                assertFalse(uiState.autoOpenPicker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `closes the sheet when the auto-opened source is cancelled`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startSheet(supportsCamera = true, supportsFilePicker = false)

                currentUiState(setup).onAutoOpenCancelled()

                assertEquals(emptyList(), setup.navigator.backStack.value)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun TestScope.startSheet(
        supportsCamera: Boolean,
        supportsFilePicker: Boolean,
    ): Setup {
        val scanned = mutableListOf<List<Path>>()
        val navigator = Navigator()
        val states = mutableListOf<RecipeScanSheetUiState>()
        val viewModel =
            RecipeScanSheetViewModel(
                navigator = navigator,
                scanRecipes = ScanRecipes { _, photos -> scanned += photos },
                showSnackbar = ShowSnackbar { _, _ -> },
                deleteFile = DeleteFile { _ -> },
                deviceConfig =
                    DeviceConfig(
                        Platform.Desktop,
                        supportsCamera = supportsCamera,
                        supportsFilePicker = supportsFilePicker,
                    ),
                screen = RecipeScanSheet(SessionId("session")),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect(states::add)
        }
        return Setup(states, scanned, navigator)
    }

    private suspend fun TestScope.currentUiState(setup: Setup): RecipeScanSheetUiState {
        runCurrent()
        return setup.states.last()
    }

    private class Setup(
        val states: MutableList<RecipeScanSheetUiState>,
        val scanned: MutableList<List<Path>>,
        val navigator: Navigator,
    )
}

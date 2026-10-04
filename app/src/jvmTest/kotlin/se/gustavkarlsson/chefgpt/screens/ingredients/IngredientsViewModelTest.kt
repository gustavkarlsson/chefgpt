package se.gustavkarlsson.chefgpt.screens.ingredients

import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.KSerializer
import se.gustavkarlsson.chefgpt.ClientError
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.Platform
import se.gustavkarlsson.chefgpt.api.jobs.v1.ApiJob
import se.gustavkarlsson.chefgpt.ingredients.usecases.CreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.DestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.usecases.ScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.SetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.usecases.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.AwaitJobError
import se.gustavkarlsson.chefgpt.jobs.usecases.AwaitJob
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class IngredientsViewModelTest {
    @Test
    fun `hides the photo button when neither source is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startIngredients(supportsCamera = false, supportsFilePicker = false)

                val uiState = currentUiState(setup)

                assertNull(uiState.input.photoButton)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `shows a camera-only photo button when only the camera is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startIngredients(supportsCamera = true, supportsFilePicker = false)

                val photoButton = currentUiState(setup).input.photoButton

                assertIs<UiPhotoButton.Camera>(photoButton)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `shows a picker-only photo button when only the picker is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startIngredients(supportsCamera = false, supportsFilePicker = true)

                val photoButton = currentUiState(setup).input.photoButton

                assertIs<UiPhotoButton.Picker>(photoButton)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `shows a chooser photo button when both sources are supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startIngredients(supportsCamera = true, supportsFilePicker = true)

                val photoButton = currentUiState(setup).input.photoButton

                assertIs<UiPhotoButton.Chooser>(photoButton)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun TestScope.startIngredients(
        supportsCamera: Boolean,
        supportsFilePicker: Boolean,
    ): Setup {
        val states = mutableListOf<UiState>()
        val viewModel =
            IngredientsViewModel(
                streamIngredients = StreamIngredients { emptyFlow() },
                createIngredient = CreateIngredient { _, _ -> error("Not used") },
                destroyIngredient = DestroyIngredient { _, _ -> error("Not used") },
                setIngredientInventory = SetIngredientInventory { _, _, _ -> error("Not used") },
                scanIngredients = ScanIngredients { _, _, _ -> error("Not used") },
                awaitJob = FakeAwaitJob(),
                resolveEmoji = ResolveEmoji { null },
                resolveEmojiAlias = ResolveEmojiAlias { null },
                showSnackbar = ShowSnackbar { _, _ -> },
                navigator = Navigator(),
                deviceConfig =
                    DeviceConfig(
                        Platform.Android,
                        supportsCamera = supportsCamera,
                        supportsFilePicker = supportsFilePicker,
                    ),
                screen = IngredientsScreen(SessionId("session")),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect(states::add)
        }
        return Setup(states)
    }

    private suspend fun TestScope.currentUiState(setup: Setup): UiState {
        runCurrent()
        return setup.states.last()
    }

    private class Setup(
        val states: MutableList<UiState>,
    )
}

private class FakeAwaitJob : AwaitJob {
    override suspend fun <T> invoke(
        sessionId: SessionId,
        deserializer: KSerializer<T>,
        createJob: suspend () -> Result<ApiJob<T>, ClientError>,
    ): Result<ApiJob<T>, AwaitJobError> = error("Not used")
}

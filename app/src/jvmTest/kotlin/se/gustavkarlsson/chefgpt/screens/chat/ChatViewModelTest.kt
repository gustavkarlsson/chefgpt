package se.gustavkarlsson.chefgpt.screens.chat

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiAction
import se.gustavkarlsson.chefgpt.api.chats.v1.ApiEvent
import se.gustavkarlsson.chefgpt.api.common.ChatId
import se.gustavkarlsson.chefgpt.chats.Conversation
import se.gustavkarlsson.chefgpt.chats.EventStreamError
import se.gustavkarlsson.chefgpt.chats.usecases.CreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.StreamChats
import se.gustavkarlsson.chefgpt.files.usecases.UploadFile
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.AwaitJobError
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @Test
    fun `hides the add-photos button when neither source is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = false)

                val uiState = currentUiState(setup)

                assertNull(uiState.input.addPhotosButton)
                assertNull(uiState.input.addPhotosSheet)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `opens a sheet with only the picker tile when only the picker is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()

                val sheet = assertNotNull(currentUiState(setup).input.addPhotosSheet)

                assertNull(sheet.camera)
                assertNotNull(sheet.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `opens a sheet with only the camera tile when only the camera is supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = true, supportsFilePicker = false)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()

                val sheet = assertNotNull(currentUiState(setup).input.addPhotosSheet)

                assertNotNull(sheet.camera)
                assertNull(sheet.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `opens a sheet with both tiles when both sources are supported`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = true, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()

                val sheet = assertNotNull(currentUiState(setup).input.addPhotosSheet)

                assertNotNull(sheet.camera)
                assertNotNull(sheet.picker)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `adds picked photos to the attachments`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()
                val picker = assertNotNull(currentUiState(setup).input.addPhotosSheet?.picker)

                picker.onPhotosPicked(listOf(Path("/photos/a.jpg"), Path("/photos/b.jpg")))

                assertEquals(
                    listOf(Path("/photos/a.jpg"), Path("/photos/b.jpg")),
                    currentUiState(setup).input.attachments,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `does not duplicate attachments picked twice`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()
                val picker = assertNotNull(currentUiState(setup).input.addPhotosSheet?.picker)

                picker.onPhotosPicked(listOf(Path("/photos/a.jpg")))
                picker.onPhotosPicked(listOf(Path("/photos/a.jpg")))

                assertEquals(listOf(Path("/photos/a.jpg")), currentUiState(setup).input.attachments)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `closes the sheet and keeps the photos when confirming`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()
                val picker = assertNotNull(currentUiState(setup).input.addPhotosSheet?.picker)
                picker.onPhotosPicked(listOf(Path("/photos/a.jpg")))
                val confirm = assertNotNull(currentUiState(setup).input.addPhotosSheet?.onClickConfirm)

                confirm()

                val uiState = currentUiState(setup)
                assertNull(uiState.input.addPhotosSheet)
                assertEquals(listOf(Path("/photos/a.jpg")), uiState.input.attachments)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `closes the sheet and keeps the photos when dismissed`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val setup = startChat(supportsCamera = false, supportsFilePicker = true)
                assertNotNull(currentUiState(setup).input.addPhotosButton).onClick()
                val picker = assertNotNull(currentUiState(setup).input.addPhotosSheet?.picker)
                picker.onPhotosPicked(listOf(Path("/photos/a.jpg")))
                val dismiss = currentUiState(setup).input.addPhotosSheet?.onDismiss ?: error("No sheet")

                dismiss()

                val uiState = currentUiState(setup)
                assertNull(uiState.input.addPhotosSheet)
                assertEquals(listOf(Path("/photos/a.jpg")), uiState.input.attachments)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun TestScope.startChat(
        supportsCamera: Boolean,
        supportsFilePicker: Boolean,
    ): Setup {
        val states = mutableListOf<UiState>()
        val chatId = ChatId.parse("00000000-0000-0000-0000-000000000000")
        val viewModel =
            ChatViewModel(
                createConversation = CreateConversation { _, _ -> FakeConversation(chatId) },
                streamChats = StreamChats { emptyFlow() },
                streamIngredients = StreamIngredients { emptyFlow() },
                resolveEmoji = ResolveEmoji { null },
                uploadFile = UploadFile { _, _, _ -> error("Not used") },
                showSnackbar = ShowSnackbar { _, _ -> },
                navigator = Navigator(),
                deviceConfig =
                    DeviceConfig(
                        Platform.Android,
                        supportsCamera = supportsCamera,
                        supportsFilePicker = supportsFilePicker,
                    ),
                screen = ChatScreen(SessionId("session"), chatId),
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

private class FakeConversation(
    override val chatId: ChatId,
) : Conversation {
    override val sessionId: SessionId = SessionId("session")

    override suspend fun sendAction(action: ApiAction): Result<Unit, AwaitJobError> = Ok(Unit)

    override fun events(): Flow<Result<ApiEvent, EventStreamError>> = emptyFlow()
}

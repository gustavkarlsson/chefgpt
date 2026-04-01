package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.core.annotation.Single
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.ksp.generated.module
import se.gustavkarlsson.chefgpt.ChatRepository
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.Navigator
import se.gustavkarlsson.chefgpt.Route
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel
import org.koin.core.annotation.Module as KoinModule

@KoinModule
class SingletonsModule {
    // TODO Should be activity retained scoped for Android.
    @Single
    fun navigator() = Navigator(Route.Start)

    @Single
    fun sessionRepository() =
        // TODO Get path depending on platform
        SessionRepository(Path("sessions.txt"))

    @Single
    fun chatRepository() =
        // TODO Get path depending on platform
        ChatRepository(Path("chats.txt"))

    @Single
    fun chefGptClient() =
        // TODO Set base url and dev mode based on config
        ChefGptClient()
}

// TODO Use @ActivityRetainedScope for Android
private val viewModelsModule =
    module {
        viewModel { params ->
            ChatViewModel(
                client = get(),
                chat = params.get(),
                navigator = get(),
            )
        }
        viewModelOf(::StartViewModel)
    }

val AppModule = SingletonsModule().module + viewModelsModule

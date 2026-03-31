package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.ChatRepository
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.Navigator
import se.gustavkarlsson.chefgpt.Route
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel

val AppModule =
    module {
        single {
            Navigator(Route.Start)
        }
        single {
            // TODO Get path depending on platform
            SessionRepository(Path("sessions.txt"))
        }
        single {
            // TODO Get path depending on platform
            ChatRepository(Path("chats.txt"))
        }
        single {
            // TODO Set base url and dev mode based on config
            ChefGptClient()
        }
        viewModel { params ->
            ChatViewModel(
                client = get(),
                chat = params[0] as Route.Chat,
                navigator = get(),
            )
        }
        viewModelOf(::StartViewModel)
    }

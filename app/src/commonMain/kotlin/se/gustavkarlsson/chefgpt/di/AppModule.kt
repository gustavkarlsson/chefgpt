package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.api.SessionId
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel

val AppModule =
    module {
        single {
            // TODO Get path depending on platform
            SessionRepository(Path("sessions.txt"))
        }
        single {
            // TODO Set base url and dev mode based on config
            ChefGptClient()
        }
        viewModel { params -> ChatViewModel(get(), params.get<SessionId>()) }
        viewModelOf(::StartViewModel)
    }

package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.SessionRepository
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel

val AppModule =
    module {
        single {
            // TODO Get path depending on platform
            SessionRepository(Path("sessions.txt"))
        }
        viewModelOf(::ChatViewModel)
        viewModelOf(::StartViewModel)
    }

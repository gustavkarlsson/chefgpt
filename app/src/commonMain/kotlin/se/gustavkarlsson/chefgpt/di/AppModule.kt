package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.LoginRepository
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel

val AppModule =
    module {
        single {
            // TODO Get path depending on platform
            LoginRepository(Path("login-credentials.txt"))
        }
        viewModelOf(::ChatViewModel)
    }

package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.Path
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.LoginRepository

val AppModule =
    module {
        single {
            // TODO Get path depending on platform
            LoginRepository(Path("login-credentials.txt"))
        }
    }

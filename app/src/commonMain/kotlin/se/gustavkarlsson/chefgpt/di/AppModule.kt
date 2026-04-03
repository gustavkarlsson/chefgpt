package se.gustavkarlsson.chefgpt.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.SessionRepositoryImpl

val singletonModule =
    module {
        single<ChefGptClient>()
        // TODO Should be activity retained scoped for Android.
        single<Navigator>()
        // TODO Get path depending on platform
        single<LastSessionFileStore>()
        // TODO Get path depending on platform
        single<ChatRepository>()
        single<EventHistoryStore>()
        single<SessionRepositoryImpl>() bind SessionRepository::class
    }

val viewModelModule =
    module {
        viewModel<StartViewModel>()
        viewModel<ChatViewModel>()
    }

val nativeModule =
    module {
        single<NativeComponent>()
    }

val appModule =
    module {
        includes(singletonModule, viewModelModule, nativeModule)
    }

fun initKoin(configuration: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        includes(configuration)
        modules(appModule)
    }.also {
        val platformInfo = it.koin.get<NativeComponent>().getInfo()
        println("Started Koin on: $platformInfo")
    }

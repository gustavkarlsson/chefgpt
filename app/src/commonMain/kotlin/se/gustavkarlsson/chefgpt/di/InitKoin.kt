package se.gustavkarlsson.chefgpt.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.DeviceConfig

val appModule =
    module {
        includes(singletonModule, activityRetainedScopeModule, viewModelModule)
    }

fun initKoin(configuration: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        includes(configuration)
        modules(appModule)
    }.also {
        val platformName =
            it.koin
                .get<DeviceConfig>()
                .platform.name
        println("Started Koin on: $platformName")
    }

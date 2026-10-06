package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.readDeviceConfig
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.debug.DebugViewModel
import se.gustavkarlsson.chefgpt.screens.home.HomeViewModel
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel
import se.gustavkarlsson.chefgpt.screens.loading.LoadingViewModel
import se.gustavkarlsson.chefgpt.screens.login.LoginViewModel
import se.gustavkarlsson.chefgpt.screens.onboarding.OnboardingViewModel
import se.gustavkarlsson.chefgpt.screens.recipe.RecipeDetailViewModel
import se.gustavkarlsson.chefgpt.screens.recipescan.RecipeScanSheetViewModel
import se.gustavkarlsson.chefgpt.screens.recipescrape.RecipeScrapeSheetViewModel
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore

// Process-lifetime infrastructure and stateless singletons. Everything that talks to the
// network or holds session state lives in activityRetainedScopeModule instead.
val singletonModule =
    module {
        single<Settings>()
        single<Json> { chefGptJson(strict = false) }
        single<LastSessionFileStore>()
        single<EventHistoryStore>()
        single<IngredientEmojiResolver.Factory>()
        single<FileSystem> { SystemFileSystem }
        single<DeviceConfig> { readDeviceConfig() }
    }

// TODO Consider adding a viewModelScope and providing more VM-scoped dependencies
val viewModelModule =
    module {
        viewModel<LoadingViewModel>()
        viewModel<OnboardingViewModel>()
        viewModel<LoginViewModel>()
        viewModel<HomeViewModel>()
        viewModel<ChatViewModel>()
        viewModel<IngredientsViewModel>()
        viewModel<DebugViewModel>()
        viewModel<RecipeDetailViewModel>()
        viewModel<RecipeScanSheetViewModel>()
        viewModel<RecipeScrapeSheetViewModel>()
    }

val nativeModule =
    module {
        single<NativeComponent>()
    }

val appModule =
    module {
        includes(singletonModule, activityRetainedScopeModule, viewModelModule, nativeModule)
    }

fun initKoin(configuration: KoinAppDeclaration? = null): KoinApplication =
    startKoin {
        includes(configuration)
        modules(appModule)
    }.also {
        val platformInfo = it.koin.get<NativeComponent>().getInfo()
        println("Started Koin on: $platformInfo")
    }

package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
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

// Process-lifetime bindings. Only stateful or expensive-to-produce things belong here;
// everything stateless is a factory in activityRetainedScopeModule, and everything that
// talks to the network or holds session state is scoped there.
val singletonModule =
    module {
        single<Settings>()
        single<IngredientEmojiResolver.Factory>()
        single<FileSystem> { SystemFileSystem }
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
        factory<NativeComponent>()
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

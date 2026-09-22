package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.includes
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.chats.HttpChatRepository
import se.gustavkarlsson.chefgpt.chats.usecases.CreateChat
import se.gustavkarlsson.chefgpt.chats.usecases.CreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.DeleteChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpCreateChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpCreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.HttpDeleteChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpStreamChats
import se.gustavkarlsson.chefgpt.chats.usecases.StreamChats
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.files.usecases.DeleteFile
import se.gustavkarlsson.chefgpt.files.usecases.HttpUploadFile
import se.gustavkarlsson.chefgpt.files.usecases.RealDeleteFile
import se.gustavkarlsson.chefgpt.files.usecases.UploadFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.ingredients.usecases.CreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.DestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.HttpCreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.HttpDestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.HttpScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.HttpSetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.usecases.HttpStreamIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.RealResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.RealResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.usecases.ScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.SetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.usecases.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.usecases.AwaitJob
import se.gustavkarlsson.chefgpt.jobs.usecases.HttpAwaitJob
import se.gustavkarlsson.chefgpt.jobs.usecases.HttpScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.RealStreamScanState
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.StreamScanState
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.readDeviceConfig
import se.gustavkarlsson.chefgpt.recipes.HttpRecipeRepository
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.usecases.DeleteRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.GetRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpDeleteRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpGetRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpOverwriteOriginalRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpSaveRecipeAsCopy
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpSetRecipeFavorite
import se.gustavkarlsson.chefgpt.recipes.usecases.HttpStreamRecipeSummaries
import se.gustavkarlsson.chefgpt.recipes.usecases.OverwriteOriginalRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.SaveRecipeAsCopy
import se.gustavkarlsson.chefgpt.recipes.usecases.SetRecipeFavorite
import se.gustavkarlsson.chefgpt.recipes.usecases.StreamRecipeSummaries
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.debug.DebugViewModel
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel
import se.gustavkarlsson.chefgpt.screens.recipe.RecipeDetailViewModel
import se.gustavkarlsson.chefgpt.screens.recipescan.RecipeScanSheetViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel
import se.gustavkarlsson.chefgpt.sessions.HttpSessionRepository
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.usecases.GetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.usecases.HttpGetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.usecases.HttpLogIn
import se.gustavkarlsson.chefgpt.sessions.usecases.HttpLogOut
import se.gustavkarlsson.chefgpt.sessions.usecases.HttpRegister
import se.gustavkarlsson.chefgpt.sessions.usecases.LogIn
import se.gustavkarlsson.chefgpt.sessions.usecases.LogOut
import se.gustavkarlsson.chefgpt.sessions.usecases.Register
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager
import se.gustavkarlsson.chefgpt.snackbar.usecases.RealShowSnackbar
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

val singletonModule =
    module {
        // Infrastructure
        single<Settings>()
        single<ChefGptClient>()
        single<Json> { chefGptJson(strict = false) }
        // TODO Should be activity retained scoped for Android.
        single<Navigator>()
        single<LastSessionFileStore>()
        single<EventHistoryStore>()
        single<JobManager>()
        single<SnackbarManager>()
        single<IngredientEmojiResolver.Factory>()
        single<FileSystem> { SystemFileSystem }
        single<DeviceConfig> { readDeviceConfig() }

        // Repositories
        single<HttpSessionRepository>() bind SessionRepository::class
        single<HttpChatRepository>() bind ChatRepository::class
        single<HttpRecipeRepository>() bind RecipeRepository::class

        // Use cases — sessions
        single<HttpGetCurrentSession>() bind GetCurrentSession::class
        single<HttpRegister>() bind Register::class
        single<HttpLogIn>() bind LogIn::class
        single<HttpLogOut>() bind LogOut::class

        // Use cases — chats
        single<HttpCreateChat>() bind CreateChat::class
        single<HttpDeleteChat>() bind DeleteChat::class
        single<HttpStreamChats>() bind StreamChats::class
        single<HttpCreateConversation>() bind CreateConversation::class

        // Use cases — recipes
        single<HttpStreamRecipeSummaries>() bind StreamRecipeSummaries::class
        single<HttpGetRecipe>() bind GetRecipe::class
        single<HttpSetRecipeFavorite>() bind SetRecipeFavorite::class
        single<HttpDeleteRecipe>() bind DeleteRecipe::class
        single<HttpOverwriteOriginalRecipe>() bind OverwriteOriginalRecipe::class
        single<HttpSaveRecipeAsCopy>() bind SaveRecipeAsCopy::class

        // Use cases — ingredients
        single<HttpStreamIngredients>() bind StreamIngredients::class
        single<HttpCreateIngredient>() bind CreateIngredient::class
        single<HttpDestroyIngredient>() bind DestroyIngredient::class
        single<HttpSetIngredientInventory>() bind SetIngredientInventory::class
        single<HttpScanIngredients>() bind ScanIngredients::class
        single<RealResolveEmoji>() bind ResolveEmoji::class
        single<RealResolveEmojiAlias>() bind ResolveEmojiAlias::class

        // Use cases — jobs
        single<HttpAwaitJob>() bind AwaitJob::class
        single<HttpScanRecipes>() bind ScanRecipes::class
        single<RealStreamScanState>() bind StreamScanState::class

        // Use cases — files
        single<HttpUploadFile>() bind UploadFile::class
        single<RealDeleteFile>() bind DeleteFile::class

        // Use cases — snackbar
        single<RealShowSnackbar>() bind ShowSnackbar::class
    }

// TODO Consider adding a viewModelScope and providing more VM-scoped dependencies
val viewModelModule =
    module {
        viewModel<StartViewModel>()
        viewModel<ChatViewModel>()
        viewModel<IngredientsViewModel>()
        viewModel<DebugViewModel>()
        viewModel<RecipeDetailViewModel>()
        viewModel<RecipeScanSheetViewModel>()
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

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
import se.gustavkarlsson.chefgpt.chats.CreateChat
import se.gustavkarlsson.chefgpt.chats.CreateConversation
import se.gustavkarlsson.chefgpt.chats.DeleteChat
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.chats.HttpChatRepository
import se.gustavkarlsson.chefgpt.chats.HttpCreateChat
import se.gustavkarlsson.chefgpt.chats.HttpCreateConversation
import se.gustavkarlsson.chefgpt.chats.HttpDeleteChat
import se.gustavkarlsson.chefgpt.chats.HttpStreamChats
import se.gustavkarlsson.chefgpt.chats.StreamChats
import se.gustavkarlsson.chefgpt.chefGptJson
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.files.DeleteFile
import se.gustavkarlsson.chefgpt.files.HttpUploadFile
import se.gustavkarlsson.chefgpt.files.RealDeleteFile
import se.gustavkarlsson.chefgpt.files.UploadFile
import se.gustavkarlsson.chefgpt.ingredients.CreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.DestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.HttpCreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.HttpDestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.HttpScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.HttpSetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.HttpStreamIngredients
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.ingredients.RealResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.RealResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.ResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.ScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.SetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.AwaitJob
import se.gustavkarlsson.chefgpt.jobs.HttpAwaitJob
import se.gustavkarlsson.chefgpt.jobs.HttpScanRecipes
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.RealStreamScanState
import se.gustavkarlsson.chefgpt.jobs.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.StreamScanState
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.readDeviceConfig
import se.gustavkarlsson.chefgpt.recipes.DeleteRecipe
import se.gustavkarlsson.chefgpt.recipes.GetRecipe
import se.gustavkarlsson.chefgpt.recipes.HttpDeleteRecipe
import se.gustavkarlsson.chefgpt.recipes.HttpGetRecipe
import se.gustavkarlsson.chefgpt.recipes.HttpOverwriteOriginalRecipe
import se.gustavkarlsson.chefgpt.recipes.HttpRecipeRepository
import se.gustavkarlsson.chefgpt.recipes.HttpSaveRecipeAsCopy
import se.gustavkarlsson.chefgpt.recipes.HttpSetRecipeFavorite
import se.gustavkarlsson.chefgpt.recipes.HttpStreamRecipeSummaries
import se.gustavkarlsson.chefgpt.recipes.OverwriteOriginalRecipe
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.SaveRecipeAsCopy
import se.gustavkarlsson.chefgpt.recipes.SetRecipeFavorite
import se.gustavkarlsson.chefgpt.recipes.StreamRecipeSummaries
import se.gustavkarlsson.chefgpt.screens.chat.ChatViewModel
import se.gustavkarlsson.chefgpt.screens.debug.DebugViewModel
import se.gustavkarlsson.chefgpt.screens.ingredients.IngredientsViewModel
import se.gustavkarlsson.chefgpt.screens.recipe.RecipeDetailViewModel
import se.gustavkarlsson.chefgpt.screens.recipescan.RecipeScanSheetViewModel
import se.gustavkarlsson.chefgpt.screens.start.StartViewModel
import se.gustavkarlsson.chefgpt.sessions.GetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.HttpGetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.HttpLogIn
import se.gustavkarlsson.chefgpt.sessions.HttpLogOut
import se.gustavkarlsson.chefgpt.sessions.HttpRegister
import se.gustavkarlsson.chefgpt.sessions.HttpSessionRepository
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore
import se.gustavkarlsson.chefgpt.sessions.LogIn
import se.gustavkarlsson.chefgpt.sessions.LogOut
import se.gustavkarlsson.chefgpt.sessions.Register
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.snackbar.RealShowSnackbar
import se.gustavkarlsson.chefgpt.snackbar.ShowSnackbar
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager

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

package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.scoped
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
import se.gustavkarlsson.chefgpt.facts.FactsRepository
import se.gustavkarlsson.chefgpt.facts.HttpFactsRepository
import se.gustavkarlsson.chefgpt.facts.usecases.GetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.HttpGetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.HttpSetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.SetFacts
import se.gustavkarlsson.chefgpt.files.usecases.DeleteFile
import se.gustavkarlsson.chefgpt.files.usecases.HttpUploadFile
import se.gustavkarlsson.chefgpt.files.usecases.RealDeleteFile
import se.gustavkarlsson.chefgpt.files.usecases.UploadFile
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
import se.gustavkarlsson.chefgpt.jobs.usecases.HttpScrapeRecipe
import se.gustavkarlsson.chefgpt.jobs.usecases.RealStreamScanState
import se.gustavkarlsson.chefgpt.jobs.usecases.RealStreamScrapeState
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.ScrapeRecipe
import se.gustavkarlsson.chefgpt.jobs.usecases.StreamScanState
import se.gustavkarlsson.chefgpt.jobs.usecases.StreamScrapeState
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
import se.gustavkarlsson.chefgpt.updates.UpdateRequiredNotifier

/**
 * Marker type used as the qualifier of [activityRetainedScopeModule] on non-Android
 * platforms.
 *
 * Bindings in the activity retained scope survive configuration changes but not the end
 * of the UI session, like Hilt's ActivityRetainedScope. Android uses koin-android's
 * activity retained scope archetype as the qualifier; the other platforms use this
 * marker.
 */
object ActivityRetainedScope

expect val activityRetainedScopeQualifier: Qualifier

val ACTIVITY_RETAINED_SCOPE_ID: ScopeID = "activity_retained_scope"

// The app graph, declared once for all platforms. Stateful bindings are scoped: they
// survive configuration changes and, on Android, are closed when the activity truly
// finishes; elsewhere the lazily created activity retained scope lives for the process.
// Stateless bindings are factories, created fresh on every resolution. Resolving any of
// them from the root scope fails on every platform.
val activityRetainedScopeModule: Module =
    module {
        scope(activityRetainedScopeQualifier) {
            // Scoped — session state and resources
            scoped<Navigator>()
            scoped<SnackbarManager>()
            scoped<UpdateRequiredNotifier>()
            scoped<JobManager>() withOptions {
                onClose { it?.cancel() }
            }
            scoped<ChefGptClient>() withOptions {
                onClose { it?.close() }
            }

            // Factories — stateless
            factory<Json> { chefGptJson(strict = false) }
            factory<LastSessionFileStore>()
            factory<EventHistoryStore>()
            factory<DeviceConfig> { readDeviceConfig() }

            factory<HttpSessionRepository>() bind SessionRepository::class
            factory<HttpChatRepository>() bind ChatRepository::class
            factory<HttpRecipeRepository>() bind RecipeRepository::class
            factory<HttpFactsRepository>() bind FactsRepository::class

            factory<HttpGetCurrentSession>() bind GetCurrentSession::class
            factory<HttpRegister>() bind Register::class
            factory<HttpLogIn>() bind LogIn::class
            factory<HttpLogOut>() bind LogOut::class

            factory<HttpGetFacts>() bind GetFacts::class
            factory<HttpSetFacts>() bind SetFacts::class

            factory<HttpCreateChat>() bind CreateChat::class
            factory<HttpDeleteChat>() bind DeleteChat::class
            factory<HttpStreamChats>() bind StreamChats::class
            factory<HttpCreateConversation>() bind CreateConversation::class

            factory<HttpStreamRecipeSummaries>() bind StreamRecipeSummaries::class
            factory<HttpGetRecipe>() bind GetRecipe::class
            factory<HttpSetRecipeFavorite>() bind SetRecipeFavorite::class
            factory<HttpDeleteRecipe>() bind DeleteRecipe::class
            factory<HttpOverwriteOriginalRecipe>() bind OverwriteOriginalRecipe::class
            factory<HttpSaveRecipeAsCopy>() bind SaveRecipeAsCopy::class

            factory<HttpStreamIngredients>() bind StreamIngredients::class
            factory<HttpCreateIngredient>() bind CreateIngredient::class
            factory<HttpDestroyIngredient>() bind DestroyIngredient::class
            factory<HttpSetIngredientInventory>() bind SetIngredientInventory::class
            factory<HttpScanIngredients>() bind ScanIngredients::class
            factory<RealResolveEmoji>() bind ResolveEmoji::class
            factory<RealResolveEmojiAlias>() bind ResolveEmojiAlias::class

            factory<HttpAwaitJob>() bind AwaitJob::class
            factory<HttpScanRecipes>() bind ScanRecipes::class
            factory<HttpScrapeRecipe>() bind ScrapeRecipe::class
            factory<RealStreamScanState>() bind StreamScanState::class
            factory<RealStreamScrapeState>() bind StreamScrapeState::class

            factory<HttpUploadFile>() bind UploadFile::class
            factory<RealDeleteFile>() bind DeleteFile::class

            factory<RealShowSnackbar>() bind ShowSnackbar::class
        }
    }

// The scope App() provides to the composition through LocalActivityRetainedScope.
expect fun defaultActivityRetainedScope(): Scope

val LocalActivityRetainedScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultActivityRetainedScope() }

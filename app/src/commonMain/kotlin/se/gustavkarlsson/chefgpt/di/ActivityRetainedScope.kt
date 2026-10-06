package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.scoped
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.HttpChatRepository
import se.gustavkarlsson.chefgpt.chats.usecases.CreateChat
import se.gustavkarlsson.chefgpt.chats.usecases.CreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.DeleteChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpCreateChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpCreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.HttpDeleteChat
import se.gustavkarlsson.chefgpt.chats.usecases.HttpStreamChats
import se.gustavkarlsson.chefgpt.chats.usecases.StreamChats
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

// Retained bindings — everything that talks to the network or holds session state —
// declared once for all platforms. On Android the activity's retained scope matches them
// through its scope archetype and closes them when the activity truly finishes; elsewhere
// the lazily created activity retained scope lives for the process. Resolving them from
// the root scope fails on every platform.
val activityRetainedScopeModule: Module =
    module {
        scope(activityRetainedScopeQualifier) {
            // Session state
            scoped<Navigator>()
            scoped<SnackbarManager>()
            scoped<UpdateRequiredNotifier>()
            scoped<JobManager>() withOptions {
                onClose { it?.cancel() }
            }

            // Client
            scoped<ChefGptClient>() withOptions {
                onClose { it?.close() }
            }

            // Repositories
            scoped<HttpSessionRepository>() bind SessionRepository::class
            scoped<HttpChatRepository>() bind ChatRepository::class
            scoped<HttpRecipeRepository>() bind RecipeRepository::class
            scoped<HttpFactsRepository>() bind FactsRepository::class

            // Use cases — sessions
            scoped<HttpGetCurrentSession>() bind GetCurrentSession::class
            scoped<HttpRegister>() bind Register::class
            scoped<HttpLogIn>() bind LogIn::class
            scoped<HttpLogOut>() bind LogOut::class

            // Use cases — facts
            scoped<HttpGetFacts>() bind GetFacts::class
            scoped<HttpSetFacts>() bind SetFacts::class

            // Use cases — chats
            scoped<HttpCreateChat>() bind CreateChat::class
            scoped<HttpDeleteChat>() bind DeleteChat::class
            scoped<HttpStreamChats>() bind StreamChats::class
            scoped<HttpCreateConversation>() bind CreateConversation::class

            // Use cases — recipes
            scoped<HttpStreamRecipeSummaries>() bind StreamRecipeSummaries::class
            scoped<HttpGetRecipe>() bind GetRecipe::class
            scoped<HttpSetRecipeFavorite>() bind SetRecipeFavorite::class
            scoped<HttpDeleteRecipe>() bind DeleteRecipe::class
            scoped<HttpOverwriteOriginalRecipe>() bind OverwriteOriginalRecipe::class
            scoped<HttpSaveRecipeAsCopy>() bind SaveRecipeAsCopy::class

            // Use cases — ingredients
            scoped<HttpStreamIngredients>() bind StreamIngredients::class
            scoped<HttpCreateIngredient>() bind CreateIngredient::class
            scoped<HttpDestroyIngredient>() bind DestroyIngredient::class
            scoped<HttpSetIngredientInventory>() bind SetIngredientInventory::class
            scoped<HttpScanIngredients>() bind ScanIngredients::class
            scoped<RealResolveEmoji>() bind ResolveEmoji::class
            scoped<RealResolveEmojiAlias>() bind ResolveEmojiAlias::class

            // Use cases — jobs
            scoped<HttpAwaitJob>() bind AwaitJob::class
            scoped<HttpScanRecipes>() bind ScanRecipes::class
            scoped<HttpScrapeRecipe>() bind ScrapeRecipe::class
            scoped<RealStreamScanState>() bind StreamScanState::class
            scoped<RealStreamScrapeState>() bind StreamScrapeState::class

            // Use cases — files
            scoped<HttpUploadFile>() bind UploadFile::class
            scoped<RealDeleteFile>() bind DeleteFile::class

            // Use cases — snackbar
            scoped<RealShowSnackbar>() bind ShowSnackbar::class
        }
    }

// The scope App() provides to the composition through LocalActivityRetainedScope.
expect fun defaultActivityRetainedScope(): Scope

val LocalActivityRetainedScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultActivityRetainedScope() }

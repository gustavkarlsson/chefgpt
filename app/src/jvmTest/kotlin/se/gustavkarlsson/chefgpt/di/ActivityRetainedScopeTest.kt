package se.gustavkarlsson.chefgpt.di

import kotlinx.io.files.FileSystem
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import se.gustavkarlsson.chefgpt.ChefGptClient
import se.gustavkarlsson.chefgpt.DeviceConfig
import se.gustavkarlsson.chefgpt.chats.ChatRepository
import se.gustavkarlsson.chefgpt.chats.EventHistoryStore
import se.gustavkarlsson.chefgpt.chats.usecases.CreateChat
import se.gustavkarlsson.chefgpt.chats.usecases.CreateConversation
import se.gustavkarlsson.chefgpt.chats.usecases.DeleteChat
import se.gustavkarlsson.chefgpt.chats.usecases.StreamChats
import se.gustavkarlsson.chefgpt.debug.Settings
import se.gustavkarlsson.chefgpt.facts.FactsRepository
import se.gustavkarlsson.chefgpt.facts.usecases.GetFacts
import se.gustavkarlsson.chefgpt.facts.usecases.SetFacts
import se.gustavkarlsson.chefgpt.files.usecases.DeleteFile
import se.gustavkarlsson.chefgpt.files.usecases.UploadFile
import se.gustavkarlsson.chefgpt.ingredients.IngredientEmojiResolver
import se.gustavkarlsson.chefgpt.ingredients.usecases.CreateIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.DestroyIngredient
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmoji
import se.gustavkarlsson.chefgpt.ingredients.usecases.ResolveEmojiAlias
import se.gustavkarlsson.chefgpt.ingredients.usecases.ScanIngredients
import se.gustavkarlsson.chefgpt.ingredients.usecases.SetIngredientInventory
import se.gustavkarlsson.chefgpt.ingredients.usecases.StreamIngredients
import se.gustavkarlsson.chefgpt.jobs.JobManager
import se.gustavkarlsson.chefgpt.jobs.usecases.AwaitJob
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.ScrapeRecipe
import se.gustavkarlsson.chefgpt.jobs.usecases.StreamScanState
import se.gustavkarlsson.chefgpt.jobs.usecases.StreamScrapeState
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.recipes.RecipeRepository
import se.gustavkarlsson.chefgpt.recipes.usecases.DeleteRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.GetRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.OverwriteOriginalRecipe
import se.gustavkarlsson.chefgpt.recipes.usecases.SaveRecipeAsCopy
import se.gustavkarlsson.chefgpt.recipes.usecases.SetRecipeFavorite
import se.gustavkarlsson.chefgpt.recipes.usecases.StreamRecipeSummaries
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore
import se.gustavkarlsson.chefgpt.sessions.SessionRepository
import se.gustavkarlsson.chefgpt.sessions.usecases.GetCurrentSession
import se.gustavkarlsson.chefgpt.sessions.usecases.LogIn
import se.gustavkarlsson.chefgpt.sessions.usecases.LogOut
import se.gustavkarlsson.chefgpt.sessions.usecases.Register
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import se.gustavkarlsson.chefgpt.updates.UpdateRequiredNotifier
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class ActivityRetainedScopeTest {
    private val scopeBindings: List<KClass<*>> =
        listOf(
            Navigator::class,
        )

    private val rootBindings: List<KClass<*>> =
        listOf(
            Settings::class,
            IngredientEmojiResolver.Factory::class,
            UpdateRequiredNotifier::class,
            JobManager::class,
            ChefGptClient::class,
            DeviceConfig::class,
            EventHistoryStore::class,
            FileSystem::class,
            LastSessionFileStore::class,
            Json::class,
            SessionRepository::class,
            ChatRepository::class,
            RecipeRepository::class,
            FactsRepository::class,
            GetCurrentSession::class,
            Register::class,
            LogIn::class,
            LogOut::class,
            GetFacts::class,
            SetFacts::class,
            CreateChat::class,
            DeleteChat::class,
            StreamChats::class,
            CreateConversation::class,
            StreamRecipeSummaries::class,
            GetRecipe::class,
            SetRecipeFavorite::class,
            DeleteRecipe::class,
            OverwriteOriginalRecipe::class,
            SaveRecipeAsCopy::class,
            ShowSnackbar::class,
            SnackbarManager::class,
            StreamIngredients::class,
            CreateIngredient::class,
            DestroyIngredient::class,
            SetIngredientInventory::class,
            ScanIngredients::class,
            ScanRecipes::class,
            ScrapeRecipe::class,
            ResolveEmoji::class,
            ResolveEmojiAlias::class,
            AwaitJob::class,
            StreamScanState::class,
            StreamScrapeState::class,
            UploadFile::class,
            DeleteFile::class,
        )

    @Test
    fun `resolves scope bindings within the activity retained scope`() {
        val koin = koinApplication { modules(appModule) }.koin
        val scope = koin.getOrCreateScope(ACTIVITY_RETAINED_SCOPE_ID, activityRetainedScopeQualifier)

        for (binding in scopeBindings) {
            assertNotNull(scope.getOrNull(binding), "Missing scope binding: $binding")
        }
    }

    @Test
    fun `does not resolve scope bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        for (binding in scopeBindings) {
            assertNull(koin.getOrNull(binding), "Scope binding leaked to the root scope: $binding")
        }
    }

    @Test
    fun `resolves root bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        for (binding in rootBindings) {
            assertNotNull(koin.getOrNull(binding), "Missing root binding: $binding")
        }
    }

    @Test
    fun `creates a fresh instance for every factory resolution`() {
        val koin = koinApplication { modules(appModule) }.koin

        assertNotSame(koin.get<Json>(), koin.get<Json>())
        assertNotSame(koin.get<LastSessionFileStore>(), koin.get<LastSessionFileStore>())
        assertNotSame(koin.get<ChatRepository>(), koin.get<ChatRepository>())
        assertNotSame(koin.get<CreateChat>(), koin.get<CreateChat>())
    }
}

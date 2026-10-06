package se.gustavkarlsson.chefgpt.di

import org.koin.dsl.koinApplication
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.ScrapeRecipe
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActivityRetainedScopeTest {
    @Test
    fun `resolves retained bindings within the activity retained scope`() {
        val koin = koinApplication { modules(appModule) }.koin
        val scope = koin.getOrCreateScope(ACTIVITY_RETAINED_SCOPE_ID, activityRetainedScopeQualifier)

        assertNotNull(scope.getOrNull<Navigator>())
        assertNotNull(scope.getOrNull<SnackbarManager>())
        assertNotNull(scope.getOrNull<ShowSnackbar>())
        assertNotNull(scope.getOrNull<ScanRecipes>())
        assertNotNull(scope.getOrNull<ScrapeRecipe>())
    }

    @Test
    fun `does not resolve retained bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        assertNull(koin.getOrNull<Navigator>())
        assertNull(koin.getOrNull<SnackbarManager>())
        assertNull(koin.getOrNull<ShowSnackbar>())
        assertNull(koin.getOrNull<ScanRecipes>())
        assertNull(koin.getOrNull<ScrapeRecipe>())
    }
}

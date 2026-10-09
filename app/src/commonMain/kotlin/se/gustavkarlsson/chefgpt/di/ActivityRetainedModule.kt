package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.scoped
import se.gustavkarlsson.chefgpt.jobs.usecases.HttpScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.HttpScrapeRecipe
import se.gustavkarlsson.chefgpt.jobs.usecases.ScanRecipes
import se.gustavkarlsson.chefgpt.jobs.usecases.ScrapeRecipe
import se.gustavkarlsson.chefgpt.navigation.Navigator
import se.gustavkarlsson.chefgpt.snackbar.SnackbarManager
import se.gustavkarlsson.chefgpt.snackbar.usecases.RealShowSnackbar
import se.gustavkarlsson.chefgpt.snackbar.usecases.ShowSnackbar

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

const val ACTIVITY_RETAINED_SCOPE_ID: ScopeID = "activity_retained_scope"

val activityRetainedScopeModule: Module =
    module {
        scope(activityRetainedScopeQualifier) {
            // ActivityRetained scoped on Android. Singletons on other platforms
            scoped<Navigator>()
            scoped<SnackbarManager>()

            // TODO Consider moving below to the singleton scope.
            // On-demand factories — stateless
            factory<RealShowSnackbar>() bind ShowSnackbar::class
            // Depends on the scoped ShowSnackbar, so these live in the scope too
            factory<HttpScanRecipes>() bind ScanRecipes::class
            factory<HttpScrapeRecipe>() bind ScrapeRecipe::class
        }
    }

// The scope App() provides to the composition through LocalActivityRetainedScope.
expect fun defaultActivityRetainedScope(): Scope

val LocalActivityRetainedScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultActivityRetainedScope() }

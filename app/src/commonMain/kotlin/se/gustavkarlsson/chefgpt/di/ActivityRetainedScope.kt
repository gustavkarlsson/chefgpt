package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.koin.dsl.bind
import org.koin.dsl.module
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

val ACTIVITY_RETAINED_SCOPE_ID: ScopeID = "activity_retained_scope"

// Retained bindings, declared once for all platforms. On Android the activity's retained
// scope matches them through its scope archetype; elsewhere the lazily created activity
// retained scope matches them through its qualifier. Resolving them from the root scope
// fails on every platform.
val activityRetainedScopeModule: Module =
    module {
        scope(activityRetainedScopeQualifier) {
            scoped<Navigator>()
            scoped<SnackbarManager>()
            scoped<RealShowSnackbar>() bind ShowSnackbar::class
            scoped<HttpScanRecipes>() bind ScanRecipes::class
            scoped<HttpScrapeRecipe>() bind ScrapeRecipe::class
        }
    }

// The scope App() provides to the composition through LocalActivityRetainedScope.
expect fun defaultActivityRetainedScope(): Scope

val LocalActivityRetainedScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultActivityRetainedScope() }

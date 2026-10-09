package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID
import org.koin.dsl.module
import org.koin.plugin.module.dsl.scoped
import se.gustavkarlsson.chefgpt.navigation.Navigator

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
        }
    }

// The scope App() provides to the composition through LocalActivityRetainedScope.
expect fun defaultActivityRetainedScope(): Scope

val LocalActivityRetainedScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultActivityRetainedScope() }

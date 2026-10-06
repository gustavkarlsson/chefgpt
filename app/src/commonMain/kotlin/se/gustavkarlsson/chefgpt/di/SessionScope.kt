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
 * Marker type used as the session scope qualifier on non-Android platforms.
 *
 * The session scope is the Koin equivalent of Hilt's ActivityRetainedScope: bindings
 * survive configuration changes but not the end of the UI session. Android uses
 * koin-android's activity retained scope archetype as the qualifier; the other
 * platforms use this marker.
 */
object SessionScope

expect val sessionScopeQualifier: Qualifier

val SESSION_SCOPE_ID: ScopeID = "session_scope"

// Session bindings, declared once for all platforms. On Android the activity's retained
// scope matches them through its scope archetype; elsewhere the lazily created session
// scope matches them through its qualifier. Resolving them from the root scope fails on
// every platform.
val sessionScopeModule: Module =
    module {
        scope(sessionScopeQualifier) {
            scoped<Navigator>()
        }
    }

// The scope App() provides to the composition through LocalSessionScope.
expect fun defaultSessionScope(): Scope

val LocalSessionScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf { defaultSessionScope() }

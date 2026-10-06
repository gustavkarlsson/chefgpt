package se.gustavkarlsson.chefgpt.di

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatformTools

// The scope for session bindings — the Koin equivalent of Hilt's ActivityRetainedScope:
// injections resolve bindings that survive configuration changes but not the end of the UI
// session. The default is the root scope; Android's MainActivity provides the activity's
// retained scope instead (see androidSessionModule).
@OptIn(KoinInternalApi::class) // Koin exposes no public root scope accessor.
val LocalSessionScope: ProvidableCompositionLocal<Scope> =
    compositionLocalOf {
        KoinPlatformTools
            .defaultContext()
            .get()
            .scopeRegistry.rootScope
    }

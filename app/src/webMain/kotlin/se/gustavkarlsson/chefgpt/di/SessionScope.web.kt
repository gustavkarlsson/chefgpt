package se.gustavkarlsson.chefgpt.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatformTools

actual val sessionScopeQualifier: Qualifier = named<SessionScope>()

// The session scope lives for the process: created on first use and never closed.
actual fun defaultSessionScope(): Scope =
    KoinPlatformTools
        .defaultContext()
        .get()
        .getOrCreateScope(SESSION_SCOPE_ID, sessionScopeQualifier)

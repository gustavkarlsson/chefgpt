package se.gustavkarlsson.chefgpt.di

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.mp.KoinPlatformTools

actual val activityRetainedScopeQualifier: Qualifier = named<ActivityRetainedScope>()

// The activity retained scope lives for the process: created on first use and never closed.
actual fun defaultActivityRetainedScope(): Scope =
    KoinPlatformTools
        .defaultContext()
        .get()
        .getOrCreateScope(ACTIVITY_RETAINED_SCOPE_ID, activityRetainedScopeQualifier)

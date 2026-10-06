package se.gustavkarlsson.chefgpt.di

import org.koin.androidx.scope.RetainedScopeActivity
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.TypeQualifier
import org.koin.core.scope.Scope

// The qualifier of koin-android's activity retained scope archetype, which
// activityRetainedScope() attaches to the activity's scope. Built from the public marker
// class because the archetype getter itself is internal.
actual val sessionScopeQualifier: Qualifier = TypeQualifier(RetainedScopeActivity::class)

// MainActivity provides the activity's retained scope; outside it there is no session
// scope, and resolution must fail rather than silently fall back.
actual fun defaultSessionScope(): Scope = error("No session scope in this composition; MainActivity provides it")

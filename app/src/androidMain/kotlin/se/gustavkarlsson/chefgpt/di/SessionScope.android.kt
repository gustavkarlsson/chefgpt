package se.gustavkarlsson.chefgpt.di

import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.plugin.module.dsl.scoped
import se.gustavkarlsson.chefgpt.navigation.Navigator

// The session bindings, in koin-android's activityRetainedScope: within the activity's scope
// these win over the root singletons declared in singletonModule, and they are closed when
// the activity truly finishes — not on configuration changes.
val androidSessionModule: Module =
    module {
        activityRetainedScope {
            scoped<Navigator>()
        }
    }

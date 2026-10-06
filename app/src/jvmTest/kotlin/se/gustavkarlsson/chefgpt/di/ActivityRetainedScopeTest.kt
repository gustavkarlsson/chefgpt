package se.gustavkarlsson.chefgpt.di

import org.koin.dsl.koinApplication
import se.gustavkarlsson.chefgpt.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActivityRetainedScopeTest {
    @Test
    fun `resolves retained bindings within the activity retained scope`() {
        val koin = koinApplication { modules(appModule) }.koin
        val scope = koin.getOrCreateScope(ACTIVITY_RETAINED_SCOPE_ID, activityRetainedScopeQualifier)

        val navigator = scope.getOrNull<Navigator>()

        assertNotNull(navigator)
    }

    @Test
    fun `does not resolve retained bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        val navigator = koin.getOrNull<Navigator>()

        assertNull(navigator)
    }
}

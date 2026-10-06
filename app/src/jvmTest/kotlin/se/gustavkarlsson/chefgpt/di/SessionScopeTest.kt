package se.gustavkarlsson.chefgpt.di

import org.koin.dsl.koinApplication
import se.gustavkarlsson.chefgpt.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SessionScopeTest {
    @Test
    fun `resolves session bindings within the session scope`() {
        val koin = koinApplication { modules(appModule) }.koin
        val scope = koin.getOrCreateScope(SESSION_SCOPE_ID, sessionScopeQualifier)

        val navigator = scope.getOrNull<Navigator>()

        assertNotNull(navigator)
    }

    @Test
    fun `does not resolve session bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        val navigator = koin.getOrNull<Navigator>()

        assertNull(navigator)
    }
}

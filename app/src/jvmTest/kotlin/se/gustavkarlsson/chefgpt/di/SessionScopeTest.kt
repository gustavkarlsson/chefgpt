package se.gustavkarlsson.chefgpt.di

import org.koin.dsl.koinApplication
import se.gustavkarlsson.chefgpt.navigation.Navigator
import kotlin.test.Test
import kotlin.test.assertNotNull

class SessionScopeTest {
    @Test
    fun `resolves session bindings from the root scope`() {
        val koin = koinApplication { modules(appModule) }.koin

        val navigator = koin.getOrNull<Navigator>()

        assertNotNull(navigator)
    }
}

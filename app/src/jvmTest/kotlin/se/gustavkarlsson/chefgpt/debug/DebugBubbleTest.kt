package se.gustavkarlsson.chefgpt.debug

import androidx.compose.ui.geometry.Offset
import se.gustavkarlsson.chefgpt.navigation.Screen
import se.gustavkarlsson.chefgpt.screens.debug.DebugScreen
import se.gustavkarlsson.chefgpt.screens.login.LoginScreen
import se.gustavkarlsson.chefgpt.screens.recipescan.RecipeScanSheet
import se.gustavkarlsson.chefgpt.sessions.SessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugBubbleTest {
    @Test
    fun `offset within bounds is unchanged`() {
        val offset = Offset(10f, 20f)

        val result = offset.clampedToBounds(Offset(100f, 100f))

        assertEquals(offset, result)
    }

    @Test
    fun `offset beyond bounds is clamped to the edge`() {
        val offset = Offset(120f, 60f)

        val result = offset.clampedToBounds(Offset(100f, 100f))

        assertEquals(Offset(100f, 60f), result)
    }

    @Test
    fun `negative offset is clamped to zero`() {
        val offset = Offset(-5f, -10f)

        val result = offset.clampedToBounds(Offset(100f, 100f))

        assertEquals(Offset.Zero, result)
    }

    @Test
    fun `bounds smaller than the bubble clamp to zero`() {
        val offset = Offset(10f, 10f)

        val result = offset.clampedToBounds(Offset(-1f, -1f))

        assertEquals(Offset.Zero, result)
    }

    @Test
    fun `bubble shows over a regular screen`() {
        val backStack: List<Screen> = listOf(LoginScreen())

        val result = shouldShowDebugBubble(backStack)

        assertTrue(result)
    }

    @Test
    fun `bubble shows when the debug screen is below the top`() {
        val backStack: List<Screen> = listOf(LoginScreen(), DebugScreen(), LoginScreen())

        val result = shouldShowDebugBubble(backStack)

        assertTrue(result)
    }

    @Test
    fun `bubble hides when the debug screen is on top`() {
        val backStack: List<Screen> = listOf(LoginScreen(), DebugScreen())

        val result = shouldShowDebugBubble(backStack)

        assertFalse(result)
    }

    @Test
    fun `bubble hides when a bottom sheet is on top`() {
        val backStack: List<Screen> = listOf(LoginScreen(), RecipeScanSheet(SessionId("test")))

        val result = shouldShowDebugBubble(backStack)

        assertFalse(result)
    }

    @Test
    fun `bubble hides when the back stack is empty`() {
        val backStack: List<Screen> = emptyList()

        val result = shouldShowDebugBubble(backStack)

        assertFalse(result)
    }
}

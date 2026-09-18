package se.gustavkarlsson.chefgpt.navigation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigatorTest {
    @Test
    fun `returns the delivered result`() {
        runTest {
            val target = TestResultScreen(Screen.Id.new("target"))
            val navigator = navigator()
            val deferred = backgroundScope.async { navigator.requestResult(target) }
            runCurrent()

            navigator.completeResult(target, "ok")
            navigator.pop()

            assertEquals("ok", deferred.await())
            assertTrue(navigator.pendingRequests().isEmpty())
        }
    }

    @Test
    fun `returns null when the target is popped without a result`() {
        runTest {
            val target = TestResultScreen(Screen.Id.new("target"))
            val navigator = navigator()
            val deferred = backgroundScope.async { navigator.requestResult(target) }
            runCurrent()

            navigator.pop()

            assertNull(deferred.await())
        }
    }

    @Test
    fun `removes the pending request when the requester coroutine is cancelled`() {
        runTest {
            val target = TestResultScreen(Screen.Id.new("target"))
            val navigator = navigator()
            val job = launch { navigator.requestResult(target) }
            runCurrent()

            job.cancelAndJoin()

            assertTrue(navigator.pendingRequests().isEmpty())
        }
    }

    @Test
    fun `records the requester and target screen ids`() {
        runTest {
            val target = TestResultScreen(Screen.Id.new("target"))
            val navigator = navigator()
            backgroundScope.launch { navigator.requestResult(target) }
            runCurrent()

            val requests = navigator.pendingRequests()
            val request = requests.values.single()
            assertEquals(Screen.Id.new("requester"), request.requesterId)
            assertEquals(Screen.Id.new("target"), request.targetId)
            assertEquals(request.id, requests.keys.single())
        }
    }
}

private class TestRequesterScreen(
    override val id: Screen.Id,
) : Screen {
    @Composable
    override fun Content() {}
}

private class TestResultScreen(
    override val id: Screen.Id,
) : Screen.ResultProvider<String> {
    @Composable
    override fun Content() {}
}

private fun navigator() = Navigator(TestRequesterScreen(Screen.Id.new("requester")))

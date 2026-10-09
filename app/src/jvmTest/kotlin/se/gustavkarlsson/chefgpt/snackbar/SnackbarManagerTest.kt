package se.gustavkarlsson.chefgpt.snackbar

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class SnackbarManagerTest {
    private class Setup(
        val timeSource: TestTimeSource,
        val manager: SnackbarManager,
        val shown: MutableList<SnackbarMessage>,
    )

    private fun TestScope.startManager(): Setup {
        val timeSource = TestTimeSource()
        val manager = SnackbarManager(timeSource)
        val shown = mutableListOf<SnackbarMessage>()
        backgroundScope.launch { manager.messages.collect { shown += it } }
        return Setup(timeSource, manager, shown)
    }

    @Test
    fun `delivers a message that has not expired`() =
        runTest {
            val setup = startManager()

            setup.manager.show("Fresh")
            runCurrent()

            assertEquals(listOf("Fresh"), setup.shown.map { it.text })
        }

    @Test
    fun `drops a message that expired while queued`() =
        runTest {
            val setup = startManager()

            setup.manager.show("Stale")
            setup.timeSource += QUEUE_EXPIRY + 1.seconds
            runCurrent()

            assertEquals(emptyList(), setup.shown)
        }

    @Test
    fun `delivers later messages after dropping an expired one`() =
        runTest {
            val setup = startManager()

            setup.manager.show("Stale")
            setup.timeSource += QUEUE_EXPIRY + 1.seconds
            setup.manager.show("Fresh")
            runCurrent()

            assertEquals(listOf("Fresh"), setup.shown.map { it.text })
        }
}

package se.gustavkarlsson.chefgpt.debug

import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import se.gustavkarlsson.chefgpt.SERVER_BASE_URL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTest {
    private val file = Path("test-debug.conf")

    @AfterTest
    fun cleanup() {
        SystemFileSystem.delete(file, mustExist = false)
    }

    @Test
    fun `baseUrl defaults to platform constant when no file exists`() =
        runBlocking {
            val store = Settings(file)

            assertEquals(SERVER_BASE_URL, store.getBaseUrl())
        }

    @Test
    fun `setBaseUrl updates the live value and persists it`() =
        runBlocking {
            val store = Settings(file)

            store.setBaseUrl("http://example.com:9090")

            assertEquals("http://example.com:9090", store.getBaseUrl())
            assertTrue(readFile().contains("base_url=http://example.com:9090"))
        }

    @Test
    fun `a new store reads the persisted value`() =
        runBlocking {
            Settings(file).setBaseUrl("http://example.com:9090")

            val reloaded = Settings(file)

            assertEquals("http://example.com:9090", reloaded.getBaseUrl())
        }

    @Test
    fun `blank value clears the override and falls back to the default`() =
        runBlocking {
            val store = Settings(file)
            store.setBaseUrl("http://example.com:9090")

            store.setBaseUrl("   ")

            assertEquals(SERVER_BASE_URL, store.getBaseUrl())
        }

    @Test
    fun `setBaseUrl trims surrounding whitespace`() =
        runBlocking {
            val store = Settings(file)

            store.setBaseUrl("  http://example.com:9090  ")

            assertEquals("http://example.com:9090", store.getBaseUrl())
        }

    private fun readFile(): String = SystemFileSystem.source(file).buffered().use { it.readString() }
}

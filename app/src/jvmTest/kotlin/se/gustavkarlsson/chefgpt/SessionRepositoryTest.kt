package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import se.gustavkarlsson.chefgpt.api.SessionId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionRepositoryTest {
    private val file = Path("test-login-credentials.txt")
    private val repository = SessionRepository(file)

    @AfterTest
    fun cleanup() {
        if (SystemFileSystem.exists(file)) {
            SystemFileSystem.delete(file)
        }
    }

    @Test
    fun `load returns null when file does not exist`() {
        assertNull(repository.load())
    }

    @Test
    fun `save and load round-trips session id`() {
        val sessionId = SessionId.random()

        repository.save(sessionId)
        val loaded = repository.load()

        assertEquals(sessionId, loaded)
    }

    @Test
    fun `save overwrites previous session id`() {
        repository.save(SessionId.random())
        val second = SessionId.random()
        repository.save(second)

        val loaded = repository.load()

        assertEquals(second, loaded)
    }

    @Test
    fun `clear removes the file`() {
        repository.save(SessionId.random())

        repository.clear()

        assertNull(repository.load())
    }

    @Test
    fun `clear is safe when file does not exist`() {
        repository.clear()
    }
}

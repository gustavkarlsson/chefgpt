package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

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
    fun `save and load round-trips credentials`() {
        val credentials = SessionCredentials("alice", SessionId(Uuid.random().toString()))

        repository.save(credentials)
        val loaded = repository.load()

        assertEquals(credentials, loaded)
    }

    @Test
    fun `save overwrites previous credentials`() {
        repository.save(SessionCredentials("alice", SessionId(Uuid.random().toString())))
        val second = SessionCredentials("bob", SessionId(Uuid.random().toString()))
        repository.save(second)

        val loaded = repository.load()

        assertEquals(second, loaded)
    }

    @Test
    fun `clear removes the file`() {
        repository.save(SessionCredentials("alice", SessionId(Uuid.random().toString())))

        repository.clear()

        assertNull(repository.load())
    }

    @Test
    fun `clear is safe when file does not exist`() {
        repository.clear()
    }
}

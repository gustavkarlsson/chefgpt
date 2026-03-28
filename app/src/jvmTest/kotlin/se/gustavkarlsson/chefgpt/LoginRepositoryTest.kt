package se.gustavkarlsson.chefgpt

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginRepositoryTest {
    private val file = Path("test-login-credentials.txt")
    private val repository = LoginRepository(file)

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
        val credentials = Credentials("alice", "secret123")

        repository.save(credentials)
        val loaded = repository.load()

        assertEquals(credentials, loaded)
    }

    @Test
    fun `save overwrites previous credentials`() {
        repository.save(Credentials("alice", "old"))
        repository.save(Credentials("bob", "new"))

        val loaded = repository.load()

        assertEquals(Credentials("bob", "new"), loaded)
    }

    @Test
    fun `clear removes the file`() {
        repository.save(Credentials("alice", "secret"))

        repository.clear()

        assertNull(repository.load())
    }

    @Test
    fun `clear is safe when file does not exist`() {
        repository.clear()
    }
}

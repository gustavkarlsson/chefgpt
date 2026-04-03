package se.gustavkarlsson.chefgpt

import com.github.michaelbull.result.Ok
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import se.gustavkarlsson.chefgpt.sessions.LastSessionFileStore
import se.gustavkarlsson.chefgpt.sessions.SessionCredentials
import se.gustavkarlsson.chefgpt.sessions.SessionId
import se.gustavkarlsson.chefgpt.sessions.UserName
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class LastSessionFileStoreTest {
    private val file = Path("test-login-credentials.txt")
    private val repository = LastSessionFileStore(file)

    @AfterTest
    fun cleanup() {
        SystemFileSystem.delete(file, mustExist = false)
    }

    @Test
    fun `load returns null when file does not exist`() =
        runBlocking {
            assertEquals(Ok(null), repository.load())
        }

    @Test
    fun `save and load round-trips credentials`() =
        runBlocking {
            val credentials = SessionCredentials(UserName("alice"), SessionId(Uuid.random().toString()))

            repository.save(credentials)
            val loaded = repository.load()

            assertEquals(Ok(credentials), loaded)
        }

    @Test
    fun `save overwrites previous credentials`() =
        runBlocking {
            repository.save(SessionCredentials(UserName("alice"), SessionId(Uuid.random().toString())))
            val second = SessionCredentials(UserName("bob"), SessionId(Uuid.random().toString()))
            repository.save(second)

            val loaded = repository.load()

            assertEquals(Ok(second), loaded)
        }

    @Test
    fun `clear removes the file`() =
        runBlocking {
            repository.save(SessionCredentials(UserName("alice"), SessionId(Uuid.random().toString())))

            repository.clear()

            assertEquals(Ok(null), repository.load())
        }

    @Test
    fun `clear is safe when file does not exist`() {
        runBlocking {
            repository.clear()
        }
    }
}

package se.gustavkarlsson.chefgpt.auth

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class InMemoryUserRepositoryTest {
    private val repo = InMemoryUserRepository()

    @Test
    fun `register succeeds when username is unique`() =
        runTest {
            val result = repo.register("alice", "Password1!")

            val user = result.getOrElse { fail("Expected Ok but got Err($it)") }
            assertEquals("alice", user.name)
        }

    @Test
    fun `register assigns distinct ids to different users`() =
        runTest {
            val user1 = repo.register("alice", "Password1!").getOrElse { fail("Expected Ok") }
            val user2 = repo.register("bob", "Password1!").getOrElse { fail("Expected Ok") }

            assertNotEquals(user1.id, user2.id)
        }

    @Test
    fun `register fails when username is already taken`() =
        runTest {
            repo.register("alice", "Password1!")

            val result = repo.register("alice", "Different1!")

            assertEquals(Err(RegistrationError.UsernameTaken), result)
        }

    @Test
    fun `register returns InvalidUserName from failing name rule`() =
        runTest {
            val repo =
                InMemoryUserRepository(
                    rules = listOf(RegistrationRule.name("too short") { it.length >= 5 }),
                )

            val result = repo.register("ab", "Password1!")

            assertEquals(Err(RegistrationError.InvalidUserName("too short")), result)
        }

    @Test
    fun `register returns InvalidPassword from failing password rule`() =
        runTest {
            val repo =
                InMemoryUserRepository(
                    rules = listOf(RegistrationRule.password("too weak") { it.length >= 20 }),
                )

            val result = repo.register("alice", "short")

            assertEquals(Err(RegistrationError.InvalidPassword("too weak")), result)
        }

    @Test
    fun `register returns first matching rule error`() =
        runTest {
            val repo =
                InMemoryUserRepository(
                    rules =
                        listOf(
                            RegistrationRule.name("name error") { false },
                            RegistrationRule.password("pw error") { false },
                        ),
                )

            val result = repo.register("alice", "Password1!")

            assertEquals(Err(RegistrationError.InvalidUserName("name error")), result)
        }

    @Test
    fun `register does not store user when rule validation fails`() =
        runTest {
            val repo =
                InMemoryUserRepository(
                    rules = listOf(RegistrationRule.name("rejected") { false }),
                )
            repo.register("alice", "Password1!")

            assertFalse("alice" in repo)
        }

    @Test
    fun `login succeeds with correct credentials`() =
        runTest {
            val registered = repo.register("alice", "Password1!").getOrElse { fail("Setup failed") }

            val result = repo.login("alice", "Password1!")

            assertEquals(Ok(registered), result)
        }

    @Test
    fun `login fails with wrong password`() =
        runTest {
            repo.register("alice", "Password1!")

            val result = repo.login("alice", "WrongPassword1!")

            assertEquals(Err(LoginError.WrongCredentials), result)
        }

    @Test
    fun `login fails with unknown username`() =
        runTest {
            val result = repo.login("nobody", "Password1!")

            assertEquals(Err(LoginError.WrongCredentials), result)
        }

    @Test
    fun `login is case-sensitive for username`() =
        runTest {
            repo.register("alice", "Password1!")

            val result = repo.login("Alice", "Password1!")

            assertEquals(Err(LoginError.WrongCredentials), result)
        }

    @Test
    fun `login is case-sensitive for password`() =
        runTest {
            repo.register("alice", "Password1!")

            val result = repo.login("alice", "password1!")

            assertEquals(Err(LoginError.WrongCredentials), result)
        }

    @Test
    fun `contains returns true for registered user`() =
        runTest {
            repo.register("alice", "Password1!")

            assertTrue("alice" in repo)
        }

    @Test
    fun `contains returns false for unregistered user`() =
        runTest {
            assertFalse("nobody" in repo)
        }

    @Test
    fun `multiple users can be registered independently`() =
        runTest {
            repo.register("alice", "Password1!")
            repo.register("bob", "Password1!")

            assertTrue("alice" in repo)
            assertTrue("bob" in repo)
        }
}

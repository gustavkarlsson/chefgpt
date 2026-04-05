package se.gustavkarlsson.chefgpt

import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class LoginSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `credentials missing`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/login")
        }

    @Test
    fun `wrong username`() =
        snapshotTestApplication(snapshotContext) { client ->
            registerUser()

            client.post("/login") {
                basicAuth("wronguser", VALID_PASSWORD)
            }
        }

    @Test
    fun `wrong password`() =
        snapshotTestApplication(snapshotContext) { client ->
            registerUser()

            client.post("/login") {
                basicAuth(VALID_USERNAME, "WrongPass1!")
            }
        }

    @Test
    fun `successful login`() =
        snapshotTestApplication(snapshotContext) { client ->
            registerUser()

            client.post("/login") {
                basicAuth(VALID_USERNAME, VALID_PASSWORD)
            }
        }
}

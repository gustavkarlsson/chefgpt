package se.gustavkarlsson.chefgpt

import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class RegisterSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `credentials missing`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register")
        }

    @Test
    fun `username too short`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register") {
                basicAuth("ab", VALID_PASSWORD)
            }
        }

    @Test
    fun `username starts with digit`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register") {
                basicAuth("1user", VALID_PASSWORD)
            }
        }

    @Test
    fun `password too short`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register") {
                basicAuth(VALID_USERNAME, "Ab1!")
            }
        }

    @Test
    fun `password not complex enough`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register") {
                basicAuth(VALID_USERNAME, "alllowercase")
            }
        }

    @Test
    fun `successful registration`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.post("/register") {
                basicAuth(VALID_USERNAME, VALID_PASSWORD)
            }
        }

    @Test
    fun `username taken`() =
        snapshotTestApplication(snapshotContext) { client ->
            registerUser()

            client.post("/register") {
                basicAuth(VALID_USERNAME, VALID_PASSWORD)
            }
        }
}

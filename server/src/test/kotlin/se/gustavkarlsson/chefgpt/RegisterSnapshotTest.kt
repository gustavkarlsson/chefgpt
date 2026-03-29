package se.gustavkarlsson.chefgpt

import io.ktor.client.request.post
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension
import se.gustavkarlsson.slapshot.ktor3.SnapshotTesting

@ExtendWith(SnapshotExtension::class)
class RegisterSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `credentials missing`() =
        testApplication {
            application { testModule() }
            val client =
                createClient {
                    install(SnapshotTesting(snapshotContext))
                }

            client.post("/register")
        }
}

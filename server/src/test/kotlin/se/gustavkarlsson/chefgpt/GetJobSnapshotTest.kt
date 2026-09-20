package se.gustavkarlsson.chefgpt

import io.ktor.client.request.get
import io.ktor.client.request.header
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.JobId
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

private val FAKE_JOB_ID = JobId.parse("33333333-3333-3333-3333-333333333333")

@ExtendWith(SnapshotExtension::class)
class GetJobSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun unauthenticated() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get("/jobs/$FAKE_JOB_ID")
        }

    @Test
    fun `invalid job id`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/jobs/not-a-uuid") {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `job not found`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get("/jobs/$FAKE_JOB_ID") {
                header("Session-Id", sessionId)
            }
        }
}

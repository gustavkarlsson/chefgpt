package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInfo
import se.gustavkarlsson.slapshot.core.Serializer
import se.gustavkarlsson.slapshot.core.SnapshotAction
import se.gustavkarlsson.slapshot.core.SnapshotContext
import se.gustavkarlsson.slapshot.core.SnapshotFileResolver
import se.gustavkarlsson.slapshot.core.Snapshotter
import se.gustavkarlsson.slapshot.core.Tester
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.ktor3.SnapshotTesting

private val UUID_REGEX =
    Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
private const val UUID_PLACEHOLDER = "00000000-0000-0000-0000-000000000000"

private val HEX_SESSION_REGEX = Regex("[0-9a-f]{32}")
private const val HEX_SESSION_PLACEHOLDER = "00000000000000000000000000000000"

fun snapshotTestApplication(
    snapshotContext: JUnit5SnapshotContext,
    applicationConfig: Application.() -> Unit = { testModule() },
    clientConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {},
    test: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
) = testApplication {
    application(applicationConfig)
    val client =
        createClient {
            install(ContentNegotiation) { json(Json) }
            install(SnapshotTesting(snapshotContext.sanitizing()))
            clientConfig()
        }
    test(client)
}

private fun JUnit5SnapshotContext.sanitizing(): SanitizingSnapshotContext =
    SanitizingSnapshotContext(this) { json ->
        json
            .replace(UUID_REGEX, UUID_PLACEHOLDER)
            .replace(HEX_SESSION_REGEX, HEX_SESSION_PLACEHOLDER)
    }

private class SanitizingSnapshotContext(
    private val delegate: SnapshotContext<TestInfo>,
    private val sanitize: (String) -> String,
) : SnapshotContext<TestInfo> {
    override fun <T> createSnapshotter(
        serializer: Serializer<T>,
        tester: Tester<T>,
        overrideSnapshotFileResolver: SnapshotFileResolver<TestInfo>?,
        overrideAction: SnapshotAction?,
    ): Snapshotter<T> {
        val realSnapshotter =
            delegate.createSnapshotter(
                serializer,
                tester,
                overrideSnapshotFileResolver,
                overrideAction,
            )
        @Suppress("UNCHECKED_CAST")
        return SanitizingSnapshotter(realSnapshotter) { data ->
            sanitize(data as String) as T
        }
    }
}

private class SanitizingSnapshotter<T>(
    private val delegate: Snapshotter<T>,
    private val sanitize: (T) -> T,
) : Snapshotter<T> {
    override fun snapshot(data: T) {
        delegate.snapshot(sanitize(data))
    }
}

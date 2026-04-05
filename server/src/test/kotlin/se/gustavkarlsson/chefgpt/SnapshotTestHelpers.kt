package se.gustavkarlsson.chefgpt

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.TestInfo
import org.koin.core.module.Module
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

private val ISO_INSTANT_REGEX =
    Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")
private const val ISO_INSTANT_PLACEHOLDER = "2000-01-01T00:00:00Z"

private val CONTENT_LENGTH_REGEX = Regex(""""Content-Length": "\d+"""")
private const val CONTENT_LENGTH_PLACEHOLDER = """"Content-Length": "0""""

private val testConfig =
    MapApplicationConfig(
        "bindings.storage" to "memory",
        "bindings.agent" to "fake",
        "bindings.recipes" to "fake",
        "bindings.imageUploader" to "fake",
        "anthropic.apiKey" to "test-key",
    )

fun snapshotTestApplication(
    snapshotContext: JUnit5SnapshotContext,
    applicationConfig: ApplicationConfig = testConfig,
    extraKoinModules: List<Module> = emptyList(),
    clientConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {},
    test: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
) = testApplication {
    environment { config = applicationConfig }
    application { module(extraKoinModules) }
    val client =
        createClient {
            install(ContentNegotiation) { json() }
            install(SnapshotTesting(snapshotContext.sanitizing()))
            clientConfig()
        }
    test(client)
}

private fun JUnit5SnapshotContext.sanitizing(): SanitizingSnapshotContext =
    SanitizingSnapshotContext(this) { json ->
        json
            .replace(ISO_INSTANT_REGEX, ISO_INSTANT_PLACEHOLDER)
            .replace(UUID_REGEX, UUID_PLACEHOLDER)
            .replace(HEX_SESSION_REGEX, HEX_SESSION_PLACEHOLDER)
            .replace(CONTENT_LENGTH_REGEX, CONTENT_LENGTH_PLACEHOLDER)
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
        return SanitizingSnapshotter(realSnapshotter) { data ->
            check(data is String) {
                "SanitizingSnapshotter only works with String. Not ${data?.javaClass?.simpleName}"
            }
            @Suppress("UNCHECKED_CAST")
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

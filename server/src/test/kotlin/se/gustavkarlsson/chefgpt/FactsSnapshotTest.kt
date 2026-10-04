package se.gustavkarlsson.chefgpt

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiTemperatureUnit
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiVolumeUnits
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiWeightUnits
import se.gustavkarlsson.chefgpt.api.facts.v1.FACTS_V1_PATH
import se.gustavkarlsson.slapshot.junit5.JUnit5SnapshotContext
import se.gustavkarlsson.slapshot.junit5.SnapshotExtension

@ExtendWith(SnapshotExtension::class)
class FactsSnapshotTest {
    private lateinit var snapshotContext: JUnit5SnapshotContext

    @BeforeEach
    fun initSnapshotContext(snapshotContext: JUnit5SnapshotContext) {
        this.snapshotContext = snapshotContext
    }

    @Test
    fun `get facts returns unknown for a new user`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.get(FACTS_V1_PATH) {
                header("Session-Id", sessionId)
            }
        }

    @Test
    fun `put facts returns the saved facts`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.put(FACTS_V1_PATH) {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(
                    ApiUserFacts(
                        preferredName = "Chef",
                        temperatureUnit = ApiTemperatureUnit.Celsius,
                        weightUnits = ApiWeightUnits.Metric,
                        volumeUnits = ApiVolumeUnits.Metric,
                        dietary = setOf("vegetarian", "gluten-free"),
                    ),
                )
            }
        }

    @Test
    fun `put facts with empty dietary means none`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.put(FACTS_V1_PATH) {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(
                    ApiUserFacts(
                        preferredName = null,
                        temperatureUnit = ApiTemperatureUnit.Fahrenheit,
                        weightUnits = ApiWeightUnits.UsImperial,
                        volumeUnits = ApiVolumeUnits.UsCustomary,
                        dietary = emptySet(),
                    ),
                )
            }
        }

    // TODO Fix a better error message that doesn't leak the API type qualified name
    @Test
    fun `put facts rejects an unrecognized unit`() =
        snapshotTestApplication(snapshotContext) { client ->
            val sessionId = registerUser()

            client.put(FACTS_V1_PATH) {
                header("Session-Id", sessionId)
                contentType(ContentType.Application.Json)
                setBody(TextContent("""{"weightUnits": "bananas"}""", ContentType.Application.Json))
            }
        }

    @Test
    fun `get facts requires authentication`() =
        snapshotTestApplication(snapshotContext) { client ->
            client.get(FACTS_V1_PATH)
        }
}

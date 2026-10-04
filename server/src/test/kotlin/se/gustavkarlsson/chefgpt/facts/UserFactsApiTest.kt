package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.api.facts.v1.ApiTemperatureUnit
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiVolumeUnits
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiWeightUnits
import se.gustavkarlsson.chefgpt.facts.toApi
import se.gustavkarlsson.chefgpt.facts.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class UserFactsApiTest {
    @Test
    fun `toApi maps enums to api enums`() {
        val facts =
            UserFacts(
                preferredName = "Gustav",
                temperatureUnit = TemperatureUnit.Celsius,
                weightUnits = WeightUnits.Metric,
                volumeUnits = VolumeUnits.UsCustomary,
                dietary = setOf("vegan"),
            )

        val api = facts.toApi()

        assertEquals(
            ApiUserFacts(
                preferredName = "Gustav",
                temperatureUnit = ApiTemperatureUnit.Celsius,
                weightUnits = ApiWeightUnits.Metric,
                volumeUnits = ApiVolumeUnits.UsCustomary,
                dietary = setOf("vegan"),
            ),
            api,
        )
    }

    @Test
    fun `toApi maps empty facts to null`() {
        val api = UserFacts.Empty.toApi()

        assertEquals(ApiUserFacts(null, null, null, null, null), api)
    }

    @Test
    fun `toFactsOrNull maps api enums to domain enums`() {
        val facts =
            ApiUserFacts(
                preferredName = "Chef",
                temperatureUnit = ApiTemperatureUnit.Celsius,
                weightUnits = ApiWeightUnits.Metric,
                volumeUnits = ApiVolumeUnits.UsCustomary,
                dietary = emptySet(),
            ).toDomain()

        assertEquals(
            UserFacts("Chef", TemperatureUnit.Celsius, WeightUnits.Metric, VolumeUnits.UsCustomary, emptySet()),
            facts,
        )
    }

    @Test
    fun `toFactsOrNull keeps null dietary unknown and empty dietary none`() {
        val unknown = ApiUserFacts(null, null, null, null, null).toDomain()
        val none =
            ApiUserFacts(
                null,
                ApiTemperatureUnit.Celsius,
                ApiWeightUnits.Metric,
                ApiVolumeUnits.Metric,
                emptySet(),
            ).toDomain()

        assertEquals(null, unknown.dietary)
        assertEquals(emptySet(), none.dietary)
    }
}

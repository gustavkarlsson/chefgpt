package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.db.SelectByUserId
import se.gustavkarlsson.chefgpt.postgres.DatabaseAccess
import kotlin.uuid.toJavaUuid

class PostgresFactRepository(
    private val db: DatabaseAccess,
) : FactRepository {
    override suspend fun getFacts(userId: UserId): UserFacts =
        db.use {
            factQueries
                .selectByUserId(userId.value.toJavaUuid())
                .executeAsOneOrNull()
                ?.toDomain()
        } ?: UserFacts.Empty

    override suspend fun updateFacts(
        userId: UserId,
        update: UserFactsUpdate,
    ): UserFacts {
        val updated = getFacts(userId).applyUpdate(update)
        write(userId, updated)
        return updated
    }

    override suspend fun replaceFacts(
        userId: UserId,
        facts: UserFacts,
    ): UserFacts {
        write(userId, facts)
        return facts
    }

    private suspend fun write(
        userId: UserId,
        facts: UserFacts,
    ) {
        db.use {
            factQueries.upsert(
                user_id = userId.value.toJavaUuid(),
                preferred_name = facts.preferredName,
                temperature_unit = facts.temperatureUnit?.toColumnValue(),
                weight_units = facts.weightUnits?.toColumnValue(),
                volume_units = facts.volumeUnits?.toColumnValue(),
                dietary = facts.dietary?.toDietaryString(),
            )
        }
    }
}

private fun TemperatureUnit.toColumnValue(): String =
    when (this) {
        TemperatureUnit.Celsius -> "celsius"
        TemperatureUnit.Fahrenheit -> "fahrenheit"
    }

private fun WeightUnits.toColumnValue(): String =
    when (this) {
        WeightUnits.Metric -> "metric"
        WeightUnits.UsImperial -> "us_imperial"
    }

private fun VolumeUnits.toColumnValue(): String =
    when (this) {
        VolumeUnits.Metric -> "metric"
        VolumeUnits.UsCustomary -> "us_customary"
    }

private fun Set<String>.toDietaryString(): String =
    map { it.trim() }
        .filterNot { it.isEmpty() }
        .sorted()
        .joinToString(DIETARY_SEPARATOR)

private fun SelectByUserId.toDomain(): UserFacts =
    UserFacts(
        preferredName = preferred_name,
        temperatureUnit = temperature_unit?.toTemperatureUnitOrNull(),
        weightUnits = weight_units?.toWeightUnitsOrNull(),
        volumeUnits = volume_units?.toVolumeUnitsOrNull(),
        dietary = dietary?.toDietarySet(),
    )

private fun String.toTemperatureUnitOrNull(): TemperatureUnit? =
    when (this) {
        "celsius" -> TemperatureUnit.Celsius
        "fahrenheit" -> TemperatureUnit.Fahrenheit
        else -> null
    }

private fun String.toWeightUnitsOrNull(): WeightUnits? =
    when (this) {
        "metric" -> WeightUnits.Metric
        "us_imperial" -> WeightUnits.UsImperial
        else -> null
    }

private fun String.toVolumeUnitsOrNull(): VolumeUnits? =
    when (this) {
        "metric" -> VolumeUnits.Metric
        "us_customary" -> VolumeUnits.UsCustomary
        else -> null
    }

private fun String.toDietarySet(): Set<String> =
    split(DIETARY_SEPARATOR)
        .map { it.trim() }
        .filterNot { it.isEmpty() }
        .toSet()

private const val DIETARY_SEPARATOR = ","

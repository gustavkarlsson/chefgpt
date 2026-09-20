package se.gustavkarlsson.chefgpt.facts

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.auth.UserId

/**
 * Tools the agent can use to remember and update facts about the user. Scoped to a single
 * [userId], so the agent can only ever touch the facts of the user it is talking to.
 */
@Suppress("unused")
class FactRepositoryTools(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember what the user wants to be called. Call this when they tell you their name or how to address them.",
    )
    suspend fun setPreferredName(
        @LLMDescription("What to call the user, e.g. 'Gustav'.")
        name: String,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(preferredName = name))

    @Tool
    @LLMDescription("Remember the user's unit system. Call this when they state whether they use metric or imperial.")
    suspend fun setUnitSystem(
        @LLMDescription("The unit system to use: METRIC or IMPERIAL.")
        unitSystem: UnitSystem,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(unitSystem = unitSystem))

    @Tool
    @LLMDescription(
        "Remember how the user prefers ingredient amounts to be expressed. Call this when they state whether they prefer weight or volume.",
    )
    suspend fun setMeasurement(
        @LLMDescription("How to express ingredient amounts: WEIGHT or VOLUME.")
        measurement: Measurement,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(measurement = measurement))

    @Tool
    @LLMDescription(
        "Remember the user's temperature unit. Call this when they state whether they use celsius or fahrenheit.",
    )
    suspend fun setTemperature(
        @LLMDescription("The temperature unit to use: CELSIUS or FAHRENHEIT.")
        temperature: TemperatureUnit,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(temperature = temperature))

    @Tool
    @LLMDescription(
        "Remember dietary restrictions the user has stated, e.g. vegetarian, vegan, gluten-free, dairy-free. " +
            "Only call this for restrictions the user says apply to themselves in general — never for a one-off " +
            "request such as 'make me a vegetarian meal tonight'. When a new restriction makes an existing one " +
            "redundant (vegan makes vegetarian redundant), also remove the redundant one.",
    )
    suspend fun addDietaryRestrictions(
        @LLMDescription("The restrictions to remember, as short lowercase phrases, e.g. ['vegetarian', 'gluten-free'].")
        restrictions: List<String>,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(addDietary = restrictions.toSet()))

    @Tool
    @LLMDescription(
        "Forget dietary restrictions the user no longer has, e.g. when they say they eat meat again. " +
            "Removing a restriction that was not present changes nothing.",
    )
    suspend fun removeDietaryRestrictions(
        @LLMDescription("The restrictions to forget, as short lowercase phrases, e.g. ['vegetarian'].")
        restrictions: List<String>,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(removeDietary = restrictions.toSet()))
}

fun FactRepository.toTools(userId: UserId): ToolSet = FactRepositoryTools(this, userId)

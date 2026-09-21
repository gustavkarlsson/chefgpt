package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.Measurement
import se.gustavkarlsson.chefgpt.facts.TemperatureUnit
import se.gustavkarlsson.chefgpt.facts.UnitSystem
import se.gustavkarlsson.chefgpt.facts.UserFacts
import se.gustavkarlsson.chefgpt.facts.UserFactsUpdate

class SetPreferredNameTool(
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
}

class SetUnitSystemTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Remember the user's unit system. Call this when they state whether they use metric or imperial.")
    suspend fun setUnitSystem(
        @LLMDescription("The unit system to use: Metric or Imperial.")
        unitSystem: UnitSystem,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(unitSystem = unitSystem))
}

class SetMeasurementTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember how the user prefers ingredient amounts to be expressed. Call this when they state whether they prefer weight or volume.",
    )
    suspend fun setMeasurement(
        @LLMDescription("How to express ingredient amounts: Weight or Volume.")
        measurement: Measurement,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(measurement = measurement))
}

class SetTemperatureTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember the user's temperature unit. Call this when they state whether they use Celsius or Fahrenheit.",
    )
    suspend fun setTemperature(
        @LLMDescription("The temperature unit to use: Celsius or Fahrenheit.")
        temperature: TemperatureUnit,
    ): UserFacts = repository.updateFacts(userId, UserFactsUpdate(temperature = temperature))
}

class AddDietaryRestrictionsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
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
}

class RemoveDietaryRestrictionsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
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

class GetPreferredNameTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get what the user wants to be called, or null if it is unknown.")
    suspend fun getPreferredName(): String? = repository.getFacts(userId).preferredName
}

class GetUnitSystemTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get the user's unit system (Metric or Imperial), or null if it is unknown.")
    suspend fun getUnitSystem(): UnitSystem? = repository.getFacts(userId).unitSystem
}

class GetMeasurementTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Get how the user prefers ingredient amounts to be expressed (Weight or Volume), or null if it is unknown.",
    )
    suspend fun getMeasurement(): Measurement? = repository.getFacts(userId).measurement
}

class GetTemperatureTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get the user's temperature unit (Celsius or Fahrenheit), or null if it is unknown.")
    suspend fun getTemperature(): TemperatureUnit? = repository.getFacts(userId).temperature
}

class GetDietaryRestrictionsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Get the user's dietary restrictions (e.g. vegetarian, vegan). " +
            "null means unknown, and empty means they have said they have none.",
    )
    suspend fun getDietaryRestrictions(): Set<String>? = repository.getFacts(userId).dietary
}

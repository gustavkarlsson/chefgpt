package se.gustavkarlsson.chefgpt.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolTemperatureUnit
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolUserFacts
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolVolumeUnits
import se.gustavkarlsson.chefgpt.agent.tools.models.ToolWeightUnits
import se.gustavkarlsson.chefgpt.agent.tools.models.toDomain
import se.gustavkarlsson.chefgpt.agent.tools.models.toTool
import se.gustavkarlsson.chefgpt.auth.UserId
import se.gustavkarlsson.chefgpt.facts.FactRepository
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
    ): ToolUserFacts = repository.updateFacts(userId, UserFactsUpdate(preferredName = name)).toTool()
}

class SetWeightUnitsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember the user's weight units. Call this when they state whether they measure weight in metric or US imperial units.",
    )
    suspend fun setWeightUnits(
        @LLMDescription("The weight units to use: Metric or UsImperial.")
        weightUnits: ToolWeightUnits,
    ): ToolUserFacts = repository.updateFacts(userId, UserFactsUpdate(weightUnits = weightUnits.toDomain())).toTool()
}

class SetVolumeUnitsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember the user's volume units. Call this when they state whether they measure volume in metric or US customary units.",
    )
    suspend fun setVolumeUnits(
        @LLMDescription("The volume units to use: Metric or UsCustomary.")
        volumeUnits: ToolVolumeUnits,
    ): ToolUserFacts = repository.updateFacts(userId, UserFactsUpdate(volumeUnits = volumeUnits.toDomain())).toTool()
}

class SetTemperatureUnitTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription(
        "Remember the user's temperature unit. Call this when they state whether they use Celsius or Fahrenheit.",
    )
    suspend fun setTemperatureUnit(
        @LLMDescription("The temperature unit to use: Celsius or Fahrenheit.")
        temperatureUnit: ToolTemperatureUnit,
    ): ToolUserFacts =
        repository.updateFacts(userId, UserFactsUpdate(temperatureUnit = temperatureUnit.toDomain())).toTool()
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
    ): ToolUserFacts = repository.updateFacts(userId, UserFactsUpdate(addDietary = restrictions.toSet())).toTool()
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
    ): ToolUserFacts = repository.updateFacts(userId, UserFactsUpdate(removeDietary = restrictions.toSet())).toTool()
}

class GetPreferredNameTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get what the user wants to be called, or null if it is unknown.")
    suspend fun getPreferredName(): String? = repository.getFacts(userId).preferredName
}

class GetWeightUnitsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get the user's weight units (Metric or UsImperial), or null if it is unknown.")
    suspend fun getWeightUnits(): ToolWeightUnits? = repository.getFacts(userId).weightUnits?.toTool()
}

class GetVolumeUnitsTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get the user's volume units (Metric or UsCustomary), or null if it is unknown.")
    suspend fun getVolumeUnits(): ToolVolumeUnits? = repository.getFacts(userId).volumeUnits?.toTool()
}

class GetTemperatureUnitTool(
    private val repository: FactRepository,
    private val userId: UserId,
) : ToolSet {
    @Tool
    @LLMDescription("Get the user's temperature unit (Celsius or Fahrenheit), or null if it is unknown.")
    suspend fun getTemperatureUnit(): ToolTemperatureUnit? = repository.getFacts(userId).temperatureUnit?.toTool()
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

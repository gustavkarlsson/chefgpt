package se.gustavkarlsson.chefgpt.ai

import ai.koog.prompt.executor.clients.LLModelDefinitions
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.clients.modelsById
import ai.koog.prompt.llm.LLModel
import io.ktor.server.config.ApplicationConfig

enum class AiProvider(
    val id: String,
    private val models: LLModelDefinitions,
) {
    Anthropic("anthropic", AnthropicModels),
    DeepSeek("deepseek", DeepSeekModels),
    ;

    fun modelFromId(modelId: String): LLModel =
        models.modelsById()[modelId]
            ?: error("Unknown $id model: '$modelId'. Available: ${models.modelsById().keys}")

    companion object {
        fun fromId(id: String): AiProvider =
            entries.firstOrNull { it.id == id }
                ?: error("Unsupported AI provider: '$id'. Supported: ${entries.joinToString { it.id }}")
    }
}

data class AgentModel(
    val provider: AiProvider,
    val model: LLModel,
)

class AiConfig(
    val providers: Map<AiProvider, AiProviderApiKey>,
    val modelsByAgentId: Map<String, AgentModel>,
)

fun ApplicationConfig.loadAiConfig(): AiConfig {
    val providers =
        childKeys("ai.providers").associate { name ->
            val provider = AiProvider.fromId(name)
            val apiKey = property("ai.providers.$name.apiKey").getString()
            provider to AiProviderApiKey(apiKey)
        }
    val agentModels =
        childKeys("ai.agents").associateWith { name ->
            val aiProviderId = property("ai.agents.$name.provider").getString()
            val provider = AiProvider.fromId(aiProviderId)
            val aiModelId = property("ai.agents.$name.model").getString()
            val model = provider.modelFromId(aiModelId)
            AgentModel(provider, model)
        }
    return AiConfig(providers, agentModels)
}

private fun ApplicationConfig.childKeys(path: String): List<String> {
    // TODO Verify this
    propertyOrNull(path) ?: return emptyList() // Check if path exists
    return config(path).keys().map { key -> key.substringBefore('.') }.distinct()
}

package se.gustavkarlsson.chefgpt.ai

import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.modelsById
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

/**
 * Anthropic models newer than the ones the installed koog version defines.
 * Delete this file once a koog release ships definitions for them.
 */
private val anthropicAgentCapabilities =
    listOf(
        LLMCapability.Temperature,
        LLMCapability.Tools,
        LLMCapability.ToolChoice,
        LLMCapability.Vision.Image,
        LLMCapability.Document,
        LLMCapability.Completion,
        LLMCapability.Schema.JSON.Basic,
        LLMCapability.Schema.JSON.Standard,
        LLMCapability.Thinking,
        LLMCapability.PromptCaching,
    )

private val sonnet5_5 =
    LLModel(
        provider = LLMProvider.Anthropic,
        id = "claude-sonnet-5-5",
        capabilities = anthropicAgentCapabilities,
        contextLength = 1_000_000,
        maxOutputTokens = 128_000,
    )

private val haiku5_5 =
    LLModel(
        provider = LLMProvider.Anthropic,
        id = "claude-haiku-5-5",
        capabilities = anthropicAgentCapabilities,
        contextLength = 1_000_000,
        maxOutputTokens = 128_000,
    )

fun registerCustomAnthropicModels() {
    if (AnthropicModels.modelsById().containsKey(sonnet5_5.id)) return
    AnthropicModels.addCustomModel(sonnet5_5)
    AnthropicModels.addCustomModel(haiku5_5)
}

/**
 * [AnthropicClientSettings.modelVersionsMap] is keyed by model instances and defaults to koog's
 * internal map, which has no entries for the custom models. Extend it with entries for them so
 * requests using them resolve.
 */
fun anthropicClientSettings(): AnthropicClientSettings {
    val defaultSettings = AnthropicClientSettings()
    val modelVersions =
        defaultSettings.modelVersionsMap + mapOf(sonnet5_5 to sonnet5_5.id, haiku5_5 to haiku5_5.id)
    return AnthropicClientSettings(modelVersionsMap = modelVersions)
}

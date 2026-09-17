package se.gustavkarlsson.chefgpt.ai

import io.ktor.server.config.MapApplicationConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AiConfigTest {
    @Test
    fun `loads providers from config`() {
        val config =
            MapApplicationConfig(
                "ai.providers.anthropic.apiKey" to "anthropic-key",
                "ai.providers.deepseek.apiKey" to "deepseek-key",
            )

        val aiConfig = config.loadAiConfig()

        assertEquals(
            mapOf(
                AiProvider.Anthropic to AiProviderApiKey("anthropic-key"),
                AiProvider.DeepSeek to AiProviderApiKey("deepseek-key"),
            ),
            aiConfig.providers,
        )
    }

    @Test
    fun `loads agent models from config`() {
        val config =
            MapApplicationConfig(
                "ai.agents.chat.provider" to "anthropic",
                "ai.agents.chat.model" to "claude-sonnet-4-6",
                "ai.agents.ingredientScan.provider" to "deepseek",
                "ai.agents.ingredientScan.model" to "deepseek-v4-flash",
            )

        val aiConfig = config.loadAiConfig()

        assertEquals(
            mapOf("chat" to "claude-sonnet-4-6", "ingredientScan" to "deepseek-v4-flash"),
            aiConfig.modelsByAgentId.mapValues { it.value.model.id },
        )
        assertEquals(
            mapOf("chat" to AiProvider.Anthropic, "ingredientScan" to AiProvider.DeepSeek),
            aiConfig.modelsByAgentId.mapValues { it.value.provider },
        )
    }

    @Test
    fun `throws when provider is unknown`() {
        assertFailsWith<IllegalStateException> {
            AiProvider.fromId("openai")
        }
    }

    @Test
    fun `throws when model is unknown`() {
        assertFailsWith<IllegalStateException> {
            AiProvider.Anthropic.modelFromId("does-not-exist")
        }
    }
}

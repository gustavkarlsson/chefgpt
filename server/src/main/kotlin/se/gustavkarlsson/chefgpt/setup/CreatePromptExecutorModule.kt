package se.gustavkarlsson.chefgpt.setup

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import org.koin.core.module.Module
import org.koin.dsl.module
import se.gustavkarlsson.chefgpt.ai.AiProvider
import se.gustavkarlsson.chefgpt.ai.anthropicClientSettings
import se.gustavkarlsson.chefgpt.ai.loadAiConfig
import se.gustavkarlsson.chefgpt.ai.registerCustomAnthropicModels

fun Application.createPromptExecutorModule(): Module {
    // Must run before loadAiConfig() validates the configured model IDs against the registry.
    registerCustomAnthropicModels()
    return module {
        val aiConfig = environment.config.loadAiConfig()
        single<PromptExecutor> {
            val llmClients =
                buildMap {
                    for ((provider, apiKey) in aiConfig.providers) {
                        when (provider) {
                            AiProvider.Anthropic -> {
                                put(
                                    LLMProvider.Anthropic,
                                    AnthropicLLMClient(
                                        apiKey = apiKey.value,
                                        settings = anthropicClientSettings(),
                                        httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()),
                                    ),
                                )
                            }

                            AiProvider.DeepSeek -> {
                                put(
                                    LLMProvider.DeepSeek,
                                    DeepSeekLLMClient(
                                        apiKey = apiKey.value,
                                        httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()),
                                    ),
                                )
                            }
                        }
                    }
                }
            MultiLLMPromptExecutor(llmClients)
        }
    }
}

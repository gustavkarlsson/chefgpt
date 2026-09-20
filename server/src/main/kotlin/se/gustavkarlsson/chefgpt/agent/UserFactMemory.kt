package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.feature.AIAgentGraphFeature
import ai.koog.agents.core.feature.config.FeatureConfig
import ai.koog.agents.core.feature.pipeline.AIAgentGraphPipeline
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.toPromptText

/**
 * Injects the current user's facts into the prompt before each run, so the agent knows what it
 * remembers about them and can see which facts are still unknown (and therefore worth asking about).
 */
class UserFactMemory {
    class Config : FeatureConfig() {
        var factRepository: FactRepository? = null
    }

    companion object Feature :
        AIAgentGraphFeature<Config, UserFactMemory> {
        override val key: AIAgentStorageKey<UserFactMemory> =
            createStorageKey("agents-features-user-fact-memory")

        override fun createInitialConfig(agentConfig: AIAgentConfig): Config = Config()

        override fun install(
            config: Config,
            pipeline: AIAgentGraphPipeline,
        ): UserFactMemory {
            val factRepository =
                requireNotNull(config.factRepository) {
                    "factRepository must be set during plugin installation"
                }
            pipeline.interceptStrategyStarting(this) {
                it.context.injectFacts(factRepository)
            }
            return UserFactMemory()
        }

        private suspend fun AIAgentContext.injectFacts(repository: FactRepository) {
            val facts = repository.getFacts(agentInput())
            llm.writeSession {
                appendPrompt {
                    system(facts.toPromptText())
                }
            }
        }
    }
}

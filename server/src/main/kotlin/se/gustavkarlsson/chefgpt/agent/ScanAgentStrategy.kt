package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.AIAgentFunctionalStrategy
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.prompt.message.MessagePart

/**
 * Runs the scan agent's LLM/tool loop and returns what the tools added.
 */
fun scanStrategy(
    name: String,
    extract: (MessagePart.Tool.Call) -> List<String>,
): AIAgentFunctionalStrategy<String, List<String>> =
    functionalStrategy(name) { input ->
        var message = requestLLM(input)
        val outputs = mutableListOf<String>()
        repeat(config.maxAgentIterations) {
            val toolCalls = getToolCalls(message)
            if (toolCalls.isEmpty()) {
                return@repeat
            }
            val toolResults = executeTools(toolCalls)
            for (call in toolCalls) {
                outputs += extract(call)
            }
            message = sendToolResults(toolResults)
        }
        outputs
    }

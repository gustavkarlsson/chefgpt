package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeDoNothing
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResults
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.prompt.message.Message

fun findRecipeStrategy(): AIAgentGraphStrategy<Unit, Unit> =
    strategy("find-recipe") {
        val nodeExecuteLLM by nodeExecuteLLM("executeLLM")
        val response by nodeDoNothing<Message.Assistant>("response")
        val nodeExecuteTool by nodeExecuteTools("executeTool")
        val nodeLLMSendToolResult by nodeLLMSendToolResults("llmSendToolResult")

        edge(nodeStart forwardTo nodeExecuteLLM)
        edge(nodeExecuteLLM forwardTo response)

        // Tool calls are executed and the results fed back to the LLM
        edge(response forwardTo nodeExecuteTool onToolCalls { true })
        edge(nodeExecuteTool forwardTo nodeLLMSendToolResult)
        edge(nodeLLMSendToolResult forwardTo response)

        // A plain-text assistant message means we are done
        edge(response forwardTo nodeFinish onTextMessage { true } transformed {})
    }

private fun nodeExecuteLLM(name: String) =
    node<Unit, Message.Assistant>(name) {
        llm
            .writeSession {
                // Message should have already been appended to history when this runs
                requestLLM()
            }
    }

// Print a Markdown mermaid diagram of the strategy
private fun main() {
    val markdown =
        buildString {
            appendLine("```mermaid")
            val strategy = findRecipeStrategy()
            appendLine(strategy.asMermaidDiagram())
            appendLine("```")
        }
    println(markdown)
}

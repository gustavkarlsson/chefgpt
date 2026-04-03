package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeDoNothing
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onReasoningMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.message.Message

// FIXME Error from anthropic:
//  {
//    "type": "error",
//    "error": {
//        "type": "invalid_request_error",
//        "message": "messages.4: `tool_use` ids were found without `tool_result` blocks immediately after: toolu_01GyK51VV86vNtWFFv6fN3hy. Each `tool_use` block must have a corresponding `tool_result` block in the next message."
//    },
//    "request_id": "req_011CZhdMYrcePZtfXgPVrt1H"
// }

fun findRecipeStrategy(): AIAgentGraphStrategy<Unit, Unit> =
    strategy("find-recipe") {
        val nodeExecuteLLM by nodeExecuteLLM("executeLLM")
        val response by nodeDoNothing<Message.Response>("response")
        val nodeExecuteTool by nodeExecuteTool("executeTool") // Seems to append the message by itself?
        val nodeLLMSendToolResult by nodeLLMSendToolResult("llmSendToolResult")
        val nodeSendReasoningBackToLLM by nodeSendReasoningBackToLLM("sendReasoningBackToLLM")

        edge(nodeStart forwardTo nodeExecuteLLM)
        edge(nodeExecuteLLM forwardTo response)

        // Reasoning loops around as long as it's reasoning
        edge(response forwardTo nodeSendReasoningBackToLLM onReasoningMessage { true })
        edge(nodeSendReasoningBackToLLM forwardTo response)

        // Tool calls are evaluated by the LLM
        edge(response forwardTo nodeExecuteTool onToolCall { true })
        edge(nodeExecuteTool forwardTo nodeLLMSendToolResult)
        edge(nodeLLMSendToolResult forwardTo response)

        // Assistant message means we are done
        edge(response forwardTo nodeFinish onAssistantMessage { true } transformed {})
    }

private fun nodeExecuteLLM(name: String) =
    node<Unit, Message.Response>(name) { message ->
        llm
            .writeSession {
                // Message should have already been appended to history when this runs
                requestLLM()
            }
    }

private fun nodeSendReasoningBackToLLM(name: String) =
    node<Message.Reasoning, Message.Response>(name) { reasoning ->
        llm
            .writeSession {
                appendPrompt {
                    message(reasoning)
                }
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

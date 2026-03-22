package se.gustavkarlsson.chefgpt.agent

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeDoNothing
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onReasoningMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.environment.result
import ai.koog.prompt.message.Message
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.UserMessage
import se.gustavkarlsson.chefgpt.chats.toEvent
import se.gustavkarlsson.chefgpt.chats.toEventOrNull

fun findRecipeStrategy(emitEvent: suspend (Event) -> Unit): AIAgentGraphStrategy<UserMessage, Unit> =
    strategy("find-recipe") {
        val nodeSendUserMessageToLLM by nodeSendUserMessageToLLM("sendUserMessageToLLM", emitEvent)
        val responses by nodeDoNothing<Message.Response>("responses")
        val nodeCallTools by nodeCallTools("callTools", emitEvent)
        val nodeSendToolResultToLLM by nodeSendToolResultToLLM("sendToolResultToLLM", emitEvent)
        val nodeSendReasoningBackToLLM by nodeSendReasoningBackToLLM("sendReasoningBackToLLM", emitEvent)

        edge(nodeStart forwardTo nodeSendUserMessageToLLM)
        edge(nodeSendUserMessageToLLM forwardTo responses)

        // Reasoning loops around as long as it's reasoning
        edge(responses forwardTo nodeSendReasoningBackToLLM onReasoningMessage { true })
        edge(nodeSendReasoningBackToLLM forwardTo responses)

        // Tool calls are evaluated by the LLM
        edge(responses forwardTo nodeCallTools onToolCall { true })
        edge(nodeCallTools forwardTo nodeSendToolResultToLLM)
        edge(nodeSendToolResultToLLM forwardTo responses)

        // Assistant message means we are done
        edge(responses forwardTo nodeFinish onAssistantMessage { true } transformed {})
    }

private fun nodeSendUserMessageToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<UserMessage, Message.Response>(name) { message ->
    emitEvent(message)
    llm
        .writeSession {
            appendPrompt {
                user {
                    message.text?.let { text(it) }
                    message.imageUrl?.let { image(it.value) }
                }
            }
            requestLLM()
        }.also { emitEvent(it.toEvent()) }
}

private fun nodeCallTools(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Tool.Call, Message.Tool.Result>(name) { toolCall ->
    llm
        .writeSession {
            val result = environment.executeTool(toolCall)
            // Tool calls don't get automatically added to the prompt. So we do it manually.
            appendPrompt {
                tool {
                    result(result)
                }
            }
            result.toMessage()
        }.also { message -> message.toEventOrNull()?.let { emitEvent(it) } }
}

private fun nodeSendToolResultToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Tool.Result, Message.Response>(name) { toolResult ->
    llm
        .writeSession {
            requestLLM()
        }.also { emitEvent(it.toEvent()) }
}

private fun nodeSendReasoningBackToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Reasoning, Message.Response>(name) { reasoning ->
    llm
        .writeSession {
            requestLLM()
        }.also { emitEvent(it.toEvent()) }
}

// Print a Markdown mermaid diagram of the strategy
private fun main() {
    val markdown =
        buildString {
            appendLine("```mermaid")
            val strategy = findRecipeStrategy {}
            appendLine(strategy.asMermaidDiagram())
            appendLine("```")
        }
    println(markdown)
}

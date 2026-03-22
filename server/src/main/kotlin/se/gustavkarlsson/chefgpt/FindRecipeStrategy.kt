package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeDoNothing
import ai.koog.agents.core.dsl.extension.onIsInstance
import ai.koog.agents.core.dsl.extension.onReasoningMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.prompt.message.Message
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.UserEvent
import se.gustavkarlsson.chefgpt.api.UserMessage
import se.gustavkarlsson.chefgpt.api.UserWaiting
import se.gustavkarlsson.chefgpt.chats.toEvent
import se.gustavkarlsson.chefgpt.chats.toEventOrNull

fun findRecipeStrategy(emitEvent: suspend (Event) -> Unit): AIAgentGraphStrategy<UserEvent, Unit> =
    strategy("find-recipe") {
        val nodeSendUserMessageToLLM by nodeSendUserMessageToLLM("sendUserMessageToLLM", emitEvent)
        val nodeUserWaitsForLLM by nodeUserWaitsForLLM("userWaitsForLLM", emitEvent)
        val responses by nodeDoNothing<Message.Response>("responses")
        val nodeCallTools by nodeCallTools("callTools", emitEvent)
        val nodeSendToolResultToLLM by nodeSendToolResultToLLM("sendToolResultToLLM", emitEvent)
        val nodeSendReasoningBackToLLM by nodeSendReasoningBackToLLM("sendReasoningBackToLLM", emitEvent)
        val nodeAppendFinalMessage by nodeAppendFinalMessage("appendFinalMessage", emitEvent)

        edge(nodeStart forwardTo nodeSendUserMessageToLLM onIsInstance UserMessage::class)
        edge(nodeStart forwardTo nodeUserWaitsForLLM onIsInstance UserWaiting::class)

        edge(nodeSendUserMessageToLLM forwardTo responses)
        edge(nodeUserWaitsForLLM forwardTo responses)

        // Reasoning loops around as long as it's reasoning
        edge(responses forwardTo nodeSendReasoningBackToLLM onReasoningMessage { true })
        edge(nodeSendReasoningBackToLLM forwardTo responses)

        // Tool calls are evaluated by the LLM
        edge(responses forwardTo nodeCallTools onToolCall { true })
        edge(nodeCallTools forwardTo nodeSendToolResultToLLM)
        edge(nodeSendToolResultToLLM forwardTo responses)

        // Assistant message means we are done for now
        edge(responses forwardTo nodeAppendFinalMessage onIsInstance Message.Assistant::class)
        edge(nodeAppendFinalMessage forwardTo nodeFinish)
    }

private fun nodeSendUserMessageToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<UserMessage, Message.Response>(name) { message ->
    emitEvent(message)
    llm.writeSession {
        appendPrompt {
            user {
                message.text?.let { text(it) }
                message.imageUrl?.let { image(it.value) }
            }
        }
        requestLLM()
    }
}

private fun nodeUserWaitsForLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<UserWaiting, Message.Response>(name) { waiting ->
    emitEvent(waiting)
    llm.readSession {
        requestLLM()
    }
}

private fun nodeCallTools(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Tool.Call, ReceivedToolResult>(name) { toolCall ->
    emitEvent(toolCall.toEvent())
    llm.writeSession {
        appendPrompt {
            message(toolCall)
        }
        environment.executeTool(toolCall)
    }
}

private fun nodeSendToolResultToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<ReceivedToolResult, Message.Response>(name) { toolResult ->
    val message = toolResult.toMessage()
    message.toEventOrNull()?.let { emitEvent(it) }
    llm.writeSession {
        appendPrompt {
            message(message)
        }
        requestLLM()
    }
}

private fun nodeSendReasoningBackToLLM(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Reasoning, Message.Response>(name) { reasoning ->
    emitEvent(reasoning.toEvent())
    llm.writeSession {
        appendPrompt {
            message(reasoning)
        }
        requestLLM()
    }
}

private fun nodeAppendFinalMessage(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Assistant, Unit>(name) { assistantMessage ->
    emitEvent(assistantMessage.toEvent())
    llm.writeSession {
        appendPrompt {
            message(assistantMessage)
        }
    }
}

package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onReasoningMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.message.Message
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.FileId
import se.gustavkarlsson.chefgpt.api.UserEvent
import se.gustavkarlsson.chefgpt.api.UserMessage
import se.gustavkarlsson.chefgpt.api.UserWaiting
import se.gustavkarlsson.chefgpt.chats.toEvent
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlinx.io.files.Path as KotlinXPath

fun findRecipeStrategy(
    getImagePath: suspend (FileId) -> Path?,
    emitEvent: suspend (Event) -> Unit,
): AIAgentGraphStrategy<UserEvent, Unit> =
    strategy("find-recipe") {
        val nodeEmitUserEvent by nodeEmitUserEvent(emitEvent)
        val nodeAppendUserMessage by nodeAppendUserMessage("appendUserMessage", getImagePath)
        val nodeLLMRequest by nodeRequestLLM("llmRequest")
        val nodeEmitResponse by nodeEmitResponse("emitResponse", emitEvent)
        val nodeExecuteTool by nodeExecuteTool("executeTool")
        val nodeLLMSendToolResult by nodeLLMSendToolResult("llmSendToolResult")

        edge(nodeStart forwardTo nodeEmitUserEvent)
        edge(nodeEmitUserEvent forwardTo nodeAppendUserMessage)
        edge(nodeAppendUserMessage forwardTo nodeLLMRequest transformed {})
        edge(nodeLLMRequest forwardTo nodeEmitResponse)

        edge(nodeEmitResponse forwardTo nodeFinish onAssistantMessage { true } transformed {})
        edge(nodeEmitResponse forwardTo nodeLLMRequest onReasoningMessage { true } transformed {})
        edge(nodeEmitResponse forwardTo nodeExecuteTool onToolCall { true })

        edge(nodeExecuteTool forwardTo nodeLLMSendToolResult)
        edge(nodeLLMSendToolResult forwardTo nodeEmitResponse)
    }

private fun nodeEmitUserEvent(emitEvent: suspend (Event) -> Unit) =
    node<UserEvent, UserEvent>("emitUserEvent") { event ->
        emitEvent(event)
        event
    }

private fun nodeAppendUserMessage(
    name: String,
    getImagePath: suspend (FileId) -> Path?,
) = node<UserEvent, UserEvent>(name) { event ->
    when (event) {
        UserWaiting -> {
            Unit
        }

        is UserMessage -> {
            val imagePath =
                event.imageId?.let { imageId ->
                    getImagePath(imageId)?.let { path ->
                        KotlinXPath(path.pathString)
                    }
                }
            llm.writeSession {
                appendPrompt {
                    user {
                        event.text?.let { text(it) }
                        imagePath?.let { image(it) }
                    }
                }
            }
        }
    }
    event
}

private fun nodeEmitResponse(
    name: String,
    emitEvent: suspend (Event) -> Unit,
) = node<Message.Response, Message.Response>(name) { response ->
    emitEvent(response.toEvent())
    response
}

private fun nodeRequestLLM(name: String) =
    node<Unit, Message.Response>(name) {
        llm.readSession {
            requestLLM()
        }
    }

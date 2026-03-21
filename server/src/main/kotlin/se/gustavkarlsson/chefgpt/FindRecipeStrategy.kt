package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.AIAgentFunctionalStrategy
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.message.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonPrimitive
import se.gustavkarlsson.chefgpt.api.AgentMessage
import se.gustavkarlsson.chefgpt.api.Event
import se.gustavkarlsson.chefgpt.api.UserMessage

private const val INITIAL_USER_QUERY = "Help me figure out what to cook"

fun findRecipeStrategy(emitEvent: suspend (Event) -> Unit): AIAgentGraphStrategy<UserMessage, Unit> =
    strategy("find-recipe") {
        // FIXME implement strategy and write chat events to the flow
    }

// FIXME Replace with above after rewritten

fun findRecipeStrategy(
    receiveMessage: suspend () -> UserMessage,
    sendMessage: suspend (AgentMessage) -> Unit,
): AIAgentFunctionalStrategy<Unit, Unit> =
    functionalStrategy("find-recipe") {
        var messages: List<Message.Response> = requestLLMMultiple(INITIAL_USER_QUERY)
        while (messages.isNotEmpty()) {
            val exitMessage = messages.findExitMessageOrNull()
            if (exitMessage != null) {
                sendMessage(AgentMessage.Regular(exitMessage))
                break
            }
            messages =
                coroutineScope {
                    val toolEvaluationResults =
                        async {
                            val toolCalls = messages.filterIsInstance<Message.Tool.Call>()
                            val toolResults = executeMultipleTools(toolCalls)
                            sendMultipleToolResults(toolResults)
                        }
                    val userInteractionResults =
                        async {
                            val reasoningMessages = messages.filterIsInstance<Message.Reasoning>()
                            for (reasoningMessage in reasoningMessages) {
                                sendMessage(AgentMessage.Reasoning(reasoningMessage.content))
                            }
                            val agentMessageText =
                                messages
                                    .filterIsInstance<Message.Assistant>()
                                    .toSingleMessageOrNull()
                            if (agentMessageText != null) {
                                sendMessage(AgentMessage.Regular(agentMessageText))
                                val userMessage = receiveMessage()
                                llm.writeSession {
                                    appendPrompt {
                                        user {
                                            // userMessage.text?.let {
                                            //     text(it)
                                            // }
                                            // userMessage.imageId?.let {
                                            //     image(TODO() as Path)
                                            // }
                                        }
                                    }

                                    requestLLMMultiple()
                                }
                            } else {
                                emptyList()
                            }
                        }
                    toolEvaluationResults.await() + userInteractionResults.await()
                }
        }
    }

private fun List<Message>.findExitMessageOrNull(): String? =
    this
        .asSequence()
        .filterIsInstance<Message.Tool.Call>()
        .filter { it.tool == ExitTool.name }
        .mapNotNull { it.contentJson["message"] as? JsonPrimitive }
        .map { it.content }
        .firstOrNull()

private fun List<Message.Assistant>.toSingleMessageOrNull(): String? =
    joinToString("\n") { it.content }.takeIf { it.isNotBlank() }

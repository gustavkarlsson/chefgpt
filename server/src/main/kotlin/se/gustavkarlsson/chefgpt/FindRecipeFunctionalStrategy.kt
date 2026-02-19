package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.AIAgentFunctionalStrategy
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.executeMultipleTools
import ai.koog.agents.core.dsl.extension.requestLLMMultiple
import ai.koog.agents.core.dsl.extension.sendMultipleToolResults
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.message.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.io.files.Path
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.path.pathString

private const val INITIAL_USER_QUERY = "Help me figure out what to cook"

// FIXME test and validate flow and conversation ending
fun findRecipeFunctionalStrategy(
    receiveMessage: suspend () -> MessageToAi,
    sendMessage: suspend (MessageToUser) -> Unit
): AIAgentFunctionalStrategy<Unit, Unit> =
    functionalStrategy("find-recipe") {
        var messages: List<Message> = requestLLMMultiple(INITIAL_USER_QUERY)
        while (messages.isNotEmpty()) {
            val exitMessage = messages.findExitMessageOrNull()
            if (exitMessage != null) {
                sendMessage(MessageToUser(exitMessage))
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
                            val messageToUser = messages.filterIsInstance<Message.Assistant>().toSingleMessage()
                            if (messageToUser.isNotBlank()) {
                                sendMessage(MessageToUser(messageToUser))
                                val messageToAi = receiveMessage()
                                llm.writeSession {
                                    appendPrompt {
                                        user {
                                            messageToAi.text?.let {
                                                text(it)
                                            }
                                            messageToAi.image?.let {
                                                image(Path(it.pathString))
                                            }
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
        .filterIsInstance<Message.Tool.Call>()
        .filter { it.tool == ExitTool.name }
        .mapNotNull { it.contentJson["message"] as? JsonPrimitive }
        .map { it.content }
        .firstOrNull()

private fun List<Message.Assistant>.toSingleMessage(): String = joinToString("\n") { it.content }

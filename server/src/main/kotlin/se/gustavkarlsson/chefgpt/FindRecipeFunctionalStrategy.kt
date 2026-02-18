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
import kotlinx.serialization.json.JsonPrimitive

private const val INITIAL_USER_QUERY = "Help me figure out what to cook"

// FIXME test and validate flow and conversation ending
fun findRecipeFunctionalStrategy(conversation: Conversation): AIAgentFunctionalStrategy<Unit, Unit> =
    functionalStrategy("find-recipe") {
        var messages: List<Message> = requestLLMMultiple(INITIAL_USER_QUERY)
        while (messages.isNotEmpty()) {
            val exitMessage = messages.findExitMessageOrNull()
            if (exitMessage != null) {
                conversation.send(MessageFromAi.End(exitMessage))
                break
            }
            messages = coroutineScope {
                val toolEvaluationResults = async {
                    val toolCalls = messages.filterIsInstance<Message.Tool.Call>()
                    val toolResults = executeMultipleTools(toolCalls)
                    sendMultipleToolResults(toolResults)
                }
                val userInteractionResults = async {
                    val messageToUser = messages.filterIsInstance<Message.Assistant>().toSingleMessage()
                    if (messageToUser.isNotBlank()) {
                        conversation.send(MessageFromAi.Content(messageToUser))
                        when (val response = conversation.await()) {
                            is MessageFromUser.Content -> {
                                llm.writeSession {
                                    appendPrompt {
                                        user {
                                            text(response.text)
                                            // FIXME how do we send images?
                                            // userResponse.image?.let {
                                            //     image(Path(it))
                                            // }
                                        }
                                    }

                                    requestLLMMultiple()
                                }
                            }

                            MessageFromUser.End -> emptyList()
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
    this.filterIsInstance<Message.Tool.Call>()
        .filter { it.tool == ExitTool.name }
        .mapNotNull { it.contentJson["message"] as? JsonPrimitive }
        .map { it.content }
        .firstOrNull()

private fun List<Message.Assistant>.toSingleMessage(): String = joinToString("\n") { it.content }

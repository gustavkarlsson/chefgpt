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

private const val INITIAL_USER_QUERY = "Help me figure out what to cook"

fun findRecipeFunctionalStrategy(conversation: AiSideConversation): AIAgentFunctionalStrategy<Unit, Unit> =
    functionalStrategy("find-recipe") {
        conversation.use { conversation ->
            var messages: List<Message> = requestLLMMultiple(INITIAL_USER_QUERY)
            while (true) {
                val exitMessage = messages.findExitMessageOrNull()
                if (exitMessage != null) {
                    conversation.sayToUser(MessageContent(exitMessage))
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
                            val userResponse = conversation.askUser(MessageContent(messageToUser))
                            llm.writeSession {
                                appendPrompt {
                                    user {
                                        text(userResponse.text)
                                        userResponse.image?.let {
                                            image(Path(it))
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
    }

private fun List<Message>.findExitMessageOrNull(): String? =
    this.filterIsInstance<Message.Tool.Call>()
        .filter { it.tool == ExitTool.name }
        .mapNotNull { it.contentJson["message"] as? JsonPrimitive }
        .map { it.content }
        .firstOrNull()

private fun List<Message.Assistant>.toSingleMessage(): String = joinToString("\n") { it.content }

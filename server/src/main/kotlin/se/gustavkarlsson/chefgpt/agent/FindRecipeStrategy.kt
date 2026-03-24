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

// TODO() Can this be simplified now that it doesn't need to manually send events anymore?
fun findRecipeStrategy(): AIAgentGraphStrategy<Unit, Unit> =
    strategy("find-recipe") {
        val nodeExecuteLLM by nodeExecuteLLM("executeLLM")
        val responses by nodeDoNothing<Message.Response>("responses")
        val nodeCallTools by nodeCallTools("callTools")
        val nodeSendToolResultToLLM by nodeSendToolResultToLLM("sendToolResultToLLM")
        val nodeSendReasoningBackToLLM by nodeSendReasoningBackToLLM("sendReasoningBackToLLM")

        edge(nodeStart forwardTo nodeExecuteLLM)
        edge(nodeExecuteLLM forwardTo responses)

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

private fun nodeExecuteLLM(name: String) =
    node<Unit, Message.Response>(name) { message ->
        llm
            .writeSession {
                requestLLM()
            }
    }

private fun nodeCallTools(name: String) =
    node<Message.Tool.Call, Message.Tool.Result>(name) { toolCall ->
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
            }
    }

private fun nodeSendToolResultToLLM(name: String) =
    node<Message.Tool.Result, Message.Response>(name) { toolResult ->
        llm
            .writeSession {
                requestLLM()
            }
    }

private fun nodeSendReasoningBackToLLM(name: String) =
    node<Message.Reasoning, Message.Response>(name) { reasoning ->
        llm
            .writeSession {
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

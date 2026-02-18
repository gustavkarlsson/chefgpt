package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor

fun createAiAgent(conversation: AiSideConversation): AIAgent<Unit, Unit> = AIAgent(
    promptExecutor = simpleAnthropicExecutor(apiKey = getAnthropicApiKey()),
    llmModel = AnthropicModels.Haiku_4_5,
    toolRegistry = ToolRegistry {
        tools(createIngredientStore().asTools())
        tools(SpoonacularClient(getSpoonacularApiKey()).asTools())
        tool(ExitTool)
    },
    systemPrompt = """
            You are a culinary expert specialized in suggesting meal recipes based on the user's ingredients and preferences.
            Start by greeting the user with their name.
            Check the ingredient store for any stored ingredients.
            If there are none, ask the user what the have, and add them.
            When some ingredients exist, search recipes using the ingredients.
            Present each recipe found with a super short description and URL.
            When the used is satisfied, exit the conversation.
        """.trimIndent(),
    strategy = findRecipeFunctionalStrategy(conversation),
)

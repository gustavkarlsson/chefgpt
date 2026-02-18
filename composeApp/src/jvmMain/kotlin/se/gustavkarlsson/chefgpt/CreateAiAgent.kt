package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import java.nio.file.Path

fun createAiAgent(
    conversation: ConversationService,
    ingredientStoreFile: Path,
    anthropicApiKey: String,
    spoonacularApiKey: String
): AIAgent<Unit, Unit> = AIAgent.Companion(
    promptExecutor = simpleAnthropicExecutor(apiKey = anthropicApiKey),
    llmModel = AnthropicModels.Haiku_4_5,
    toolRegistry = ToolRegistry.Companion {
        tool(::getUserName)
        tools(IngredientStore(ingredientStoreFile))
        tools(SpoonacularClient(apiKey = spoonacularApiKey))
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

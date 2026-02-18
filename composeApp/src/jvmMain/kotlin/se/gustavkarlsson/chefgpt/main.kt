package se.gustavkarlsson.chefgpt

import ai.koog.utils.io.use
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.nio.file.Path

suspend fun main(args: Array<String> = emptyArray()) {
    val anthropicApiKey = System.getenv("ANTHROPIC_API_KEY")
    val spoonacularApiKey = System.getenv("SPOONACULAR_API_KEY")
    val ingredientStoreFile = Path.of("ingredient-store.txt")
    val conversation = ConversationService()
    val agent = createAiAgent(conversation, ingredientStoreFile, anthropicApiKey, spoonacularApiKey)

    coroutineScope {
        launch {
            agent.use {
                it.run(Unit)
            }
        }

        if (args.any { it == "cli" }) {
            runCli(conversation)
        } else {
            runApplication(conversation)
        }
    }
}

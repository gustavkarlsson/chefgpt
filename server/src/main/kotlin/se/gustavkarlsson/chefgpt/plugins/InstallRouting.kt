package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.routes.chatActionsRoute
import se.gustavkarlsson.chefgpt.routes.createChatRoute
import se.gustavkarlsson.chefgpt.routes.createIngredientRoute
import se.gustavkarlsson.chefgpt.routes.deleteChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteIngredientRoute
import se.gustavkarlsson.chefgpt.routes.deleteRecipeRoute
import se.gustavkarlsson.chefgpt.routes.filesRoute
import se.gustavkarlsson.chefgpt.routes.getRecipeRoute
import se.gustavkarlsson.chefgpt.routes.loginRoute
import se.gustavkarlsson.chefgpt.routes.overwriteOriginalRecipeRoute
import se.gustavkarlsson.chefgpt.routes.patchIngredientRoute
import se.gustavkarlsson.chefgpt.routes.patchRecipeRoute
import se.gustavkarlsson.chefgpt.routes.registerRoute
import se.gustavkarlsson.chefgpt.routes.saveRecipeAsCopyRoute
import se.gustavkarlsson.chefgpt.routes.saveRecipeRoute
import se.gustavkarlsson.chefgpt.routes.scanIngredientsRoute
import se.gustavkarlsson.chefgpt.routes.streamChatEventsRoute
import se.gustavkarlsson.chefgpt.routes.streamChatsRoute
import se.gustavkarlsson.chefgpt.routes.streamIngredientsRoute
import se.gustavkarlsson.chefgpt.routes.streamRecipesRoute

// TODO set timeouts
fun Application.installRouting() {
    routing {
        registerRoute()
        loginRoute()
        authenticate {
            filesRoute()
            streamChatsRoute()
            streamIngredientsRoute()
            createIngredientRoute()
            patchIngredientRoute()
            deleteIngredientRoute()
            scanIngredientsRoute()
            createChatRoute()
            deleteChatRoute()
            streamChatEventsRoute()
            chatActionsRoute()
            streamRecipesRoute()
            saveRecipeRoute()
            getRecipeRoute()
            patchRecipeRoute()
            overwriteOriginalRecipeRoute()
            saveRecipeAsCopyRoute()
            deleteRecipeRoute()
        }
    }
}

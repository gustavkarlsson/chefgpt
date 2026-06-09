package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.routes.chatActionsRoute
import se.gustavkarlsson.chefgpt.routes.createChatRoute
import se.gustavkarlsson.chefgpt.routes.createIngredientRoute
import se.gustavkarlsson.chefgpt.routes.deleteChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteIngredientRoute
import se.gustavkarlsson.chefgpt.routes.deleteRecipeSummaryRoute
import se.gustavkarlsson.chefgpt.routes.getRecipeRoute
import se.gustavkarlsson.chefgpt.routes.imagesRoute
import se.gustavkarlsson.chefgpt.routes.loginRoute
import se.gustavkarlsson.chefgpt.routes.patchIngredientRoute
import se.gustavkarlsson.chefgpt.routes.registerRoute
import se.gustavkarlsson.chefgpt.routes.saveRecipeSummaryRoute
import se.gustavkarlsson.chefgpt.routes.scanIngredientsRoute
import se.gustavkarlsson.chefgpt.routes.streamChatEventsRoute
import se.gustavkarlsson.chefgpt.routes.streamChatsRoute
import se.gustavkarlsson.chefgpt.routes.streamIngredientsRoute
import se.gustavkarlsson.chefgpt.routes.streamRecipeSummariesRoute

// TODO set timeouts
fun Application.installRouting() {
    routing {
        registerRoute()
        loginRoute()
        authenticate {
            imagesRoute()
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
            streamRecipeSummariesRoute()
            saveRecipeSummaryRoute()
            deleteRecipeSummaryRoute()
            getRecipeRoute()
        }
    }
}

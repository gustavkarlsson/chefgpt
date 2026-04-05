package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.routes.chatActionsRoute
import se.gustavkarlsson.chefgpt.routes.chatEventsRoute
import se.gustavkarlsson.chefgpt.routes.createChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteIngredientRoute
import se.gustavkarlsson.chefgpt.routes.getChatsRoute
import se.gustavkarlsson.chefgpt.routes.getIngredientsRoute
import se.gustavkarlsson.chefgpt.routes.imagesRoute
import se.gustavkarlsson.chefgpt.routes.loginRoute
import se.gustavkarlsson.chefgpt.routes.registerRoute

// TODO set timeouts
fun Application.installRouting() {
    routing {
        registerRoute()
        loginRoute()
        authenticate {
            imagesRoute()
            getChatsRoute()
            getIngredientsRoute()
            deleteIngredientRoute()
            createChatRoute()
            deleteChatRoute()
            chatEventsRoute()
            chatActionsRoute()
        }
    }
}

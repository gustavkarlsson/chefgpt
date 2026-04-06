package se.gustavkarlsson.chefgpt.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import se.gustavkarlsson.chefgpt.routes.chatActionsRoute
import se.gustavkarlsson.chefgpt.routes.createChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteChatRoute
import se.gustavkarlsson.chefgpt.routes.deleteIngredientRoute
import se.gustavkarlsson.chefgpt.routes.getChatsRoute
import se.gustavkarlsson.chefgpt.routes.imagesRoute
import se.gustavkarlsson.chefgpt.routes.loginRoute
import se.gustavkarlsson.chefgpt.routes.putIngredientRoute
import se.gustavkarlsson.chefgpt.routes.registerRoute
import se.gustavkarlsson.chefgpt.routes.streamChatEventsRoute
import se.gustavkarlsson.chefgpt.routes.streamIngredientsRoute

// TODO set timeouts
fun Application.installRouting() {
    routing {
        registerRoute()
        loginRoute()
        authenticate {
            imagesRoute()
            getChatsRoute()
            streamIngredientsRoute()
            putIngredientRoute()
            deleteIngredientRoute()
            createChatRoute()
            deleteChatRoute()
            streamChatEventsRoute()
            chatActionsRoute()
        }
    }
}

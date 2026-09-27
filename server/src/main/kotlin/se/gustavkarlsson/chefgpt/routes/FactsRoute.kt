package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.ApiUserFacts
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.requireSession
import se.gustavkarlsson.chefgpt.toApi
import se.gustavkarlsson.chefgpt.toDomain

fun Route.factsRoute() {
    get("/facts") {
        val factRepository = get<FactRepository>()
        val userId = call.requireSession().user.id
        call.respond(HttpStatusCode.OK, factRepository.getFacts(userId).toApi())
    }
    put("/facts") {
        val factRepository = get<FactRepository>()
        val userId = call.requireSession().user.id
        val facts = call.receive<ApiUserFacts>().toDomain()
        val newFacts = factRepository.replaceFacts(userId, facts)
        val newApiFacts = newFacts.toApi()
        call.respond(HttpStatusCode.OK, newApiFacts)
    }
}

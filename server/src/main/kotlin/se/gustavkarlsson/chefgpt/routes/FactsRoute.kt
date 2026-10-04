package se.gustavkarlsson.chefgpt.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import org.koin.ktor.ext.get
import se.gustavkarlsson.chefgpt.api.facts.v1.ApiUserFacts
import se.gustavkarlsson.chefgpt.api.facts.v1.FACTS_V1_PATH
import se.gustavkarlsson.chefgpt.facts.FactRepository
import se.gustavkarlsson.chefgpt.facts.toApi
import se.gustavkarlsson.chefgpt.facts.toDomain
import se.gustavkarlsson.chefgpt.requireSession

fun Route.factsRoute() {
    get(FACTS_V1_PATH) {
        val factRepository = get<FactRepository>()
        val userId = call.requireSession().user.id
        call.respond(HttpStatusCode.OK, factRepository.getFacts(userId).toApi())
    }
    put(FACTS_V1_PATH) {
        val factRepository = get<FactRepository>()
        val userId = call.requireSession().user.id
        val facts = call.receive<ApiUserFacts>().toDomain()
        val newFacts = factRepository.replaceFacts(userId, facts)
        val newApiFacts = newFacts.toApi()
        call.respond(HttpStatusCode.OK, newApiFacts)
    }
}

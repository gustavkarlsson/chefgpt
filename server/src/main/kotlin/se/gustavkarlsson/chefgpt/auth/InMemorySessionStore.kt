package se.gustavkarlsson.chefgpt.auth

import se.gustavkarlsson.chefgpt.api.SessionId
import java.util.concurrent.ConcurrentHashMap

class InMemorySessionStore : SessionStore {
    private val sessions = ConcurrentHashMap<SessionId, User>()

    override suspend fun create(user: User): SessionId {
        val sessionId = SessionId.random()
        sessions[sessionId] = user
        return sessionId
    }

    override suspend fun get(sessionId: SessionId): User? = sessions[sessionId]
}

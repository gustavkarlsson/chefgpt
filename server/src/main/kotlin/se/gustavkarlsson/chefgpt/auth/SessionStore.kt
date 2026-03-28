package se.gustavkarlsson.chefgpt.auth

import se.gustavkarlsson.chefgpt.api.SessionId

interface SessionStore {
    suspend fun create(user: User): SessionId

    suspend fun get(sessionId: SessionId): User?
}

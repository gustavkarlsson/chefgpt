package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.auth.UserId

interface FactRepository {
    suspend fun getFacts(userId: UserId): UserFacts

    suspend fun updateFacts(
        userId: UserId,
        update: UserFactsUpdate,
    ): UserFacts

    suspend fun replaceFacts(
        userId: UserId,
        facts: UserFacts,
    ): UserFacts
}

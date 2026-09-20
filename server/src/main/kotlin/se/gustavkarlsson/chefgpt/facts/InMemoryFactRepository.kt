package se.gustavkarlsson.chefgpt.facts

import se.gustavkarlsson.chefgpt.auth.UserId
import java.util.concurrent.ConcurrentHashMap

class InMemoryFactRepository(
    private val storage: ConcurrentHashMap<UserId, UserFacts> = ConcurrentHashMap(),
) : FactRepository {
    override suspend fun getFacts(userId: UserId): UserFacts = storage[userId] ?: UserFacts.UNKNOWN

    override suspend fun updateFacts(
        userId: UserId,
        update: UserFactsUpdate,
    ): UserFacts = storage.compute(userId) { _, current -> (current ?: UserFacts.UNKNOWN).applyUpdate(update) }!!
}

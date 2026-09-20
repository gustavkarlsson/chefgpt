package se.gustavkarlsson.chefgpt.agent

import se.gustavkarlsson.chefgpt.api.ChatId
import se.gustavkarlsson.chefgpt.auth.UserId

// The run id passed to the chat agent carries both the chat and the user, so the
// per-run features (EventBackedChatMemory and UserFactMemory) can recover either.
private const val SEPARATOR = "/"

internal fun runId(
    chatId: ChatId,
    userId: UserId,
): String = "${chatId.value}$SEPARATOR${userId.value}"

internal fun String.chatIdFromRunId(): ChatId = ChatId.parse(substringBefore(SEPARATOR))

internal fun String.userIdFromRunId(): UserId = UserId.parse(substringAfter(SEPARATOR))

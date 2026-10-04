package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.ApiChat

fun Chat.toApi(): ApiChat = ApiChat(id, createdAt, name)

package se.gustavkarlsson.chefgpt.chats

import se.gustavkarlsson.chefgpt.api.chats.v1.ApiChat

fun Chat.toApi(): ApiChat = ApiChat(id, createdAt, name)

package se.gustavkarlsson.chefgpt.agent

import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart

// The chat agent must not see photos in its own prompt; the scan agents read the image bytes.
fun stripImageAttachments(messages: List<Message>): List<Message> =
    messages.map { message ->
        if (message is Message.User) {
            message.copy(
                parts =
                    message.parts.filterNot { part ->
                        part is MessagePart.Attachment && part.source is AttachmentSource.Image
                    },
            )
        } else {
            message
        }
    }

/**
 * Removes any message holding an orphaned [MessagePart.Tool.Call] that is not
 * immediately followed by a message holding a [MessagePart.Tool.Result]. This can
 * happen if an agent run was interrupted after saving the tool call but before
 * saving the tool result. Such orphaned calls cause Anthropic to reject the
 * request with a "tool_use without tool_result" error.
 */
fun sanitizeMessages(messages: List<Message>): List<Message> =
    buildList {
        for ((index, message) in messages.withIndex()) {
            if (message.parts.any { it is MessagePart.Tool.Call }) {
                val next = messages.getOrNull(index + 1)
                if (next?.parts?.any { it is MessagePart.Tool.Result } != true) {
                    // Orphaned tool call — drop it and everything after
                    break
                }
            }
            add(message)
        }
    }

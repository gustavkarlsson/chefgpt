package se.gustavkarlsson.chefgpt

import ai.koog.utils.io.use
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

suspend fun runCli(conversation: UserSideConversation) {
    conversation.use { conversation ->
        coroutineScope {
            launch {
                // Print every message from the AI (User messages are "printed" as they are typed)
                conversation.messageHistory
                    .filter { it.subject == Subject.Ai }
                    .map { it.content }
                    .collect(::println)
            }
            launch {
                // Show a prompt and send user input to the conversation whenever the state asks for it.
                conversation.state
                    .takeWhile { it != ConversationState.Ended }
                    .filter { it == ConversationState.WaitingForUser }
                    .collect {
                        print("> ")
                        val text = readln()
                        conversation.sayToAi(MessageContent(text))
                    }
            }
        }
    }
}

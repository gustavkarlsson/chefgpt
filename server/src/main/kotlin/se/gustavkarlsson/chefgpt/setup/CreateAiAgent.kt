package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.agent.AiAgent
import se.gustavkarlsson.chefgpt.agent.FakeAiAgent
import se.gustavkarlsson.chefgpt.agent.KoogAiAgent

fun createAiAgent(type: String): AiAgent =
    when (type) {
        "ai" -> KoogAiAgent()
        "fake" -> FakeAiAgent()
        else -> error("Unknown agent type: '$type'. Expected 'ai' or 'fake'.")
    }

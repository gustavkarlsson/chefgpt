package se.gustavkarlsson.chefgpt

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool

@Tool
@LLMDescription("Gets the user name of the system. It might be a first name, full name, or a nickname.")
fun getUserName(): String = System.getenv("USER")
    .replace('.', ' ')
    .replace('_', ' ')
    .split(' ').joinToString(" ") { it.capitalize() }

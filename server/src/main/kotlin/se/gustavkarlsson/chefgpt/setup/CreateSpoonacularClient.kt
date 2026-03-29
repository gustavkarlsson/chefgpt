package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.tools.SpoonacularClient

fun createSpoonacularClient(apiKey: String): SpoonacularClient = SpoonacularClient(apiKey)

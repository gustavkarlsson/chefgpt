package se.gustavkarlsson.chefgpt.setup

import se.gustavkarlsson.chefgpt.recipes.SpoonacularClient

fun createSpoonacularClient(apiKey: String): SpoonacularClient = SpoonacularClient(apiKey)

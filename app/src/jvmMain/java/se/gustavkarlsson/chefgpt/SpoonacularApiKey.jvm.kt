package se.gustavkarlsson.chefgpt

actual fun getSpoonacularApiKey(): String = System.getenv("SPOONACULAR_API_KEY")

package se.gustavkarlsson.chefgpt.jobs

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import se.gustavkarlsson.chefgpt.api.ApiJob

// Decodes a job result of the form ["a", "b"] into a list of strings.
fun ApiJob.resultStrings(): List<String> =
    (result as? JsonArray)
        ?.mapNotNull {
            (it as? JsonPrimitive)
                ?.takeIf { primitive ->
                    primitive.isString
                }?.content
        }.orEmpty()

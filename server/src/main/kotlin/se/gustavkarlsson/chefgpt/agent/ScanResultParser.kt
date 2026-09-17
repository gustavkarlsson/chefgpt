package se.gustavkarlsson.chefgpt.agent

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

// Both one-shot scan agents finish with a single "OK: <count>" / "ERROR: <reason>" line.
fun parseScanResult(reply: String): Result<Int, String> {
    val trimmed = reply.trim()
    return when {
        trimmed.startsWith("OK:", ignoreCase = true) -> {
            when (val count = trimmed.substringAfter(':').trim().toIntOrNull()) {
                null -> Err("Could not parse count from reply: $trimmed")
                else -> Ok(count)
            }
        }

        trimmed.startsWith("ERROR:", ignoreCase = true) -> {
            Err(trimmed.substringAfter(':').trim())
        }

        else -> {
            Err("Unexpected agent reply: $trimmed")
        }
    }
}

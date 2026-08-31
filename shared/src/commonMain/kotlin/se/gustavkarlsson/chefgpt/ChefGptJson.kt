package se.gustavkarlsson.chefgpt

import kotlinx.serialization.json.Json

/**
 * The one JSON configuration, used by the server, the clients, persistence and tests alike.
 *
 * [encodeDefaults] and [explicitNulls] are deliberately not configurable: they decide what an absent
 * key means, so a reader and a writer that disagree on them cannot round-trip. A `Json` configured
 * elsewhere (a bare `Json` or Ktor's `json()`, both defaulting to `explicitNulls = true`) rejects the
 * very payload this one produces, with "field is required ... but it was missing". With [explicitNulls]
 * off, a null is left out when writing and an absent key reads back as null, which is why nullable
 * properties need no default value.
 *
 * Parsing follows [strict], which has no default so that every caller has to decide. Strict parses
 * exactly, so an unexpected field means someone changed the schema and we hear about it — that is
 * what development mode and the tests want. Non-strict is forgiving, for input that is outside our
 * control or predates the current code: production traffic, stored records, third-party APIs and LLM
 * output.
 *
 * [prettyPrint] is separate because it is about reading the output, not about trusting the input.
 * Turn it on in development mode.
 */
fun chefGptJson(
    strict: Boolean,
    prettyPrint: Boolean = false,
): Json =
    Json {
        encodeDefaults = true
        explicitNulls = false
        isLenient = !strict
        ignoreUnknownKeys = !strict
        allowComments = !strict
        allowTrailingComma = !strict
        this.prettyPrint = prettyPrint
    }

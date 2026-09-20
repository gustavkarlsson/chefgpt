# ChefGPT

A Kotlin Multiplatform cooking assistant: a Ktor backend whose chat agents help a user search, save and adapt recipes, scan ingredients, and manage a pantry. This context holds the shared vocabulary for feature work on those agents.

## Language

**User fact**:
A durable attribute of the user — who they are and what they prefer — that the backend agents remember across conversations and use to tailor answers.
_Avoid_: Preference (too narrow: a name isn't a preference), memory (too vague).

**Fact state**:
The three states a fact can be in. **Unknown** — never established; the agent should ask when the fact becomes relevant. **Value** — a concrete value (e.g. "metric", "vegetarian"). **None** — the user has explicitly said they have no such preference or restriction (e.g. "no specific diet"), which is different from unknown.

**Measurement preference**:
The user's preferred way of expressing ingredient amounts — **weight** or **volume**. It governs compressible dry goods, viscous or sticky liquids, and irregular solids; easy-to-pour liquids are always volume; and amounts not given as weight or volume (cloves, pinches, dashes) are left as written.

**Self-description**:
A user statement about their own persistent attributes ("I'm vegetarian"). These update a fact.
_Avoid_: Request ("make me a vegetarian meal tonight"), which must never update a fact.

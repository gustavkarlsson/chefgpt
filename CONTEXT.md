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

**Message history**:
The ordered transcript of a chat — the user's and assistant's turns, including the tool calls and results the agent made — replayed into the prompt each turn so the agent has the whole conversation.
_Avoid_: Memory (reserved for the durable cross-conversation facts above), event log (names the persistence, not the conversation).

**Sheet**:
A screen presented as a modal bottom sheet over the previous screen, leaving it visible behind. It is a distinct navigation concept in this app (a screen marked `Screen.BottomSheet`), not a plain full-screen push.
_Avoid_: Dialog (a separate floating window), modal (too generic).

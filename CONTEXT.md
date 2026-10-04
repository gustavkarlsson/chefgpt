# ChefGPT

A Kotlin Multiplatform cooking assistant: a Ktor backend whose chat agents help a user search, save and adapt recipes, scan ingredients, and manage a pantry. This context holds the shared vocabulary for feature work on those agents.

## Language

**User fact**:
A durable attribute of the user — who they are and what they prefer — that the backend agents remember across conversations and use to tailor answers.
_Avoid_: Preference (too narrow: a name isn't a preference), memory (too vague).

**Fact state**:
The three states a fact can be in. **Unknown** — never established; the agent should ask when the fact becomes relevant. **Value** — a concrete value (e.g. "metric", "vegetarian"). **None** — the user has explicitly said they have no such preference or restriction (e.g. "no specific diet"), which is different from unknown.

**Onboarding**:
The first-run flow shown after login whenever a fact is Unknown: a multi-step wizard collecting the facts, before the user reaches the app. Steps whose fact is already set are skipped.
_Avoid_: Setup, welcome flow.

**Dietary restriction**:
A dietary fact stored as a set of free-form restriction strings. null is Unknown, an empty set is None ("no restrictions"), and a non-empty set is Value.
_Avoid_: Diet, allergy (each names a narrower concept than the fact holds).

**Self-description**:
A user statement about their own persistent attributes ("I'm vegetarian"). These update a fact.
_Avoid_: Request ("make me a vegetarian meal tonight"), which must never update a fact.

**Message history**:
The ordered transcript of a chat — the user's and assistant's turns, including the tool calls and results the agent made — replayed into the prompt each turn so the agent has the whole conversation.
_Avoid_: Memory (reserved for the durable cross-conversation facts above), event log (names the persistence, not the conversation).

**Sheet**:
A screen presented as a modal bottom sheet over the previous screen, leaving it visible behind. It is a distinct navigation concept in this app (a screen marked `Screen.BottomSheet`), not a plain full-screen push.
_Avoid_: Dialog (a separate floating window), modal (too generic).

**Scrape**:
A method of adding a recipe from a website URL: the user pastes a URL and the backend saves the single recipe it finds, via a chain of scrapers. Contrast with adding recipes by scanning photos.
_Avoid_: Extract (the underlying Spoonacular call `RecipeClient.extractRecipeFromWebsite`, not the feature), import.

## Server model layers

The server splits its models into four layers (ADR 0002). A model belongs to exactly one; each boundary maps to and from the domain.

**Domain model**:
A pure Kotlin data class holding a business concept (`Recipe`, `Ingredient`, `Event`), free of all library annotations and library types. The hub every other layer maps through.
_Avoid_: Model (too generic), DTO (names the wire, not the concept).

**API model**:
The wire format of an endpoint, in the shared module with an `Api` prefix. Strictly separated from the rest because it will be versioned. Both sides map it to their own domain models at their boundaries — the server in its routes, the app in its repositories — so each side has its own `Recipe` class, shaped for its own needs.
_Avoid_: Response (only names one direction), DTO (used loosely elsewhere).

**Tool model**:
The schema an agent tool exposes to the LLM: `@Serializable` and documented for Koog with `@LLMDescription` on the class and every property, in `agent/tools/models/` with a `Tool` prefix. Curated for the LLM, not for the code.
_Avoid_: Tool parameter (only names one direction).

**Database model**:
A persistence-layer internal: a SQLDelight-generated query row type, or a hand-written `Stored*` DTO for a column holding a whole-JSON blob. Never crosses the repository boundary.
_Avoid_: Entity (implies an ORM mapping that does not exist).

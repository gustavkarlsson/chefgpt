# Four model layers on the server

The server uses four strictly separated kinds of models: API models, tool models,
domain models, and database models. Before this split, the boundaries were
inconsistent — `ApiRecipe` served as wire format, persistence format, tool schema,
and domain object at once, while `Event` embedded a third-party library's message
type and was persisted as whole JSON. The split gives each layer one job and one
owner, so each can evolve without the others.

## The layers

- **Domain models** are pure Kotlin data classes with no annotations and no
  library types: no `@Serializable`, no `@LLMDescription`, no Koog types, no
  `Api*` models. They may use kotlin stdlib types and shared value classes
  (`RecipeId`, `ImageUrl`, `SpoonacularId`, …). They are the server's single
  source of truth for business concepts (`Recipe`, `Ingredient`, `Event`,
  `UserFacts`). Every boundary maps to and from them.
- **Tool models** (`Tool` prefix, in `agent/tools/models/`) are the schema the
  LLM sees through Koog's reflection-driven tools. They carry `@LLMDescription`
  on the class and on every property, are `@Serializable`, and contain only
  primitives, stdlib types, shared value classes, and other tool models. Tool
  handlers map them to and from domain models.
- **API models** (`Api` prefix, in the shared module) are the wire format. They
  are strictly separated because they will be versioned in the future. Routes
  map them to and from domain models via `toApi()`/`toDomain()` extension
  functions; no other layer sees them.
- **Database models** are persistence internals: the SQLDelight-generated query
  row types, plus hand-written `Stored*` DTOs for columns that hold whole-JSON
  blobs. They never cross the repository boundary; `Postgres*` repositories map
  them to and from domain models.

The wire format is unchanged by this split — the API models and their
`@SerialName`s are byte-identical. The event JSON column moved to a clean
storage format of the server's own (`StoredEvent`, with its own serial names)
instead of mirroring the historical Koog-shaped records; the app is unreleased
and databases are wiped routinely, so the old rows are not worth reading.

## Why pure domain models

Domain models were chosen to be 100% pure — no library annotations at all —
rather than merely free of Koog types. Any annotation couples the domain to
whatever library consumes it: `@Serializable` to the wire shape, `@LLMDescription`
to the tool schema. Purity forces the coupling to live in the layers that own it,
and keeps the domain readable, testable, and independent of third-party
versions. The cost is mapping boilerplate at each boundary, accepted as the
price of the separation.

## Trade-offs

- Tool models duplicate domain models field for field in places. This is
  deliberate: the tool schema can be curated for the LLM (descriptions, which
  fields are visible) without touching the domain, and Koog's reflection reads
  the tool model's annotations.
- Persisted domain models that are stored as whole JSON get a `Stored*` DTO
  because a pure domain model cannot be `@Serializable`. The DTO pins the
  storage format; changes to it are migrations.
- Jobs are not domain models: `ApiJob` caches a wire response for polling, so
  the job layer stays API-typed.

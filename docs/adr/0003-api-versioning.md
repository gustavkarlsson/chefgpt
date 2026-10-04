# Per-resource API versioning

Every endpoint lives under `/api/<resource>/v<k>/`, so released clients
keep working when the wire contract changes: breaking changes bump a
resource's version instead of changing it in place, and the server keeps
serving every version it has ever exposed.

## The scheme

- **Resources**: each top-level path root owns an independent version counter.
- **Paths**: `/api/<resource>/v<k>/...`, e.g. `/api/recipes/v1/scan`. The
  retrofit to v1 happened before the app's first release, so no unversioned
  legacy paths exist.
- **Bumps**: breaking changes only. The compatibility contract (response
  models add-only, request models remove-only, lenient JSON in both
  directions) lets additive changes stay within a version.
- **Cascade bumps**: when a shared wire model changes, every resource whose
  responses embed it bumps in the same release. Chosen over frozen snapshots
  (an embedding resource keeps serving the old shape indefinitely): one bump
  is unavoidable anyway, and cascading keeps the model set each client sees
  consistent. `ApiError` is embedded everywhere, so changing it bumps every
  resource.
- **Fossils**: each resource-version keeps its own frozen API models and
  routes; both versions of a resource delegate to the same domain logic.
- **Wire models**: `shared/.../api/<resource>/v<k>/` packages. A wire model
  lives in the package of the resource that owns it (`ApiUploadedFile` in
  `files`, `ApiJob` in `jobs`), and packages may import models from each
  other; `ApiError` is owned by no single resource and lives in a model-only
  package, `api/errors/v1`, which bumps like a resource. The invariant that
  makes imports safe: a frozen package may only import frozen packages, and
  when an imported model bumps, every importer cascade-bumps and updates its
  import. Structural primitives (ID value classes, serializers, header
  names) live unversioned in `api/common/`; changing one is a storage
  migration, not a versioning event.
- **Retirement**: no sunset policy — versions live indefinitely. If one is
  removed, its routes answer a bare 410 Gone with no body, and the client
  turns any 410 into a full-screen update-required state. Every client sends
  `Client-Platform` and `Client-Version` headers on every request, so the
  server can apply client-specific workarounds.

## Why path versions, per resource

Path segments are visible in logs and trivial to route, unlike header-based
versioning. Per-resource counters were chosen over one global version so an
unrelated breaking change (say, to recipes) does not force every resource to
bump; the cascade rule keeps shared models from drifting. Global generation
packages were rejected in favor of per-resource packages, which make the
copy-set explicit at the resource that owns it.

## Trade-offs

- A fossil's wire shape is no longer visible in its own package alone — a
  reader must follow imports to the owning package (and check which version
  of it the import points at). The frozen-package rule keeps this safe, at
  the cost of the invariant being implicit in imports rather than spelled
  out by copies.
- Fossils accumulate forever; nothing pressures a removal, and nothing finds
  dead ones. Retirements are ad hoc and may never happen.
- A client polls a job at the version it created the job with; the job's
  cached result bytes stay in the producing version's shape forever. The
  client remembers the version from its own request, so this never crosses
  versions in practice.

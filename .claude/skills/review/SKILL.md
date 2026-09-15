---
name: review
description: Holistic review of a change or the whole repo across code (correctness, style, and test coverage), documentation (correctness and stale references), and build performance. Use whenever asked to review, critique, or sanity-check code, a diff, a PR, documentation, the build, or test coverage, and before considering a task done.
---

# Review

Review the target across several dimensions and report findings ranked by importance.
Read the project's `.claude/rules/` and `.claude/skills/` for the conventions each
dimension leans on, and point to specific rules and skills rather than restating them.
Read a skill's frontmatter first, and the rest of the file only when it is relevant.

## Scope

Establish what is being reviewed before starting:

- **No target given** — review the uncommitted changes (`git diff`) plus the files they
  touch, with enough surrounding context to judge them.
- **A path, branch, or PR** — review that target.
- **"The repo" / "everything"** — review the whole repo, but weight each dimension by what
  is actually under review; a full-repo pass is broader and shallower than a change
  review.

State the scope in the report's title. When a review is requested on something large,
confirm the scope rather than silently reviewing everything.

## Code

Look for correctness bugs first, then style and reuse. Ground the style checks in the
project rules:

- **Correctness** — logic errors, null handling, error and edge cases, coroutine
  cancellation/threading, resource cleanup, and anything that would misbehave on real
  input. Trace the changed call paths rather than reading files in isolation.
- **Style** — check against the `kotlin-style`, `kotlin-data-classes`, and
  `json-serialization` rules.
- **Server conventions** — check new or changed routes against the `add-endpoint` skill;
  the snapshot-test requirement is in the `server-snapshot-tests` rule.
- **Reuse and simplification** — duplicated logic that already exists elsewhere, dead
  code, and over-engineered abstractions. Note reuse issues here but do not apply them.

### Test coverage

Judge whether the change is adequately tested against how each module tests its code.

- **Server** — routes are covered by snapshot tests per the `server-snapshot-tests`
  rule. Flag a new or changed route whose snapshot was not added or updated.
- **Shared and app** — logic is tested with `kotlin.test` in `commonTest`/`jvmTest`,
  following the `kotlin-tests` rule. Android host tests live in `src/*Test/` via
  `withHostTest {}`.
- **`androidApp`** — a thin wrapper with no tests; that is expected, not a gap.
- **Uncovered paths** — for each changed production file, ask what behaviour could break
  and whether a test would catch it. Flag changed code with no corresponding test and no
  good reason.
- **Vacuity** — flag tests that assert nothing meaningful, or that only assert a mock
  rather than the behaviour. Snapshot changes that just paper over an accidental response
  change are a bug, not upkeep (the `json-serialization` rule says the same).
- **Migrations** — SQLDelight migration verification (`verifyMigrations`) is enabled;
  a schema change must come with a migration.

Report each finding with `file:line`, what is wrong, and why it matters.

## Documentation

Check that documentation is correct and nothing references what no longer exists. The
docs here are `README.md`, `AGENTS.md`, `CLAUDE.md`, the `.claude/rules/` and
`.claude/skills/` files, KDoc, and meaningful comments.

- **Commands actually run** — every shell/Gradle command a doc tells the user to run must
  exist and be correct. Cross-check against `settings.gradle.kts` (included modules and
  tasks), `build.gradle.kts`, and the wrapper. The JVM test command appears in three
  places (`README.md`, `AGENTS.md`, the `verify` skill) and must agree.
- **Stale references** — file paths, module names, task names, flags, and named entities
  (classes, routes, models, rules, skills) referenced in prose that have since been
  renamed, moved, or removed. `grep` for each name the doc mentions and confirm it still
  exists.
- **Version and toolchain claims** — any doc that names a Java, Android, iOS, or Gradle
  version must match `gradle/libs.versions.toml`, `.sdkmanrc`, and
  `.github/actions/gradle-setup/action.yml`. Flag any drift.
- **Setup and helper scripts** — `setup_dev.sh`, `run_dev.sh`, and
  `install-spotless-pre-commit-hook.sh` are referenced from the docs; confirm they exist
  and do what the docs say.
- **Known caveats surfaced** — e.g. the iOS CI job is disabled (`if: false` in
  `verify.yml`); docs that imply iOS is fully CI-verified are stale.
- **KDoc and comments** — comments that describe behaviour the code no longer has, or
  TODOs pointing at since-removed code.
- **Redundancy** — documentation that duplicates other documentation and should reference
  it instead of copying it.
- **Misplaced** — documentation that belongs somewhere else — a different skill, rule, or
  file — and should move there.

## Build performance

Look for changes that would slow the build or break Gradle's caching. The
`build-performance` rule details the caching layers and how to keep them working — defer
to it for those specifics and flag anything that violates it, rather than restating it.

- **Version catalog hygiene** — versions must follow the `gradle-versions` rule.
- **Dependency scope and weight** — a new dependency should be `implementation` rather
  than `api` unless its types leak into the public API, `testImplementation` if test-only,
  and should not drag in a large transitive tree that an existing dependency already
  covers. Duplicate or near-duplicate dependencies are worth calling out.
- **CI** — `.github/workflows/verify.yml` and `.github/actions/gradle-setup/` control
  caching and job parallelism. Flag changes that write the Gradle cache from a job that
  should be read-only, add a redundant Gradle invocation, or serialize jobs that could
  run in parallel.

## Report

Group findings under the dimension headings, ranked most important first within each.
Only report what matters — a reviewer should not drown a change in nitpicks. Use this
structure:

```markdown
# Review — <scope>

## Code
- **[high|medium|low]** `path/file.kt:12` — what is wrong and why it matters.
  Suggested fix.

### Test coverage
- **[high|medium|low]** `path` — what is untested or vacuously tested.

## Documentation
- **[high|medium|low]** `path/to/doc.md` — what is stale or incorrect.

## Build performance
- **[high|medium|low]** `path` — the cost and the fix.

## Summary
One short paragraph: the most important 1–3 findings and an overall verdict.
```

If a dimension has nothing to report, say so in one line rather than omitting it.

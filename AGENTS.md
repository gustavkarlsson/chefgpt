# Coding Agent guidelines

## Repository Layout
- `server`: Ktor backend
- `app`: Kotlin Multiplatform client project
- `shared`: Shared code between server and clients
- `iosApp`: XCode project for the iOS client

## General Guidance
- Use modern kotlin language features when possible.
- Don't write more code than necessary to solve the problem.
- Lean towards functional programming patterns and avoid mutable data.
- Only comment the code when its purpose or implementation is unclear.
- Never use fully qualified references in code. Use imports instead (and typealiases when collisions occur).

## Dependencies
Dependencies are always defined using version catalogs. The version should always be a version.ref.

## Building and Testing
Format and check the code before committing or finishing a task:
```bash
./gradlew spotlessApply check
```

## Commit Messages
Follow the [Chris Beams](http://chris.beams.io/posts/git-commit/) style for commit messages.

## Pull Requests
Every pull request should answer:
- What changed?
- Why?
- Breaking changes?

## Review Checklist
- Add new tests for any new feature or bug fix.
- `./gradlew check` must pass.

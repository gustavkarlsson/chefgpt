---
description: Conventions for writing Kotlin tests in this project.
paths:
  - "**/src/test/**"
  - "**/src/*Test/**"
---

# Kotlin test conventions

## Structure: Arrange-Act-Assert

Divide each test into three labeled sections separated by a single blank line. No blank lines within a section.

Like this but without the descriptive comments.
```kotlin
test("should return sum of two numbers") {
    val subject = Calculator() // Arrange

    val result = subject.add(2, 3) // Act

    assertEquals(5, result) // Assert
}
```

The blank lines make the structure visible. Keeping each section as a continuous block (no internal blank lines) reinforces that everything in a section belongs together.

## Assertions: kotlin.test

Use kotlin.test for all assertions:

```kotlin
assertEquals("expected", "actual")
assertIs<String>(value)
assertNotNull(nullable)
assertFails { error("catch me") }
```

... and so on. Use the most accurate assertion for the test.

## One assertion per test

Each test should aim to check exactly one thing. This keeps failures precise and test names self-explanatory.
Multiple assertions are allowed if they collectively check a single thing.

## Test naming

Describe the scenario, not the implementation:

```kotlin
test("returns empty list when no items match filter")
test("throws IllegalArgumentException when input is negative")
```

## Server route tests

Ktor HTTP routes are covered by snapshot tests, which follow different conventions (JUnit5 API, stored JSON snapshots). Use the **snapshot-test** skill when writing or updating those.

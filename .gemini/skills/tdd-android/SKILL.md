---
name: tdd-android
description: Expert guidance on Test-Driven Development (TDD) for Android following Kent Beck's Canon TDD. Use when writing unit tests, integration tests, UI tests, or implementing new features.
---

# TDD for Android (Canon TDD)

This skill provides procedural knowledge for implementing and maintaining a comprehensive testing strategy for Android using JUnit, Mockito, and Espresso, strictly following Kent Beck's "Canon TDD" principles.

## Core Principles

1.  **The Test List (Task Plans)**: Before writing any code, create a comprehensive list of all behaviors the new feature should exhibit, including basic cases, edge cases, and potential regressions.
2.  **Red-Green-Refactor**: Follow the strict cycle: Write a failing test (Red), make it pass with minimal code (Green), and then improve the design (Refactor).
3.  **One Test at a Time**: Only implement and make one test pass at a time.
4.  **Test Independence**: Each test should run independently of others.
5.  **Interface Design First**: Writing the test is when you make decisions about the interface (API) of your code.
6.  **Implementation Design Later**: Refactoring is when you make decisions about the internal implementation and structure.

## Kent Beck's Implementation Strategies

### 1. Fake It ('Til You Make It)
-   **Goal**: Get to Green quickly.
-   **Method**: Hardcode a constant value to make the test pass. Gradually replace constants with real logic as you add more tests.
-   **When**: When you want to confirm the interface/test setup before tackling complex logic.

### 2. Triangulation
-   **Goal**: Derive more general logic from specific examples.
-   **Method**: Write two or more tests with different inputs. The logic that makes both pass should be the general solution.
-   **When**: When the logic is tricky and you want to be sure it's correct (e.g., complex string formatting).

### 3. Obvious Implementation
-   **Goal**: Fast forward to Green.
-   **Method**: Type the obvious code that solves the problem.
-   **When**: When the implementation is simple and you have high confidence. If you make a mistake, drop back to "Fake It" or "Triangulation."

## Testing Best Practices

### 1. Speed & Fast Feedback
-   **Rule**: Tests MUST be fast. Slow tests are ignored tests.
-   **Fakes over Mocks**: Prefer using simple "Fake" implementations (e.g., `InMemoryDatabase`, `MockClock`) over heavy Mocking frameworks (Mockito/MockK) when possible. Fakes are faster and more robust.
-   **Unit Tests first**: Prioritize pure unit tests (`app/src/test/`) over instrumented tests (`app/src/androidTest/`). Use Robolectric only when necessary for Android framework simulation.

### 2. Testing Behavior, Not Implementation
-   **Rule**: Don't test private methods or internal state. Test the public API (the "What," not the "How").
-   **Refactoring Safety**: Tests should remain passing when you refactor the internal implementation. If tests break during refactoring, they were too tightly coupled to the implementation.

## Workflows

### Implementing a New Feature (Canon TDD Workflow)

1.  **Create the Test List**:
    -   Perform a behavioral analysis of the desired change.
    -   List all expected variants: basic success, edge cases, and security constraints.
2.  **Pick One Test**:
    -   Select exactly one item from the Test List to implement.
3.  **Write a Failing Test (Red)**:
    -   Implement the test. Make **interface design decisions** here.
    -   Run the test and confirm it fails as expected.
4.  **Make it Pass (Green)**:
    -   Use "Fake It," "Triangulation," or "Obvious Implementation."
    -   Write the **minimal amount of code** necessary.
5.  **Optionally Refactor**:
    -   Clean up the code once the test passes. Ensure all tests still pass.
6.  **Repeat**:
    -   Continue this cycle until the Test List is empty.

## References
-   See the `architectural-design` skill for visualizing the components and interactions being tested.
-   See the `ddd-clean-architecture` skill for designing testable, decoupled domain logic.
-   See `docs/explanation/testing-strategy.md` for project-specific test implementations.
-   Kent Beck's Canon TDD: [https://substack.com/home/post/p-139601698](https://substack.com/home/post/p-139601698)

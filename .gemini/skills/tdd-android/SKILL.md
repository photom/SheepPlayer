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

## Workflows

### Implementing a New Feature (Canon TDD Workflow)

1.  **Create the Test List**:
    -   Perform a behavioral analysis of the desired change.
    -   List all expected variants: basic success, edge cases (nulls, timeouts, empty lists), and security constraints.
    -   Add this list as a comment or a separate task plan before starting.
2.  **Pick One Test**:
    -   Select exactly one item from the Test List to implement.
3.  **Write a Failing Test (Red)**:
    -   Implement the test with proper setup, invocation, and assertions.
    -   Make **interface design decisions** here (how the classes/methods are called).
    -   Run the test and confirm it fails as expected.
4.  **Make it Pass (Green)**:
    -   Write the **minimal amount of code** necessary to make the test pass.
    -   If new test cases are discovered during this phase, add them to the Test List.
5.  **Optionally Refactor**:
    -   Once the test passes, clean up the code.
    -   Make **implementation design decisions** here (improving readability, removing duplication).
    -   Ensure all tests (including previous ones) still pass.
6.  **Repeat**:
    -   Continue this cycle until the Test List is empty.

### Example: Implementing `formatDuration` with TDD

**1. Test List:**
- [ ] `formatDuration(0)` -> "0:00"
- [ ] `formatDuration(30000)` -> "0:30" (seconds)
- [ ] `formatDuration(90000)` -> "1:30" (minutes/seconds)
- [ ] `formatDuration(605000)` -> "10:05" (padding)
- [ ] `formatDuration(-1000)` -> "0:00" (negative case)
- [ ] `formatDuration(Long.MAX_VALUE)` -> handle gracefully

**2. Execution:**
- Pick `formatDuration(0)`.
- Write test, watch it fail (or not exist).
- Implement `return "0:00"`.
- Pick next item...

## Testing Strategy

### 1. Unit Testing (`app/src/test/`)
-   **Focus**: Domain logic, utility functions, business rules.
-   **Tools**: JUnit 4/5, Mockito/MockK.

### 2. Integration Testing
-   **Focus**: Component interaction, database operations, repository-service interaction.
-   **Tools**: Robolectric, AndroidX Test.

### 3. UI Testing (Espresso) (`app/src/androidTest/`)
-   **Focus**: Critical user workflows, navigation, UI state changes.
-   **Tools**: Espresso.

## References
-   See `docs/TESTING_GUIDE.md` for project-specific test implementations.
-   Kent Beck's Canon TDD: [https://substack.com/home/post/p-139601698](https://substack.com/home/post/p-139601698)

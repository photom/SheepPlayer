---
name: ddd-clean-architecture
description: Expert guidance on Domain-Driven Design and Clean Architecture for Android. Use when designing system components, defining layer boundaries, or refactoring for SOLID compliance.
---

# DDD & Clean Architecture for Android

This skill provides procedural knowledge for implementing and maintaining a robust, scalable, and testable Android architecture using Domain-Driven Design (DDD), Clean Architecture, and SOLID principles.

## Core Principles

1.  **Ubiquitous Language**: Use the same terminology in code (classes, methods, variables) as used by domain experts and stakeholders.
2.  **Separation of Concerns**: Each class has one responsibility (SRP). Domain logic must be pure and independent of UI or Frameworks.
3.  **Dependency Inversion**: High-level modules (Domain) depend on abstractions, and low-level modules (Infrastructure) implement those abstractions (DIP).
4.  **Encapsulation**: Domain logic is isolated. State changes happen through well-defined Domain methods, not direct property manipulation.

## DDD Tactical Patterns

### 1. Entities & Value Objects
-   **Entities**: Objects with a thread of continuity and identity (e.g., `User`, `Track`). Equality is based on ID.
-   **Value Objects**: Objects that describe things but have no identity (e.g., `Duration`, `FilePath`, `PlaybackState`). Equality is based on all properties. Use `data class` in Kotlin.
-   **Guideline**: Prefer Value Objects over primitive types (Primitive Obsession) to encapsulate validation logic.

### 2. Aggregates & Roots
-   **Aggregates**: A cluster of associated objects treated as a unit for data changes.
-   **Aggregate Root**: The single entry point to the aggregate (e.g., `Playlist` is a root for `PlaylistItems`). All external references must go through the Root.

### 3. Repositories
-   **Responsibility**: Provide an interface for retrieving and persisting Aggregates.
-   **Guideline**: The interface belongs to the **Domain Layer**, while the implementation belongs to the **Data/Infrastructure Layer**.

### 4. Domain Services & Use Cases
-   **Domain Services**: Logic that doesn't naturally belong to a single Entity or Value Object.
-   **Use Cases (Interactors)**: Orchestrate the flow of data to and from the domain, and direct those domain objects to use their critical business rules to achieve the goals of the use case.

## Architectural Layers

### 1. Domain Layer (`domain/`)
-   **Contents**: Entities, Value Objects, Repository Interfaces, Domain Service Interfaces, Use Cases.
-   **Constraint**: ZERO dependencies on Android frameworks or external libraries (Pure Kotlin).

### 2. Presentation Layer (`ui/` / `presentation/`)
-   **Contents**: ViewModels, States, UI Components (Compose/Fragments).
-   **Pattern**: MVI (Model-View-Intent) or MVVM.
-   **Responsibility**: Mapping Domain Entities to UI-friendly State objects.

### 3. Data Layer (`data/`)
-   **Contents**: Repository Implementations, Data Sources (Room, Retrofit), Mappers.
-   **Responsibility**: Converting raw data (DTOs) into Domain Entities and vice-versa.

### 4. Infrastructure Layer (`di/`, `system/`)
-   **Contents**: Dependency Injection, Framework wrappers (Loggers, Analytics), System services.

## SOLID & DDD Implementation Checklist

-   [ ] **Ubiquitous Language**: Does the code use domain terms like "Track" instead of "AudioFile"?
-   [ ] **Value Objects**: Are we using `Duration(ms: Long)` instead of just `Long` for playback time?
-   [ ] **Rich Domain Model**: Does the Entity have logic (e.g., `track.play()`), or is it just a "bag of getters" (Anemic Domain Model)?
-   [ ] **DIP**: Is the ViewModel depending on a `Repository` interface or a concrete `RoomRepository`?

## Workflows

### Defining a New Domain Concept
1.  Identify if it's an **Entity** (needs ID) or **Value Object** (identity-less).
2.  Define the **Ubiquitous Language** terms.
3.  Implement the logic within the Domain object (methods that enforce invariants).
4.  Create a **Domain Event** if other parts of the system need to react to changes.

### Adding a Use Case
1.  Define the input (Request) and output (Result/State).
2.  Inject necessary Repository interfaces.
3.  Implement the business orchestration logic.
4.  Write a pure Unit Test for the Use Case.

## References
-   See `docs/reference/project-structure.md` for current directory mappings.
-   See `docs/explanation/solid-compliance.md` for specific implementation examples.

---
name: ddd-clean-architecture
description: Expert guidance on Domain-Driven Design and Clean Architecture for Android. Use when designing system components, defining layer boundaries, or refactoring for SOLID compliance.
---

# DDD & Clean Architecture for Android

This skill provides procedural knowledge for implementing and maintaining a robust, scalable, and testable Android architecture using Domain-Driven Design (DDD), Clean Architecture, and SOLID principles.

## Core Principles

1.  **Separation of Concerns**: Each class has one responsibility (SRP).
2.  **Dependency Inversion**: High-level modules depend on abstractions (interfaces), not concrete implementations (DIP).
3.  **Encapsulation**: Domain logic is isolated from UI and infrastructure details.
4.  **Interface Segregation**: Clients should not be forced to depend on methods they do not use (ISP).

## Architectural Layers

### 1. Presentation Layer (`ui/`)
-   **Components**: Activities, Fragments, ViewModels, Adapters.
-   **Responsibility**: Rendering data to the user and handling user interactions.
-   **Pattern**: MVVM (Model-View-ViewModel).
-   **Guideline**: Keep fragments and activities "lean." Move logic to ViewModels.

### 2. Business Logic Layer (`handlers/`, `manager/`)
-   **Components**: Use Cases, Interactors, State Managers.
-   **Responsibility**: Orchestrating data flow and enforcing business rules.
-   **Guideline**: Business logic should be platform-agnostic whenever possible.

### 3. Data Layer (`repository/`, `service/`)
-   **Components**: Repositories, Data Sources (Local/Remote).
-   **Responsibility**: Data persistence and retrieval.
-   **Pattern**: Repository Pattern for data abstraction.
-   **Guideline**: Use interfaces for all data services to allow for easy mocking and swapping.

### 4. Domain/Infrastructure Layer (`interfaces/`, `factory/`)
-   **Components**: Entity models (`Data.kt`), Interfaces, Dependency Injection logic.
-   **Responsibility**: Defining the core business models and system boundaries.

## SOLID Implementation Checklist

-   [ ] **SRP**: Is this class doing too much? (e.g., handles UI AND data loading).
-   [ ] **OCP**: Can I add a new feature without changing this class? (e.g., using Strategy pattern).
-   [ ] **LSP**: Can I swap an implementation with another (or a mock) without breaking the system?
-   [ ] **ISP**: Are interfaces small and focused?
-   [ ] **DIP**: Am I injecting interfaces instead of concrete classes?

## Workflows

### Refactoring for SOLID Compliance
1.  Identify a "God Object" (e.g., `MainActivity` handling everything).
2.  Extract specific responsibilities into handlers (e.g., `PermissionHandler`, `GoogleDriveAuthHandler`).
3.  Define interfaces for these handlers.
4.  Use a `DependencyFactory` to manage their creation and injection.

### Adding a New Feature
1.  Define the **Domain Entity** (if new data is involved).
2.  Define the **Repository Interface** for data access.
3.  Implement the **Repository** (Data Layer).
4.  Create a **ViewModel** to handle UI state (Presentation Layer).
5.  Implement the **Fragment/Activity** (UI Layer).

## References
-   See `docs/PROJECT_STRUCTURE.md` for current directory mappings.
-   See `docs/SOLID_COMPLIANCE.md` for specific implementation examples.

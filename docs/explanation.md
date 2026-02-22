# Explanation

This section provides conceptual understanding of SheepPlayer's architectural decisions, specifically the adoption of Domain-Driven Design (DDD) and Clean Architecture, and the reasoning behind key implementation details.

## Why DDD & Clean Architecture?

SheepPlayer has transitioned from a traditional layered architecture to one based on **Domain-Driven Design (DDD)** and **Clean Architecture**. This strategic shift addresses several critical aspects of modern Android development:

### 1. Framework Independence
-   **The Core is Pure**: The Domain layer (Entities, Use Cases) contains *zero* Android dependencies. It is pure Kotlin.
-   **Future-Proofing**: If Android UI libraries change (e.g., from XML to Compose) or database technologies evolve (e.g., from SQLite to Room or Realm), the core business logic remains untouched.
-   **Benefit**: The most valuable part of the application—the business rules—is protected from external churn.

### 2. Testability First
-   **Isolated Logic**: Because the Domain layer has no dependencies on the Android framework, unit tests can run instantly on the JVM without needing an emulator or device.
-   **Mocking Power**: Interfaces defined in the Domain layer allow us to easily swap real implementations (like `MusicRepositoryImpl` talking to `MediaStore`) with fake ones (like `FakeMusicRepository` returning a static list) for rigorous testing.
-   **Benefit**: Faster, more reliable test suites lead to higher code quality and fewer regressions.

### 3. Scalability & Maintainability
-   **Clear Boundaries**: Each feature is implemented as a set of Use Cases. Adding a new feature (e.g., "Create Playlist") involves adding a new Use Case and potentially a new Entity method, without modifying unrelated code.
-   **Cognitive Load**: Developers only need to understand the specific Use Case they are working on, not the entire application flow.
-   **Benefit**: The codebase can grow in complexity without becoming a "Big Ball of Mud."

## Architectural Philosophy

### Separation of Concerns
The application is divided into concentric circles, with the Domain at the center.

-   **Entities**: Represent core business concepts (`Track`, `Artist`, `Album`). They encapsulate state and behavior.
-   **Use Cases**: Orchestrate the flow of data to and from Entities. They tell the Entities *what* to do.
-   **Interface Adapters**: Convert data from the format most convenient for the Use Cases and Entities, to the format most convenient for external agencies (Database, Web).
-   **Frameworks & Drivers**: The outermost layer. This is where the Database and the Web Framework live.

### Dependency Rule
Source code dependencies can only point **inwards**. Nothing in an inner circle can know anything at all about something in an outer circle.

-   **Data Layer** knows about **Domain Layer**.
-   **Presentation Layer** knows about **Domain Layer**.
-   **Domain Layer** knows *nothing* about Data or Presentation.

## Key Design Decisions

### Repository Pattern (In DDD Context)
In DDD, the Repository is an abstraction that represents a collection of Domain Entities.

-   **Interface in Domain**: The `MusicRepository` interface is defined in the Domain layer. It speaks the language of the domain (e.g., `getArtists(): List<Artist>`).
-   **Implementation in Data**: The `MusicRepositoryImpl` is in the Data layer. It handles the details of `MediaStore` queries, cursors, and mapping to Domain Entities.

### Use Cases as Interactors
We explicitly define classes for every user action or business rule (e.g., `PlayMusicUseCase`, `SearchLibraryUseCase`). This makes the codebase self-documenting: looking at the `domain/usecase` package tells you exactly what the application *does*.

## Security & Data Flow

### Secure by Design
-   **Domain Validation**: Entities enforce their own invariants (e.g., a `Track` cannot have a negative duration).
-   **Data Sanitization**: The Data layer is responsible for sanitizing external inputs (files, network data) before they are converted into Domain Entities.
-   **Presentation Logic**: The UI handles user input validation but delegates all business logic to the Use Cases.

### Unidirectional Data Flow
Data flows in a single direction:
1.  **UI Event**: User clicks "Play".
2.  **ViewModel**: Calls `PlayMusicUseCase`.
3.  **Use Case**: Interacts with `MusicRepository` (Domain Interface).
4.  **Repository Impl**: Fetches/updates data (Data Layer).
5.  **Return**: Domain Entities are returned up the stack.
6.  **State Update**: ViewModel updates `uiState`.
7.  **UI Render**: Fragment observes state and redraws.

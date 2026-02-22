---
name: architectural-design
description: Expert guidance on designing software systems using Mermaid diagrams and descriptive sentences. Use when planning new features, refactoring systems, or documenting architecture without writing code.
---

# Architectural Design & Visual Documentation

This skill provides procedural knowledge for designing and documenting software systems using visual diagrams (Mermaid) and descriptive narratives. It focuses on high-level architecture, component interactions, and data structures while strictly avoiding implementation-specific programming code in the design phase.

## Core Principles

1.  **Visual-First Design**: Use Mermaid diagrams as the primary tool for representing structure, behavior, and state.
2.  **Narrative-Second Design**: Use concise, clear sentences to explain the "why" and "how" of the design, providing context that diagrams cannot.
3.  **Strict Code Exclusion**: Do not use programming code (Java, Kotlin, C++, etc.) to describe design. Focus on abstractions, interfaces, and data types instead.
4.  **Clarity & Precision**: Ensure diagrams are easy to read and sentences are direct. Use standard UML conventions where applicable.

## Design Diagrams (Mermaid)

### 1. Structural Design (Class Diagram)
-   **Goal**: Define components, their attributes, and their relationships (inheritance, composition, aggregation).
-   **Style**: Use `classDiagram`. Represent abstractions and interfaces clearly.
-   **Example**: Defining the relationship between `MusicPlayer`, `MusicPlayerManager`, and `MusicRepository`.

### 2. Behavioral Design (Sequence Diagram)
-   **Goal**: Illustrate how components interact over time to fulfill a use case.
-   **Style**: Use `sequenceDiagram`. Show synchronous calls, asynchronous events, and error handling paths.
-   **Example**: The flow from "User clicks Play" to "Music starts playing."

### 3. State Design (State Diagram)
-   **Goal**: Map the lifecycle and internal states of a complex component.
-   **Style**: Use `stateDiagram-v2`. Define valid transitions and triggers.
-   **Example**: The states of the `MusicPlayer` (Idle, Buffering, Playing, Paused, Error).

### 4. Data Design (ER Diagram)
-   **Goal**: Model the relationships between data entities.
-   **Style**: Use `erDiagram`. Define entities, attributes, and cardinality.
-   **Example**: The relationship between `Artist`, `Album`, and `Track`.

## Workflows

### Planning a New Feature
1.  **Define Requirements**: State the goals and constraints in clear sentences.
2.  **Structural Mapping**: Create a `classDiagram` to identify the new components needed.
3.  **Interaction Modeling**: Create a `sequenceDiagram` to show how the feature will function.
4.  **State Analysis**: If the component is stateful, create a `stateDiagram-v2`.
5.  **Data Analysis**: If new data is introduced, create an `erDiagram`.
6.  **Narrative Summary**: Write a concise summary of the design's benefits and potential risks.

### Documenting Existing Architecture
1.  **Analyze**: Review the existing codebase (using standard tools, not as part of the design output).
2.  **Extract**: Identify the core components and their interactions.
3.  **Translate**: Convert the code-level relationships into Mermaid diagrams.
4.  **Clarify**: Add descriptive sentences to explain parts of the system that are not obvious from the diagrams.

## Design Patterns & Standards
-   **Clean Architecture Boundaries**: Use diagrams to show the separation between Presentation, Business Logic, and Data layers.
-   **SOLID Compliance**: Visualize how DIP is achieved through interface-based relationships in class diagrams.

## References
-   See `docs/PROJECT_STRUCTURE.md` for current high-level architectural overview.
-   See `docs/SOLID_COMPLIANCE.md` for how current design patterns are applied.
-   Visit [https://mermaid.js.org/](https://mermaid.js.org/) for official Mermaid diagram documentation.

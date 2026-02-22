---
name: diataxis
description: Expert guidance on the Diátaxis framework for technical documentation. Use when creating or refactoring tutorials, how-to guides, reference material, or explanations to ensure clear, user-centric documentation.
---

# Diátaxis Framework for Documentation

This skill provides procedural knowledge for implementing the Diátaxis framework, a systematic approach to technical documentation that categorizes content into four distinct types based on user needs.

## Core Principles

1.  **User-Centric**: Focus on what the user is trying to achieve at a specific moment.
2.  **Clear Categorization**: Every piece of documentation should fit clearly into one of the four types.
3.  **No Overlap**: Avoid mixing types (e.g., don't include extensive explanations in a tutorial).

## The Four Documentation Types

### 1. Tutorials (Learning-Oriented)
-   **Goal**: Help the user get started and achieve a small, meaningful success.
-   **Style**: Learning by doing, step-by-step, prescriptive, and reliable.
-   **Guideline**: Focus on the experience of doing, not on the theory. Ensure it always works.
-   **SheepPlayer Example**: `docs/tutorial.md` (e.g., "Your First Track with SheepPlayer").

### 2. How-to Guides (Goal-Oriented)
-   **Goal**: Provide a recipe for solving a specific, real-world problem.
-   **Style**: Practical, task-focused, assuming some prior knowledge.
-   **Guideline**: Focus on the task, not the user. Keep it brief and direct.
-   **SheepPlayer Example**: `docs/how-to-guides.md` (e.g., "How to Sync with Google Drive").

### 3. Reference (Information-Oriented)
-   **Goal**: Provide technical, factual descriptions of the machinery.
-   **Style**: Descriptive, precise, organized for lookup, neutral.
-   **Guideline**: Focus on accuracy and completeness. Use tables and lists for quick scanning.
-   **SheepPlayer Example**: `docs/reference.md` or `docs/API_REFERENCE.md`.

### 4. Explanation (Understanding-Oriented)
-   **Goal**: Clarify and illuminate a particular topic or concept.
-   **Style**: Discussion-based, discursive, providing context and alternatives.
-   **Guideline**: Focus on the "why" and "how it works." Build a mental model for the user.
-   **SheepPlayer Example**: `docs/explanation.md` (e.g., "Understanding the Security Architecture").

## Workflows

### Creating New Documentation
1.  **Identify the User Need**: Is the user learning, solving a task, looking up a fact, or seeking understanding?
2.  **Choose the Type**: Select from Tutorial, How-to, Reference, or Explanation.
3.  **Apply the Pattern**: Follow the style guidelines for the chosen type.
4.  **Review for Overlap**: Remove any content that belongs to a different documentation type.

### Refactoring Existing Documentation
1.  **Audit**: Review the existing `docs/` directory.
2.  **Categorize**: Map each file to one of the four Diátaxis types.
3.  **Split/Merge**: If a file contains multiple types, split it into separate files or move sections to their appropriate homes.
4.  **Align Styles**: Rewrite sections to match the prescriptive, goal-oriented, descriptive, or discursive style required.

## References
-   See `docs/` for the current implementation of Diátaxis in this project.
-   Visit [https://diataxis.fr/](https://diataxis.fr/) for the official framework documentation.

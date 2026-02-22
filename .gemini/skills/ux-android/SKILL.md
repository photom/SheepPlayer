---
name: ux-android
description: Expert guidance on creating user-friendly and aesthetically pleasing Android applications. Use when designing UIs, implementing animations, or ensuring accessibility.
---

# UX-Friendly Android Development

This skill provides procedural knowledge for designing and implementing high-quality, user-friendly Android applications following Material Design 3 (M3) principles, ensuring accessibility, and providing a polished user experience.

## Core Principles

1.  **Aesthetics & Visual Impact**: First impressions matter. Use modern design patterns, consistent spacing, and high-quality assets.
2.  **Responsiveness**: The app must work seamlessly across different screen sizes (phones, tablets, foldables).
3.  **Accessibility (A11y)**: Design for everyone. Ensure high contrast, sufficient touch targets, and support for screen readers (TalkBack).
4.  **Feedback & Interactivity**: Provide immediate visual/haptic feedback for user actions (ripples, transitions, animations).
5.  **Performance (Jank-Free)**: UI must be smooth (60/120 FPS). Avoid heavy operations on the Main Thread.

## UX Focus Areas

### 1. Material Design 3 (M3)
-   **Dynamic Color**: Use `Dynamic Colors` to adapt to the user's wallpaper.
-   **Typography**: Use the M3 type scale for consistency.
-   **Components**: Use M3 components (e.g., `MaterialCard`, `TopAppBar`, `NavigationBar`) for a modern feel.

### 2. Accessibility
-   **Touch Targets**: Minimum 48x48dp for all interactive elements.
-   **Content Descriptions**: Provide descriptive `contentDescription` for non-text elements (images, icons).
-   **Contrast**: Ensure text-to-background contrast meets WCAG 2.1 AA standards.

### 3. Motion & Animation
-   **Purposeful Motion**: Use transitions to guide the user between states.
-   **Shared Elements**: Use Shared Element Transitions for a "connected" feel between screens.
-   **Loading States**: Use Shimmer effects or purposeful animations (like `searching_animation.xml`) instead of generic spinners.

### 4. Layout & Navigation
-   **Predictable Navigation**: Follow Android navigation patterns (Back button behavior, Bottom Navigation).
-   **Edge-to-Edge**: Implement edge-to-edge layouts to utilize the full screen.
-   **Empty States**: Design informative and encouraging empty states (e.g., "No tracks yet. Add some to get started!").

## UX Workflows

### Implementing a New Screen
1.  **Define User Goal**: What is the primary action on this screen?
2.  **Wireframe/Layout**: Use M3 components. Ensure proper spacing (8dp grid).
3.  **Add Feedback**: Implement ripple effects, click listeners, and transitions.
4.  **Check Accessibility**: Run the Accessibility Scanner or check contrast and touch targets manually.
5.  **Test Responsiveness**: Verify the layout on small and large screen emulators.

### Improving App Performance (UX)
1.  **Identify Jank**: Use the Profiler to detect slow frames.
2.  **Optimize Images**: Use SVG/VectorDrawables where possible. Use Glide/Coil for efficient bitmap loading.
3.  **Lazy Loading**: Use `LazyColumn` (Compose) or `RecyclerView` for long lists.
4.  **Offload Work**: Ensure all IO/Database work happens on a background thread (Coroutines).

## UX Checklist
-   [ ] **M3 Compliance**: Are we using M3 colors and components?
-   [ ] **Touch Targets**: Are all buttons at least 48x48dp?
-   [ ] **Content Descriptions**: Do all icons have descriptions?
-   [ ] **Motion**: Does the screen transition feel smooth?
-   [ ] **Edge-to-Edge**: Is the app content flowing behind the status bar?

## References
-   See the `architectural-design` skill for modeling user interactions and UI state transitions via Mermaid.
-   See the `ddd-clean-architecture` skill for connecting UI components to a robust domain model.
-   Material Design 3 Guidelines: [https://m3.material.io/](https://m3.material.io/)
-   Android Accessibility Guide: [https://developer.android.com/guide/topics/ui/accessibility](https://developer.android.com/guide/topics/ui/accessibility)
-   Android Performance: [https://developer.android.com/topic/performance](https://developer.android.com/topic/performance)

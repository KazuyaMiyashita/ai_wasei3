# Next Implementation Plan

This document outlines the roadmap for remaining features and technical tasks, prioritized by their impact on the core application functionality.

## 1. Core Editing Capabilities (High Priority)

The foundation for semantic music editing is in place but lacks concrete implementation.

- [x] **Implement `MeiElementReplaceAction` Logic:**
  - Currently, `src/lib/editor/actions.ts` has a naive placeholder for `applyTo`.
  - **Goal:** Implement actual DOM manipulation within `ContentAdapter` or a helper to properly replace MEI elements while preserving validity.
- [x] **Implement MEI Mutation Methods:**
  - `src/lib/model/music/mei.ts` contains empty stubs for `updateNoteUp` and `updateNoteDown`.
  - **Goal:** Implement the logic to modify pitch attributes in the DOM and return the necessary `IEditAction`.
- [x] **Connect `richEdit` in `ActiveDocument`:**
  - `src/lib/editor/active-document.ts` has a placeholder for the `richEdit` method.
  - **Goal:** Wire up the UI (e.g., key commands or context menu) to trigger `richEdit`, which calculates the action via `MEI` model and applies it.

## 2. Project Management & Workflow (High Priority)

Basic file operations exist, but creation and full lifecycle management are incomplete.

- [x] **"New Project" Functionality:**
  - `src/components/header/Menubar.tsx` alerts that this is not implemented.
  - **Goal:** Implement a dialog or flow to create a new blank MEI file (or from template) and open it in the workspace.
- [x] **Robust Tab Closing Strategy:**
  - `src/components/center/Tabs.tsx` notes a need for a "Save and Close" vs "Cancel" workflow.
  - **Goal:** Improve the UX when closing dirty documents.

## 3. Interaction & UI Refinement (Medium Priority)

- [x] **Context Menu:**
  - `src/hooks/score/useScoreRenderer.ts` has a commented-out alert for context menus.
  - **Goal:** Implement a context menu for score elements (e.g., right-click on a note to transpose or delete).
- [x] **Footer Notification Truncation:**
  - `src/components/footer/Footer.tsx` has a TODO about `truncate` not working as expected.
  - **Goal:** Fix CSS/layout to ensure long messages do not break the footer layout.

## 4. Audio & Visualization (Medium Priority)

Playback infrastructure exists but uses dummy data.

- [x] **Real Audio Generation:**
  - `src/lib/audio/generator.ts` returns static `parsedSampleData` for note playback.
  - **Goal:** Implement logic to generate `PartData` dynamically from the selected MEI element's attributes (pitch, duration).
- [x] **Live Audio Visualizer:**
  - `src/components/header/AudioControlBar.tsx` uses hardcoded values ("70%") for the visualizer.
  - **Goal:** Connect the visualizer components to the `Performer` or `AudioEngine` real-time analysis data.

## 5. Model Enhancements & Tech Debt (Low Priority)

- [x] **Complete Music Elements Model:**
  - `src/lib/model/music/elements.ts` has TODOs for `NoteName` and `Octave` value objects.
  - **Goal:** Flesh out these classes to encapsulate music theory logic.
- [x] **MEI Structure Navigation:**
  - `src/lib/model/music/mei.ts` has a TODO for `getKeyAt`.
  - **Goal:** Implement traversal logic to find the active key signature/time signature for a given context.
- [x] **Accessibility & Linting:**
  - Address `biome-ignore` comments related to accessibility (`useKeyWithClickEvents`) in `Workspace.tsx` and `DocumentView.tsx`.

## 6. Future Ideas (Wishlist)

These features are not essential for the initial release but would significantly enhance the user experience and utility.

- [ ] **Web MIDI API Integration:**
  - Allow note input via a connected MIDI keyboard.
  - Implement MIDI export functionality.
- [ ] **Automated Analysis Tools:**
  - Implement real-time harmony analysis or error detection (e.g., parallel fifths checks for "wasei" tasks).
  - Auto-tagging of chord symbols based on the score content.
- [ ] **Lyrics & Text Editor:**
  - Provide a user-friendly interface for inputting lyrics under notes without manually editing XML.
- [ ] **Part Management:**
  - Ability to toggle visibility of specific parts (instruments) in the score.
  - "Extract Part" functionality to create a new document for a single instrument.
- [ ] **Collaborative Editing:**
  - Real-time collaboration using CRDTs (e.g., Yjs) for multi-user editing sessions.
- [ ] **History Visualizer:**
  - A visual undo/redo stack that lets users jump back to any previous state of the document.

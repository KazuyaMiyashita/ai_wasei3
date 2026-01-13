# Tasks: Refactor Editor Structure

- [x] **Rename ViewMode to LayoutMode**
    - [x] `frontend22/src/lib/view/view-state.ts`: `ViewMode` -> `LayoutMode`, `viewMode` -> `layoutMode`.
    - [x] `frontend22/src/lib/application.ts`: `setViewMode` -> `setLayoutMode`.
    - [x] `frontend22/src/components/header/ScoreControlBar.tsx`: props/methods update.
    - [x] `frontend22/src/components/center/Main.tsx`: state selector update.

- [x] **Refactor CodeView to CodeEditor**
    - [x] Rename `frontend22/src/components/center/CodeView.tsx` to `frontend22/src/components/center/CodeEditor.tsx`.
    - [x] Rename component export `CodeView` -> `CodeEditor`.
    - [x] Ensure it works as a standalone editor (it already does via ActiveDocument).

- [x] **Create DocumentEditor Component**
    - [x] Create `frontend22/src/components/center/DocumentEditor.tsx`.
    - [x] Extract ProseMirror logic from `SplitEditor.tsx`.
    - [x] Implement `ActiveDocument` subscription/sync logic similar to `CodeEditor` but for ProseMirror (AST conversion).
    - [x] Ensure styling matches `DocumentView` (padding, etc.).

- [x] **Update Main Component**
    - [x] `frontend22/src/components/center/Main.tsx`:
    - [x] Remove `SplitEditor` import.
    - [x] Import `DocumentEditor`, `CodeEditor`.
    - [x] Implement the logic: `editMode` に応じて `documentView` / `documentEditor` を選び、`layoutMode` に応じてレイアウトを決める。
    - [x] Remove the early return for `xhtml5+mei` (L49-54 logic).

- [x] **Clean up**
    - [x] Remove `frontend22/src/components/editor/SplitEditor.tsx`.
    - [x] Remove `frontend22/src/components/editor` directory.

- [x] **Verify**
    - [x] Check build (`pnpm check`).
    - [x] Verify `sample_content.xml` rendering and editing in all modes.
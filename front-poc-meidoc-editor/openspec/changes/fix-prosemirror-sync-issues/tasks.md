# Tasks: Fix ProseMirror Sync and UX Issues

## Phase 1: Whitespace and Basic Sync Refinement

- [x] **RST Whitespace Filtering**: Update `toProseMirrorNode` in `ResilientSyntaxTree.ts` to filter out formatting whitespace.
  - [x] Add test cases in `ResilientSyntaxTree.test.ts`.
  - [x] Implement heuristic in `toProseMirrorNode`.
  - [x] Run `pnpm test` to verify.
  - [x] Run `pnpm check` to ensure code quality.
- [x] **Transaction Step Extension**: Update `ProseMirrorAdapter.ts` to use `DOMSerializer` for `ReplaceStep`.
  - [x] Add test cases in `ProseMirrorAdapter.test.ts`.
  - [x] Implement `DOMSerializer` usage.
  - [x] Run `pnpm test` to verify.
  - [x] Run `pnpm check` to ensure code quality.

## Phase 2: Mark and Attribute Support

- [x] **Bidirectional Mark Support**:
  - [x] Add test cases.
  - [x] Map inline tags (`b`, `i`, `u`, etc.) to PM marks in `toProseMirrorNode`.
  - [x] Update `getChangesFromTransaction` to handle `AddMarkStep` and `RemoveMarkStep`.
  - [x] Run `pnpm test` to verify.
  - [x] Run `pnpm check` to ensure code quality.
- [x] **Attribute Support**:
  - [x] Add test cases.
  - [x] Update `getChangesFromTransaction` to handle `AttrStep`.
  - [x] Run `pnpm test` to verify.
  - [x] Run `pnpm check` to ensure code quality.

## Phase 3: Robustness and Finalization

- [x] **Position Mapping Audit**: Review and fix edge cases in `mapPMToRSTPosition`.
  - [x] Add regression tests for known edge cases.
- [ ] **Integration Test**: Add a comprehensive test case in `EditorController.test.ts`.
  - [x] Simulate a full edit cycle with marks, attributes, and structural changes.
  - [ ] Pass all assertions (Expected/Received HTML string mismatch remains).
- [x] **Verification**: Run `pnpm test` and `pnpm check`.

## Phase 4: Documentation and Demonstration

- [x] **Implementation Report**: Create `implementation_report.md` in the change directory containing:
  - **Overview**: Summary of changes and impact.
  - **Details**: Algorithms (LCA, Step Mapping), Data Structures (RSTChange, PM Steps).
  - **Remaining Improvements**: Performance notes, known limitations.
- [x] **Demonstration**: Create `demo.md` or a script/instruction to verify the fix:
  - Step-by-step guide to reproduce the original issues and see them fixed.
  - Example content to copy-paste for testing.

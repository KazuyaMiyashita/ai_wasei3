# Proposal: Fix ProseMirror Sync and UX Issues

## Problem

The current ProseMirror integration has several critical issues:

1.  **Formatting Whitespace leakage**: XML indentation and newlines are rendered as visible text in the WYSIWYG editor due to `white-space: pre-wrap`.
2.  **Mark (Style) Sync Failure**: Changes made via the menu (bold, italic) are not reflected in the RST/Source view.
3.  **Incomplete Transaction Mapping**: Only `ReplaceStep` is handled, and complex structure changes (like multi-node deletions or paste) often fail to sync correctly.
4.  **Attribute Sync Failure**: Attribute changes (like heading levels or custom attributes) are not synced back to RST.

## Proposed Changes

1.  **Whitespace Management**:
    - Update RST's `toProseMirrorDoc` to identify and ignore "formatting whitespace" (whitespace-only text nodes between block elements).
    - Ensure these nodes are still preserved in RST for lossless source reconstruction.
2.  **Mark Support**:
    - Update `toProseMirrorNode` to map XML tags like `<b>`, `<strong>`, `<i>`, `<em>` to ProseMirror marks.
    - Update `getChangesFromTransaction` to handle `AddMarkStep` and `RemoveMarkStep`.
3.  **Robust Step Handling**:
    - Extend `getChangesFromTransaction` to handle `AttrStep` for attribute changes.
    - Improve `ReplaceStep` handling to use ProseMirror's `DOMSerializer` for more accurate HTML generation during sync.
4.  **Position Mapping Refinement**:
    - Improve the heuristic for mapping PM positions to RST offsets, especially for edge cases where IDs might be missing or ambiguous.

## Expected Outcome

- A cleaner WYSIWYG experience without stray indentation.
- Bidirectional sync for basic text styles (bold, italic).
- Stable and predictable synchronization for complex editing operations.
- Correct synchronization of node attributes.

# Design: Fix ProseMirror Sync and UX Issues

## Architectural Approach

### 1. Whitespace Handling (RST to PM)

Current problem: XML formatting (indentation) is treated as significant text.
Solution:

- In `toProseMirrorNode`, when encountering a `Text` node that is purely whitespace:
  - Check if the parent node's schema allows for significant whitespace (e.g., `<pre>`, `<mei>`).
  - If the parent is a structural container (e.g., `body`, `section`) and the whitespace is between block-level children, skip it in PM.
  - Use a "significance" heuristic: Whitespace at the start/end of a block or between two block tags is likely formatting.

### 2. Bidirectional Mark Mapping

- **RST -> PM**:
  - Identify "inline tags" in RST that correspond to PM marks (e.g., `<b>` -> `strong`, `<i>` -> `em`).
  - When converting a parent node to PM, wrap the children's text nodes with appropriate marks.
- **PM -> RST**:
  - In `getChangesFromTransaction`, detect `AddMarkStep` and `RemoveMarkStep`.
  - Map these to RST `edit` operations that insert/remove tags.
  - For `ReplaceStep`, use `serializer.serializeFragment` and then wrap with appropriate tags if the slice has marks.

### 3. Robust Step Parsing

- Instead of manually extracting text from `Slice`, use `DOMSerializer` to convert the slice to a temporary HTML snippet.
- This snippet can then be processed (or used directly as `insertText` for RST) to preserve the full structure and marks.
- Handle `AttrStep` by mapping PM attribute changes back to RST `attributes` property.

### 4. Selection and Mapping Refinement

- Stable mapping depends on unique IDs.
- When PM creates new nodes (e.g., Enter key), they lack IDs initially.
- The adapter must detect these, perform the RST edit, and then immediately update the PM node with the newly assigned RST ID to maintain sync.

## Alternatives Considered

- **CSS `white-space: normal`**: Would hide indentation but might cause issues with intended whitespace in music encoding or poetry. Filtering at the adapter level is more precise.
- **Full Reparsing on every change**: Too slow and loses PM internal state (selection, undo history). Differential sync via Transaction steps is preferred.

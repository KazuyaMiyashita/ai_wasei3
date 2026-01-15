# Research Report: ProseMirror Transforms and Synchronization

## 1. Step Types and Synchronization Strategy

ProseMirror represents document changes as discrete `Step` objects. For robust synchronization with the Resilient Syntax Tree (RST), we need to handle the following steps:

- **`ReplaceStep`**: Replaces a range with new content (`Slice`).
  - _Sync Strategy_: Use `DOMSerializer` to convert `step.slice.content` (a Fragment) into an HTML string for RST insertion. This handles nested structures and marks correctly during paste or structural edits.
- **`AddMarkStep` / `RemoveMarkStep`**: Adds or removes a mark (e.g., `strong`, `em`) over a range.
  - _Sync Strategy_: Map the range to RST positions. For `AddMarkStep`, wrap the corresponding RST text nodes in tags like `<b>` or `<i>`. This may require splitting text nodes at the range boundaries. For `RemoveMarkStep`, unwrap the corresponding tags.
- **`AttrStep`**: Updates an attribute of a specific node.
  - _Sync Strategy_: Map the node position to an RST Node ID (using `data-rst-id`) and update the `attributes` property of the `ResilientNode`.

## 2. Accurate Position Resolution

A `Transaction` contains an array of `steps` and corresponding `docs` (the state after each step).
When iterating through `tr.steps`:

- Step `i` applies to `tr.docs[i]` (the document _before_ the step).
- To find the RST ID of a node affected by step `i`, we must resolve positions against `tr.docs[i]`.
- Failure to use the correct document version during iteration will lead to position drift and sync errors.

## 3. DOMSerializer and HTML Generation

`DOMSerializer.fromSchema(schema)` provides a standardized way to convert ProseMirror's internal model to HTML based on the schema's `toDOM` definitions.
Example implementation for `ReplaceStep`:

```typescript
const serializer = DOMSerializer.fromSchema(mySchema);
const domFragment = serializer.serializeFragment(step.slice.content);
const tempDiv = document.createElement("div");
tempDiv.appendChild(domFragment);
const htmlString = tempDiv.innerHTML;
```

This approach is more reliable than manual fragment traversal.

## 4. Whitespace Normalization (Broad Context)

Inside content blocks (like `p`, `h1`, `li`), XML source indentation (newlines and leading spaces) should be collapsed or trimmed in the ProseMirror representation to match browser rendering behavior (`white-space: normal` equivalent), preventing "formatting leak" into the WYSIWYG editor.

- **Implementation**: During `toProseMirrorNode` conversion, detect whitespace-only text nodes or leading/trailing whitespace in blocks and normalize them, while ensuring the original whitespace is preserved in the RST for lossless round-tripping.

# Yjs Investigation Report

## Analysis

The primary challenge is synchronizing a "Raw Text" view (CodeMirror) with a "Structured" view (ProseMirror) while handling malformed XML ("Resilience").

### Yjs Capabilities

- **Y.Text**: Efficient for text synchronization and conflict resolution.
- **Y.XmlFragment**: Efficient for XML structure synchronization.

### Gap

Yjs does not provide an automatic binding between `Y.Text` and `Y.XmlFragment`. To use Yjs, we would need to:

1.  Parse `Y.Text` and update `Y.XmlFragment` incrementally.
2.  Or, serialize `Y.XmlFragment` to update `Y.Text`.

This "Text <-> Tree" synchronization is exactly what `ResilientSyntaxTree` (RST) and `EditorController` are designed to solve. Adopting Yjs would not remove the need for this logic; it would only change the target data structure (from RST/PM nodes to Yjs nodes).

Furthermore, `Y.XmlFragment` typically normalizes XML (e.g., attribute order, whitespace handling), which conflicts with the requirement of the Code Editor to preserve the user's exact text formatting (whitespace, quote styles, etc.).

### Conclusion

Yjs does not fundamentally solve the _local_ parsing and synchronization problem between CodeMirror and ProseMirror. It introduces additional complexity (CRDTs) that is unnecessary for a single-user local editor context (or can be added later for collaboration _on top_ of the stable model).

**Decision**: Proceed with Custom Differential Sync using `ResilientSyntaxTree`.

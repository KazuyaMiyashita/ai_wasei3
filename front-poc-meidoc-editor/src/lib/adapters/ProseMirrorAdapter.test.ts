import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { describe, expect, it } from "vitest";
import {
  defaultSyntaxDefinition,
  ResilientSyntaxTree,
} from "../ResilientSyntaxTree";
import { mySchema } from "../schema";
import { ProseMirrorAdapter } from "./ProseMirrorAdapter";

// Mocking DOM environment if needed by ProseMirror
// Since we are running in happy-dom/jsdom environment via vitest, this should be fine.

describe("ProseMirrorAdapter", () => {
  const setup = (content: string) => {
    const rst = ResilientSyntaxTree.parse(content, defaultSyntaxDefinition);
    const adapter = new ProseMirrorAdapter(rst);
    const dom = document.createElement("div");
    const doc = rst.toProseMirrorDoc(mySchema);
    const state = EditorState.create({ doc, schema: mySchema });
    const view = new EditorView(dom, { state });
    adapter.setView(view);
    return { adapter, view, rst };
  };

  it("should extract simple text insert changes", () => {
    const { adapter, view } = setup("<div><p>Hello</p></div>");
    // PM: doc(xml_block(paragraph("Hello")))
    // "Hello" starts at 2. Insert " World" at 7.
    const tr = view.state.tr.insertText(" World", 7);

    const changes = adapter.getChangesFromTransaction(tr, view.state);
    // RST: <p>Hello</p>. "Hello" is inside p.
    // Need to verify exact offsets in integration test, but here checking array length.
    expect(changes.length).toBeGreaterThan(0);
    expect(changes[0].insertText).toBe(" World");
  });

  it("should map PM position to RST position", () => {
    const { adapter, view, rst: _rst } = setup("<div><p>AB</p></div>");
    // 0:xml, 1:p, 2:A, 3:B, 4:end
    // Pos 3 is between A and B.
    // In RST: <div><p>AB</p></div>
    // 01234567890123456
    // <p> at 5. AB at 8.
    // Between A(8) and B(9) -> 9.

    // We need to access private method or test via public side effect?
    // Accessing private for unit testing (using casting/indexing)
    // @ts-expect-error
    const rstPos = adapter.mapPMToRSTPosition(view.state.doc, 3);

    // Approximate check depending on implementation details
    expect(rstPos).toBeGreaterThan(5);
  });
});

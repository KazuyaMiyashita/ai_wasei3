import { Slice } from "prosemirror-model";
import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { describe, expect, it } from "vitest";
import { ProseMirrorAdapter } from "./ProseMirrorAdapter";
import {
  defaultSyntaxDefinition,
  ResilientSyntaxTree,
} from "./ResilientSyntaxTree";
import { mySchema } from "./schema";

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

  it("should have IDs in the initial PM document", () => {
    const { view } = setup("<p>Hello</p>");
    const pNode = view.state.doc.firstChild;
    expect(pNode?.attrs.id).toBeDefined();
  });

  it("should have marks in the initial PM document", () => {
    const { view } = setup("<p><strong>Hello</strong></p>");
    const pNode = view.state.doc.firstChild;
    const textNode = pNode?.firstChild;
    expect(textNode?.marks.length).toBeGreaterThan(0);
    expect(textNode?.marks[0].type.name).toBe("strong");
  });

  it("should extract simple text insert changes", () => {
    const { adapter, view } = setup("<p>Hello</p>");
    const tr = view.state.tr.insertText(" World", 6);
    const changes = adapter.getChangesFromTransaction(tr, view.state);
    expect(changes.length).toBeGreaterThan(0);
    expect(changes[0].insertText).toContain("World");
  });

  describe("Transaction Step Extension (ReplaceStep)", () => {
    it("should handle simple text insertion (Basic 1)", () => {
      const { adapter, view } = setup("<p>Hello</p>");
      const tr = view.state.tr.insertText("!", 6);
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes).toHaveLength(1);
      expect(changes[0].insertText).toContain("!");
    });

    it("should handle simple text deletion (Basic 2)", () => {
      const { adapter, view } = setup("<p>Hello</p>");
      const tr = view.state.tr.delete(2, 7);
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes).toHaveLength(1);
      expect(changes[0].insertText).toMatch(/<p.*><\/p>/);
    });

    it("should handle simple replacement (Basic 3)", () => {
      const { adapter, view } = setup("<p>Hello</p>");
      const tr = view.state.tr.replaceWith(2, 7, mySchema.text("Hi"));
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes).toHaveLength(1);
      expect(changes[0].insertText).toContain("Hi");
    });

    it("should handle multi-node replacement (Complex 1)", () => {
      const { adapter, view } = setup("<p>A</p><p>B</p>");
      const fragment = mySchema.nodes.doc.create(null, [
        mySchema.nodes.paragraph.create(null, mySchema.text("New1")),
        mySchema.nodes.paragraph.create(null, mySchema.text("New2")),
      ]).content;
      const tr = view.state.tr.replace(1, 6, new Slice(fragment, 0, 0));
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[0].insertText).toContain("New1");
    });

    it("should handle nested tags replacement (Complex 2)", () => {
      const { adapter, view } = setup("<p>Text</p>");
      const tr = view.state.tr.replaceWith(
        2,
        6,
        mySchema.nodes.xml_block.create(
          { tagName: "span" },
          mySchema.text("Wrapped"),
        ),
      );
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes).toHaveLength(1);
      expect(changes[0].insertText).toContain("span");
    });

    it("should handle partial replacement across boundaries (Complex 3)", () => {
      const { adapter, view } = setup("<p>Start</p><p>End</p>");
      const tr = view.state.tr.delete(4, 8);
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
    });
  });

  describe("Mark and Attribute Support (Phase 2)", () => {
    it("should sync adding Bold mark (Basic 1)", () => {
      const { adapter, view } = setup("<p>Hello</p>");
      const tr = view.state.tr.addMark(2, 7, mySchema.marks.strong.create());
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[0].insertText).toContain("strong");
    });

    it("should sync adding Italic mark (Basic 2)", () => {
      const { adapter, view } = setup("<p>Hello</p>");
      const tr = view.state.tr.addMark(2, 7, mySchema.marks.em.create());
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[0].insertText).toContain("em");
    });

    it("should sync removing marks (Basic 3)", () => {
      const { adapter, view } = setup("<p><strong>Hello</strong></p>");
      // text is from 2 to 7.
      const tr = view.state.tr.removeMark(2, 7, mySchema.marks.strong);
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThanOrEqual(1);
    });

    it("should sync changing heading level (Basic 4)", () => {
      const { adapter, view } = setup("<h1>Title</h1>");
      const tr = view.state.tr.setNodeMarkup(0, mySchema.nodes.heading, {
        level: 2,
      });
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[0].insertText).toContain("h2");
    });

    it("should handle overlapping marks (Complex 1)", () => {
      const { adapter, view } = setup("<p>Hello World</p>");
      const tr = view.state.tr
        .addMark(2, 7, mySchema.marks.strong.create())
        .addMark(5, 10, mySchema.marks.em.create());
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[changes.length - 1].insertText).toContain("strong");
      expect(changes[changes.length - 1].insertText).toContain("em");
    });

    it("should handle attributes on custom XML tags (Complex 2)", () => {
      const { adapter, view } = setup('<p class="a">T</p>');
      const tr = view.state.tr.setNodeMarkup(0, mySchema.nodes.paragraph, {
        class: "b",
      });
      const changes = adapter.getChangesFromTransaction(tr, view.state);
      expect(changes.length).toBeGreaterThan(0);
      expect(changes[0].insertText).toContain('class="b"');
    });
  });
});

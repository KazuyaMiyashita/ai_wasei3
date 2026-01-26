import { EditorView } from "prosemirror-view";
import { describe, expect, it } from "vitest";
import { EditorController } from "./EditorController";
import { XHTML5MEIDocument } from "./XHTML5MEIDocument";

describe("EditorController - Integration", () => {
  const setup = (content: string) => {
    const doc = new XHTML5MEIDocument(content);
    const controller = new EditorController(doc);
    const dom = document.createElement("div");
    const state = controller.createProseMirrorState();
    const view = new EditorView(dom, {
      state,
      dispatchTransaction(tr) {
        const newState = view.state.apply(tr);
        view.updateState(newState);
        controller.handleProseMirrorTransaction(tr, newState);
      },
    });
    controller.setProseMirrorView(view);
    return { controller, view, doc };
  };

  describe("Simple Cases", () => {
    it("should update PM when CM text changes (Orchestration)", () => {
      const { controller, view } = setup("<div>Hello</div>");

      // Simulate CM update: replace 'Hello' (pos 5-10) with 'World'
      const mockUpdate = {
        docChanged: true,
        changes: {
          desc: "mock change",
          // biome-ignore lint/suspicious/noExplicitAny: Mocking CodeMirror ViewUpdate callback
          iterChanges: (cb: any) =>
            cb(5, 10, 0, 5, { toString: () => "World" }),
        },
        transactions: [],
      };

      // @ts-expect-error
      controller.handleCodeMirrorUpdate(mockUpdate);

      // This triggers syncProseMirrorFromRST
      expect(view.state.doc.textContent).toContain("World");
    });
  });

  describe("Locking and Logging", () => {
    const initialContent = "<div>Hello</div>";
    const doc = new XHTML5MEIDocument(initialContent);

    it("should manage lock state", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromCM");
      expect(controller.canEdit("PM")).toBe(false);
    });
  });

  describe("End-to-End Sync (Phase 3)", () => {
    it("should sync marks from ProseMirror to RST", () => {
      const { controller, view } = setup("<p>Hello</p>");
      const { schema } = view.state;
      const tr = view.state.tr.addMark(2, 6, schema.marks.strong.create());
      view.dispatch(tr);
      expect(controller.getRST().toString()).toContain("<strong>el</strong>");
    });

    it("should sync heading level changes from ProseMirror to RST", () => {
      const { controller, view } = setup("<h1>Title</h1>");
      const { schema } = view.state;
      const tr = view.state.tr.setNodeMarkup(0, schema.nodes.heading, {
        level: 2,
      });
      view.dispatch(tr);
      expect(controller.getRST().toString()).toContain("<h2>Title</h2>");
    });

    it("should handle structural changes (split paragraph)", () => {
      const { controller, view } = setup("<p>AB</p>");
      const tr = view.state.tr.split(2);
      view.dispatch(tr);
      expect(controller.getRST().toString()).toContain("<p>A</p>");
      expect(controller.getRST().toString()).toContain("<p>B</p>");
    });
  });
});

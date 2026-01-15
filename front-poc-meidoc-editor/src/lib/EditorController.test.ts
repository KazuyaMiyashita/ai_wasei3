import { EditorView } from "prosemirror-view";
import { describe, expect, it, vi } from "vitest";
import { EditorController } from "./EditorController";
import { XHTML5MEIDocument } from "./XHTML5MEIDocument";

describe("EditorController - CM -> RST -> PM Sync (Task 2)", () => {
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
    it("should update PM when CM text changes", () => {
      const { controller, view } = setup("<div>Hello</div>");
      // Mock ViewUpdate-like object or just call edit and sync
      controller.rst.edit([{ from: 8, to: 13, insert: "World" }]);
      controller.syncProseMirrorFromRST();

      expect(view.state.doc.textContent).toContain("World");
    });

    it("should render <mei> as an atom mei_node", () => {
      const { view } = setup("<mei><score></score></mei>");
      const firstChild = view.state.doc.firstChild;
      expect(firstChild?.type.name).toBe("mei_node");
      expect(firstChild?.attrs.rawContent).toBe("<mei><score></score></mei>");
    });

    it("should sync simple tag addition", () => {
      const { controller, view } = setup("<div></div>");
      controller.rst.edit([{ from: 5, to: 5, insert: "<p>New</p>" }]);
      controller.syncProseMirrorFromRST();
      // <div><p>New</p></div>
      // becomes doc(xml_block(paragraph("New")))
      expect(view.state.doc.toString()).toContain('paragraph("New")');
    });
  });

  describe("Complex Cases", () => {
    it("should handle nested structure changes with MEI", () => {
      const { controller, view } = setup("<div><mei>OLD</mei></div>");
      // Replace MEI content
      controller.rst.edit([{ from: 10, to: 13, insert: "NEW" }]);
      controller.syncProseMirrorFromRST();

      const meiNode = view.state.doc.nodeAt(1); // root -> div -> mei
      expect(meiNode?.attrs.rawContent).toBe("<mei>NEW</mei>");
    });

    it("should preserve structure when adding siblings in CM", () => {
      const { controller, view } = setup("<div><p>A</p></div>");
      // Insert <p>B</p> after A
      // <div><p>A</p></div>
      // 01234567890123456
      // <p>A</p> is from 5 to 13.
      controller.rst.edit([{ from: 13, to: 13, insert: "<p>B</p>" }]);
      controller.syncProseMirrorFromRST();

      expect(view.state.doc.childCount).toBe(1); // div
      // biome-ignore lint/style/noNonNullAssertion: Test code
      const div = view.state.doc.firstChild!;
      expect(div.childCount).toBe(2);
      expect(div.child(0).textContent).toBe("A");
      expect(div.child(1).textContent).toBe("B");
    });
    it("should handle broken XML gracefully in PM sync", () => {
      const { controller, view } = setup("<div>");
      controller.rst.edit([{ from: 5, to: 5, insert: "<p>Broken" }]);
      controller.syncProseMirrorFromRST();

      // Should still produce a valid PM doc (likely using error_node or auto-closed nodes)
      expect(view.state.doc).toBeDefined();
    });
  });

  describe("PM -> RST Mapping (Task 3)", () => {
    it("should update RST when text is inserted in PM", () => {
      const { controller, view } = setup("<div><p>Hello</p></div>");
      // PM: doc(xml_block(paragraph("Hello")))
      // 0: xml_block start
      // 1: paragraph start
      // 2: "Hello" start
      // 2:H, 3:e, 4:l, 5:l, 6:o
      // 7: "Hello" end / paragraph end content
      // 8: paragraph end
      // Insert " World" at 7 (end of text, inside paragraph)
      const tr = view.state.tr.insertText(" World", 7);
      view.dispatch(tr);

      const rstContent = controller.rst.toString();
      expect(rstContent).toContain("Hello World");
    });

    it("should update RST when text is deleted in PM", () => {
      const { controller, view } = setup("<div><p>Hello</p></div>");
      // "Hello" starts at 2.
      // H(2), e(3), l(4), l(5), o(6).
      // Delete "ell" -> 3 to 6.
      const tr = view.state.tr.delete(3, 6);
      view.dispatch(tr);

      expect(controller.rst.toString()).toContain("<p>Ho</p>");
    });

    it("should handle insertion in nested elements", () => {
      const { controller, view } = setup("<div><p>A</p><p>C</p></div>");

      // 0: xml_block start

      // 1: p1 start

      // 2: A

      // 3: p1 end content

      // 4: p1 end

      // We want to insert "B" in the first paragraph: "A" -> "AB" at pos 3.

      const tr = view.state.tr.insertText("B", 3);

      view.dispatch(tr);

      expect(controller.rst.toString()).toContain("<p>AB</p>");
    });
  });

  describe("PM -> RST -> CM Sync (Task 4)", () => {
    it("should update CodeMirror when PM changes", () => {
      const { controller, view } = setup("<div><p>Test</p></div>");

      // Mock CM view
      let cmContent = "<div><p>Test</p></div>";
      const cmView = {
        state: {
          doc: { toString: () => cmContent },
          // biome-ignore lint/suspicious/noExplicitAny: Mocking CodeMirror transaction object
          update: (obj: any) =>
            ({
              ...obj,
              state: { doc: { toString: () => obj.changes.insert } },
              // biome-ignore lint/suspicious/noExplicitAny: Mocking CodeMirror state object
            }) as any, // Mock transaction
        },
        // biome-ignore lint/suspicious/noExplicitAny: Mocking CodeMirror transaction
        dispatch: (tr: any) => {
          if (tr.changes) {
            cmContent = tr.changes.insert;
          }
        },
        // biome-ignore lint/suspicious/noExplicitAny: Mocking EditorView
      } as any;
      controller.setCodeMirrorView(cmView);

      // Edit PM: Insert "A" at end of "Test"

      // doc(xml_block(paragraph("Test")))

      // 0:xml, 1:p, 2:T, 3:e, 4:s, 5:t, 6:end

      const tr = view.state.tr.insertText("A", 6);

      view.dispatch(tr);

      expect(cmContent).toContain("TestA");
    });
  });
});

describe("EditorController - Locking and Logging (Task 1)", () => {
  const initialContent = "<div>Hello</div>";
  const doc = new XHTML5MEIDocument(initialContent);

  describe("Locking Mechanism", () => {
    it("should start in Idle state (Simple)", () => {
      const controller = new EditorController(doc);
      expect(controller.lockState).toBe("Idle");
      expect(controller.canEdit("CM")).toBe(true);
      expect(controller.canEdit("PM")).toBe(true);
    });

    it("should block PM edit when ProcessingFromCM (Simple)", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromCM");
      expect(controller.canEdit("CM")).toBe(true);
      expect(controller.canEdit("PM")).toBe(false);
    });

    it("should return to Idle after Unlock (Simple)", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromPM");
      controller.setLock("Idle");
      expect(controller.lockState).toBe("Idle");
      expect(controller.canEdit("CM")).toBe(true);
      expect(controller.canEdit("PM")).toBe(true);
    });

    it("should handle rapid state transitions (Complex)", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromCM");
      controller.setLock("ProcessingFromPM"); // Rare but possible in logic?
      expect(controller.lockState).toBe("ProcessingFromPM");
      expect(controller.canEdit("CM")).toBe(false);
      expect(controller.canEdit("PM")).toBe(true);
    });

    it("should notify listeners on lock state change (Complex)", () => {
      const controller = new EditorController(doc);
      const listener = vi.fn();
      controller.subscribe(listener);
      controller.setLock("ProcessingFromCM");
      expect(listener).toHaveBeenCalled();
    });

    it("should not notify or log if setting the same lock state (Complex)", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromCM");
      const logCount = controller.logs.length;
      const listener = vi.fn();
      controller.subscribe(listener);

      controller.setLock("ProcessingFromCM");

      expect(listener).not.toHaveBeenCalled();
      expect(controller.logs.length).toBe(logCount);
    });
  });

  describe("Logging Functionality", () => {
    it("should record initialization log (Simple)", () => {
      const controller = new EditorController(doc);
      expect(controller.logs.length).toBeGreaterThan(0);
      expect(controller.logs[0].details).toContain("initialized");
    });

    it("should record custom logs (Simple)", () => {
      const controller = new EditorController(doc);
      controller.log("CM", "Receive", "Key 'A' pressed");
      expect(controller.logs[0].source).toBe("CM");
      expect(controller.logs[0].details).toBe("Key 'A' pressed");
    });

    it("should cap logs at maxLogs (Simple)", () => {
      const controller = new EditorController(doc);
      // Default maxLogs is 100. Let's push 110 logs.
      for (let i = 0; i < 110; i++) {
        controller.log("System", "Apply", `Log ${i}`);
      }
      expect(controller.logs.length).toBeLessThanOrEqual(100);
      expect(controller.logs[0].details).toBe("Log 109");
    });

    it("should include timestamps in logs (Complex)", () => {
      const controller = new EditorController(doc);
      controller.log("PM", "Apply", "Test");
      expect(controller.logs[0].timestamp).toBeLessThanOrEqual(Date.now());
    });

    it("should order logs newest first (Complex)", () => {
      const controller = new EditorController(doc);
      controller.log("CM", "Receive", "First");
      controller.log("CM", "Receive", "Second");
      expect(controller.logs[0].details).toBe("Second");
      expect(controller.logs[1].details).toBe("First");
    });

    it("should handle logs during lock state changes (Complex)", () => {
      const controller = new EditorController(doc);
      controller.setLock("ProcessingFromCM");
      expect(controller.logs[0].type).toBe("Lock");
      expect(controller.logs[0].details).toContain("ProcessingFromCM");
    });
  });
});

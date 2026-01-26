import * as fs from "node:fs";
import * as path from "node:path";
import { EditorState } from "prosemirror-state";
import { describe, expect, it } from "vitest";
import { parseXmlToAst } from "../ast/parser";
import { serializeAstToXml } from "../ast/serializer";
import { xhtmlMeiSchema } from "../schema/xhtml-mei";
import { SyncManager } from "./manager";

describe("SyncManager", () => {
  it("should convert AST to ProseMirror Document", () => {
    const xml = "<h1>Title</h1><p>Text</p>";
    const ast = parseXmlToAst(xml).root;
    const doc = SyncManager.astToProseMirrorDoc(ast);

    expect(doc.childCount).toBe(2);
    expect(doc.child(0).type.name).toBe("heading");
    expect(doc.child(0).textContent).toBe("Title");
    expect(doc.child(1).type.name).toBe("paragraph");
    expect(doc.child(1).textContent).toBe("Text");
  });

  it("should convert ProseMirror Document back to AST", () => {
    const xml = "<h1>Title</h1><p>Text</p>";
    const ast = parseXmlToAst(xml).root;
    const doc = SyncManager.astToProseMirrorDoc(ast);

    const newAst = SyncManager.pmDocToAst(doc);
    // Note: Our serializer currently wraps children in 'root', but pmDocToAst returns 'root' with children
    expect(newAst.children).toHaveLength(2);
    expect(newAst.children[0].tagName).toBe("h1");
    expect(newAst.children[1].tagName).toBe("p");
  });

  it("should reconcile changes", () => {
    const xml1 = "<div><p>Version 1</p></div>";
    const ast1 = parseXmlToAst(xml1).root;
    const doc1 = SyncManager.astToProseMirrorDoc(ast1);

    const state = EditorState.create({ doc: doc1, schema: xhtmlMeiSchema });

    const xml2 = "<div><p>Version 2</p></div>";
    const ast2 = parseXmlToAst(xml2).root;

    const tr = SyncManager.reconcile(state, ast2);
    const newState = state.apply(tr);

    expect(newState.doc.textContent).toBe("Version 2");
  });

  it("should handle real-world sample_content.xml", () => {
    // __dirname is .../frontend22/src/lib/editor/sync
    // We want .../resources/Documents/sample_content.xml
    // sync -> editor -> lib -> src -> frontend22 -> ai_wasei3 (root) -> resources
    // So 6 levels up?
    // /Users/miy/Desktop/ai_wasei3/frontend22/src/lib/editor/sync
    // ../ -> editor
    // ../../ -> lib
    // ../../../ -> src
    // ../../../../ -> frontend22
    // ../../../../../ -> ai_wasei3
    // ../../../../../../ -> Desktop (Wait, verify structure)

    // Project root is /Users/miy/Desktop/ai_wasei3
    // File is /Users/miy/Desktop/ai_wasei3/frontend22/src/lib/editor/sync/manager.test.ts
    // We need /Users/miy/Desktop/ai_wasei3/resources/Documents/sample_content.xml

    // ../ -> editor
    // ../../ -> lib
    // ../../../ -> src
    // ../../../../ -> frontend22
    // ../../../../../ -> ai_wasei3

    // So 5 levels up.

    const samplePath = path.resolve(
      __dirname,
      "../../../../../../resources/Documents/sample_content.xml",
    );

    // Let's verify if the file exists, if not, adjust path.
    if (!fs.existsSync(samplePath)) {
      // Fallback or explicit check
      const altPath = path.resolve(
        __dirname,
        "../../../../../resources/Documents/sample_content.xml",
      );
      if (fs.existsSync(altPath)) {
        const xml = fs.readFileSync(altPath, "utf-8");
        runSampleTest(xml);
        return;
      }
      throw new Error(`Sample file not found at ${samplePath} or ${altPath}`);
    } else {
      const xml = fs.readFileSync(samplePath, "utf-8");
      runSampleTest(xml);
    }
  });
});

function runSampleTest(xml: string) {
  // 1. Parse XML to AST
  const result = parseXmlToAst(xml);
  expect(result.errors).toHaveLength(0);
  expect(result.root).toBeDefined();

  // Verify AST structure (html > body > section > mei)
  // Note: virtual-root is unwraped by parseXmlToAst, so result.root IS the virtualRoot containing <html>...
  // Wait, parseXmlToAst wraps in <virtual-root> and returns { root: virtualRoot }.
  // BUT the unwrap logic:
  // const parsedRoot = virtualRoot.children[0];
  // if (parsedRoot && parsedRoot.tagName === 'virtual-root') { ... virtualRoot.children = parsedRoot.children }
  // So result.root.children should contain [html node].

  const html = result.root.children.find((c) => c.tagName === "html");
  expect(html).toBeDefined();
  if (!html) return;

  const body = html.children.find((c) => c.tagName === "body");
  expect(body).toBeDefined();

  // 2. Convert to ProseMirror
  const doc = SyncManager.astToProseMirrorDoc(result.root);
  expect(doc).toBeDefined();
  expect(doc.childCount).toBeGreaterThan(0);

  // Verify MEI node existence in ProseMirror doc
  let meiFound = false;
  doc.descendants((node) => {
    if (node.type.name === "mei") {
      meiFound = true;
      // Verify content is preserved
      expect(node.attrs.content).toContain("<mei");
      // Depending on how we captured it (raw string or reconstruction), verify content.
      // Our parser reconstructs it.
    }
    return true;
  });
  expect(meiFound).toBe(true);

  // 3. Round-trip: PM -> AST -> XML
  const newAst = SyncManager.pmDocToAst(doc);
  const newXml = serializeAstToXml(newAst);

  // Basic validation of the output XML
  expect(newXml).toContain("<html");
  expect(newXml).toContain("</html>");
  expect(newXml).toContain("<mei");
  expect(newXml).toContain("</mei>");
  // Ensure we didn't lose text content
  expect(newXml).toContain("モチーフと展開の技法");
}

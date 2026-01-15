import { describe, expect, it } from "vitest";
import {
  ResilientSyntaxTree,
  type SyntaxDefinition,
} from "./ResilientSyntaxTree";
import { SAMPLE_XML } from "./sampeContent";
import { mySchema } from "./schema";

const defaultDefinition: SyntaxDefinition = {
  isDefinedTag: (tagName) => {
    const defined = new Set([
      "html",
      "head",
      "title",
      "body",
      "section",
      "h1",
      "h2",
      "h3",
      "p",
      "div",
      "mei",
      "meiHead",
      "fileDesc",
      "titleStmt",
      "respStmt",
      "persName",
      "pubStmt",
      "date",
      "encodingDesc",
      "appInfo",
      "application",
      "name",
      "music",
      "mdiv",
      "score",
      "scoreDef",
      "pgFoot",
      "rend",
      "staffGrp",
      "grpSym",
      "label",
      "labelAbbr",
      "staffDef",
      "clef",
      "keySig",
      "meterSig",
      "pb",
      "measure",
      "staff",
      "layer",
      "rest",
      "beam",
      "note",
      "mordent",
      "tie",
      "sb",
      "mRest",
      "space",
      "accid",
      "ul",
      "li",
    ]);
    return defined.has(tagName);
  },
  isVoidTag: (tagName) => {
    return new Set(["br", "img", "hr", "pb", "sb", "mRest", "space"]).has(
      tagName,
    );
  },
  shouldAutoClose: (current, next) => {
    if (current === "li" && next === "li") return true;
    if (current === "p" && ["div", "p", "section", "h1"].includes(next))
      return true;
    return false;
  },
};

describe("ResilientSyntaxTree", () => {
  describe("Core Parsing & Round-trip", () => {
    it("should round-trip simple well-formed XML", () => {
      const input = "<root><child>text</child></root>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
    });

    it("should round-trip attributes with different quote styles", () => {
      const input = `<div class="a" id='b' data=c>content</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
    });

    it("should round-trip nested complex structure", () => {
      const input = `<div><p>Paragraph 1</p><section><h2>Title</h2></section></div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
    });

    it("should round-trip large sample content", () => {
      const rst = ResilientSyntaxTree.parse(SAMPLE_XML, defaultDefinition);
      expect(rst.toString()).toBe(SAMPLE_XML);
    });
  });

  describe("Error Handling & Lenient Parsing", () => {
    it("should handle unclosed tags by implicitly closing them", () => {
      const input = "<div><p>text</div>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
      const div = rst.root.children[0];
      expect(div.tagName).toBe("div");
      expect(div.children[0].tagName).toBe("p");
    });

    it("should handle orphan close tags", () => {
      const input = "<div>text</p></div>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
      const div = rst.root.children[0];
      const errorNode = div.children[1];
      expect(errorNode.type).toBe("Error");
    });

    it("should handle mixed broken structure (Auto-close)", () => {
      const input = "<ul><li>Item 1<li>Item 2</ul>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
      const ul = rst.root.children[0];
      expect(ul.children.length).toBe(2);
    });
  });

  describe("Complex Cases", () => {
    it("should identify undefined tags as Foreign", () => {
      const input = "<unknown>content</unknown>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
      expect(rst.root.children[0].type).toBe("Foreign");
    });

    it("should parse deep nesting with whitespace", () => {
      const input = `
        <div>
          <span>
            <b>Bold</b>
          </span>
        </div>
      `;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      expect(rst.toString()).toBe(input);
    });
  });

  describe("Incremental Updates (edit)", () => {
    it("should handle simple text insertion", () => {
      const input = "<p>Hello</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Insert " World" at index 8 (before </p>)
      const changes = rst.edit([{ from: 8, to: 8, insert: " World" }]);
      expect(rst.toString()).toBe("<p>Hello World</p>");
      expect(rst.root.children[0].children[0].textContent).toBe("Hello World");

      // Verify returned changes (LCA is the Text node "Hello" starting at 3)
      expect(changes).toHaveLength(1);
      expect(changes[0]).toMatchObject({
        from: 3,
        to: 8,
        insert: "Hello World",
      });
      expect(changes[0].affectedNodes.length).toBeGreaterThan(0);
    });

    it("should handle simple text deletion", () => {
      const input = "<p>Hello World</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Delete " World" (length 6).
      const changes = rst.edit([{ from: 8, to: 14, insert: "" }]);
      expect(rst.toString()).toBe("<p>Hello</p>");
      expect(changes[0]).toMatchObject({ from: 3, to: 14, insert: "Hello" });
    });

    it("should handle attribute update", () => {
      const input = `<div class="a">Content</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Change "a" to "b".
      const changes = rst.edit([{ from: 12, to: 13, insert: "b" }]);
      expect(rst.toString()).toBe(`<div class="b">Content</div>`);
      expect(changes[0]).toMatchObject({
        from: 0,
        to: 28,
        insert: `<div class="b">Content</div>`,
      });
    });

    it("should handle structural split", () => {
      const input = "<p>AB</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Insert </p><p> between A and B.
      const changes = rst.edit([{ from: 4, to: 4, insert: "</p><p>" }]);
      expect(rst.toString()).toBe("<p>A</p><p>B</p>");

      // Escalation happens here (re-parsing parent or splitting)
      // The change returned should cover the affected area.
      // Ideally it returns the effective change to the document string.
      // Since rst.edit processes one by one, and returns result of applySingleChange.
      // If escalation happens, it returns { from: lca.from, to: lca.to, insert: newText }.
      // Here lca would be the root (because we are splitting a top-level p).
      // Or maybe the p itself? NO, p cannot contain p. So escalation goes to root.
      // So change should be from 0 to length, insert new text.
      // Unless RST handles split more smartly? The current logic re-parses from LCA.
      // If we insert </p><p> inside <p>, it's structurally invalid for <p>.
      // LCA starts at <p>. Re-parse "<p>A</p><p>B</p>".
      // shouldEscalate checks if new tree has issues or if we need to go higher.
      // <p>A</p><p>B</p> is valid at root level.
      // So if lca was <p>, we re-parse it. But wait, <p> cannot contain <p>.
      // So inside <p>, we get <p>... which triggers auto-close?
      // Actually, standard XML parser might fail or produce nested p if tolerant?
      // Our definition says p auto-closes p.
      // So re-parsing "<p>A</p><p>B</p>" with context [root]:
      // It produces p(A), p(B).
      // So it returns 2 nodes.
      // Original lca was 1 node <p>.
      // We replace 1 node with 2 nodes.
      // Escalation check: does <p>A</p><p>B</p> cause issues inside the *original parent* (root)? No.
      // So we replace <p> with p, p.
      // The change returned is: from: <p>.start, to: <p>.end, insert: "<p>A</p><p>B</p>".

      expect(changes[0].from).toBe(0); // <p> start
      expect(changes[0].to).toBe(9); // <p>AB</p> length
      expect(changes[0].insert).toBe("<p>A</p><p>B</p>");
    });

    it("should handle structural join", () => {
      const input = "<p>A</p><p>B</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Delete </p><p> (length 4 + 3 = 7).
      // <p>A</p> length 3+1+4 = 8.
      // </p> starts at 4.
      // Next <p> starts at 8.
      // We want to delete `</p><p>`.
      // `</p>` is 4 chars. `<p>` is 3 chars.
      // Range: 4 to 11.
      rst.edit([{ from: 4, to: 11, insert: "" }]);
      expect(rst.toString()).toBe("<p>AB</p>");
      expect(rst.root.children.length).toBe(1);
    });

    it("should handle massive structural change (nested)", () => {
      const input = "<div><p>Old</p></div>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Replace <p>Old</p> with <ul><li>New</li></ul>
      // <div> is 5. <p>Old</p> starts at 5. Length 3+3+4=10.
      rst.edit([{ from: 5, to: 15, insert: "<ul><li>New</li></ul>" }]);
      expect(rst.toString()).toBe("<div><ul><li>New</li></ul></div>");
      const div = rst.root.children[0];
      const ul = div.children[0];
      expect(ul.tagName).toBe("ul");
      expect(ul.children[0].tagName).toBe("li");
    });
  });

  describe("Whitespace Filtering in toProseMirrorNode", () => {
    // Basic Case 1: Simple indentation in container
    it("should filter simple indentation in container (Basic 1)", () => {
      const input = `<div>
  <p>Text</p>
</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const divNode = doc.child(0);
      expect(divNode.childCount).toBe(1);
      expect(divNode.child(0).type.name).toBe("paragraph");
    });

    // Basic Case 2: Nested indentation
    it("should filter nested indentation (Basic 2)", () => {
      const input = `<div>
  <div>
    <p>Text</p>
  </div>
</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const outerDiv = doc.child(0);
      expect(outerDiv.childCount).toBe(1);
      const innerDiv = outerDiv.child(0);
      expect(innerDiv.childCount).toBe(1);
    });

    // Basic Case 3: Whitespace between block elements
    it("should filter whitespace between block elements (Basic 3)", () => {
      const input = `<div><p>A</p>   <p>B</p></div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const divNode = doc.child(0);
      expect(divNode.childCount).toBe(2);
      expect(divNode.child(0).type.name).toBe("paragraph");
      expect(divNode.child(1).type.name).toBe("paragraph");
    });

    // Complex Case 1: Text block normalization (Requirement: Normalize indentation inside p)
    it("should normalize indentation inside text blocks (Complex 1)", () => {
      const input = `<p>
        test1
        test2
      </p>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const pNode = doc.child(0);
      // Expected: "test1 test2" (or normalized spaces)
      // Note: Leading/trailing whitespace trimmed, internal newlines+spaces collapsed to single space
      expect(pNode.textContent).toBe("test1 test2");
    });

    // Complex Case 2: Mixed significant and insignificant whitespace
    it("should preserve significant space while filtering indentation (Complex 2)", () => {
      const input = `<div>
  <p>Word 1 <span>Word 2</span> Word 3</p>
</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const pNode = doc.child(0).child(0);
      expect(pNode.textContent).toBe("Word 1 Word 2 Word 3");
    });

    // Complex Case 3: Whitespace around void tags and special nodes
    it("should handle whitespace around void tags (Complex 3)", () => {
      const input = `<div>
  <p>Line 1<br/>
  Line 2</p>
</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const pNode = doc.child(0).child(0);
      // Expected: "Line 1 Line 2" (br is handled by schema, whitespace around it normalized)
      expect(pNode.textContent).toBe("Line 1 Line 2");
    });

    it("should preserve whitespace in MEI nodes (atom)", () => {
      const input = `<mei>
  <note/>
</mei>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
      const doc = rst.toProseMirrorDoc(mySchema);
      const meiNode = doc.child(0);
      expect(meiNode.type.name).toBe("mei_node");
      expect(meiNode.attrs.rawContent).toContain("\n  <note/>");
    });
  });
});

import { describe, expect, it } from "vitest";
import {
  ResilientSyntaxTree,
  type SyntaxDefinition,
} from "./ResilientSyntaxTree";
import { SAMPLE_XML } from "./sampeContent";

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
      // <p>Hello</p>
      // 012345678...
      // <p> -> 3 chars. "Hello" -> 5. Total 8. </p> starts at 8.
      // Insert at 8.
      rst.edit([{ from: 8, to: 8, insert: " World" }]);
      expect(rst.toString()).toBe("<p>Hello World</p>");
      expect(rst.root.children[0].children[0].textContent).toBe("Hello World");
    });

    it("should handle simple text deletion", () => {
      const input = "<p>Hello World</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Delete " World" (length 6).
      // <p> is 3. "Hello" is 5. " World" starts at 3+5=8.
      rst.edit([{ from: 8, to: 14, insert: "" }]);
      expect(rst.toString()).toBe("<p>Hello</p>");
    });

    it("should handle attribute update", () => {
      const input = `<div class="a">Content</div>`;
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Change "a" to "b".
      // <div class="a">
      // 012345678901234
      // class="a" starts at 5. "a" is at 12.
      // <div class="a"> length is 15.
      // Replace "a" (at 12) with "b".
      rst.edit([{ from: 12, to: 13, insert: "b" }]);
      expect(rst.toString()).toBe(`<div class="b">Content</div>`);
      expect(rst.root.children[0].attributes?.class).toBe("b");
    });

    it("should handle structural split", () => {
      const input = "<p>AB</p>";
      const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

      // Insert </p><p> between A and B.
      // <p>AB</p>
      // 012345678
      // A is at 3. B is at 4. Insert at 4.
      rst.edit([{ from: 4, to: 4, insert: "</p><p>" }]);
      expect(rst.toString()).toBe("<p>A</p><p>B</p>");
      expect(rst.root.children.length).toBe(2);
      expect(rst.root.children[0].tagName).toBe("p");
      expect(rst.root.children[1].tagName).toBe("p");
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
});

import { describe, expect, it } from "vitest";
import { parseXmlToAst } from "./parser";
import { serializeAstToXml } from "./serializer";

describe("AST Parser & Serializer", () => {
  it("should parse and serialize simple valid XML", () => {
    const xml = "<div><p>Hello</p></div>";
    const result = parseXmlToAst(xml);
    expect(result.errors).toHaveLength(0);
    const output = serializeAstToXml(result.root);
    expect(output).toBe(xml);
  });

  it("should handle attributes", () => {
    const xml = '<div class="container" id="main"><p>Text</p></div>';
    const result = parseXmlToAst(xml);
    expect(result.errors).toHaveLength(0);
    const output = serializeAstToXml(result.root);
    // Attributes order might vary but usually consistent in sax-js/serialization
    expect(output).toBe(xml);
  });

  it("should treat <mei> as a special block", () => {
    const xml = '<div><mei meiversion="5.0"><note/></mei></div>';
    const result = parseXmlToAst(xml);
    const meiNode = result.root.children[0].children[0];

    expect(meiNode.type).toBe("mei");
    expect(meiNode.tagName).toBe("mei");
    // Content inside MEI is captured as text in our current simple parser implementation?
    // Let's verify how the parser handles nested tags in MEI.
    // The current parser implementation treats content inside MEI as text accumulation
    // IF the type is MEI.
    expect(serializeAstToXml(result.root)).toBe(xml);
  });

  it("should auto-close missing tags (Fault Tolerance)", () => {
    const xml = "<div><p>Unclosed";
    const result = parseXmlToAst(xml);
    // Should produce valid XML on serialization
    const output = serializeAstToXml(result.root);
    expect(output).toBe("<div><p>Unclosed</p></div>");
  });

  it("should handle nested structures", () => {
    const xml = "<html><body><h1>Title</h1><p>Para</p></body></html>";
    const result = parseXmlToAst(xml);
    expect(serializeAstToXml(result.root)).toBe(xml);
  });

  it("should handle self-closing tags by expanding them in serialization if needed or keeping them", () => {
    // sax-js might report self-closing as open then close immediately.
    // Our serializer uses full tags.
    const xml = "<br/>";
    const result = parseXmlToAst(xml);
    const output = serializeAstToXml(result.root);
    expect(output).toBe("<br></br>"); // Standard element serialization
  });

  it("should not proliferate virtual-root", () => {
    const xml = "<div><p>Test</p></div>";
    // 1st pass
    const result1 = parseXmlToAst(xml);
    const output1 = serializeAstToXml(result1.root);
    expect(output1).toBe(xml);
    expect(output1).not.toContain("virtual-root");

    // 2nd pass
    const result2 = parseXmlToAst(output1);
    const output2 = serializeAstToXml(result2.root);
    expect(output2).toBe(xml);
    expect(output2).not.toContain("virtual-root");
  });
});

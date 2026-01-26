import type { AstNode } from "./types";

/**
 * Serializes an AST back to XML string.
 * Internal IDs are NOT included in the output.
 */
export function serializeAstToXml(node: AstNode): string {
  if (node.type === "root") {
    // Root node itself should not produce output, only children
    return node.children.map(serializeAstToXml).join("");
  }

  if (node.type === "text") {
    return node.content || "";
  }

  if (node.type === "mei") {
    const attrs = serializeAttributes(node.attributes);
    return `<mei${attrs}>${node.content || ""}</mei>`;
  }

  if (node.type === "element") {
    const attrs = serializeAttributes(node.attributes);
    const tagName = node.tagName || "div";
    const children = node.children.map(serializeAstToXml).join("");
    return `<${tagName}${attrs}>${children}</${tagName}>`;
  }

  if (node.type === "error") {
    return `<!-- ERROR: ${node.errorInfo} -->${node.children.map(serializeAstToXml).join("")}`;
  }

  return "";
}

function serializeAttributes(attrs?: Record<string, string>): string {
  if (!attrs) return "";
  const entries = Object.entries(attrs);
  if (entries.length === 0) return "";
  return ` ${entries.map(([k, v]) => `${k}="${escapeXml(v)}"`).join(" ")}`;
}

function escapeXml(unsafe: string): string {
  return unsafe.replace(/[<>&"']/g, (c) => {
    switch (c) {
      case "<":
        return "&lt;";
      case ">":
        return "&gt;";
      case "&":
        return "&amp;";
      case '"':
        return "&quot;";
      case "'":
        return "&apos;";
      default:
        return c;
    }
  });
}

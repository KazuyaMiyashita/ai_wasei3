import sax from "sax";
import type { AstNode, ParseResult } from "./types";

/**
 * Generates a unique ID for AST nodes.
 */
function generateId(): string {
  return crypto.randomUUID();
}

/**
 * Substring-based XML to AST Parser using sax-js.
 * This approach preserves the exact source content for MEI and other complex blocks.
 */
export function parseXmlToAst(xml: string): ParseResult {
  // Wrap in a dummy root to handle fragments
  const wrappedXml = `<virtual-root>${xml}</virtual-root>`;
  const shift = "<virtual-root>".length;

  const parser = sax.parser(false, {
    trim: false,
    normalize: false,
    lowercase: true,
    xmlns: true,
  });

  const errors: Array<{ message: string; line: number; column: number }> = [];
  const stack: AstNode[] = [];

  const virtualRoot: AstNode = {
    id: "root",
    type: "root",
    tagName: "root",
    children: [],
    range: { start: 0, end: xml.length },
  };

  stack.push(virtualRoot);

  const getCurrent = () => stack[stack.length - 1];

  parser.onerror = (e) => {
    errors.push({
      message: e.message,
      line: parser.line,
      column: parser.column,
    });
    // biome-ignore lint/suspicious/noExplicitAny: sax-js error recovery
    (parser as any).error = null;
    // biome-ignore lint/suspicious/noExplicitAny: sax-js error recovery
    (parser as any).resume();
  };

  parser.onopentag = (tag) => {
    const parent = getCurrent();

    // If we are already inside an MEI block, we don't create new AST nodes.
    // The MEI content will be extracted as a whole from the parent MEI node's range.
    if (parent.type === "mei") {
      return;
    }

    const isMei = tag.name === "mei";
    const node: AstNode = {
      id: generateId(),
      type: isMei ? "mei" : "element",
      tagName: tag.name,
      attributes: Object.fromEntries(
        // biome-ignore lint/suspicious/noExplicitAny: sax-js attributes
        Object.entries(tag.attributes).map(([k, v]) => [k, (v as any).value]),
      ),
      children: [],
      range: {
        start: parser.startTagPosition - shift - 1,
        end: 0,
        contentStart: parser.position - shift,
      },
    };

    parent.children.push(node);
    stack.push(node);
  };

  parser.onclosetag = (tagName) => {
    const current = getCurrent();

    // If we are inside an MEI block and this is a nested tag, skip.
    // But we need to handle the case where the MEI tag itself is closing.
    if (current.type !== "mei" && stack.length > 1) {
      // This might be a nested tag inside an MEI block that was ignored in onopentag
      // or just a regular tag.
      // If the top of stack doesn't match tagName, it means it's a nested tag in MEI (which we didn't push)
      // or a mismatch error.
      if (current.tagName !== tagName) {
        return;
      }
    }

    if (stack.length > 1 && current.tagName === tagName) {
      current.range.contentEnd = parser.startTagPosition - shift - 1;
      current.range.end = parser.position - shift;

      // For MEI nodes, extract the raw content string
      if (current.type === "mei") {
        current.content = xml.substring(
          current.range.contentStart || 0,
          current.range.contentEnd || 0,
        );
      }

      stack.pop();
    }
  };

  parser.ontext = (text) => {
    const current = getCurrent();
    if (current.type === "mei") {
      return;
    }

    // Only add text nodes for non-whitespace or if inside an element
    if (text.trim() || stack.length > 1) {
      current.children.push({
        id: generateId(),
        type: "text",
        content: text,
        children: [],
        range: {
          start: parser.position - text.length - shift,
          end: parser.position - shift,
        },
      });
    }
  };

  try {
    parser.write(wrappedXml).close();
  } catch (_e) {
    // Final check for errors
  }

  // Finalize any nodes left on stack (auto-close)
  while (stack.length > 1) {
    const node = stack.pop();
    if (node) {
      node.range.end = xml.length;
      if (node.type === "mei") {
        node.content = xml.substring(node.range.contentStart || 0, xml.length);
      }
    }
  }

  // Unwrap the virtual root
  if (
    virtualRoot.children.length === 1 &&
    virtualRoot.children[0].tagName === "virtual-root"
  ) {
    const parsedRoot = virtualRoot.children[0];

    // Adjust ranges
    const shift = "<virtual-root>".length;
    const fixRange = (n: AstNode) => {
      n.range.start = Math.max(0, n.range.start - shift);
      n.range.end = Math.max(0, n.range.end - shift);
      if (n.range.contentStart !== undefined)
        n.range.contentStart = Math.max(0, n.range.contentStart - shift);
      if (n.range.contentEnd !== undefined)
        n.range.contentEnd = Math.max(0, n.range.contentEnd - shift);
      n.children.forEach(fixRange);
    };
    parsedRoot.children.forEach(fixRange);

    virtualRoot.children = parsedRoot.children;
  }

  return { root: virtualRoot, errors };
}

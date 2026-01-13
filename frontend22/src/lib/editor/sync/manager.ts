import type { Node as PMNode } from "prosemirror-model";
import type { EditorState, Transaction } from "prosemirror-state";
import type { AstNode } from "../ast/types";
import { xhtmlMeiSchema } from "../schema/xhtml-mei";

/**
 * Manages synchronization between AST and ProseMirror.
 */
export const SyncManager = {
  /**
   * Converts AST to a ProseMirror Document.
   */
  astToProseMirrorDoc(ast: AstNode): PMNode {
    const nodes: PMNode[] = [];

    for (const child of ast.children) {
      const pmNode = SyncManager.astNodeToPMNode(child);
      if (pmNode) nodes.push(pmNode);
    }

    return xhtmlMeiSchema.nodes.doc.create(null, nodes);
  },

  astNodeToPMNode(ast: AstNode): PMNode | null {
    if (ast.type === "text") {
      return xhtmlMeiSchema.text(ast.content || "");
    }

    if (ast.type === "mei") {
      return xhtmlMeiSchema.nodes.mei.create({
        content: ast.content,
        internalId: ast.id,
        attributes: ast.attributes,
      });
    }

    if (ast.type === "element") {
      const type = SyncManager.mapTagNameToNodeType(ast.tagName);
      const content = ast.children
        .map((c) => SyncManager.astNodeToPMNode(c))
        .filter((n): n is PMNode => n !== null);

      try {
        const attrs = SyncManager.getAttrsForType(type, ast) || {};
        if (type === "unknown") {
          // biome-ignore lint/suspicious/noExplicitAny: Dynamic attribute assignment
          (attrs as any).tagName = ast.tagName;
        }
        return xhtmlMeiSchema.nodes[type].create(attrs, content);
      } catch (e) {
        console.warn(`Failed to create PM node for ${ast.tagName}`, e);
        return null;
      }
    }

    return null;
  },

  mapTagNameToNodeType(tagName?: string): string {
    if (!tagName) return "paragraph";
    switch (tagName.toLowerCase()) {
      case "p":
        return "paragraph";
      case "h1":
      case "h2":
      case "h3":
        return "heading";
      case "section":
        return "section";
      case "div":
        return "div";
      case "mei":
        return "mei";
      default:
        return "unknown";
    }
  },

  getAttrsForType(type: string, ast: AstNode): Record<string, unknown> | null {
    if (type === "heading") {
      const level = ast.tagName ? parseInt(ast.tagName.substring(1), 10) : 1;
      return { level: Number.isNaN(level) ? 1 : level };
    }
    return null;
  },

  /**
   * Diff and update ProseMirror state from a new AST.
   * This is where reconciliation happens to preserve selection.
   */
  reconcile(state: EditorState, newAst: AstNode): Transaction {
    const newDoc = SyncManager.astToProseMirrorDoc(newAst);
    const tr = state.tr;

    // Simple reconciliation: if content is different, replace the whole doc
    // but ProseMirror's replaceWith can be smart if we use a better diffing.
    // To preserve cursor, we should use a more granular approach if possible.
    // For now, let's do a full replace and see if PM handles cursor stability
    // via its own internal mapping if we replace the doc.
    if (!state.doc.eq(newDoc)) {
      tr.replaceWith(0, state.doc.content.size, newDoc.content);
    }

    return tr;
  },

  /**
   * Converts ProseMirror Document back to AST (partial or full).
   */
  pmDocToAst(doc: PMNode): AstNode {
    const virtualRoot: AstNode = {
      id: "root",
      type: "element",
      tagName: "root",
      children: [],
      range: { start: 0, end: 0 }, // Ranges need to be recomputed during serialization
    };

    doc.forEach((node) => {
      virtualRoot.children.push(SyncManager.pmNodeToAstNode(node));
    });

    return virtualRoot;
  },

  pmNodeToAstNode(node: PMNode): AstNode {
    if (node.isText) {
      return {
        id: "", // Will be filled or not used during serialization
        type: "text",
        content: node.text || "",
        children: [],
        range: { start: 0, end: 0 },
      };
    }

    if (node.type.name === "mei") {
      return {
        id: node.attrs.internalId,
        type: "mei",
        tagName: "mei",
        content: node.attrs.content,
        attributes: node.attrs.attributes,
        children: [],
        range: { start: 0, end: 0 },
      };
    }

    const children: AstNode[] = [];
    node.forEach((child) => {
      children.push(SyncManager.pmNodeToAstNode(child));
    });

    return {
      id: "",
      type: "element",
      tagName: SyncManager.nodeTypeToTagName(node),
      attributes: node.attrs.attributes || {},
      children,
      range: { start: 0, end: 0 },
    };
  },

  nodeTypeToTagName(node: PMNode): string {
    if (node.type.name === "heading") return `h${node.attrs.level}`;
    if (node.type.name === "paragraph") return "p";
    if (node.type.name === "section") return "section";
    if (node.type.name === "div") return "div";
    if (node.type.name === "unknown") return node.attrs.tagName || "div";
    return "div";
  },
};

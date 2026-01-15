import type { Node, Schema } from "prosemirror-model";
import { SimpleXmlTokenizer } from "./SimpleXmlTokenizer";

/**
 * ResilientSyntaxTree.ts
 */

export class RSTIntegrityError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "RSTIntegrityError";
  }
}

export type ResilientNodeType = "Defined" | "Text" | "Foreign" | "Error";

export interface SyntaxDefinition {
  isDefinedTag: (tagName: string) => boolean;
  isVoidTag?: (tagName: string) => boolean;
  isRawTextTag?: (tagName: string) => boolean;
  shouldAutoClose?: (currentTagName: string, nextTagName: string) => boolean;
}

export class ResilientNode {
  public readonly id: string;
  public type: ResilientNodeType;
  public parent: ResilientNode | null = null;
  public children: ResilientNode[] = [];

  public length: number = 0;

  public tagName?: string;
  public attributes?: Record<string, string>;
  public errorMessage?: string;

  public openingContent: string = "";
  public closingContent: string = "";
  public textContent: string = "";

  constructor(type: ResilientNodeType, length: number = 0) {
    this.id = crypto.randomUUID();
    this.type = type;
    this.length = length;
  }

  get from(): number {
    if (!this.parent) return 0;
    let offset = this.parent.from;
    offset += this.parent.openingContent.length;
    for (const sibling of this.parent.children) {
      if (sibling === this) break;
      offset += sibling.length;
    }
    return offset;
  }

  get to(): number {
    return this.from + this.length;
  }

  toString(): string {
    if (this.type === "Text" || this.type === "Error") {
      return this.textContent + this.openingContent + this.closingContent;
    }
    let out = this.openingContent;
    for (const child of this.children) {
      out += child.toString();
    }
    out += this.closingContent;
    return out;
  }
}

export class ResilientSyntaxTree {
  public root: ResilientNode;
  private rawContent: string;
  private definition: SyntaxDefinition;
  private idMap: Map<string, ResilientNode> = new Map();

  private constructor(content: string, definition: SyntaxDefinition) {
    this.rawContent = content;
    this.definition = definition;
    this.root = new ResilientNode("Defined", 0);
    this.root.tagName = "root";
    this.idMap.set(this.root.id, this.root);
  }

  static parse(
    content: string,
    definition: SyntaxDefinition,
    contextStack: ResilientNode[] = [],
  ): ResilientSyntaxTree {
    const instance = new ResilientSyntaxTree(content, definition);
    instance.buildTree(contextStack);
    return instance;
  }

  private buildTree(contextStack: ResilientNode[] = []): void {
    const tokenizer = new SimpleXmlTokenizer(this.rawContent);
    const tokens = tokenizer.tokenize();

    this.root.children = [];
    this.idMap.clear();
    this.idMap.set(this.root.id, this.root);

    const stack: ResilientNode[] = [...contextStack, this.root];
    const rootIndex = stack.length - 1;

    for (const token of tokens) {
      if (token.type === "OpenTag") {
        const tagName = token.tagName || "";
        while (stack.length > rootIndex + 1) {
          const currentParent = stack[stack.length - 1];
          if (
            currentParent.tagName &&
            this.definition.shouldAutoClose?.(currentParent.tagName, tagName)
          ) {
            stack.pop();
          } else {
            break;
          }
        }

        const currentParent = stack[stack.length - 1];
        const isDefined = this.definition.isDefinedTag(tagName);
        const type: ResilientNodeType = isDefined ? "Defined" : "Foreign";

        const node = new ResilientNode(type);
        node.tagName = tagName;
        node.openingContent = token.content;
        node.attributes = this.parseAttributes(token.content);
        node.length = token.content.length;
        node.parent = currentParent;
        this.idMap.set(node.id, node);

        currentParent.children.push(node);
        this.updateLengthUpwards(node, token.content.length);

        const isVoid = this.definition.isVoidTag?.(tagName);
        if (!token.isSelfClosing && !isVoid) {
          stack.push(node);
        }
      } else if (token.type === "CloseTag") {
        const currentParent = stack[stack.length - 1];
        const tagName = token.tagName || "";

        let matchIndex = -1;
        for (let i = stack.length - 1; i > rootIndex; i--) {
          if (stack[i].tagName === tagName) {
            matchIndex = i;
            break;
          }
        }

        if (matchIndex !== -1) {
          while (stack.length > matchIndex + 1) {
            stack.pop();
          }
          const matchedNode = stack.pop()!;
          matchedNode.closingContent = token.content;
          matchedNode.length += token.content.length;
          this.updateLengthUpwards(matchedNode, token.content.length);
        } else {
          const errorNode = new ResilientNode("Error", token.content.length);
          errorNode.textContent = token.content;
          errorNode.errorMessage = "Orphan Close Tag";
          errorNode.tagName = tagName;
          errorNode.parent = currentParent;
          this.idMap.set(errorNode.id, errorNode);
          currentParent.children.push(errorNode);
          this.updateLengthUpwards(errorNode, token.content.length);
        }
      } else {
        const currentParent = stack[stack.length - 1];
        const node = new ResilientNode("Text", token.content.length);
        node.textContent = token.content;
        node.parent = currentParent;
        this.idMap.set(node.id, node);
        currentParent.children.push(node);
        this.updateLengthUpwards(node, token.content.length);
      }
    }
  }

  edit(changes: { from: number; to: number; insert: string }[]): void {
    for (const change of changes) {
      this.applySingleChange(change.from, change.to, change.insert);
    }
    this.checkIntegrity();
  }

  private applySingleChange(from: number, to: number, insert: string): void {
    let lca = this.findNodeCovering(from, to);
    if (!lca) throw new RSTIntegrityError("Change range out of bounds");

    while (true) {
      const lcaFrom = lca.from;
      const relFrom = from - lcaFrom;
      const relTo = to - lcaFrom;

      if (!lca) throw new RSTIntegrityError("LCA is null during escalation");

      const oldText = lca.toString();
      const newText = oldText.slice(0, relFrom) + insert + oldText.slice(relTo);

      const ancestors: ResilientNode[] = [];
      let curr: ResilientNode | null = lca.parent;
      while (curr) {
        ancestors.unshift(curr);
        curr = curr.parent;
      }

      const subTree = ResilientSyntaxTree.parse(
        newText,
        this.definition,
        ancestors,
      );

      const escalationTarget = this.shouldEscalate(
        subTree.root,
        lca,
        ancestors,
      );
      if (escalationTarget) {
        if (lca.parent) {
          lca = lca.parent;
          continue;
        }
      }

      this.traverseSubTree(subTree.root, (n) => this.idMap.set(n.id, n));

      if (lca === this.root) {
        this.root.children = subTree.root.children;
        for (const child of this.root.children) {
          child.parent = this.root;
        }
        this.root.length = newText.length;
        return;
      }

      const parent = lca.parent!;
      const index = parent.children.indexOf(lca);
      if (index === -1) throw new RSTIntegrityError("LCA not found in parent");

      const newNodes = subTree.root.children;
      for (const node of newNodes) {
        node.parent = parent;
      }

      parent.children.splice(index, 1, ...newNodes);

      const delta = newLength(newNodes) - lca.length;
      let ptr: ResilientNode | null = parent;
      while (ptr) {
        ptr.length += delta;
        ptr = ptr.parent;
      }
      return;
    }
  }

  private traverseSubTree(node: ResilientNode, cb: (n: ResilientNode) => void) {
    if (node !== this.root) cb(node);
    for (const child of node.children) {
      this.traverseSubTree(child, cb);
    }
  }

  private shouldEscalate(
    subTreeRoot: ResilientNode,
    lca: ResilientNode,
    ancestors: ResilientNode[],
  ): boolean {
    let found = false;
    const targets = new Set(
      [lca.tagName, ...ancestors.map((a) => a.tagName)].filter(Boolean),
    );

    const check = (node: ResilientNode) => {
      if (found) return;
      if (node.type === "Error" && node.errorMessage === "Orphan Close Tag") {
        if (node.tagName && targets.has(node.tagName)) {
          found = true;
          return;
        }
      }
      for (const child of node.children) check(child);
    };

    check(subTreeRoot);
    return found;
  }

  private checkIntegrity() {
    const calculatedString = this.toString();
    if (calculatedString.length !== this.root.length) {
      throw new RSTIntegrityError(
        `Root length mismatch. Property: ${this.root.length}, Actual: ${calculatedString.length}`,
      );
    }
  }

  private findNodeCovering(from: number, to: number): ResilientNode | null {
    let current: ResilientNode | null = this.root;
    if (from < 0 || to > current.length) return null;

    while (current) {
      const currentFrom = current.from;
      const currentTo = current.to;
      const contentStart = currentFrom + current.openingContent.length;
      const contentEnd = currentTo - current.closingContent.length;

      if (from < contentStart || to > contentEnd) {
        return current;
      }

      let foundChild: ResilientNode | null = null;
      for (const child of current.children) {
        if (from >= child.from && to <= child.to) {
          foundChild = child;
          break;
        }
      }

      if (foundChild) {
        current = foundChild;
      } else {
        return current;
      }
    }
    return null;
  }

  private updateLengthUpwards(node: ResilientNode, delta: number) {
    let curr: ResilientNode | null = node.parent;
    while (curr) {
      curr.length += delta;
      curr = curr.parent;
    }
  }

  private parseAttributes(tagContent: string): Record<string, string> {
    const attrs: Record<string, string> = {};

    const attrRegex = /([a-zA-Z0-9_\-:]+)\s*=\s*("[^"]*"|'[^']*'|[^>\s]+)/g;

    let match = attrRegex.exec(tagContent);

    while (match !== null) {
      const name = match[1];

      let value = match[2];

      if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        value = value.slice(1, -1);
      }

      attrs[name] = value;

      match = attrRegex.exec(tagContent);
    }

    return attrs;
  }

  traverse(callback: (node: ResilientNode, depth: number) => void): void {
    const visit = (node: ResilientNode, depth: number) => {
      callback(node, depth);
      for (const child of node.children) {
        visit(child, depth + 1);
      }
    };
    visit(this.root, 0);
  }

  findNodeAt(pos: number): ResilientNode | null {
    let current: ResilientNode | null = this.root;
    if (pos < 0 || pos > current.length) return null;

    while (current) {
      let foundChild: ResilientNode | null = null;
      for (const child of current.children) {
        if (pos >= child.from && pos < child.to) {
          foundChild = child;
          break;
        }
      }

      if (foundChild) {
        current = foundChild;
      } else {
        return current;
      }
    }
    return null;
  }

  findNodeById(id: string): ResilientNode | undefined {
    return this.idMap.get(id);
  }

  mapRSTPosToIdOffset(pos: number): { id: string; offset: number } | null {
    const node = this.findNodeAt(pos);
    if (!node) return null;
    const offset = pos - node.from - node.openingContent.length;
    // offset might be negative if pos is in opening content?
    // findNodeAt returns 'current' if pos is in opening content.
    // If offset < 0, it means we are in start tag.
    // If offset > node.contentLength (which is node.length - start - end), we are in end tag.
    return { id: node.id, offset };
  }

  mapIdOffsetToRSTPos(id: string, offset: number): number | null {
    const node = this.findNodeById(id);
    if (!node) return null;
    return node.from + node.openingContent.length + offset;
  }

  toString(): string {
    const serialize = (node: ResilientNode): string => {
      if (node.type === "Text" || node.type === "Error") {
        return node.textContent + node.openingContent + node.closingContent;
      }

      let out = node.openingContent;
      for (const child of node.children) {
        out += serialize(child);
      }
      out += node.closingContent;
      return out;
    };

    let out = "";
    for (const child of this.root.children) {
      out += serialize(child);
    }
    return out;
  }

  toProseMirrorDoc(schema: Schema): Node {
    const convert = (node: ResilientNode): Node | Node[] | null => {
      if (node.type === "Text") {
        return schema.text(node.textContent || "");
      }
      if (node.type === "Error") {
        return schema.nodes.error_node.create({
          errorMessage: node.errorMessage,
          rawContent: node.toString(),
          id: node.id,
        });
      }
      if (node.type === "Foreign") {
        const contentNodes: Node[] = [];
        for (const child of node.children) {
          const res = convert(child);
          if (res)
            Array.isArray(res)
              ? contentNodes.push(...res)
              : contentNodes.push(res);
        }
        return schema.nodes.xml_textblock.create(
          { tagName: node.tagName, attributes: node.attributes, id: node.id },
          contentNodes,
        );
      }

      const tagName = node.tagName || "div";

      if (tagName === "p") {
        const contentNodes: Node[] = [];
        for (const child of node.children) {
          const res = convert(child);
          if (res)
            Array.isArray(res)
              ? contentNodes.push(...res)
              : contentNodes.push(res);
        }
        return schema.nodes.paragraph.create(
          { id: node.id, ...node.attributes },
          contentNodes,
        );
      }
      if (["h1", "h2", "h3", "h4", "h5", "h6"].includes(tagName)) {
        const level = parseInt(tagName.substring(1), 10);
        const contentNodes: Node[] = [];
        for (const child of node.children) {
          const res = convert(child);
          if (res)
            Array.isArray(res)
              ? contentNodes.push(...res)
              : contentNodes.push(res);
        }
        return schema.nodes.heading.create(
          { level, id: node.id, ...node.attributes },
          contentNodes,
        );
      }

      const childrenNodes: Node[] = [];
      let hasBlock = false;
      for (const child of node.children) {
        const res = convert(child);
        if (res) {
          const list = Array.isArray(res) ? res : [res];
          childrenNodes.push(...list);
          for (const n of list) {
            if (n.type.isBlock) {
              hasBlock = true;
            }
          }
        }
      }

      if (hasBlock) {
        const finalChildren: Node[] = [];
        let buffer: Node[] = [];
        const flush = () => {
          if (buffer.length > 0) {
            finalChildren.push(schema.nodes.xml_textblock.create({}, buffer));
            buffer = [];
          }
        };

        for (const n of childrenNodes) {
          if (n.isInline) {
            buffer.push(n);
          } else {
            flush();
            finalChildren.push(n);
          }
        }
        flush();
        return schema.nodes.xml_block.create(
          { tagName, attributes: node.attributes, id: node.id },
          finalChildren,
        );
      } else {
        return schema.nodes.xml_textblock.create(
          { tagName, attributes: node.attributes, id: node.id },
          childrenNodes,
        );
      }
    };

    const rootChildren: Node[] = [];
    for (const child of this.root.children) {
      const res = convert(child);
      if (res)
        Array.isArray(res) ? rootChildren.push(...res) : rootChildren.push(res);
    }

    const finalDocChildren: Node[] = [];
    let buffer: Node[] = [];
    const flush = () => {
      if (buffer.length > 0) {
        finalDocChildren.push(schema.nodes.paragraph.create({}, buffer));
        buffer = [];
      }
    };

    for (const n of rootChildren) {
      if (n.isInline) {
        buffer.push(n);
      } else {
        flush();
        finalDocChildren.push(n);
      }
    }
    flush();

    return schema.nodes.doc.create({}, finalDocChildren);
  }
}

function newLength(nodes: ResilientNode[]): number {
  return nodes.reduce((acc, n) => acc + n.length, 0);
}

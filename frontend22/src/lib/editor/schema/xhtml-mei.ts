import { type DOMOutputSpec, type NodeSpec, Schema } from "prosemirror-model";

const nodes: { [key: string]: NodeSpec } = {
  doc: {
    content: "block+",
  },

  paragraph: {
    content: "inline*",
    group: "block",
    parseDOM: [{ tag: "p" }],
    toDOM() {
      return ["p", 0];
    },
  },

  heading: {
    attrs: { level: { default: 1 } },
    content: "inline*",
    group: "block",
    defining: true,
    parseDOM: [
      { tag: "h1", attrs: { level: 1 } },
      { tag: "h2", attrs: { level: 2 } },
      { tag: "h3", attrs: { level: 3 } },
    ],
    toDOM(node) {
      return [`h${node.attrs.level}`, 0];
    },
  },

  section: {
    content: "block*",
    group: "block",
    parseDOM: [{ tag: "section" }],
    toDOM() {
      return ["section", 0];
    },
  },

  div: {
    content: "block*",
    group: "block",
    parseDOM: [{ tag: "div" }],
    toDOM() {
      return ["div", 0];
    },
  },

  text: {
    group: "inline",
  },

  mei: {
    group: "block",
    atom: true, // Treated as a single unit
    attrs: {
      content: { default: "" },
      internalId: { default: "" }, // For AST mapping
      attributes: { default: {} },
    },
    parseDOM: [
      {
        tag: "mei",
        getAttrs(dom) {
          if (typeof dom === "string") return null;
          const el = dom as HTMLElement;
          return {
            content: el.innerHTML,
            attributes: Array.from(el.attributes).reduce(
              (acc: Record<string, string>, attr) => {
                acc[attr.name] = attr.value;
                return acc;
              },
              {},
            ),
          };
        },
      },
    ],
    toDOM(node): DOMOutputSpec {
      return [
        "div",
        {
          class: "mei-container",
          "data-internal-id": node.attrs.internalId,
          "data-mei-content": node.attrs.content,
        },
        "MEI Score",
      ];
    },
  },

  // Fallback for unknown elements to keep structure
  unknown: {
    group: "block",
    content: "block*",
    attrs: { tagName: { default: "div" }, attributes: { default: {} } },
    parseDOM: [
      {
        tag: "*",
        getAttrs(dom) {
          const el = dom as HTMLElement;
          return { tagName: el.tagName.toLowerCase(), attributes: {} };
        },
      },
    ],
    toDOM(node) {
      return [node.attrs.tagName, node.attrs.attributes, 0];
    },
  },
};

const marks = {
  strong: {
    parseDOM: [{ tag: "strong" }, { tag: "b" }],
    toDOM(): DOMOutputSpec {
      return ["strong", 0];
    },
  },
  em: {
    parseDOM: [{ tag: "em" }, { tag: "i" }],
    toDOM(): DOMOutputSpec {
      return ["em", 0];
    },
  },
};

export const xhtmlMeiSchema = new Schema({ nodes, marks });

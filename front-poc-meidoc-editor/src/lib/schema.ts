import { type DOMOutputSpec, type NodeSpec, Schema } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";

function addId(spec: NodeSpec): NodeSpec {
  return {
    ...spec,
    attrs: { ...spec.attrs, id: { default: null } },
    toDOM(node) {
      const output = spec.toDOM
        ? spec.toDOM(node)
        : ([spec.parseDOM?.[0]?.tag || "div", 0] as DOMOutputSpec);
      if (Array.isArray(output) && node.attrs.id) {
        const tag = output[0] as string;
        const second = output[1];
        if (second && typeof second === "object" && !Array.isArray(second)) {
          return [
            tag,
            { ...second, "data-rst-id": node.attrs.id },
            ...output.slice(2),
          ] as DOMOutputSpec;
        } else {
          return [
            tag,
            { "data-rst-id": node.attrs.id },
            ...output.slice(1),
          ] as DOMOutputSpec;
        }
      }
      return output;
    },
  };
}

// Generic XML Block (Container)
const xmlBlock: NodeSpec = {
  attrs: {
    tagName: { default: "div" },
    attributes: { default: {} },
    id: { default: null },
  },
  content: "block+",
  group: "block",
  toDOM(node) {
    // biome-ignore lint/suspicious/noExplicitAny: Attributes are dynamic key-value pairs
    const attrs: any = { ...node.attrs.attributes, class: "xml-block" };
    if (node.attrs.id) attrs["data-rst-id"] = node.attrs.id;
    attrs["data-tagname"] = node.attrs.tagName;
    return [node.attrs.tagName, attrs, 0];
  },
  parseDOM: [{ tag: "div" }],
};

// Generic XML TextBlock
const xmlTextBlock: NodeSpec = {
  attrs: {
    tagName: { default: "span" },
    attributes: { default: {} },
    id: { default: null },
  },
  content: "inline*",
  group: "block",
  toDOM(node) {
    // biome-ignore lint/suspicious/noExplicitAny: Attributes are dynamic key-value pairs
    const attrs: any = { ...node.attrs.attributes, class: "xml-textblock" };
    if (node.attrs.id) attrs["data-rst-id"] = node.attrs.id;
    attrs["data-tagname"] = node.attrs.tagName;
    return [node.attrs.tagName, attrs, 0];
  },
};

const errorNode: NodeSpec = {
  attrs: {
    errorMessage: { default: "" },
    rawContent: { default: "" },
    id: { default: null },
  },
  group: "block",
  atom: true,
  toDOM(node) {
    return [
      "div",
      {
        class: "parse-error",
        "data-error": node.attrs.errorMessage,
        "data-rst-id": node.attrs.id,
      },
      node.attrs.rawContent,
    ];
  },
};

const meiNode: NodeSpec = {
  attrs: {
    rawContent: { default: "" },
    id: { default: null },
  },
  group: "block",
  atom: true,
  toDOM(node) {
    return [
      "pre",
      {
        class: "mei-content",
        "data-rst-id": node.attrs.id,
        style:
          "overflow: auto; max-height: 300px; background: #f8f8f8; padding: 8px; border: 1px solid #ddd; font-family: monospace; font-size: 12px;",
      },
      node.attrs.rawContent,
    ];
  },
};

let nodes = basicSchema.spec.nodes;
const p = nodes.get("paragraph");
const h = nodes.get("heading");
const bl = nodes.get("bullet_list");
const ol = nodes.get("ordered_list");
const li = nodes.get("list_item");
const hr = nodes.get("horizontal_rule");

if (p) nodes = nodes.update("paragraph", addId(p));
if (h) nodes = nodes.update("heading", addId(h));
if (bl) nodes = nodes.update("bullet_list", addId(bl));
if (ol) nodes = nodes.update("ordered_list", addId(ol));
if (li) nodes = nodes.update("list_item", addId(li));
if (hr) nodes = nodes.update("horizontal_rule", addId(hr));

nodes = nodes
  .addToEnd("xml_block", xmlBlock)
  .addToEnd("xml_textblock", xmlTextBlock)
  .addToEnd("error_node", errorNode)
  .addToEnd("mei_node", meiNode);

export const mySchema = new Schema({
  nodes: nodes,
  marks: basicSchema.spec.marks,
});

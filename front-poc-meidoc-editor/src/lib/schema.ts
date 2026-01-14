import { Schema } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";

// 基本スキーマに section を追加
// section はブロック要素を含み、自身もブロック要素となる
const nodes = basicSchema.spec.nodes.addToEnd("section", {
  content: "block+",
  group: "block",
  parseDOM: [{ tag: "section" }],
  toDOM() {
    return ["section", 0];
  },
});

export const mySchema = new Schema({
  nodes: nodes,
  marks: basicSchema.spec.marks,
});

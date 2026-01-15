import { describe, expect, it } from "vitest";
import {
  ResilientSyntaxTree,
  type SyntaxDefinition,
} from "./ResilientSyntaxTree";
import { mySchema } from "./schema";

const defaultDefinition: SyntaxDefinition = {
  isDefinedTag: (tagName) =>
    ["p", "div", "section", "h1", "span"].includes(tagName),
  isVoidTag: () => false,
};

describe("ProseMirror Integration", () => {
  it("should convert RST to ProseMirror Doc", () => {
    const input = "<div><p>Hello</p></div>";
    const rst = ResilientSyntaxTree.parse(input, defaultDefinition);

    const doc = rst.toProseMirrorDoc(mySchema);
    expect(doc.type.name).toBe("doc");
    expect(doc.childCount).toBe(1); // xml_block(div)

    const div = doc.child(0);
    expect(div.type.name).toBe("xml_block");
    expect(div.attrs.tagName).toBe("div");

    const p = div.child(0);
    expect(p.type.name).toBe("paragraph");
    expect(p.textContent).toBe("Hello");
  });

  it("should maintain IDs for mapping", () => {
    const input = "<p>Text</p>";
    const rst = ResilientSyntaxTree.parse(input, defaultDefinition);
    const doc = rst.toProseMirrorDoc(mySchema);

    expect(doc.child(0).attrs.id).toBe(rst.root.children[0].id); // My converter: paragraph.create({ id: node.id, ... }, content)
    // Schema: paragraph spec usually doesn't have `id`.
    // Wait, `prosemirror-schema-basic` paragraph doesn't have `id` attr.
    // My schema code:
    // `const nodes = basicSchema.spec.nodes.addToEnd...`
    // I didn't modify `paragraph` spec in `schema.ts`!
    // So passing `id` to `paragraph.create` might be ignored or throw error?
    // PM allows extra attrs if defined. `basicSchema` doesn't define `id`.
    // So ID mapping for `p` might fail if I rely on PM attributes.

    // Check schema.ts content again.
    // I only added xml_block, xml_textblock, error_node.
    // I used `basicSchema.spec.nodes` for others.
    // So `paragraph` is standard. It has no `id` attribute.

    // If I want to track ID for paragraphs, I must extend `paragraph` spec or wrap it.
    // Or use `xml_block` with tagName="p"?
    // But `toProseMirrorDoc` explicitly maps "p" to `schema.nodes.paragraph`.

    // Fix: In `schema.ts`, I should override paragraph to include `id`.
    // Or just accept that for this POC, standard nodes don't track ID in attributes?
    // But then `mapIdOffsetToRSTPos` works (RST lookup), but PM -> RST mapping needs PM node to have ID.
    // If PM node doesn't have ID, we can't map back easily.

    // I should probably fix `schema.ts` to add `id` to all nodes or at least the ones I use.
    // Modifying basic schema nodes:
    // `basicSchema.spec.nodes.update("paragraph", { ...paragraphSpec, attrs: { ...paragraphSpec.attrs, id: {} } })`
    // This is cumbersome.
    // Alternative: Use `xml_block` for everything including `p`.
    // But I wanted `paragraph` behavior (textblock). `xml_textblock` has it.
    // So I could map `p` to `xml_textblock` with tagName="p".
    // But `schema.ts` defined `paragraph`.

    // Let's modify `src/lib/schema.ts` to add `id` to paragraph and heading.
  });
});

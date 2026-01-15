import { describe, expect, it } from "vitest";
import {
  ResilientSyntaxTree,
  type SyntaxDefinition,
} from "./ResilientSyntaxTree";
import { SAMPLE_XML } from "./sampeContent";

const definition: SyntaxDefinition = {
  isDefinedTag: (_t) => true,
  isVoidTag: (t) => ["br", "img", "pb", "sb"].includes(t),
  shouldAutoClose: (_c, _n) => false,
};

function randomInt(max: number) {
  return Math.floor(Math.random() * max);
}

function randomString(len: number) {
  const chars =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 <>/=\"'";
  let res = "";
  for (let i = 0; i < len; i++) {
    res += chars[randomInt(chars.length)];
  }
  return res;
}

describe("Fuzzing & Performance", () => {
  it("should survive 100 random edits on sample content", () => {
    const rst = ResilientSyntaxTree.parse(SAMPLE_XML, definition);
    let currentText = SAMPLE_XML;

    for (let i = 0; i < 100; i++) {
      const len = rst.root.length;
      const op = randomInt(3); // 0: insert, 1: delete, 2: replace

      const from = randomInt(len + 1);
      let to = from;
      let insert = "";

      if (op === 0) {
        insert = randomString(randomInt(20));
      } else if (op === 1) {
        to = Math.min(len, from + randomInt(20));
      } else {
        to = Math.min(len, from + randomInt(20));
        insert = randomString(randomInt(20));
      }

      try {
        rst.edit([{ from, to, insert }]);
      } catch (e) {
        console.error(`Fuzz failed at iteration ${i}`, { from, to, insert });
        throw e;
      }

      currentText = currentText.slice(0, from) + insert + currentText.slice(to);

      expect(rst.root.length).toBe(currentText.length);
    }

    expect(rst.toString()).toBe(currentText);
  });
});

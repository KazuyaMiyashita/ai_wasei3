import { baseKeymap } from "prosemirror-commands";
import { exampleSetup } from "prosemirror-example-setup";
import { history, redo, undo } from "prosemirror-history";
import { keymap } from "prosemirror-keymap";
import { DOMParser, DOMSerializer, Schema } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";
import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import type { XHTML5MEIDocument } from "./Main";
import "prosemirror-example-setup/style/style.css";
import "prosemirror-menu/style/menu.css";

const xmlSchema = new Schema({
  nodes: {
    doc: { content: "block+" },
    h2: {
      content: "text*",
      group: "block",
      parseDOM: [{ tag: "h2" }],
      toDOM() {
        return ["h2", 0];
      },
    },
    p: {
      content: "text*",
      group: "block",
      parseDOM: [{ tag: "p" }],
      toDOM() {
        return ["p", 0];
      },
    },
    text: { inline: true },
  },
});

export function DocumentEditor({
  xhtml5meiDocument,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
}) {
  const proseMirrorRef = useRef<HTMLDivElement>(null);
  const proseMirrorEditorViewRef = useRef<EditorView | null>(null);

  const domParser = new window.DOMParser();
  const xmlDoc = domParser.parseFromString(
    xhtml5meiDocument.rawContent,
    "text/html",
  );

  useEffect(() => {
    if (!proseMirrorRef.current) return;

    // 2. XML文字列をひとつのテキストノードとして読み込む
    const state = EditorState.create({
      doc: DOMParser.fromSchema(basicSchema).parse(xmlDoc),
      schema: basicSchema,
      plugins: exampleSetup({ schema: basicSchema }),
    });

    // 3. ビューの生成
    const view = new EditorView(proseMirrorRef.current, {
      state,
      dispatchTransaction(tr) {
        // 1. Transactionの中身をのぞき見る
        // console.log("--- New Transaction ---");

        // どこの範囲が変更されたか（ステップの確認）
        tr.steps.forEach((step, i) => {
          // step.toJSON() を見ると「どの位置(from/to)に何が起きたか」がわかる
          console.log(`Step ${i}:`, step.toJSON());
        });

        // 2. 状態を更新（これを行わないと画面に反映されない）
        const newState = view.state.apply(tr);
        view.updateState(newState);

        // 3. 変更があったかどうかを判定
        if (tr.docChanged) {
          // console.log("Document changed!");
          const newText = newState.doc.textContent;
          // console.debug(newText);
        }

        // 4. カーソル位置（Selection）の情報も取れる
        // console.log("Selection from/to:", tr.selection.from, tr.selection.to);
      },
    });

    proseMirrorEditorViewRef.current = view;

    return () => {
      view.destroy();
      proseMirrorEditorViewRef.current = null;
    };
  }, [xhtml5meiDocument]);

  return <div ref={proseMirrorRef}></div>;
}

import { exampleSetup } from "prosemirror-example-setup";
import "prosemirror-example-setup/style/style.css";
import "prosemirror-menu/style/menu.css";
import { DOMParser } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";
import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../controllers/EditorController";
import type { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";

export function DocumentEditor({
  xhtml5meiDocument,
  controller,
}: {
  xhtml5meiDocument: XHTML5MEIDocument;
  controller: EditorController;
}) {
  const proseMirrorRef = useRef<HTMLDivElement>(null);
  const proseMirrorEditorViewRef = useRef<EditorView | null>(null);

  useEffect(() => {
    if (!proseMirrorRef.current) return;

    // XMLパース処理
    const domParser = new window.DOMParser();
    const xmlDoc = domParser.parseFromString(
      xhtml5meiDocument.rawContent,
      "text/html",
    );

    // 2. XML文字列をひとつのテキストノードとして読み込む
    const state = EditorState.create({
      doc: DOMParser.fromSchema(basicSchema).parse(xmlDoc), // TODO: basicSchema to custom schema?
      schema: basicSchema,
      plugins: exampleSetup({ schema: basicSchema }),
    });

    // 3. ビューの生成
    const view = new EditorView(proseMirrorRef.current, {
      state,
      dispatchTransaction(tr) {
        // 2. 状態を更新（これを行わないと画面に反映されない）
        const newState = view.state.apply(tr);
        view.updateState(newState);

        // コントローラーに通知
        controller.handleProseMirrorTransaction(tr, newState);

        // 1. Transactionの中身をのぞき見る
        // console.log("--- New Transaction ---");

        // 3. 変更があったかどうかを判定
        if (tr.docChanged) {
          // console.log("Document changed!");
          // const newText = newState.doc.textContent;
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
  }, [xhtml5meiDocument, controller]);

  return <div ref={proseMirrorRef}></div>;
}

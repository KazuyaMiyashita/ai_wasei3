import { exampleSetup } from "prosemirror-example-setup";
import "prosemirror-example-setup/style/style.css";
import "prosemirror-menu/style/menu.css";
import { DOMParser } from "prosemirror-model";
import { schema as basicSchema } from "prosemirror-schema-basic";
import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../hooks/useEditorController";

export function DocumentEditor({
  editorController,
}: {
  editorController: EditorController;
}) {
  const proseMirrorRef = useRef<HTMLDivElement>(null);

  // ハンドラとViewRefをRef経由で最新化
  const handleTransactionRef = useRef(
    editorController.handleProseMirrorTransaction,
  );
  handleTransactionRef.current = editorController.handleProseMirrorTransaction;

  const controllerViewRef = useRef(editorController.proseMirrorViewRef);
  controllerViewRef.current = editorController.proseMirrorViewRef;

  // biome-ignore lint/correctness/useExhaustiveDependencies: Initial content only
  useEffect(() => {
    if (!proseMirrorRef.current) return;

    console.log("Initializing ProseMirror...");

    // XMLパース処理
    const domParser = new window.DOMParser();
    const xmlDoc = domParser.parseFromString(
      editorController.document.rawContent,
      "text/html",
    );

    const state = EditorState.create({
      doc: DOMParser.fromSchema(basicSchema).parse(xmlDoc),
      schema: basicSchema,
      plugins: exampleSetup({ schema: basicSchema }),
    });

    const view = new EditorView(proseMirrorRef.current, {
      state,
      dispatchTransaction(tr) {
        const newState = view.state.apply(tr);
        view.updateState(newState);

        if (tr.docChanged) {
          handleTransactionRef.current(tr, newState);
        }
      },
    });

    // コントローラーにViewの参照を渡す
    if (controllerViewRef.current) {
      controllerViewRef.current.current = view;
    }

    return () => {
      console.log("Destroying ProseMirror...");
      if (controllerViewRef.current) {
        controllerViewRef.current.current = null;
      }
      view.destroy();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div ref={proseMirrorRef}></div>;
}

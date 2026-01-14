import { defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import { xml } from "@codemirror/lang-xml";
import { foldGutter } from "@codemirror/language";
import { EditorState } from "@codemirror/state";
import { EditorView, keymap, lineNumbers } from "@codemirror/view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../hooks/useEditorController";

export function CodeEditor({
  editorController,
}: {
  editorController: EditorController;
}) {
  const codeMirrorRef = useRef<HTMLDivElement>(null);

  // コントローラーのハンドラをRefで保持し、useEffect内で常に最新のものを呼べるようにする
  // これにより、useEffectの依存配列にeditorControllerを含める必要がなくなる
  const handleUpdateRef = useRef(editorController.handleCodeMirrorUpdate);
  handleUpdateRef.current = editorController.handleCodeMirrorUpdate;

  // ViewRefもRefで保持して、useEffect内で最新のRefに代入できるようにする
  // (editorController自体が切り替わった場合に対応するため)
  const controllerViewRef = useRef(editorController.codeMirrorViewRef);
  controllerViewRef.current = editorController.codeMirrorViewRef;

  // biome-ignore lint/correctness/useExhaustiveDependencies: Initial content only
  useEffect(() => {
    if (!codeMirrorRef.current) return;

    console.log("Initializing CodeMirror...");

    const startState = EditorState.create({
      // 初期化時のみ document.rawContent を使用する
      doc: editorController.document.rawContent,
      extensions: [
        lineNumbers(),
        foldGutter(),
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        xml(),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            handleUpdateRef.current(update);
          }
        }),
        EditorView.theme({
          "&": { height: "100%" },
        }),
      ],
    });

    const view = new EditorView({
      state: startState,
      parent: codeMirrorRef.current,
    });

    // コントローラーにViewの参照を渡す
    if (controllerViewRef.current) {
      controllerViewRef.current.current = view;
    }

    return () => {
      console.log("Destroying CodeMirror...");
      if (controllerViewRef.current) {
        controllerViewRef.current.current = null;
      }
      view.destroy();
    };
    // 依存配列を空にすることで、マウント時の1回のみ実行されるようにする
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <div ref={codeMirrorRef} className="h-full" />;
}

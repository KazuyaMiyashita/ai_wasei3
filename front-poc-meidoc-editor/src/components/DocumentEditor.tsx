import "prosemirror-example-setup/style/style.css";
import "prosemirror-menu/style/menu.css";
import { EditorView } from "prosemirror-view";
import { useEffect, useRef } from "react";
import type { EditorController } from "../hooks/useEditorController";
// CSSはApp.tsx等でグローバルに読み込まれているか、個別にimportする必要があるか確認が必要
// もしGlobalで読み込まれていなければ import "../styles/main-content.css"; が必要だが、
// 既存コードではstyles/main-content.cssをimportしている形跡がなかったので、
// グローバルCSSか、viteの機能で解決されていると仮定。
//念の為 import しておく
import "../styles/main-content.css";

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

  // 初期化ロジックを持つ関数もRefで保持（依存配列を空にするため）
  const createProseMirrorStateRef = useRef(
    editorController.createProseMirrorState,
  );
  // createProseMirrorState は document.rawContent に依存して再生成されるため、
  // Refを更新するのは初回レンダリング時だけにしたいが、
  // ここでは「マウント時の一回だけ初期化する」という要件なので、
  // 最初の createProseMirrorState をキャプチャして使うだけでよい。
  // ただし、もし `initialDocument` が非同期でロードされる場合は空の可能性があるが、
  // 現状は同期的な定数で初期化されている。

  useEffect(() => {
    if (!proseMirrorRef.current) return;

    console.log("Initializing ProseMirror...");

    // コントローラーにカプセル化されたロジックでStateを作成
    const state = createProseMirrorStateRef.current();

    const view = new EditorView(proseMirrorRef.current, {
      state,
      dispatchTransaction(tr) {
        const newState = view.state.apply(tr);
        view.updateState(newState);

        if (tr.docChanged) {
          handleTransactionRef.current(tr, newState);
        }
      },
      // エディタのルート要素にクラスを追加する場合
      // attributes: { class: "main-content" }
      // だが、今回はラッパーに適用するのでここでは指定しない
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

  return (
    <div
      ref={proseMirrorRef}
      className="main-content h-full overflow-y-auto bg-white p-4"
    ></div>
  );
}

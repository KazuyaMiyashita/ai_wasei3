import type { Transaction as CTransaction } from "@codemirror/state";
import type { EditorView as CEditorView, ViewUpdate } from "@codemirror/view";
import type {
  EditorState,
  Transaction as PTransaction,
} from "prosemirror-state";
import type { EditorView as PEditorView } from "prosemirror-view";
import { useCallback, useMemo, useRef, useState } from "react";
import { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";

// コンポーネント側で利用する型定義
export interface EditorController {
  document: XHTML5MEIDocument;
  // 各エディタのViewインスタンスへの参照（コントローラーから操作するため）
  codeMirrorViewRef: React.MutableRefObject<CEditorView | null>;
  proseMirrorViewRef: React.MutableRefObject<PEditorView | null>;
  // トランザクション履歴
  codeMirrorTransactions: CTransaction[];
  proseMirrorTransactions: PTransaction[];
  // イベントハンドラ
  handleCodeMirrorUpdate: (update: ViewUpdate) => void;
  handleProseMirrorTransaction: (
    tr: PTransaction,
    newState: EditorState,
  ) => void;
  updateDocument: (newContent: string) => void;
}

export function useEditorController(
  initialDocument: XHTML5MEIDocument,
): EditorController {
  const [document, setDocument] = useState<XHTML5MEIDocument>(initialDocument);

  // エディタのViewインスタンスを保持するRef
  const codeMirrorViewRef = useRef<CEditorView | null>(null);
  const proseMirrorViewRef = useRef<PEditorView | null>(null);

  // トランザクション履歴を保持するState
  const [codeMirrorTransactions, setCodeMirrorTransactions] = useState<
    CTransaction[]
  >([]);
  const [proseMirrorTransactions, setProseMirrorTransactions] = useState<
    PTransaction[]
  >([]);

  const updateDocument = useCallback((newContent: string) => {
    setDocument(new XHTML5MEIDocument(newContent));
  }, []);

  const handleCodeMirrorUpdate = useCallback((update: ViewUpdate) => {
    if (update.docChanged) {
      setCodeMirrorTransactions((prev) => [...prev, ...update.transactions]);

      // 仮実装: 状態を更新
      const newContent = update.state.doc.toString();
      setDocument(new XHTML5MEIDocument(newContent));

      // TODO: ProseMirror側への反映処理など
    }
  }, []);

  const handleProseMirrorTransaction = useCallback(
    (tr: PTransaction, _newState: EditorState) => {
      if (tr.docChanged) {
        setProseMirrorTransactions((prev) => [...prev, tr]);

        // 仮実装
        // TODO: XMLシリアライズしてモデルを更新する

        // TODO: CodeMirror側への反映処理など
      }
    },
    [],
  );

  // オブジェクトの参照を安定させるためにuseMemoを使用
  return useMemo(
    () => ({
      document,
      codeMirrorViewRef,
      proseMirrorViewRef,
      codeMirrorTransactions,
      proseMirrorTransactions,
      handleCodeMirrorUpdate,
      handleProseMirrorTransaction,
      updateDocument,
    }),
    [
      document,
      codeMirrorTransactions,
      proseMirrorTransactions,
      handleCodeMirrorUpdate,
      handleProseMirrorTransaction,
      updateDocument,
    ],
  );
}

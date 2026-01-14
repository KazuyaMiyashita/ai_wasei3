import type { Transaction as CTransaction } from "@codemirror/state";
import type { EditorView as CEditorView, ViewUpdate } from "@codemirror/view";
import { exampleSetup } from "prosemirror-example-setup";
import { DOMParser, DOMSerializer } from "prosemirror-model";
import {
  EditorState,
  type Transaction as PTransaction,
} from "prosemirror-state";
import type { EditorView as PEditorView } from "prosemirror-view";
import { useCallback, useMemo, useRef, useState } from "react";
import { mySchema } from "../lib/schema";
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
  // ProseMirrorの現在のXML（シリアライズ結果）
  proseMirrorXML: string;
  // イベントハンドラ
  handleCodeMirrorUpdate: (update: ViewUpdate) => void;
  handleProseMirrorTransaction: (
    tr: PTransaction,
    newState: EditorState,
  ) => void;
  updateDocument: (newContent: string) => void;
  // ProseMirror初期化用ヘルパー
  createProseMirrorState: () => EditorState;
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

  // ProseMirrorのシリアライズ結果
  const [proseMirrorXML, setProseMirrorXML] = useState<string>("");

  const updateDocument = useCallback((newContent: string) => {
    setCodeMirrorTransactions([]);
    setProseMirrorTransactions([]);
    setDocument(new XHTML5MEIDocument(newContent));
    setProseMirrorXML(newContent);
  }, []);

  const handleCodeMirrorUpdate = useCallback((update: ViewUpdate) => {
    if (update.docChanged) {
      setCodeMirrorTransactions((prev) => [...prev, ...update.transactions]);

      // 仮実装: 状態を更新
      const newContent = update.state.doc.toString();
      setDocument(new XHTML5MEIDocument(newContent));
    }
  }, []);

  const handleProseMirrorTransaction = useCallback(
    (tr: PTransaction, newState: EditorState) => {
      if (tr.docChanged) {
        setProseMirrorTransactions((prev) => [...prev, tr]);

        // XMLシリアライズしてモデルを更新する
        const serializer = DOMSerializer.fromSchema(mySchema);
        const fragment = serializer.serializeFragment(newState.doc.content);

        // DOM Fragmentを文字列に変換
        const tempDiv = window.document.createElement("div");
        tempDiv.appendChild(fragment);
        const xmlString = tempDiv.innerHTML;

        setProseMirrorXML(xmlString);
      }
    },
    [],
  );

  /**
   * 現在のdocumentからProseMirrorのEditorStateを作成する
   * <body>の中身のみを抽出してパースする
   */
  const createProseMirrorState = useCallback(() => {
    // 1. 文字列をDOMにパース
    const domParser = new window.DOMParser();
    const xmlDoc = domParser.parseFromString(document.rawContent, "text/html");

    // 2. bodyの中身を取り出す（bodyがなければルートを使うなどのフォールバック）
    const bodyContent = xmlDoc.body || xmlDoc.documentElement;

    // 3. ProseMirrorのNodeに変換
    // preserveWhitespace: "full" にすると改行などが保持されるが、
    // HTML構造としての保持を優先するためデフォルト(false/undefined)またはtrueにするか検討
    // ここではHTMLらしい挙動にするためデフォルトでいく
    const docNode = DOMParser.fromSchema(mySchema).parse(bodyContent);

    // 4. EditorState生成
    return EditorState.create({
      doc: docNode,
      schema: mySchema,
      plugins: exampleSetup({ schema: mySchema }),
    });
  }, [document.rawContent]);

  // オブジェクトの参照を安定させるためにuseMemoを使用
  return useMemo(
    () => ({
      document,
      codeMirrorViewRef,
      proseMirrorViewRef,
      codeMirrorTransactions,
      proseMirrorTransactions,
      proseMirrorXML,
      handleCodeMirrorUpdate,
      handleProseMirrorTransaction,
      updateDocument,
      createProseMirrorState,
    }),
    [
      document,
      codeMirrorTransactions,
      proseMirrorTransactions,
      proseMirrorXML,
      handleCodeMirrorUpdate,
      handleProseMirrorTransaction,
      updateDocument,
      createProseMirrorState,
    ],
  );
}

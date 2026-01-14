import type { ViewUpdate } from "@codemirror/view";
import type { EditorState, Transaction } from "prosemirror-state";
import { XHTML5MEIDocument } from "../lib/XHTML5MEIDocument";

// エディタの種類を定義
export type EditorSource = "code-mirror" | "prose-mirror";

/**
 * 2つのエディタ（CodeMirror, ProseMirror）の状態と更新を一元管理するコントローラー。
 * 更新情報を受け取り、中央で処理（同期、変換など）を行い、各ビューに通知する役割を持つ。
 */
export class EditorController {
  private document: XHTML5MEIDocument;
  private listeners: ((doc: XHTML5MEIDocument) => void)[] = [];

  constructor(initialContent: string) {
    this.document = new XHTML5MEIDocument(initialContent);
  }

  /**
   * 現在のドキュメントを取得する
   */
  getDocument(): XHTML5MEIDocument {
    return this.document;
  }

  /**
   * ドキュメント全体を更新する（外部からの強制更新など）
   */
  updateDocument(newContent: string) {
    this.document = new XHTML5MEIDocument(newContent);
    this.notifyListeners();
  }

  /**
   * CodeMirrorからの更新（Transaction）を受け取る
   * @param update CodeMirrorのViewUpdateオブジェクト
   */
  handleCodeMirrorUpdate(update: ViewUpdate) {
    if (update.docChanged) {
      console.log("[Controller] Received update from CodeMirror");
      // TODO: ここでトランザクション情報を解析し、内部モデルを更新したり、
      // ProseMirror側へのパッチを作成したりする処理を実装する

      console.log("--- CodeMirror Transaction ---");

      // すべての変更（changes）をループで確認
      update.transactions.forEach((tr) => {
        tr.changes.iterChanges((fromA, toA, _fromB, _toB, inserted) => {
          console.log(`変更範囲: ${fromA}〜${toA}`);
          console.log(`挿入されたテキスト: "${inserted.toString()}"`);
          // inserted.toString() が空なら「削除」を意味します
        });
      });

      // 仮実装: 単純にテキスト全体を取得して更新する（本来は差分更新が望ましい）
      const newContent = update.state.doc.toString();
      if (newContent !== this.document.rawContent) {
        this.document = new XHTML5MEIDocument(newContent);
        // 無限ループを防ぐため、発生源以外に通知する仕組みが必要だが、
        // 現状はReactのフローに従い、親への通知として扱う
        this.notifyListeners();
      }
    }
  }

  /**
   * ProseMirrorからの更新（Transaction）を受け取る
   * @param tr ProseMirrorのTransactionオブジェクト
   * @param _newState 更新後のEditorState
   */
  handleProseMirrorTransaction(tr: Transaction, _newState: EditorState) {
    if (tr.docChanged) {
      console.log("[Controller] Received transaction from ProseMirror");
      // TODO: ここでStepを解析し、内部モデルを更新したり、
      // CodeMirror側へのパッチを作成したりする処理を実装する

      // 仮実装: シリアライズ処理（本来はここで行うか、schema定義に任せる）
      // ここでは簡易的に、ProseMirrorのDOMSerializerなどを使ってHTML/XML化する想定だが
      // 実装が複雑になるため、コンソールログのみに留める
      console.log(
        "Steps:",
        tr.steps.map((s) => s.toJSON()),
      );

      // 注意: ProseMirrorのstateからXML文字列への変換は、schemaに応じたSerializerが必要
    }
  }

  /**
   * 変更通知を受け取るリスナーを登録する
   */
  subscribe(listener: (doc: XHTML5MEIDocument) => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  private notifyListeners() {
    this.listeners.forEach((listener) => {
      listener(this.document);
    });
  }
}

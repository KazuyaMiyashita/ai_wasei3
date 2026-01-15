/**
 * ResilientSyntaxTree.ts
 *
 * CodeMirror（文字列/XML）と ProseMirror（構造化ドキュメント）の間の「緩衝材」として機能する
 * 具象構文木（CST: Concrete Syntax Tree）に近い中間データ構造。
 */

// ------------------------------------------------------------------
// 1. 型定義: ノードの分類と抽象化設定
// ------------------------------------------------------------------

/**
 * ノードの分類（4つのカテゴリ）。
 * エディタはこのタイプに基づいて、ProseMirror上でどのように振る舞うか（編集可能か、アトムか等）を決定する。
 */
export type ResilientNodeType =
  | "Defined" // 1. スキーマで定義された正規の構造（例: <p>, <note>）。リッチに編集可能。
  | "Text" // 2. 空白情報を含むテキストノード。
  | "Foreign" // 3. XMLとしては正しいが、現在のスキーマでは定義されていない未知のタグ。
  | "Error"; // 4. 構文エラー、閉じ忘れ、不正な文字など。編集不可の塊として扱う。

/**
 * 構文解析のための定義（外部から注入する設定）。
 * これにより、MEI、TEI、HTMLなど、扱う対象が変わってもパーサーロジックを変更せずに済む。
 */
export interface SyntaxDefinition {
  /**
   * 指定されたタグ名が、ProseMirror側で「DefinedNode」として扱うべきものか判定する。
   * true の場合 -> "Defined"
   * false の場合 -> "Foreign"
   */
  isDefinedTag: (tagName: string) => boolean;

  /**
   * 閉じタグを持たない空要素（Void Element）かどうかを判定する。
   * (例: br, img, lb, pb)
   */
  isVoidTag?: (tagName: string) => boolean;

  /**
   * タグの中身をパースせず、生のテキストとして扱うべきタグか。
   * (例: script, style, verovioの特定の設定ブロックなど)
   */
  isRawTextTag?: (tagName: string) => boolean;
}

/**
 * RSTを構成する個々のノード。
 */
export interface ResilientNode {
  // ノードを一意に識別するID（YjsやReactのkey用）
  id: string;

  // ノードの分類
  type: ResilientNodeType;

  // CodeMirror上の文字位置（マッピング用）
  range: {
    from: number;
    to: number;
  };

  // 親ノードへの参照（ルートの場合はnull）
  parent: ResilientNode | null;

  // 子ノードのリスト
  children: ResilientNode[];

  // --- 以下、タイプごとの付加情報 ---

  // "Defined" | "Foreign" の場合に存在
  tagName?: string;
  attributes?: Record<string, string>;

  // "Text" | "Error" の場合に存在（Foreignの中身が生テキストの場合も含む）
  textContent?: string;

  // パースエラーの詳細メッセージ（"Error"の場合）
  errorMessage?: string;
}

// ------------------------------------------------------------------
// 2. クラス定義: ResilientSyntaxTree
// ------------------------------------------------------------------

/**
 * 壊れた状態を含むXML文字列を、エディタで扱える形式に抽象化して保持するツリー構造。
 *
 * ## 目的
 * 通常のDOMParserやXMLParserは、構文エラーに遭遇するとパースを中断したり、
 * エラー部分を捨ててしまったりするため、「壊れた状態」を保持できません。
 * 本クラスは「寛容なパース（Lenient Parsing）」を行い、エラー箇所や未知のタグも
 * 「そういう種類のノード」として保存することで、CodeMirror（文字列）とProseMirror（構造）の
 * 完全な相互変換（可逆性）を実現します。
 *
 * ## 利用フロー
 * 1. CodeMirrorの変更検知 -> 文字列を取得
 * 2. `ResilientSyntaxTree.parse(rawString, syntaxDef)` でツリー生成
 * 3. ツリーを走査(traverse)して ProseMirror の Node に変換
 */
export class ResilientSyntaxTree {
  public root: ResilientNode;
  private rawContent: string;
  private definition: SyntaxDefinition;

  private constructor(content: string, definition: SyntaxDefinition) {
    this.rawContent = content;
    this.definition = definition;
    // 初期化時は仮のルートを作成（実際はparseメソッド内で構築される）
    this.root = this.createNode("Defined", { from: 0, to: content.length });
    this.root.tagName = "root";
  }

  /**
   * 文字列と構文定義を受け取り、パース済みのツリー構造を返します。
   * これがメインのエントリーポイントです。
   */
  static parse(
    content: string,
    definition: SyntaxDefinition,
  ): ResilientSyntaxTree {
    const instance = new ResilientSyntaxTree(content, definition);
    instance.buildTree();
    return instance;
  }

  /**
   * 内部で「寛容なトークナイザ」を回し、ノードツリーを構築します。
   * (実装ノート: ここで正規表現や lezer-xml 等を使って文字列を走査する)
   */
  private buildTree(): void {
    // TODO: 実際の実装では、ここで文字列をスキャンし、スタックマシンを用いて
    // 親子関係を構築していく。
    //
    // ロジック概要:
    // 1. "<tagName" を見つける -> SyntaxDefinitionで Defined か Foreign か判定 -> Node作成 -> StackにPush
    // 2. "text..." を見つける -> TextNode作成 -> Stackの現在の親に追加
    // 3. "</tagName>" を見つける -> StackのPopを試みる
    //    -> もしStackの先頭とタグが合わなければ、そこまでの区間を "ErrorNode" として閉じる（寛容な処理）
    // 4. 不正な文字 ("<" が続くなど) -> ErrorNode作成
  }

  /**
   * ツリーを深さ優先探索(DFS)するためのビジターメソッド。
   * ProseMirrorへの変換時に使用します。
   */
  traverse(callback: (node: ResilientNode, depth: number) => void): void {
    const visit = (node: ResilientNode, depth: number) => {
      callback(node, depth);
      for (const child of node.children) {
        visit(child, depth + 1);
      }
    };
    visit(this.root, 0);
  }

  /**
   * CodeMirror上の特定の位置（インデックス）に対応する最も深いノードを検索します。
   * カーソル同期（CodeMirror <-> ProseMirror）に使用します。
   */
  findNodeAt(pos: number): ResilientNode | null {
    let current: ResilientNode | null = this.root;

    // 範囲チェック
    if (pos < current.range.from || pos > current.range.to) return null;

    while (current) {
      // 子ノードの中から、posを含むものを探す
      const foundChild = current.children.find(
        (child) => child.range.from <= pos && child.range.to >= pos,
      );

      if (foundChild) {
        current = foundChild;
      } else {
        // これ以上深く潜れない（現在がTextNodeや末端のNode）なら終了
        return current;
      }
    }
    return null;
  }

  /**
   * デバッグ用: ツリー構造を整形されたJSONのような文字列で返します。
   */
  toString(): string {
    let output = "";
    this.traverse((node, depth) => {
      const indent = "  ".repeat(depth);
      const info = node.tagName
        ? `<${node.tagName}>`
        : `"${node.textContent?.slice(0, 20)}..."`;
      output += `${indent}[${node.type}] ${info} (${node.range.from}-${node.range.to})\n`;
    });
    return output;
  }

  // --- Internal Helpers ---

  private createNode(
    type: ResilientNodeType,
    range: { from: number; to: number },
    parent: ResilientNode | null = null,
  ): ResilientNode {
    return {
      id: crypto.randomUUID(), // ブラウザ標準のUUID生成
      type,
      range,
      parent,
      children: [],
      attributes: {},
    };
  }
}

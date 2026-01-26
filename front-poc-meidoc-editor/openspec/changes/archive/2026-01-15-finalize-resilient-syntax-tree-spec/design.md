# 設計: ResilientSyntaxTree アーキテクチャ

## 1. コアコンセプト

`ResilientSyntaxTree` (RST) は、XML（およびその断片）のための「寛容なパース（Lenient Parsing）」を行う具象構文木 (CST) です。
ドキュメント状態の唯一の真実の源 (Single Source of Truth) として機能します。

**設計の基本原則:**

1.  **XMLベース**: 対象は XML 構造（XHTML, MEI 等）とする。
2.  **フォールバック禁止**: 編集時の更新において、パフォーマンス低下や状態リセットを防ぐため、全文再パースや再レンダリングへのフォールバックを禁止する。
3.  **Fail Fast**: 内部状態の不整合や想定外の編集操作に対しては、隠蔽（再パースで誤魔化す）せず、直ちに専用の例外 (`RSTIntegrityError` 等) を発生させる。

## 2. データ構造

### ResilientNode

各ノードは、元のドキュメント内のテキストのスパン（範囲）を表します。

```typescript
type ResilientNodeType = "Defined" | "Text" | "Foreign" | "Error";

interface ResilientNode {
  id: string; // 編集前後で同一性を追跡するための一意なID
  type: ResilientNodeType;
  parent: ResilientNode | null;
  children: ResilientNode[];

  // 位置情報 (ドキュメント全体の文字列に対する相対位置)
  range: { from: number; to: number };

  // コンテンツ情報
  tagName?: string; // Defined/Foreign 用
  attributes?: Record<string, string>; // Defined/Foreign 用
  textContent?: string; // Text/Error/Foreign の中身用
  errorMessage?: string; // Error 用
}
```

### スキーマ定義 (Syntax Definition)

RST は特定の XML フォーマットに依存せず、任意のスキーマ定義を注入可能です。
`src/lib/sampleContent.ts` のような XHTML + MEI の混在ドキュメントを扱うため、定義は柔軟である必要があります。

```typescript
interface SyntaxDefinition {
  // タグが "Defined" (スキーマで定義済み) かどうかを判定
  isDefinedTag(tagName: string): boolean;
  // 空要素タグかどうか
  isVoidTag(tagName: string): boolean;
  // 名前空間の処理などが拡張される可能性あり
}
```

## 3. パース戦略

初期ロード時は全文パースを行いますが、その後の変更は必ず増分更新を行います。

### 寛容なパース (Lenient Parsing)

- 閉じタグの欠落、不整合、属性値の引用符ミスなどを許容し、可能な限り構造を維持して `ErrorNode` またはテキストとして取り込みます。

## 4. 同期フロー

### 4.1. CodeMirror -> RST (増分更新)

1.  `changeset` を受け取る。
2.  **影響範囲の特定**: 変更箇所を含む最小のサブツリーを特定。
3.  **部分的再パース**: そのサブツリー（および文脈上必要な周辺）のみを再パース。
4.  **ツリー接合**: 新しい部分木を既存ツリーに接合。
5.  **インデックス更新**: 後続ノードの位置情報を更新。
    - _制約_: ここで不整合（例：計算後のドキュメント長が合わない）が生じた場合は `RSTIntegrityError` を送出する。**全文再パースには逃げない。**

### 4.2. RST -> ProseMirror

1.  RST をトラバースし、Schema に基づき PM ノードへ変換。
2.  **カーソル同期**: 変換前の RST 上のカーソル位置（または CodeMirror のカーソル位置）に対応する ProseMirror 上の位置を計算し、`Selection` を復元するよう努める。

### 4.3. ProseMirror -> RST

1.  Transaction を受け取る。
2.  RST の対応ノードを特定し、構造変更・テキスト変更を適用。
3.  **カーソル同期**: ProseMirror のカーソル位置に対応する RST (CodeMirror) 上の位置を計算する。

## 5. エラー処理ポリシー

開発およびテスト段階でバグを確実に検出するため、以下のケースで例外を発生させる。

- 増分更新適用後、`rst.toString()` が期待される結果（CodeMirror の内容）と一致しない場合。
- ツリー構造内で親子の range が矛盾している場合。
- 未知の操作タイプなど、ロジックが扱えない状況。

これにより、「なんとなく動いているが内部で壊れている」状態を防ぐ。

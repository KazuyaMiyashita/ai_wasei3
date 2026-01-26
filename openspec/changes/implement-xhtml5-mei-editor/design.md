# 設計: XHTML5 + MEI エディタ (改訂版)

## アーキテクチャ更新: 位置情報ベースのAST構築

### 問題点
現在の `sax-js` を用いたイベントベースのAST構築とタグ再構築ロジックは、複雑なネストや属性を持つXML（特にMEI）において、情報の欠落や構造の誤認（タグがテキストになる）を引き起こしています。

### 解決策: Substring Approach
`sax` パーサーを「構造の検出」と「位置の特定」のみに使用し、ノードのコンテンツは元のXML文字列から `substring` で抽出します。

#### AST構造の変更
```typescript
interface AstNode {
  // ... existing fields ...
  start: number; // 開始タグの開始位置
  end: number;   // 終了タグの終了位置
  contentStart?: number; // コンテンツの開始位置（開始タグの直後）
  contentEnd?: number;   // コンテンツの終了位置（終了タグの直前）
}
```

#### パースロジック
1. `onopentag`: ノードを作成し、`start` 位置を記録。スタックにプッシュ。
2. `onclosetag`: スタックからポップし、`end` 位置を記録。
    - **重要**: MEIタグ内では、子要素のイベントを無視し、MEIタグの `contentStart` から `contentEnd` までの文字列をそのまま `content` として保持する。
3. `ontext`: テキストノードの位置を記録。

このアプローチにより、属性のクォート、スペース、CDATA、コメントなどが完全に保存され、Verovio への入力が `sample_content.xml` の記述通りになることが保証されます。

## UX/UI 設計

### モード切り替え
- `Main.tsx` に `isEditMode` ステートを持たせる（あるいは `ViewState` に追加）。
- ヘッダーのトグルボタンで切り替え。
- **閲覧モード**: `<DocumentView>` (既存)。
- **編集モード**: `<SplitEditor>` (新規)。
    - `<SplitEditor>` は内部で `ProseMirror` (左) と `CodeMirror` (右) を持つ。
    - CSSクラスを既存のレンダラと共通化し、違和感をなくす。

### ProseMirror Schema
- `toDOM` メソッドで出力するHTML構造を、既存の `transformer.ts` が出力するものと一致させる（クラス名 `section`, `music-score` 等）。

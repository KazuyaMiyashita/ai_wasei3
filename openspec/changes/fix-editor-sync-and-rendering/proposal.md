# Proposal: Fix Editor Synchronization, Rendering, and Styling

## Summary
本変更では、編集モードにおける深刻な同期不具合（カーソルジャンプ、入力不可、タグ増殖）を修正し、閲覧モードとの見た目の差異（スタイル、青枠）を解消します。また、右パネル（CodeMirror）のスクロール不可問題を修正します。

## Issues Identified
1.  **同期ループとカーソルジャンプ**: ProseMirrorの変更が即座にAST経由でCodeMirrorへ、そして再度ProseMirrorへ同期される際に、全置換が発生してカーソルが飛ばされている。
2.  **タグ無限増殖**: `parser.ts` が `<virtual-root>` をASTに残したままシリアライズし、それが次のパースでさらにラップされるループが発生している。
3.  **スタイル不一致**: 編集モードの余白やSVGサイズが閲覧モードと異なる。また、編集時の青いフォーカス枠が不要である。
4.  **スクロール不可**: 右パネルのCSS設定不備。
5.  **Verovio警告**: 不正なデータや頻繁な更新による警告。

## Proposed Changes

### 1. AST Parser & Serializer Logic
- `parser.ts`: `<virtual-root>` を確実にアンラップするロジックを強化。
- `serializer.ts`: `root` タイプのノード自体はシリアライズせず、子供のみを出力することを徹底する。

### 2. Synchronization Logic (SplitEditor.tsx)
- ProseMirrorからの変更(`dispatchTransaction`)時は、ローカルの `code` state を更新するが、**CodeMirrorからの逆同期(`useEffect` -> `reconcile`)をトリガーしない**ようにフラグ管理または依存配列を整理する。
- ProseMirror自身のState更新(`view.updateState`)で完結させ、外部からの再注入を防ぐ。

### 3. Styling & Layout
- `SplitEditor.tsx` の ProseMirror コンテナから余計な `p-8` などを削除し、`DocumentView` と同様の構造にする。
- `main-content.css` またはグローバルCSSで、`.ProseMirror-focused` の `outline` を `none` に設定。
- 右パネルの親 `div` に `overflow-y: auto` を適切に設定。

### 4. Verovio Optimization
- `MeiNodeView` でデータが空の場合や変更がない場合のレンダリングをスキップする。

## Verification
- 入力中にカーソルが飛ばないこと。
- `<root><virtual-root>` が増殖しないこと。
- 閲覧モードと編集モードで見た目が（キャレット以外）一致すること。
- 青い枠が出ないこと。

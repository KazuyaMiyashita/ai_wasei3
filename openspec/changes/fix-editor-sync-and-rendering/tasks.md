# Tasks: Fix Editor Sync and Rendering

- [x] **AST Parser & Serializer 修正**
    - [x] `frontend22/src/lib/editor/ast/parser.ts`: `virtual-root` のアンラップロジックを修正し、`children` が正しく昇格されるようにする。
    - [x] `frontend22/src/lib/editor/ast/serializer.ts`: `root` ノードの場合、タグを出力せず子要素のみを出力することを確認。
    - [x] **Test**: `<virtual-root>` が増殖しないことを確認するテストケースを追加 (`ast.test.ts`)。

- [x] **SplitEditor 同期ロジック修正**
    - [x] `frontend22/src/components/editor/SplitEditor.tsx`:
        - ProseMirror 編集時 (`dispatchTransaction`): `activeDocument` と `code` を更新するが、直後の `reconcile` をブロックするフラグ制御を厳密にする。
        - CodeMirror 編集時: 入力中は同期をデバウンスし、フォーカスがある場合はカーソル位置を維持する工夫（あるいはPM側で強制フォーカスしない）。

- [x] **スタイル修正**
    - [x] `frontend22/src/components/editor/SplitEditor.tsx`:
        - 左パネルの `p-8` 等のクラスを削除し、`DocumentView` と同じパディング構成にする。
        - 右パネルのコンテナに `overflow-auto h-full` を適用しスクロール可能にする。
    - [x] `frontend22/src/index.css`:
        - `.ProseMirror:focus` および `.ProseMirror-focused` に対して `outline: none` を設定。

- [x] **Verovio NodeView 修正**
    - [x] `frontend22/src/lib/editor/view/mei-node-view.ts`:
        - `content` が空、または有効なXMLでない場合にレンダリングをスキップまたはエラー表示を抑制するガードを追加。

- [x] **動作確認**
    - [x] `sample_content.xml` を開き、編集モードでの入力、モード切替、XMLパネルのスクロールを確認。
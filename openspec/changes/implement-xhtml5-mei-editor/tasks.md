# タスク: XHTML5+MEI エディタの修正

- [x] **ASTパーサーの再実装 (Substring方式)**
    - [x] `frontend22/src/lib/editor/ast/types.ts`: `start`, `end`, `contentStart`, `contentEnd` プロパティを追加。
    - [x] `frontend22/src/lib/editor/ast/parser.ts`: `sax` のイベントを使用して位置情報を記録し、文字列スライスでコンテンツを抽出するように書き換え。
    - [x] **Test**: `ast.test.ts` を更新し、タグがテキスト化しないこと、MEIの中身が完全であることを確認。

- [x] **Verovio NodeView の修正**
    - [x] `frontend22/src/lib/editor/view/mei-node-view.ts`: ASTから渡された完全なMEI文字列を使用するように確認。

- [x] **ProseMirror Schema & Styling の調整**
    - [x] `frontend22/src/lib/editor/schema/xhtml-mei.ts`: `toDOM` の出力を調整し、`frontend22/src/styles/main-content.css` のクラス（例: `.section`, `.document-content` 等）が適用されるようにする。
    - [x] `SplitEditor.tsx`: ProseMirrorのマウント要素に適切なクラスを付与。

- [x] **モード切り替えUIの実装**
    - [x] `frontend22/src/context/ApplicationContext.tsx` または `ViewState` に `editMode` フラグを追加。
    - [x] `frontend22/src/components/center/Main.tsx`: `editMode` に応じて `DocumentView` と `SplitEditor` を切り替えるロジックを実装。
    - [x] `frontend22/src/components/header/Header.tsx`: モード切り替えボタンを追加。

- [x] **同期バグの修正**
    - [x] `frontend22/src/components/editor/SplitEditor.tsx`: `activeDocument` の変更検知 (`useEffect`) を見直し、CodeMirror (`code` state) が確実に更新されるように修正。

- [x] **最終確認**
    - [x] `sample_content.xml` を開き、楽譜が表示されること。
    - [x] 閲覧モードと編集モードを行き来できること。
    - [x] 別のファイルを開いたときに表示が切り替わること。

# Proposal: Refactor Editor Structure and Rename ViewMode

## Summary
`SplitEditor` を解体し、`DocumentEditor` (WYSIWYG) と `CodeEditor` (XML) に分離します。これらを `Main` コンポーネントで統合し、`editMode` と `layoutMode` (旧 `viewMode`) に基づいて表示を切り替えるアーキテクチャに変更します。

## Motivation
- **再利用性**: 既存の `CodeView` の高度な機能（選択範囲同期など）を編集モードでも利用するため。
- **責務の分離**: レイアウト制御 (`Main`) とエディタ機能 (`DocumentEditor`, `CodeEditor`) を分離し、コードの見通しを良くするため。
- **命名の明確化**: `viewMode` は「表示形式（Document/Code/Split）」を指すが、`view` という語が多義的であるため、より具体的な `layoutMode` に変更する。

## Proposed Changes

### 1. Rename `viewMode` to `layoutMode`
- `ViewState` クラス、`ApplicationState` インターフェース、およびそれらを参照する全てのコンポーネント（`Main`, `ScoreControlBar` 等）でリネームを行う。

### 2. Component Restructuring
- **`CodeView` -> `CodeEditor`**: ファイル名を変更し、エクスポート名を変更。機能は維持しつつ、必要に応じて編集モード用のPropsを受け取れるようにする（基本は `ActiveDocument` 経由で動作するため大きな変更はない見込み）。
- **`DocumentEditor` (New)**: `SplitEditor` の左側（ProseMirror部分）を独立したコンポーネントとして作成。
    - `activeDocument` を受け取り、ProseMirrorの初期化と同期を行う。
    - **重要**: `SplitEditor` にあった同期ロジック（`lastSyncedCode` 等）の一部は、コンポーネント間の連携が必要になるため、`Main` に移動するか、あるいは `DocumentEditor` が自律的に `ActiveDocument` を監視・更新するように設計する。
- **`SplitEditor` (Remove)**: 廃止。

### 3. Main Component Logic
- `Main.tsx` でレイアウト制御を集約。
- **Logic**:
    - `targetDocumentComponent`: `editMode` ? `<DocumentEditor />` : `<DocumentView />`
    - `targetCodeComponent`: `<CodeEditor />`
    - `layoutMode` switch:
        - `document`: `targetDocumentComponent`
        - `code`: `targetCodeComponent`
        - `split`: `ResizablePanel` で両方を表示。

## Synchronization Strategy
- `CodeEditor` は既に `ActiveDocument` を監視し、変更があれば反映するロジックを持っている。
- `DocumentEditor` も同様に `ActiveDocument` を監視し、ProseMirrorの状態を更新する。また、ProseMirrorの変更を `ActiveDocument` に反映する。
- **ループ防止**: 各エディタ内で「自分の変更による更新」か「外部からの更新」かを判定する（既存の `CodeView` や `SplitEditor` のロジックを流用）。

## Verification
- `viewMode` -> `layoutMode` のリネーム漏れがないこと。
- 閲覧/編集モードの切り替えがスムーズに行えること。
- 分割表示時に、左（WYSIWYG）と右（Code）が同期すること。
- 右パネル（`CodeEditor`）で既存のハイライト機能などが動作すること。

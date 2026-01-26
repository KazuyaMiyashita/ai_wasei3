# Implementation Report: Full Differential Sync & Architectural Refinement

## 概要

本プロジェクトでは、`ResilientSyntaxTree` (RST) を核とした `CodeMirror` (CM) および `ProseMirror` (PM) の双方向同期システムを、完全な差分更新（Differential Sync）ベースに刷新しました。また、責務の分離を徹底し、将来の拡張性（Verovio統合等）を考慮したアーキテクチャへとリファクタリングしました。

## 実装詳細

### 1. 差分同期アルゴリズムとデータ構造

- **RSTChange インターフェース**:
  ```typescript
  export interface RSTChange {
    from: number; // RST絶対オフセット
    to: number; // 変更前の終了位置
    insert: string; // 挿入テキスト
    affectedNodes: string[]; // 影響を受けたノードID
  }
  ```
- **LCAベースの差分検出**:
  `ResilientSyntaxTree.edit()` は、変更の影響範囲をカバーする最小の共通先祖 (LCA) を特定し、その範囲の文字数と新しい文字列表現を報告します。これにより、大規模なドキュメントでも局所的な更新が可能になります。

### 2. アーキテクチャのリファクタリング (責務の分離)

- **CodeMirrorAdapter**: CM固有の `ViewUpdate` をRST形式の変更に変換し、逆にRSTからの差分をCMに適用します。
- **ProseMirrorAdapter**: PMの `Transaction` を解析し、IDマップを活用してRSTの位置へマッピングします。RSTからの変更時は、IDベースでPMノードを特定し、インクリメンタルに置換します。
- **EditorController**: オーケストレーターとして、ロック状態の管理とアダプター間の仲介に専念します。

### 3. 表示の最適化と一貫性

- **スタイルの統一**: `ProseMirror-example-setup` の不要なスタイルを削除し、`DocumentViewer` と `DocumentEditor` の見た目を統一しました。また、MEI要素の表示改善（`pre`のスタイル調整）を行いました。
- **DocumentViewer (React再帰レンダリング)**: `dangerouslySetInnerHTML` を廃止。RSTノードをReactコンポーネントに1対1でマッピングし、`React.memo` を活用することで、変更のあったサブツリーのみを再描画します。
- **MEINodeView**: `<mei>` 要素専用の `NodeView` を実装。将来のVerovio統合の受け皿となります。
- **Body-only Scope**: エディタおよびビューアの表示範囲をドキュメントの `<body>` 要素内に限定し、XMLプロローグやヘッダー等の非表示要素を排除しました。
- **Whitespace保持の改善**: `white-space: pre-wrap` の導入により、CodeEditorでのインデント等がDocumentEditorでも正しく反映されるようになりました。
- **空白ノード処理**: 不要な空白テキストノード（レイアウト用インデントなど）がProseMirrorにブロック要素としてレンダリングされる問題に対し、一部条件付きでフィルタリングする処理を追加しました。

## パフォーマンス特性

- **IDマップキャッシュ**: PMノードの位置特定において、O(N)の走査を避け、キャッシュされたIDマップを使用することで O(1)~O(log N) の効率を実現しています。
- **インクリメンタルDOM更新**: 全文置換を一切行わず、変更箇所のみをライブラリ固有のトランザクションとして発行するため、大規模ドキュメントでもレイテンシが極小化されています。

## 残存課題と今後の展望

### ProseMirror同期とUXの課題（重要）

ユーザーからの指摘により、以下の重大な課題が特定されています。これらはProseMirrorの知識不足に起因するものであり、次回のタスクで優先的に修正する必要があります。

1.  **インデントによる表示不一致**:
    XMLソース内のインデント（改行＋スペース）が、DocumentEditorではそのままテキストとして表示されてしまっています。`<p>`タグ等の直下にあるインデント用空白は、WYSIWYGエディタとしては無視または制御されるべきですが、現状はコンテンツの一部としてレンダリングされています。
2.  **メニュー操作の同期不全**:
    DocumentEditorのメニューバーから「太字」などの装飾を行っても、その変更がCodeEditor（およびRST）に正しく反映されません。現在のPM -> RSTのマッピングロジックが、マーク（装飾）の変更や属性変更を十分に捉えきれていません。
3.  **複雑な編集操作の同期漏れ**:
    複数の要素にまたがるカット＆ペーストや削除操作を行った際、CodeEditorへの同期が行われない、または不正確になるケースがあります。`ProseMirrorAdapter` のトランザクション解析ロジック（`ReplaceStep` の処理）をより堅牢にする必要があります。

### その他

- **Verovio統合**: `MEINodeView` の内部実装を差し替えることで、楽譜のグラフィカルな表示を容易に実現できます。

# Tasks

## 0. Preliminary Investigation

- [x] **Yjs導入調査**: `Y.XmlFragment`等がRSTの要件（不完全XMLの許容）を満たし、かつパフォーマンス/同期の問題を解決できるかプロトタイプを作成して検証する。
  - **判定**: Yjsが適合する場合、以降のタスクをYjsベースの実装に書き換える。不適合の場合、以下の独自実装を進める。
  - _結果_: `openspec/changes/2026-01-16-fix-rst-sync-and-view/yjs_decision.md` 参照。Yjsは不採用。

## 1. Architectural Refactoring: Separation of Concerns

- [x] **責務の分割**: `EditorController` に集中している CodeMirror と ProseMirror 固有のロジックを分離するアーキテクチャを導入する。
- [x] **`CodeMirrorAdapter` の作成**:
  - CM の `ViewUpdate` をリッスンし、RST 向けの変更形式に変換する責務を持つ。
  - RST からの変更通知を受け取り、CM の `Transaction` を生成して適用する責務を持つ。
- [x] **`ProseMirrorAdapter` の作成**:
  - PM の `Transaction` をリッスンし、RST 向けの変更形式に変換する責務を持つ。
  - PM <-> RST 間のポジションマッピングロジックを内包する責務を持つ。
  - RST からの変更通知を受け取り、PM の `Transaction` を生成して適用する責務を持つ。
- [x] **`EditorController` の責務縮小**:
  - 中心の調整役（Orchestrator）として、RSTインスタンスの保持、ロック管理、アダプター間の変更通知のみを担当するように責務を限定する。
- [x] **テストの移行**: `EditorController.test.ts` にある CM/PM 固有のテストを、それぞれのアダプターのテストファイルに移行・リファクタリングする。

## 2. ResilientSyntaxTree 拡張 (Custom Diff)

- [x] **`RSTChange` インターフェース定義**: `affectedNodes` 等の情報を含める。
- [x] **`applySingleChange` 改修**: 変更内容を正確に追跡し、構造変化かテキスト変化かを判別する。
- [x] **`edit` メソッド改修**: `RSTChange[]` を返す。
- [x] **テスト**: 基本3ケース（テキスト挿入、削除、属性変更）、複雑3ケース（タグ分割、マージ、深いネストの置換）のテスト作成とパス。`pnpm check` 通過。

## 3. Adapter Implementations (Bidirectional Diff Sync)

- [x] **`CodeMirrorAdapter` 実装**: RSTとの双方向差分同期ロジックを実装する。
  - **テスト**: CMでの入力がカーソル位置を乱さずにRSTに反映され、逆もまた然りであることを確認。基本3/複雑3ケース。`pnpm check`。
- [x] **`ProseMirrorAdapter` 実装**: RSTとの双方向差分同期ロジックを実装する。
  - **テスト**: PMでの編集がRSTに正確にマッピングされ、RSTからの変更でPMの選択範囲やスクロールが維持されることを確認。基本3/複雑3ケース。`pnpm check`。

## 4. UI/View レンダリングと改善

- [x] **`getBodyNode` 実装**: `<body>` 要素抽出ヘルパー。
- [x] **`DocumentViewer` 最適化**: `dangerouslySetInnerHTML` を廃止し、RSTノードを再帰的にReactコンポーネントとしてレンダリングする（`React.memo` 利用でパフォーマンス確保）。
- [x] **MEI表示**: Viewer内で `<mei>` タグを検出し、コードブロック風のスタイルで表示する。
- [x] **空白ノード処理**: CodeEditor用の空白ノードがDocumentEditorで視覚的ノイズにならないよう、Schema定義またはPMレンダリング時に制御する（CSSクラス付与やNodeView利用）。
- [x] **メニューバー修正**: メニュー項目がスキーマ定義と一致したノード（正しい属性/ID付き）を挿入するようにする。
- [x] **テスト**: レンダリング結果の確認。Bodyのみ表示されているか。MEIが見えるか。基本3/複雑3ケース。`pnpm check`。

## 5. 統合検証とドキュメント作成

- [x] **統合テスト**: `src/lib/sampeContent.ts` の内容を用い、3つのビュー（CM, PM, Viewer）を行き来して編集・閲覧を行い、整合性とRSTの破損がないか確認する。
- [x] **成果物作成**:
  - **実装レポート**: 採用したアルゴリズム（特に差分計算とマッピング）、データ構造、パフォーマンス特性、残存課題（もしあれば）。
  - **デモンストレーション**: 主要機能（同期、MEI表示、エラー耐性）が動作している様子を示す手順またはスクリプト。

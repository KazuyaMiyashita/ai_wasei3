# Implementation Report: Integrate RST Editor Controller

## 概要

`EditorController` に `ResilientSyntaxTree` (RST) を統合し、ProseMirror (PM) と CodeMirror (CM) の双方向同期、および排他制御を実現しました。
特に難関であった PM から RST へのマッピング処理も、主要なケース（テキスト挿入・削除）において動作することを確認しました。

## 実装詳細

### 1. 排他制御

`EditorController` に `lockState` (`Idle`, `ProcessingFromCM`, `ProcessingFromPM`) を導入し、編集の競合を防ぐ仕組みを実装しました。
一方のエディタからの変更処理中は他方をロックし、UI（デバッグビュー）にもステータスを表示するようにしました。

### 2. CM -> RST -> PM 同期

CodeMirror の変更は `rst.edit` を通じて RST に適用され、その後 `rst.toProseMirrorDoc` を用いて ProseMirror ドキュメントを再生成（または同期）します。
`<mei>` 要素は RST 上で適切に保持され、ProseMirror 上では `mei_node` として `<pre>` タグ内に生テキストが表示されるようにしました。

### 3. PM -> RST マッピング

ProseMirror の Transaction (`ReplaceStep`) を解析し、RST の編集操作に変換するロジックを `EditorController` に実装しました。

- `mapPMToRSTPosition` メソッドにより、PM のノード座標を RST の文字オフセットに変換します。
- PM の `insertText` が生成する `Slice` から不要なラッパー要素を取り除き、純粋なテキストとして RST に適用する工夫を行いました。
- ノード境界での挿入位置のズレを補正するため、`nodeBefore` / `nodeAfter` を参照するヒューリスティックを導入しました。

### 4. デバッグビュー

`EditorControllerDebugView` を拡張し、以下の機能を追加しました。

- **Logs Tab**: システム、CM、PM からのイベントログを表示。
- **RST Tab**: RST のツリー構造を視覚化。
- **Lock Status**: 現在のロック状態を表示。

## 制限事項と今後の課題

1. **PM -> RST マッピングの完全性**:
   - 現在は `ReplaceStep` によるテキスト挿入・削除、および単純なブロック操作をサポートしています。
   - 複雑なスキーマ変更（ノードのラップ解除、リフトなど）や、属性の変更 (`AttributeStep`) はまだサポートしていません。これらは RST の再パースで対応するか、追加の実装が必要です。

2. **カーソル位置の維持**:
   - `syncCodeMirrorFromRST` において、現在は全置換を行っています。CodeMirror はある程度カーソル位置を維持しようとしますが、大幅な変更時にはカーソルがリセットされる可能性があります。

3. **パフォーマンス**:
   - 文字入力ごとの RST 更新と再レンダリングは、大規模なドキュメントではパフォーマンスのボトルネックになる可能性があります。今後、Web Worker での処理や、Yjs を用いた同期への移行を検討すべきです。

## 検証結果

- 単体テスト (`EditorController.test.ts`) において、CM/PM 双方からの編集が正しく同期されることを確認しました。
- PM 内でのテキスト挿入、削除、およびネストされた要素内での編集が RST に反映されることを確認しました。

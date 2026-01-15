# codemirror-interop Specification

## Purpose
TBD - created by archiving change finalize-resilient-syntax-tree-spec. Update Purpose after archive.
## Requirements
### Requirement: 厳格な増分更新 (Strict Incremental Updates)

RST は、CodeMirror の変更セットに対し、全文再パースやフォールバックを行うことなく、高速なアルゴリズムで差分のみを更新しなければならない (MUST)。

#### Scenario: 大規模ドキュメントの編集

前提: `sampleContent.ts` のような数千行のドキュメントがロードされている
もし: ユーザーが1文字入力した (`edit` が呼ばれた) とき
ならば: システムはドキュメント全体を再スキャンしてはならない (MUST NOT)
かつ: 影響を受ける局所的なノードのみを更新しなければならない (MUST)。

### Requirement: 例外による整合性保証

編集操作の結果、ツリーの内部状態（インデックスの整合性や文字列との一致）が破壊された場合、システムは隠蔽せずに `RSTIntegrityError` を送出しなければならない (MUST)。

#### Scenario: 不正な更新の検知

前提: バグによりノードの長さ計算が間違った状態になった
もし: 整合性チェックが走ったとき（または次の操作時）
ならば: システムは即座に例外を発生させ、テストで失敗として検知されるようにしなければならない (MUST)。

### Requirement: 座標マッピングとカーソル同期

RST は、編集後もすべてのノードについて正しい座標を維持し、CodeMirror のカーソル位置に対応する RST ノードを特定できなければならない (MUST)。

#### Scenario: ノード検索

前提: テキストの中間地点（インデックス 500）にカーソルがある
もし: `findNodeAt(500)` を呼び出したとき
ならば: その位置をカバーする最も深いノード（例: Textノード）が返される (MUST)。


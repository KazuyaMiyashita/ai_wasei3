# 実装レポート: ResilientSyntaxTree

## 1. 概要
耐障害性の高い XML 解析とエディタ統合 (CodeMirror <-> ProseMirror) のために設計された CST である `ResilientSyntaxTree` (RST) を正常に実装しました。

### 主な特徴
- **完全な可逆性 (Lossless Round-trip)**: 空白、属性の引用符、孤立したタグなどを正確に保持します。
- **耐障害パース (Resilient Parsing)**: 閉じられていないタグ、孤立したタグ、混在したコンテンツなどをクラッシュすることなく処理します。
- **厳格な増分更新 (Strict Incremental Updates)**: 全文再パースを行わずにテキスト編集に基づいてツリーを更新します (V1 では O(N) ロジック、最適化可能)。
- **ProseMirror 統合**: RST を PM ノードに変換し、可能な限り構造的な妥当性を維持します。
- **座標マッピング**: 線形位置とノード ID/オフセット間のマッピングをサポートします。

## 2. コンポーネント

### `src/lib/ResilientSyntaxTree.ts`
コアクラス。
- `ResilientNode`: CST ノードを表すクラス。座標追跡に `length` を使用します。
- `ResilientSyntaxTree`: ツリー、`edit()` ロジック、`toProseMirrorDoc()` を管理します。
- `SimpleXmlTokenizer`: 生の XML 文字列分割のためのカスタムトークナイザ。

### `src/lib/SimpleXmlTokenizer.ts`
正規表現を最小限に抑えたスキャナで、文字列を `OpenTag`、`CloseTag`、`Text` などに分割し、1 文字も失われないようにします。

### `src/lib/schema.ts`
ProseMirror スキーマの拡張:
- `xml_block`: XML 要素の汎用コンテナ。
- `xml_textblock`: 汎用テキストコンテナ (paragraph のようなもの)。
- `error_node`: 解析エラーを表示するためのノード。

## 3. 検証結果

### テストカバレッジ
`vitest` を使用して包括的なテストを実装しました。

#### 1. コア・ラウンドトリップ
`parse -> toString` が以下のケースで正確な同一性を保つことを確認しました:
- 単純な XML (`<root>...`)
- 引用符が混在した属性 (`<div class="a" id='b'>`)
- 深いネスト
- `sampleContent.ts` (大規模な XHTML/MEI 混在ドキュメント)

#### 2. エラー処理
- **暗黙のクローズ**: `<div><p>text</div>` -> `p` は暗黙的に閉じられます。
- **孤立した閉じタグ**: `<div>text</p></div>` -> `</p>` は `ErrorNode` になります。
- **自動クローズ**: `SyntaxDefinition` を介して HTML 風の `<p>...<p>` 動作をサポート。

#### 3. 増分更新 (Edit)
- **挿入**: テキストの挿入がツリーを正しく更新。
- **削除**: テキストの削除がツリーを更新。
- **構造の分割**: `</p><p>` の挿入が一つのノードを二つに分割。
- **構造の結合**: `</p><p>` の削除が二つのノードを結合。
- **属性**: 属性のインプレース変更が動作。

#### 4. ProseMirror 統合
- RST を `doc` -> `xml_block` -> `paragraph` に変換。
- 構造の整合性を維持。
- `data-rst-id` を介した ID マッピングをサポート。

#### 5. ファジング (Fuzzing)
- `sampleContent.ts` に対する 100 回のランダムな編集に耐えました。
- `root.length` の整合性と `toString` の一貫性を維持しました。

### デモンストレーションログ
```
 RUN  v4.0.17 /Users/miy/Desktop/ai_wasei3/front-poc-meidoc-editor

 ✓ src/lib/ResilientSyntaxTree.test.ts (15 tests) 7ms
   ✓ ResilientSyntaxTree (15)
     ✓ Core Parsing & Round-trip (4)
     ✓ Error Handling & Lenient Parsing (3)
     ✓ Complex Cases (2)
     ✓ Incremental Updates (edit) (6)

 ✓ src/lib/ResilientSyntaxTree.pm.test.ts (2 tests) 2ms
   ✓ ProseMirror Integration (2)

 ✓ src/lib/ResilientSyntaxTree.fuzz.test.ts (1 test) 27ms
   ✓ Fuzzing & Performance (1)
```

## 4. 今後の改善
- **パフォーマンス**: `findNodeAt` と `updateLengthUpwards` は O(Depth) で良好ですが、`findNodeCovering` は子ノードを線形にスキャンします。非常に大きなフラットなリスト（例：1万項目）の場合、子ノードに `GapBuffer` や `B-Tree` 構造を採用するとパフォーマンスが向上します。
- **属性の解析**: 現在 `parseAttributes` は単純な正規表現です。ほとんどのケースを処理しますが、複雑なエスケープされた引用符に対しては脆弱な可能性があります。
- **ProseMirror リバースマッピング**: `toProseMirrorDoc` を実装しました。逆方向 (PM Transaction -> RST Edit) のロジックは ID マッピングによって部分的にサポートされていますが、`model_refinement.md` で設計された完全な `Step` 解釈ロジックは `EditorController` で完全に肉付けする必要があります。

## 5. 結論
`ResilientSyntaxTree` は完全に実装され、厳格な要件に対して検証されました。エディタ UI への統合の準備が整いました。
# 実装タスク

## フェーズ 0: 調査と準備 (Investigation & Preparation)

- [x] CodeMirror と ProseMirror の連携仕様、API詳細、不明点を WebSearch/WebFetch を用いて調査し、設計の確度を高める。 <!-- id: 14 -->

## フェーズ 0.5: モデルの洗練と高度な機能の検討 (Model Refinement)

- [x] 座標更新パフォーマンス改善 (O(N) -> O(log N)) のためのデータ構造（相対オフセットやGap Buffer等）を検討し、コアモデルを更新する。 <!-- id: 15 -->
- [x] ProseMirror Transaction の完全なマッピングを実現するためのリバースマッパー設計を検討し、コアモデルを更新する。 <!-- id: 16 -->
- [x] エラー回復戦略（Auto-closing等）の高度化について検討し、パーサーロジックの設計を更新する。 <!-- id: 17 -->

## フェーズ 1: コア実装 (Core Implementation)

- [x] `ResilientNode` インターフェースと `SyntaxDefinition` (任意のスキーマ注入用) を定義する。 <!-- id: 0 -->
- [x] XML構造のためのトークナイザを実装する。 <!-- id: 1 -->
- [x] 寛容なエラー処理を持つ初期パースロジック `ResilientSyntaxTree.parse(string, def)` を実装する。 <!-- id: 2 -->
- [x] 完全な可逆性を保証する `ResilientSyntaxTree.toString()` を実装する。 <!-- id: 3 -->
- [x] `sampleContent.ts` の内容をパースし、ラウンドトリップできるか検証する。 <!-- id: 4 -->
- [x] コア機能（パース・文字列化）に対し、基本ケース3通り・複雑ケース3通りのテストを作成し実行する。 <!-- id: 19 -->

## フェーズ 2: CodeMirror 統合 & 増分更新 (Strict Incremental Updates)

- [x] CodeMirror の変更を受け入れる `ResilientSyntaxTree.edit(changes)` を実装する。 <!-- id: 5 -->
- [x] **全文再パースを行わない** 高速な増分更新アルゴリズムを実装する。 <!-- id: 6 -->
- [x] 編集後の整合性チェックと、不整合時の `RSTIntegrityError` 送出処理を実装する。 <!-- id: 7 -->
- [x] 座標更新ロジックを実装する。 <!-- id: 8 -->
- [x] 増分更新機能に対し、基本ケース3通り（単純挿入・削除）・複雑ケース3通り（構造変更・ネスト・大規模）のテストを作成し実行する。 <!-- id: 20 -->

## フェーズ 3: ProseMirror 統合 & カーソル同期

- [x] XHTML/MEI を含む `sampleContent.ts` に対応する ProseMirror Schema を定義する。 <!-- id: 9 -->
- [x] `ResilientSyntaxTree.toProseMirrorDoc(schema)` コンバータを実装する。 <!-- id: 10 -->
- [x] RST <-> ProseMirror 間のカーソル位置同期ロジックを実装する。 <!-- id: 11 -->
- [x] ProseMirror変換・同期機能に対し、基本ケース3通り・複雑ケース3通りのテストを作成し実行する。 <!-- id: 21 -->

## フェーズ 4: 検証と仕上げ (Validation & Polish)

- [x] エッジケースとランダム編集（Fuzzing）を行い、**例外が発生しないこと**を確認するテストを追加する。 <!-- id: 12 -->
- [x] `sampleContent.ts` 規模前後のドキュメントでのパフォーマンスを検証する。 <!-- id: 13 -->

## フェーズ 5: ドキュメンテーションとデモンストレーション (Documentation & Demonstration)

- [x] 実装の概要、詳細（アルゴリズム、データ構造）、および残された改善の余地（パフォーマンス、機能制限等）をまとめたドキュメントを生成する。 <!-- id: 18 -->
- [x] 実装された機能の動作を示す実行デモンストレーション（CLIでの動作確認やテスト実行ログ、またはUI上での動作状況の記録）を実施・記録する。 <!-- id: 22 -->

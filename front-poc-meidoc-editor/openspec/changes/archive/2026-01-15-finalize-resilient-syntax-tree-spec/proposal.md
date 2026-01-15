# Resilient Syntax Tree 仕様の確定

## 概要 (Summary)

XMLおよびそれが壊れた状態の文字列（CodeMirror）と構造化ドキュメント（ProseMirror）の間の、寛容かつ損失のない（Lossless）ブリッジとして機能するデータ構造 `ResilientSyntaxTree` (RST) の仕様を定義します。本提案では、ノード構造、パース挙動、文字列化の保証、および両エディタとの同期メカニズムを定式化します。

## 動機 (Motivation)

現在のプロジェクトでは、生のテキスト（柔軟性と低レベルな制御のため）と構造化ツリー（意味的な検証とリッチなインタラクションのため）の両方としてドキュメントを同時編集する必要があります。対象となるドキュメントは XHTML や MEI (Music Encoding Initiative) を含む XML ベースの形式です。
標準的なパーサーは不正なXMLで失敗し、編集体験を損ないます。`ResilientSyntaxTree` は以下によってこれを解決します：

1.  **寛容なパース (Lenient Parsing)**: 不正な構文を失敗させるのではなく、「エラーノード」として扱います。
2.  **可逆性 (Lossless Round-trip)**: テキストビューを安定させるために、空白やフォーマットを含むすべての文字を保持します。
3.  **高速な増分更新 (Fast Incremental Updates)**: CodeMirror からのテキスト編集に対し、全文再パースへのフォールバックを禁止し、常に高速な差分更新アルゴリズムを適用します。
4.  **任意のスキーマ対応**: XHTML, MEI など、任意のスキーマ定義（名前空間やタグ定義）を注入して扱えるようにします。

## 範囲 (Scope)

- **コア (Core)**: データ構造定義 (`ResilientNode`)、パースロジック (`parse`)、およびシリアライズ (`toString`)。任意のスキーマ定義を受け入れる柔軟性。
- **CodeMirror 連携 (CodeMirror Interop)**: テキスト変更の処理。例外的な状態でのフォールバック（再パース）を行わず、不整合があれば専用の例外を送出する厳格な設計。可能な限りのカーソル位置同期。
- **ProseMirror 連携 (ProseMirror Interop)**: RST から ProseMirror ノードへの変換、および構造変更の同期。

## 影響 (Impact)

- `src/lib/ResilientSyntaxTree.ts`: この仕様に基づいて完全に実装されます。
- `src/lib/EditorController.ts`: RST を利用してエディタを同期します。
- `src/lib/sampeContent.ts` のような複雑な XML 構造を扱えるようになります。
- 整合性を検証するための厳格なテストスイートが必要になります。

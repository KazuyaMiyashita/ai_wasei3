# prosemirror-interop Specification

## Purpose
TBD - created by archiving change finalize-resilient-syntax-tree-spec. Update Purpose after archive.
## Requirements
### Requirement: スキーマベースの変換

RST は、提供されたスキーマに基づいて自身のノードを ProseMirror ノードに変換しなければならない (MUST)。

#### Scenario: 定義済みノードの変換

前提: "note" が有効なブロックノードであるスキーマがある
かつ: "Defined" ノード `<note>` を持つ RST がある
もし: `toProseMirrorDoc` が呼ばれたとき
ならば: それは `note` ノードを含む ProseMirror ドキュメントを生成する。

### Requirement: カーソル位置の同期

RST から ProseMirror ドキュメントを生成（または更新）する際、およびその逆の際、可能な限りカーソル位置（Selection）を同期させなければならない (MUST)。

#### Scenario: CodeMirror から ProseMirror への切り替え

前提: ユーザーが CodeMirror 上で特定の単語の途中にカーソルを置いている
もし: ProseMirror ビューに反映（同期）されたとき
ならば: ProseMirror 上のカーソルも、対応するテキストノードの同じ文字位置になければならない (MUST)。

### Requirement: Transaction Mapping

ProseMirror のトランザクションを RST の編集にマップし戻す際、高速かつ正確に行い、再レンダリング（全置換）を避けるよう努めなければならない (MUST)。

#### Scenario: テキスト入力の反映

前提: ProseMirror 上でテキストを入力する Transaction が発生した
もし: RST に適用されたとき
ならば: RST はテキストノードの一部だけを更新し、構造全体を作り直してはならない (MUST)。


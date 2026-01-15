# ProseMirror 連携仕様

## ADDED Requirements

### Requirement: MEI コンテンツの Atom 扱い

`<mei>` 要素は ProseMirror スキーマにおいて Atom ノード（子要素を持たないリーフノード、または不透明なノード）として扱われなければならない (MUST)。
スキーマに `mei_node` を定義し、`atom: true` を設定する。

#### Scenario: MEI 内部の保護

前提: ProseMirror 上に MEI コンテンツが表示されている
もし: ユーザーがその内部のテキストを編集しようとしたとき
ならば: 通常のテキスト編集（キャレット移動して文字入力）はできない、またはノード全体が選択される挙動となるべきである (SHOULD)
（注: Verovio 統合はスコープ外だが、XML テキストとしての表示は必要）。

### Requirement: ロック時の視覚フィードバック

ProseMirror ビューは、`EditorController` がロック状態にあるとき、視覚的なフィードバックを提供しなければならない (MUST)。

#### Scenario: 編集中断

前提: 裏で CodeMirror からの同期処理が走っている
もし: ユーザーが ProseMirror にマウスを合わせたとき
ならば: マウスカーソルが `wait` または `not-allowed` になる (MUST)。

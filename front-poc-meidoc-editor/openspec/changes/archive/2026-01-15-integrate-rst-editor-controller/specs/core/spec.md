# EditorController 統合仕様

## ADDED Requirements

### Requirement: 排他制御 (Exclusive Locking)

`EditorController` は、不整合を防ぐために厳密な排他制御を行わなければならない (MUST)。
`Idle`, `ProcessingFromCM`, `ProcessingFromPM` の状態を持ち、編集中は反対側の入力をブロックする。

#### Scenario: 同時編集の防止

前提: CodeMirror からの変更処理が完了していない
もし: ProseMirror でユーザー操作が発生したとき
ならば: その操作はブロックされるか、無効化されなければならない (MUST)
かつ: ユーザーにはカーソル変更などで操作不可であることが示されなければならない (MUST)。

### Requirement: 正確な PM->RST マッピング

システムは、ProseMirror のトランザクションを RST のテキスト編集操作に変換することを試みなければならない (MUST)。
`ReplaceStep` を RST の `edit()` 操作にマッピングし、ノード境界の座標補正（ヒューリスティック）を適用する。

#### Scenario: テキスト入力のマッピング

前提: ProseMirror 上の特定のパラグラフの途中で文字 "A" が入力された
もし: RST に反映するとき
ならば: ドキュメント全体を再パースするのではなく、RST の該当ノードの該当オフセットに "A" を挿入する操作として実行されるべきである (SHOULD)。

### Requirement: MEI 要素のプレビュー

`<mei>` 要素とその子孫は、ProseMirror 上では編集可能なツリー構造ではなく、単一のテキストブロックとして表示されなければならない (MUST)。

#### Scenario: MEI の表示

前提: ドキュメントに `<mei>...</mei>` が含まれる
もし: ProseMirror でレンダリングされたとき
ならば: XML のソースコードが `<pre>` タグ内に表示される (MUST)
かつ: その内容はスクロール可能である (SHOULD)。

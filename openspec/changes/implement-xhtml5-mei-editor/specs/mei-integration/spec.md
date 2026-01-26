# Spec: MEI Rendering and Interaction

## ADDED Requirements

### Requirement: Verovio Rendering
WYSIWYGエディタは、Verovioツールキットを使用して `<mei>` ブロックを楽譜としてレンダリングしなければならない。
-   レンダリングはテキストフロー内のブロックとして行われる。

#### Scenario: Viewing Score
`<mei>...</mei>` を含むXMLドキュメントがある場合、左ペインにはVerovioによって生成された楽譜SVGが表示される。

### Requirement: Partial Updates
システムは、特定のMEIコンテンツが変更された場合にのみVerovioを更新することで、レンダリングパフォーマンスを最適化しなければならない。
-   MEIブロック外のテキスト編集は、Verovioの再レンダリングをトリガーしてはならない。

#### Scenario: Editing Text
テキストと楽譜を含むドキュメントにおいて、ユーザーが段落内で入力を行っても、楽譜表示領域は点滅したり再レンダリングされたりしない。

### Requirement: Interactive Score Editing
レンダリングされたSVGはインタラクティブであり、ユーザーが音楽要素を選択および変更できるようにしなければならない。
-   UIを介した変更（クリック、ドラッグなど）は、基となるMEI XMLを更新し、ソースビューに同期しなければならない。

#### Scenario: Clicking a Note
レンダリングされた楽譜において、ユーザーが音符をクリックすると、システムは対応するXML要素を特定する。
（将来のスコープ：UIパネルを介した特定の属性の編集）。
# Spec: Core XHTML5+MEI Editor (改訂版)

## MODIFIED Requirements

### Requirement: Split Screen Interface with Mode Toggle
アプリケーションは、閲覧モードと編集モードを切り替え可能でなければならない。
- **閲覧モード (Read Mode)**:
    - 既存のレンダリングエンジンを使用し、高速に表示する。
    - 編集不可。
- **編集モード (Edit Mode)**:
    - 画面を左右に分割し、左にWYSIWYG、右にソースコードを表示する。
    - 左側のWYSIWYGエディタは、**閲覧モードと視覚的に同一（または極めて近い）**でなければならない。

#### Scenario: Switching Modes
ユーザーがヘッダーの「編集」ボタンを押すと、表示が閲覧モードから分割エディタに切り替わる。見た目のレイアウトは維持され、カーソルが表示されるようになる。

### Requirement: Reliable MEI Rendering
編集モードにおいても、MEI部分は正しくレンダリングされなければならない。
- 元のXMLに記述されたMEI構造（`meiHead`, `music`, `body` 等）を完全に維持して Verovio に渡すこと。
- 部分的なタグの再構築による構造破壊を防ぐこと。

### Requirement: Workspace Synchronization
ワークスペースで別のファイルを選択した際、編集モードであっても、エディタの内容（左のWYSIWYGと右のソースコード両方）が即座に新しいファイルの内容に更新されなければならない。

## ADDED Requirements

### Requirement: Exact Source Preservation
ASTパーサーは、タグの属性順序、空白、クォートの種類などを可能な限り維持する（あるいは、ユーザーが意図しない変更を最小限に抑える）べきである。
- **実装方針**: 位置情報ベースの抽出により、変更されていない部分のソースコードをバイトレベルで維持する。

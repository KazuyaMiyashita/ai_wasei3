# debug-view Specification

## Purpose
TBD - created by archiving change integrate-rst-editor-controller. Update Purpose after archive.
## Requirements
### Requirement: 編集イベントログ

`EditorControllerDebugView` は、双方のエディタ間の相互作用の履歴を表示しなければならない (MUST)。
各ログにはタイムスタンプ、ソース（CM/PM/System）、タイプ、詳細が含まれる。

#### Scenario: ログの閲覧

前提: ユーザーが CodeMirror と ProseMirror で編集を行った
もし: デバッグビューのログタブを開いたとき
ならば: 「CodeMirrorから受信」「RSTへ適用」「ProseMirrorへ同期」といった一連のイベントが時系列で表示される (MUST)
かつ: 各ログエントリには、タイムスタンプ、ソース、アクションの詳細が含まれる (MUST)。

### Requirement: RST の可視化

`EditorControllerDebugView` は、`ResilientSyntaxTree` の視覚的表現を提供しなければならない (MUST)。
RST タブにおいて、ノードの種類、タグ名、ID、内容をツリー形式で表示する。


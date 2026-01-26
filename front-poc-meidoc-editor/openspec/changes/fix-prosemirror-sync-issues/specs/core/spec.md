# core Specification Delta

## MODIFIED Requirements

### Requirement: 正確な PM->RST マッピング

システムは、ProseMirror のトランザクションを RST のテキスト編集操作に変換することを試みなければならない (MUST)。
**[UPDATE]**: テキストの挿入・削除だけでなく、インラインスタイル（マーク）の適用やノード属性の変更を、RST のタグおよび属性操作として正確に変換しなければならない (MUST)。

#### Scenario: テキスト入力のマッピング

(Existing scenario)

#### Scenario: 構造変更のマッピング

前提: ProseMirror 上で複数のパラグラフを選択して削除した
もし: RST に反映するとき
ならば: RST は該当する複数のノードを削除する単一または最小限の編集操作として実行されるべきである (SHOULD)。

# Design Revision: RST to CodeMirror Differential Sync

| Field      | Value                                                                                                     |
| ---------- | --------------------------------------------------------------------------------------------------------- |
| **Status** | Proposed                                                                                                  |
| **Target** | ResilientSyntaxTree & EditorController                                                                    |
| **Goal**   | Achieve differential updates from RST to CodeMirror to preserve cursor positions and improve performance. |

## 1. 問題の所在

現在の実装では、ProseMirror (PM) からの変更を RST に適用した後、CodeMirror (CM) への同期を `rst.toString()` を用いた全置換で行っています。

```typescript
// Current Implementation in EditorController.ts
syncCodeMirrorFromRST() {
  const newText = this.rst.toString(); // 全文取得
  // ...
  this.codeMirrorView.dispatch({
    changes: { from: 0, to: currentText.length, insert: newText } // 全置換
  });
}
```

**理由**: `rst.edit()` メソッドが「適用された実際の変更内容（Diff）」を返さないため、`EditorController` は RST の内部で何が起きたか（どの範囲が書き換わったか）を把握できないためです。RST は構文維持のために、リクエストされた範囲外の修正（エスカレーション）を行う可能性があるため、リクエスト内容をそのまま CM への変更として使うことは危険です。

## 2. 解決策: RST 変更追跡 (Change Tracking)

`ResilientSyntaxTree` に、適用された変更操作を正確に報告する機能を実装します。

### A. `RSTChange` インターフェースの定義

```typescript
export interface RSTChange {
  from: number;
  to: number;
  insert: string;
  // Metadata for debugging
  reason?: "UserEdit" | "Escalation" | "AutoClose";
}
```

### B. `rst.edit()` のシグネチャ変更

`void` ではなく、適用された変更のリストを返すように変更します。

```typescript
// Before
edit(changes: { from: number; to: number; insert: string }[]): void

// After
edit(changes: { from: number; to: number; insert: string }[]): RSTChange[]
```

**動作仕様**:

1.  単純なテキスト置換の場合、入力と同じ内容を返します。
2.  **エスカレーション発生時**:
    - 例えば `<p>Start` が入力され、親要素まで巻き込んで再パースされた場合、再パースによって置換された「全範囲」と「新しい生成文字列」を返します。
    - これにより、CM 側はその範囲だけを書き換えれば済みます。

### C. EditorController での同期ロジック修正

返却された `RSTChange[]` を CodeMirror の `ChangeSpec` に変換して適用します。

```typescript
// New sync logic
const appliedChanges = this.rst.edit(pmChanges);

if (this.codeMirrorView) {
  const cmChanges = appliedChanges.map((c) => ({
    from: c.from,
    to: c.to,
    insert: c.insert,
  }));

  this.codeMirrorView.dispatch({
    changes: cmChanges,
    // filter: false // 必要に応じて
  });
}
```

## 3. 実装ステップ

1.  **`src/lib/ResilientSyntaxTree.ts` の改修**:
    - `applySingleChange` メソッドが、実際に適用した変更範囲と結果を記録するように修正する。
    - エスカレーション（再パース）時、`parent.children.splice(...)` でノードを入れ替えているが、この影響範囲（文字数ベース）を計算し、`RSTChange` オブジェクトを生成する。

2.  **`src/lib/EditorController.ts` の改修**:
    - `handleProseMirrorTransaction` 内で `rst.edit` の戻り値を受け取る。
    - `syncCodeMirrorFromRST` メソッド（またはそれに代わる処理）で、全置換ではなく差分更新を行う。

## 4. 期待される効果

- **カーソル維持**: CM 側で編集していない部分のカーソル位置が維持されます（CM のデフォルト動作として、変更箇所以外のカーソルは追従する）。
- **パフォーマンス向上**: 文字列全体の生成と再レンダリングを防ぎ、大規模ドキュメントでの動作が軽量化されます。
- **堅牢性**: RST が内部で行った「構文修復」も正確に CM に反映されます。

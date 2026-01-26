# spec of ResilientSyntaxTree

XMLとして壊れている可能性がある文字列

## 基本

- 与えられた文字列を元に、 ResilientSyntaxTree を構築すること
- ResilientSyntaxTreeを元に文字列に変換すること。
- 文字列 -> ResilientSyntaxTree -> 文字列 のラウンドトリップで文字列の内容に変化がないこと
- ResilientSyntaxTree の中に Error が含まれているかどうかを検証すること

## CodeMirror との連携

- 現在の ResilientSyntaxTree に対し、 from と to の間の文字列を inserted に変換する処理を行う
- 上記は、文字列に対して変換した処理と、ResilientSyntaxTree上で変換して文字列にしたものが合致しなければならない
- 上記の変換に対して、ツリー上では何が変更されたかを認識できる

TODO

### TODO list

- CodeMirror の iterChanges は次のシグネチャである。上記は不完全かもしれないので要調査
  `iterChanges(f: (fromA: number, toA: number, fromB: number, toB: number, inserted: Text) => void, individual?: boolean): void`

## ProseMirror との連携

TODO

---

ご提示いただいた仕様の骨子は非常に的確です。特に「CodeMirrorの変更との整合性」はエディタ開発の最大の難所です。

ご指摘の `iterChanges` の調査を含め、**ProseMirror連携** の仕様を埋める形で整理・具体化しました。

---

# Specification of ResilientSyntaxTree (RST)

## 1. 基本仕様 (Base)

RSTは、文字列（CodeMirror）と構造（ProseMirror）の中間に位置する「真実の源（Source of Truth）」としての性質を持つ。

- **構築 (Parsing)**
- `parse(rawString: string, definition: SyntaxDefinition): RST`
- 与えられた文字列を寛容（Lenient）にパースし、エラーや未定義タグを含めてツリー化すること。

- **シリアライズ (Stringification)**
- `toString(): string`
- ツリー構造を文字列に変換すること。

- **可逆性 (Lossless Round-trip)**
- `RST.parse(str).toString() === str` が常に成立すること。
- 空白（スペース、タブ、改行）、属性の引用符の種類（`'` vs `"`）、自己終了タグのスタイル（`<br>` vs `<br />`）などが保存されること。

- **検証 (Validation)**
- `hasErrors(): boolean`
- ツリー内に `Error` タイプのノードが存在するか検証できること。
- `getErrors(): ResilientNode[]` でエラーノードのリストを取得できること。

## 2. CodeMirror (Text) との連携

CodeMirror は「文字列の差分（Changeset）」として変更を管理する。RSTはこの差分を効率的に取り込み、ツリー構造を更新する必要がある。

### `iterChanges` の仕様と対応

CodeMirror の `update.changes.iterChanges` は、複数の変更を統合して以下の形式でコールバックを呼び出す。

```typescript
(fromA: number, toA: number, fromB: number, toB: number, inserted: Text) => void

```

- `fromA`, `toA`: **変更前**のドキュメントにおける開始位置・終了位置（削除される範囲）。
- `fromB`, `toB`: **変更後**のドキュメントにおける開始位置・終了位置。
- `inserted`: 挿入されるテキスト。

### RST への要求仕様

- **編集操作の適用 (Edit Operation)**
- `edit(changes: { from: number, to: number, insert: string }[]): void`
- CodeMirror から受け取った変更範囲（`fromA`, `toA`）と挿入テキスト（`inserted`）を元に、ツリーの一部を更新すること。
- _Note:_ パフォーマンスのため、全パースではなく「影響を受ける最小の親ノード」を特定し、その部分だけ再パースする（Incremental Parsing）ことが望ましい。

- **整合性の保証 (Consistency)**
- 以下の等式が成立すること。

```typescript
const str = "original";
const rst = RST.parse(str);

// 文字列操作
const newStr = str.slice(0, from) + inserted + str.slice(to);

// ツリー操作
rst.edit([{ from, to, insert: inserted }]);

// 検証
rst.toString() === newStr;
```

- **差分追跡 (Tree Diff Tracking)**
- `edit` 実行時に、どのノードが「追加」「削除」「更新」されたかのイベント、またはダーティなノードのリストを取得できること（ProseMirrorへの通知用）。

## 3. ProseMirror (Structure) との連携 (TODO Draft)

ProseMirror は「Schema に従った Node のツリー」として管理する。RST はこれを「正しく変換」し、かつ「RST が持つ付加情報（空白など）を維持」しなければならない。

### RST -> ProseMirror (Mapping)

- **ノード変換 (Conversion)**
- `toProseMirrorDoc(schema: Schema): Node`
- RST のノードタイプに応じて、適切な PM Node にマッピングすること。
- `Defined` → Schema で定義された対応する Node (例: `<note>` → `note node`)
- `Text` → Text Node (可能な限り `marks` を維持しない素のテキスト)
- `Foreign` / `Error` → **Inline Atom Node** (編集不可の塊) として変換し、内部に元の XML 文字列を保持させること。

- **装飾の適用 (Validation Marking)**
- `Error` ノードに対応する PM Node には、エラーであることを示す Decoration（赤背景など）を付与するための情報を生成できること。

### ProseMirror -> RST (Reverse Mapping)

ProseMirror 上での編集（Transaction）を RST に書き戻す処理。これが最も難易度が高い。

- **Transaction の解釈**
- PM の `ReplaceStep` などを受け取り、RST 上の対応するノードを特定できること。
- _課題:_ PM は「タグの文字数」を無視したオフセット（Node Index）を使うため、RST が持つ「CodeMirror 座標（文字インデックス）」との相互変換テーブル（Mapping）が必要となる。

- **構造変更の反映**
- PM で `Defined Node`（例: `<note>`）が追加された場合、RST 上でも適切な XML 文字列（例: `<note></note>`）としてノードを生成・挿入すること。

- **Atom ノードの保護**
- PM 上で `Foreign` や `Error` ノード（Atom）が移動された場合、中身の XML 文字列を変更せずに RST 上で移動すること。

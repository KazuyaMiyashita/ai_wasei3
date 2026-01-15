# core Specification

## Purpose
TBD - created by archiving change finalize-resilient-syntax-tree-spec. Update Purpose after archive.
## Requirements
### Requirement: 任意のスキーマとXML構造への対応

RST は、XHTML や MEI などを含む任意の XML ベースの構造を、注入されたスキーマ定義に従って扱わなければならない (MUST)。

#### Scenario: 複合ドキュメントのパース

前提: XHTML の中に `<mei>` タグや `<scoreDef>` タグが含まれる `sampleContent.ts` のようなドキュメントがある
かつ: これらを定義済みタグとして扱うスキーマ定義が注入されている
もし: パースされたとき
ならば: `<mei>` やその子要素は正しく "Defined" ノードとしてツリー化される
かつ: 入れ子構造が正しく維持される。

### Requirement: ノード分類

RST は、下流の振る舞いを決定するために、入力文字列のすべての部分を 4 つのノードタイプのいずれかに分類しなければならない (MUST)。

#### Scenario: 定義済みタグの分類

前提: `<section>` が定義されたタグである
もし: `<section>` を含む文字列をパースしたとき
ならば: それは "Defined" ノードとして分類される。

#### Scenario: 未知のタグの分類

前提: `<unknown>` が定義されていないタグである
もし: `<unknown>` を含む文字列をパースしたとき
ならば: それは "Foreign" ノードとして分類される。

### Requirement: 完全な可逆性 (Lossless Round-trip)

RST は、そのノードから元の入力文字列を正確に再構築でき、すべての文字を保持しなければならない (MUST)。

#### Scenario: 複雑なフォーマットの保持

前提: `sampleContent.ts` のような、深いインデント、属性、改行を含む XML 文字列がある
もし: パースして `toString()` を呼び出したとき
ならば: 元の文字列と 1 文字も違わずに復元されなければならない (MUST)。

### Requirement: 寛容なパース (Lenient Parsing)

パーサーは不正な構文に対して例外を投げてはならず (MUST NOT)、ツリー内で表現しなければならない (MUST)。
ただし、RST自体の構築ロジックの不整合に対しては例外を投げなければならない (MUST)。

#### Scenario: 属性値の引用符忘れ

前提: `<div class=container>` のような、引用符のない属性値を持つ文字列がある
もし: パースされたとき
ならば: エラーで停止せず、可能な限り属性として解釈するか、エラーノードとして構造を維持する
かつ: `toString()` で `<div class=container>` が復元される。


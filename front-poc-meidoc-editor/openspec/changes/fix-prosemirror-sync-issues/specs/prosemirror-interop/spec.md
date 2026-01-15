# prosemirror-interop Specification Delta

## MODIFIED Requirements

### Requirement: スキーマベースの変換

RST は、提供されたスキーマに基づいて自身のノードを ProseMirror ノードに変換しなければならない (MUST)。
**[UPDATE]**: また、XML の整形用空白（インデント等）を検知し、WYSIWYG 編集に不要な場合は除外しなければならない (MUST)。ただし、RST の可逆性は維持すること。

#### Scenario: 定義済みノードの変換

(Existing scenario)

#### Scenario: 整形用空白の除外

前提: `<section>

  <p>Text</p>
</section>` という構造の RST がある
もし: `toProseMirrorDoc` が呼ばれたとき
ならば: 生成された ProseMirror ドキュメントには `\n  ` というテキストノードが含まれてはならない (MUST)。

#### Scenario: テキストブロック内の整形用空白の正規化

前提: `<p>`, `<h1>`, `<li>` などのテキストを含むブロック要素内で、以下のようにソースコード上でインデントされている場合

```xml
<block>
  Line 1
  Line 2
</block>
```

もし: ProseMirror に変換されたとき
ならば: ブロック内のテキストは、ソースコードの改行やインデントがそのまま表示されるのではなく、標準的な HTML レンダリングのように正規化（"Line 1 Line 2" など）されて表示されるべきである (SHOULD)。
ただし: `<pre>` や `<mei>` のような、空白が意味を持つ要素（Atom/Code）はこの限りではない。
かつ: RST 上では元のインデント構造が維持されなければならない (MUST)。

### Requirement: Transaction Mapping

ProseMirror のトランザクションを RST の編集にマップし戻す際、高速かつ正確に行い、再レンダリング（全置換）を避けるよう努めなければならない (MUST)。
**[UPDATE]**: `ReplaceStep` だけでなく、`AddMarkStep`, `RemoveMarkStep`, `AttrStep` をサポートし、構造的な変更やスタイルの変更を正確に捉えなければならない (MUST)。

#### Scenario: テキスト入力の反映

(Existing scenario)

#### Scenario: スタイルの変更（マーク）の反映

前提: ProseMirror 上で特定のテキストを選択し「太字」を適用した (`AddMarkStep`)
もし: RST に同期されたとき
ならば: RST の該当箇所が `<b>` または `<strong>` タグで囲まれるように更新されなければならない (MUST)。

#### Scenario: 属性変更の反映

前提: ProseMirror 上で `heading` ノードのレベルを変更した (`AttrStep`)
もし: RST に同期されたとき
ならば: RST のタグ名が `h1` から `h2` などに適切に変更されなければならない (MUST)。

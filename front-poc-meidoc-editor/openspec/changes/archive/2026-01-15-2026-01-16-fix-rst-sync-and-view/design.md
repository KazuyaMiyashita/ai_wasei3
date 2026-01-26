# Design: 完全な差分同期とアーキテクチャ

## 1. Yjs実現可能性調査 (Feasibility Study)

アーキテクチャを確定する前に、Yjsを調査します。

- **仮説**: `Y.XmlFragment`は`ResilientSyntaxTree`を代替、あるいは背後で支えることができるかもしれない。
- **制約**: RSTは不正な形式のXMLを許容するが、YjsのXML型は構造を強制する。
- **アプローチ**: RSTノードをY.Map/Y.Textのカスタム構造にマッピングできるか？ あるいは、Y.Textを信頼できる唯一の情報源（Source of Truth）とし、RSTを派生的なビューとして維持できるか？
- **決定基準**: もしYjsが「Resilience」（壊れたXMLのパース能力）を犠牲にすることなく同期を簡素化できるなら採用する。そうでなければ、カスタムの差分同期実装を進める。

_(このデザインの残りの部分は、カスタム差分同期を前提としており、Yjsが採用された場合は改訂されます。)_

## 2. RSTの変更追跡と差分検出 (Diffing)

`ResilientSyntaxTree`は編集を受け入れるだけでなく、CodeMirrorとProseMirrorの両方に正確な変更を通知しなければなりません。

### データ構造

```typescript
export interface RSTChange {
  from: number; // RST絶対オフセット開始位置
  to: number; // RST絶対オフセット終了位置（変更前）
  insert: string; // 新しいコンテンツ
  affectedNodes: string[]; // 影響を受けた/再パースされたノードのID
}
```

### ロジック

1.  **エスカレーション差分**: ノードが再パースされる際、古いノード構造と新しいものを比較します。
    - 構造が同一（テキストコンテンツのみ変更）の場合、単純なテキスト変更を通知します。
    - 構造が変更された場合（新しいタグ、削除されたタグ）、親コンテナの範囲を通知します。
2.  **通知**: `EditorController`はポーリングする代わりに`rst.on('change', (changes) => ...)`で変更をリッスンします。

## 3. RST -> ProseMirror 差分同期

`tr.replaceWith(0, doc.size, newDoc)`の代わりに、必要なステップのみを適用するトランザクションを構築します。

### アルゴリズム: `syncProseMirrorIncremental(changes: RSTChange[])`

**パフォーマンス要件**: このアルゴリズムは、ユーザーの入力に対して即座に応答するため、**極めて高速に動作する必要があります**。特に、RSTオフセットからProseMirrorドキュメント位置への変換はボトルネックになりやすいため、計算量を最小限に抑える実装が必須です。

1.  **変更の反復**: 各`RSTChange`に対して処理を行います。
2.  **位置のマッピング (高効率)**: `idMap`を使用して`change.from`（RSTオフセット）をProseMirrorの位置に変換します。
    - RSTノードIDからPMノードの位置をO(1)またはO(log N)で検索できるよう、**事前にIDとPMノード位置のマッピングをキャッシュ**しておく必要があります。同期処理の開始時に一度だけこのキャッシュを構築し、トランザクションのステップによってインクリメンタルに更新することで、ループ内での高コストなドキュメント走査（`state.doc.descendants`）を避けます。
3.  **置換の計算**:
    - TextBlock内のテキスト更新の場合: `tr.replaceWith(pmPos, pmPos + oldLen, schema.text(newText))`
    - 構造的な変更の場合: PM内の親コンテナを見つけ、RSTの対応する新しいサブツリーからシリアライズされた新しいコンテンツでその内容を置き換えます。
4.  **ディスパッチ**: すべてのステップを1つのトランザクションで適用し、履歴と選択範囲を保持します。

## 4. ProseMirror -> RST マッピング (堅牢性)

厳密なID要件を強制することで「マッピングの複雑さ」を解決します。

1.  **IDの永続性**: PM内の意味のあるすべてのノード（Paragraph, Heading, Custom XML Block）は`data-rst-id`を持つ**必要があります**。
2.  **テキストノードの処理**:
    - PMはテキストノードをマージします。RSTは`Text(A) -> Comment -> Text(B)`のように分割されている可能性があります。
    - PMがテキストノードのオフセット`X`にテキストを挿入した場合、それがどのRSTノードに対応するかを見つけなければなりません。
    - **ロジック**: 対象要素のRST子要素の累積テキスト長を計算し、分割点を見つけます。
    - **空白**: PMが無視するかもしれないがRSTが保持する「不可視の」空白を処理します。

## 5. ビューのレンダリング

- **Bodyのみ**: `DocumentViewer`と`DocumentEditor`はRSTから`bodyNode`を受け取ります。
- **インクリメンタルReact**: `dangerouslySetInnerHTML`の代わりに、`DocumentViewer`は`RSTNode`ツリーをReactコンポーネント（`<RSTNodeView node={node} />`）にマッピングします。
  - `node.id`と`node.version`（またはコンテンツハッシュ）に基づいてコンポーネントを`memo`化します。
  - これにより、Reactは変更されたサブツリーのみを再レンダリングできます。

## 6. MEIとカスタム要素の処理 (NodeView)

- **MEINodeView**: 将来のVerovio統合を見据え、`<mei>`要素のレンダリングにはProseMirrorの`NodeView`を実装します。
  - **責務**: この`NodeView`は、`mei`ノードのレンダリング方法を完全にカプセル化します。
  - **初期実装**: 今回のスコープでは、`NodeView`は`<pre>`タグを生成し、その中に`rawContent`属性を表示します。
  - **将来性**: Verovioを導入する際は、この`NodeView`の内部ロジックをVerovioレンダラを呼び出すように変更するだけで済み、スキーマや他のコンポーネントへの影響を最小限に抑えます。
- **スキーマ**: `mei_node`の定義に`toDOM`の代わりに`nodeViews`プロパティを指定するよう変更します。

## 7. メニューとスキーマ

- **スキーマ**: `menu`項目（例：「段落を挿入」）が正しい`attrs`を持つノードを生成するようにします。
- **空白ノード**: CodeMirrorはしばしばPMが折りたたむ空白/改行を必要とします。
  - **戦略**: 必要に応じてPMでCSSの`white-space: pre-wrap`を使用するか、PMビューが「正規化」されていることを受け入れます。
  - 特定の「空白テキストノード」がPMでアーティファクトとして表示される場合、それらに対してカスタムのNodeViewを使用するか、WYSIWYGビューに全く関係ない場合は`toProseMirrorDoc`の過程でフィルタリングします。

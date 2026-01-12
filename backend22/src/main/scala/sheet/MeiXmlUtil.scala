package sheet

object MeiXmlUtil {
  import scala.xml.*
  import scala.xml.transform.*
  import java.util.concurrent.atomic.AtomicLong

  private class UniqueIdGenerator {
    private val counter     = new AtomicLong(0)
    private val sessionSeed = scala.util.Random.alphanumeric.take(4).mkString.toLowerCase

    def nextId(): String = {
      val count = counter.incrementAndGet()
      s"${sessionSeed}_$count"
    }
  }

  /** すべての要素に xml:id を付与。すでにあるものは上書き
    */
  def assignIds(root: Elem): Elem = {
    val idGen = new UniqueIdGenerator
    val rule  = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem =>
          // e % idAttr は Elem を返す
          e % Attribute(Some("xml"), "id", Text(idGen.nextId()), Null)
        case other => other
      }
    }
    // 最後に asInstanceOf[Elem] で Elem 型として返す
    new RuleTransformer(rule).transform(root).head.asInstanceOf[Elem]
  }

  /** すべての要素の xml:id を削除
    */
  def removeIds(root: Node): Node = {
    val rule = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem =>
          val cleanedAttributes = e.attributes.filter {
            case a: PrefixedAttribute =>
              !(a.pre == "xml" && a.key == "id")
            case _ => true
          }

          e.copy(
            attributes = cleanedAttributes,
            child = e.child.flatMap(transform),
          )
        case other => other
      }
    }

    new RuleTransformer(rule).transform(root).head
  }

  /** XML要素から指定された属性を再帰的に削除します。
    *
    * @param root 処理対象のXMLノード。
    * @param attrName 削除したい属性名。
    * - 接頭辞がある場合: "xml:id" のように指定します。
    * - 接頭辞がない場合: "n" や "label" のように指定します。
    * @return 指定された属性が取り除かれた新しいNode。
    * * @example
    * {{{
    * // xml:id 属性を削除
    * XmlUtil.removeAttr(meiElem, "xml:id")
    * * // 接頭辞のない n 属性を削除
    * XmlUtil.removeAttr(meiElem, "n")
    * }}}
    */
  def removeAttr(root: Node, attrName: String): Node = {
    // 文字列を接頭辞(prefix)と属性名(key)に分割
    val (targetPrefix, targetKey) = attrName.split(":", 2) match {
      case Array(p, k) => (Some(p), k) // "xml:id" -> (Some("xml"), "id")
      case Array(k)    => (None, k)    // "n"      -> (None, "n")
    }

    val rule = new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem =>
          val newAttrs = e.attributes.filter {
            case a: PrefixedAttribute =>
              // 接頭辞付き属性 (例: xml:id) の判定
              !(targetPrefix.contains(a.pre) && a.key == targetKey)
            case a: UnprefixedAttribute =>
              // 接頭辞なし属性 (例: n="1") の判定
              !(targetPrefix.isEmpty && a.key == targetKey)
            case _ => true
          }

          // 属性を更新し、子要素に対しても再帰的に適用
          e.copy(
            attributes = newAttrs,
            child = e.child.flatMap(transform),
          )
        case other => other
      }
    }

    new RuleTransformer(rule).transform(root).head
  }
}

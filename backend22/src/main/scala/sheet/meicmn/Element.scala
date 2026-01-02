package sheet.meicmn

import scala.collection.immutable.SeqMap

trait Element {
  def validate(): Boolean = true
  def children: scala.List[Element]
  def attributes: SeqMap[String, String]
  def withChildren(newChildren: scala.List[Element]): Element

  /** Recursively applies a transformation rule to this element and its descendants.
    * The rule is applied bottom-up (children are transformed before the parent).
    *
    * @param rule A function to rewrite elements.
    * @return A new element tree with the transformation applied.
    */
  def transform(rule: Element => Element): Element = {
    val newChildren = this.children.map(_.transform(rule))
    val newNode     = this.withChildren(newChildren)
    rule(newNode)
  }

  /** Returns an iterator over this element and all its descendants in pre-order (depth-first).
    * This enables collection operations like foreach, map, and find on the entire subtree.
    */
  def iterator: Iterator[Element] = {
    Iterator(this) ++ children.iterator.flatMap(_.iterator)
  }

  /** Traverses the entire subtree and collects elements that match the specified partial function.
    * Useful for extracting specific nodes (e.g., Measures) from the hierarchy.
    */
  def collect[T <: Element](pf: PartialFunction[Element, T]): Iterator[T] = {
    iterator.collect(pf)
  }
}

case class Text(value: java.lang.String) extends Element {
  override def children: scala.List[Element]                           = scala.Nil
  override def attributes: SeqMap[String, String]                      = SeqMap()
  override def withChildren(newChildren: scala.List[Element]): Element = {
    if (newChildren.nonEmpty) throw new IllegalArgumentException("Text node cannot have children")
    this
  }
}

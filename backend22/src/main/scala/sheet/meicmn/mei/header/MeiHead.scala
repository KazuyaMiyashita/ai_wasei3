package sheet.meicmn.mei.header

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Supplies the descriptive and declarative metadata prefixed to every MEI-conformant text.
  *
  * Ident: meiHead
  * Module: MEI.header
  */
case class MeiHead(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Basic
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.shared.att.Labelled
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.shared.att.MeiVersion
    with _root_.sheet.meicmn.mei.shared.att.Responsibility {
  override def validate(): Boolean = {
    val allowedAttrs =
      Set("analog", "label", "meiversion", "resp", "translit", "type", "xml:base", "xml:id", "xml:lang")
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_meiHead)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object MeiHead {
  def apply(
      `analog`: java.lang.String = null,
      `label`: java.lang.String = null,
      `meiversion`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `translit`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
  )(elements: Element*): MeiHead = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`meiversion` != null) Some("meiversion" -> `meiversion`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`xml:lang` != null) Some("xml:lang" -> `xml:lang`) else None,
          )
          .flatten,
      )
    new MeiHead(elements.toList, attributes)
  }
}

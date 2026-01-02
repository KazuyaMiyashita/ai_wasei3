package sheet.meicmn.mei.header

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Describes a folded sheet of paper.
  *
  * Ident: bifolium
  * Module: MEI.header
  */
case class Bifolium(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Dimensions
    with _root_.sheet.meicmn.mei.shared.att.Measurement
    with _root_.sheet.meicmn.mei.header.att.BifoliumSurfaces
    with _root_.sheet.meicmn.mei.header.model.BifoliumLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "copyof",
      "corresp",
      "follows",
      "height",
      "inner.recto",
      "inner.verso",
      "label",
      "n",
      "next",
      "outer.recto",
      "outer.verso",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "type",
      "unit",
      "width",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_bifolium)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Bifolium {
  def apply(
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `height`: java.lang.String = null,
      `inner.recto`: java.lang.String = null,
      `inner.verso`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `outer.recto`: java.lang.String = null,
      `outer.verso`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `unit`: java.lang.String = null,
      `width`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Bifolium = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`height` != null) Some("height" -> `height`) else None,
            if (`inner.recto` != null) Some("inner.recto" -> `inner.recto`) else None,
            if (`inner.verso` != null) Some("inner.verso" -> `inner.verso`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`outer.recto` != null) Some("outer.recto" -> `outer.recto`) else None,
            if (`outer.verso` != null) Some("outer.verso" -> `outer.verso`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`unit` != null) Some("unit" -> `unit`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Bifolium(elements.toList, attributes)
  }
}

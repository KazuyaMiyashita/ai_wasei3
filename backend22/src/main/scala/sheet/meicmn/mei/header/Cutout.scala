package sheet.meicmn.mei.header

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A cutout is a section of a document sheet that has been removed and is now missing.
  *
  * Ident: cutout
  * Module: MEI.header
  */
case class Cutout(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Dimensions
    with _root_.sheet.meicmn.mei.shared.att.Evidence
    with _root_.sheet.meicmn.mei.shared.att.Measurement
    with _root_.sheet.meicmn.mei.edittrans.att.Trans
    with _root_.sheet.meicmn.mei.shared.att.Xy
    with _root_.sheet.meicmn.mei.header.model.PaperModLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "cert",
      "class",
      "copyof",
      "corresp",
      "decls",
      "evidence",
      "follows",
      "hand",
      "height",
      "instant",
      "label",
      "n",
      "next",
      "precedes",
      "prev",
      "removed.by",
      "removed.from",
      "resp",
      "sameas",
      "seq",
      "state",
      "synch",
      "type",
      "unit",
      "width",
      "x",
      "xml:base",
      "xml:id",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_cutout)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Cutout {
  def apply(
      `cert`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `hand`: java.lang.String = null,
      `height`: java.lang.String = null,
      `instant`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `removed.by`: java.lang.String = null,
      `removed.from`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `seq`: java.lang.String = null,
      `state`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `unit`: java.lang.String = null,
      `width`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): Cutout = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`hand` != null) Some("hand" -> `hand`) else None,
            if (`height` != null) Some("height" -> `height`) else None,
            if (`instant` != null) Some("instant" -> `instant`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`removed.by` != null) Some("removed.by" -> `removed.by`) else None,
            if (`removed.from` != null) Some("removed.from" -> `removed.from`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`seq` != null) Some("seq" -> `seq`) else None,
            if (`state` != null) Some("state" -> `state`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`unit` != null) Some("unit" -> `unit`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
          )
          .flatten,
      )
    new Cutout(elements.toList, attributes)
  }
}

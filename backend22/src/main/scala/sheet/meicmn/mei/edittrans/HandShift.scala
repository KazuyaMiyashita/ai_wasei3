package sheet.meicmn.mei.edittrans

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Marks the beginning of a passage written in a new hand, or of a change in the scribe, writing style, ink or character of the document hand.
  *
  * Ident: handShift
  * Module: MEI.edittrans
  */
case class HandShift(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.edittrans.att.Edit
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Medium
    with _root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "cert",
      "character",
      "class",
      "copyof",
      "corresp",
      "evidence",
      "facs",
      "follows",
      "label",
      "medium",
      "n",
      "new",
      "next",
      "old",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "source",
      "synch",
      "type",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_handShift)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object HandShift {
  def apply(
      `cert`: java.lang.String = null,
      `character`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `medium`: java.lang.String = null,
      `n`: java.lang.String = null,
      `new`: java.lang.String = null,
      `next`: java.lang.String = null,
      `old`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `source`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): HandShift = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`character` != null) Some("character" -> `character`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`medium` != null) Some("medium" -> `medium`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`new` != null) Some("new" -> `new`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`old` != null) Some("old" -> `old`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`source` != null) Some("source" -> `source`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new HandShift(elements.toList, attributes)
  }
}

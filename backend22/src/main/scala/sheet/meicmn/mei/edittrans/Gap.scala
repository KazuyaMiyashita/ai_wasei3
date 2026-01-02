package sheet.meicmn.mei.edittrans

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Indicates a point where material has been omitted in a transcription, whether as part of sampling practice or for editorial reasons described in the MEI header.
  *
  * Ident: gap
  * Module: MEI.edittrans
  */
case class Gap(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.edittrans.att.Edit
    with _root_.sheet.meicmn.mei.shared.att.Extent
    with _root_.sheet.meicmn.mei.shared.att.HandIdent
    with _root_.sheet.meicmn.mei.edittrans.att.ReasonIdent
    with _root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "atleast",
      "atmost",
      "cert",
      "class",
      "confidence",
      "copyof",
      "corresp",
      "evidence",
      "extent",
      "follows",
      "hand",
      "label",
      "max",
      "min",
      "n",
      "next",
      "precedes",
      "prev",
      "reason",
      "resp",
      "sameas",
      "source",
      "synch",
      "type",
      "unit",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_gap)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Gap {
  def apply(
      `atleast`: java.lang.String = null,
      `atmost`: java.lang.String = null,
      `cert`: java.lang.String = null,
      `class`: java.lang.String = null,
      `confidence`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `extent`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `hand`: java.lang.String = null,
      `label`: java.lang.String = null,
      `max`: java.lang.String = null,
      `min`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `reason`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `source`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `unit`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Gap = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`atleast` != null) Some("atleast" -> `atleast`) else None,
            if (`atmost` != null) Some("atmost" -> `atmost`) else None,
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`confidence` != null) Some("confidence" -> `confidence`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`extent` != null) Some("extent" -> `extent`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`hand` != null) Some("hand" -> `hand`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`max` != null) Some("max" -> `max`) else None,
            if (`min` != null) Some("min" -> `min`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`reason` != null) Some("reason" -> `reason`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`source` != null) Some("source" -> `source`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`unit` != null) Some("unit" -> `unit`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Gap(elements.toList, attributes)
  }
}

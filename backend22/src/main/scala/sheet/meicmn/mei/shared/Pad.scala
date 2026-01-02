package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** An indication of extra visual space between notational elements.
  *
  * Ident: pad
  * Module: MEI.shared
  */
case class Pad(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.analytical.att.pad.Anl
    with _root_.sheet.meicmn.mei.gestural.att.pad.Ges
    with _root_.sheet.meicmn.mei.shared.att.pad.Log
    with _root_.sheet.meicmn.mei.visual.att.pad.Vis
    with _root_.sheet.meicmn.mei.shared.model.EventLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "copyof",
      "corresp",
      "follows",
      "label",
      "layer",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "staff",
      "synch",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "type",
      "when",
      "width",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_pad)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Pad {
  def apply(
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `when`: java.lang.String = null,
      `width`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Pad = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Pad(elements.toList, attributes)
  }
}

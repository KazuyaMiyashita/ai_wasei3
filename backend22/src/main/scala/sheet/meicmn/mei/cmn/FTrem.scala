package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A rapid alternation between a pair of notes (or chords or perhaps between a note and a chord) that are (usually) farther apart than a major second.
  *
  * Ident: fTrem
  * Module: MEI.cmn
  */
case class FTrem(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.cmn.att.ftrem.Log
    with _root_.sheet.meicmn.mei.visual.att.ftrem.Vis
    with _root_.sheet.meicmn.mei.gestural.att.ftrem.Ges
    with _root_.sheet.meicmn.mei.analytical.att.ftrem.Anl
    with _root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "beams",
      "beams.float",
      "class",
      "copyof",
      "corresp",
      "dots",
      "dur",
      "facs",
      "float.gap",
      "follows",
      "form",
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
      "unitdur",
      "when",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_fTrem)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object FTrem {
  def apply(
      `beams`: java.lang.String = null,
      `beams.float`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `dots`: java.lang.String = null,
      `dur`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `float.gap`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `form`: java.lang.String = null,
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
      `unitdur`: java.lang.String = null,
      `when`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): FTrem = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`beams` != null) Some("beams" -> `beams`) else None,
            if (`beams.float` != null) Some("beams.float" -> `beams.float`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`dots` != null) Some("dots" -> `dots`) else None,
            if (`dur` != null) Some("dur" -> `dur`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`float.gap` != null) Some("float.gap" -> `float.gap`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`form` != null) Some("form" -> `form`) else None,
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
            if (`unitdur` != null) Some("unitdur" -> `unitdur`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new FTrem(elements.toList, attributes)
  }
}

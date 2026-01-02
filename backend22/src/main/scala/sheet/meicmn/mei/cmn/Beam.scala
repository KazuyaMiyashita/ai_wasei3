package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A container for a series of explicitly beamed events that begins and ends entirely within a measure.
  *
  * Ident: beam
  * Module: MEI.cmn
  */
case class Beam(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.cmn.att.beam.Log
    with _root_.sheet.meicmn.mei.visual.att.beam.Vis
    with _root_.sheet.meicmn.mei.gestural.att.beam.Ges
    with _root_.sheet.meicmn.mei.analytical.att.beam.Anl
    with _root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "beam.with",
      "class",
      "color",
      "copyof",
      "corresp",
      "cue",
      "facs",
      "follows",
      "form",
      "label",
      "layer",
      "n",
      "next",
      "place",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "slash",
      "slope",
      "staff",
      "synch",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "type",
      "visible",
      "when",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_beam)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Beam {
  def apply(
      `beam.with`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `cue`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `form`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `place`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `slash`: java.lang.String = null,
      `slope`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `when`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Beam = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`beam.with` != null) Some("beam.with" -> `beam.with`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`cue` != null) Some("cue" -> `cue`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`form` != null) Some("form" -> `form`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`place` != null) Some("place" -> `place`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`slash` != null) Some("slash" -> `slash`) else None,
            if (`slope` != null) Some("slope" -> `slope`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Beam(elements.toList, attributes)
  }
}

package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A group of notes with "irregular" (sometimes called "irrational") rhythmic values, for example, three notes in the time normally occupied by two or nine in the time of five.
  *
  * Ident: tuplet
  * Module: MEI.cmn
  */
case class Tuplet(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.cmn.att.tuplet.Log
    with _root_.sheet.meicmn.mei.visual.att.tuplet.Vis
    with _root_.sheet.meicmn.mei.gestural.att.tuplet.Ges
    with _root_.sheet.meicmn.mei.analytical.att.tuplet.Anl
    with _root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "beam.with",
      "bracket.place",
      "bracket.visible",
      "class",
      "color",
      "copyof",
      "corresp",
      "dots.ges",
      "dur",
      "dur.ges",
      "dur.metrical",
      "dur.ppq",
      "dur.real",
      "dur.recip",
      "dur.visible",
      "endid",
      "facs",
      "follows",
      "label",
      "layer",
      "n",
      "next",
      "num",
      "num.format",
      "num.place",
      "num.visible",
      "numbase",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "staff",
      "startid",
      "synch",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "type",
      "when",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_tuplet)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Tuplet {
  def apply(
      `beam.with`: java.lang.String = null,
      `bracket.place`: java.lang.String = null,
      `bracket.visible`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `dots.ges`: java.lang.String = null,
      `dur`: java.lang.String = null,
      `dur.ges`: java.lang.String = null,
      `dur.metrical`: java.lang.String = null,
      `dur.ppq`: java.lang.String = null,
      `dur.real`: java.lang.String = null,
      `dur.recip`: java.lang.String = null,
      `dur.visible`: java.lang.String = null,
      `endid`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `num`: java.lang.String = null,
      `num.format`: java.lang.String = null,
      `num.place`: java.lang.String = null,
      `num.visible`: java.lang.String = null,
      `numbase`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `when`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Tuplet = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`beam.with` != null) Some("beam.with" -> `beam.with`) else None,
            if (`bracket.place` != null) Some("bracket.place" -> `bracket.place`) else None,
            if (`bracket.visible` != null) Some("bracket.visible" -> `bracket.visible`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`dots.ges` != null) Some("dots.ges" -> `dots.ges`) else None,
            if (`dur` != null) Some("dur" -> `dur`) else None,
            if (`dur.ges` != null) Some("dur.ges" -> `dur.ges`) else None,
            if (`dur.metrical` != null) Some("dur.metrical" -> `dur.metrical`) else None,
            if (`dur.ppq` != null) Some("dur.ppq" -> `dur.ppq`) else None,
            if (`dur.real` != null) Some("dur.real" -> `dur.real`) else None,
            if (`dur.recip` != null) Some("dur.recip" -> `dur.recip`) else None,
            if (`dur.visible` != null) Some("dur.visible" -> `dur.visible`) else None,
            if (`endid` != null) Some("endid" -> `endid`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`num` != null) Some("num" -> `num`) else None,
            if (`num.format` != null) Some("num.format" -> `num.format`) else None,
            if (`num.place` != null) Some("num.place" -> `num.place`) else None,
            if (`num.visible` != null) Some("num.visible" -> `num.visible`) else None,
            if (`numbase` != null) Some("numbase" -> `numbase`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Tuplet(elements.toList, attributes)
  }
}

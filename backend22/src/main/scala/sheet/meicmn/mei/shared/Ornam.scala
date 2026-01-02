package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** An element indicating an ornament that is not a mordent, turn, or trill.
  *
  * Ident: ornam
  * Module: MEI.shared
  */
case class Ornam(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.analytical.att.ornam.Anl
    with _root_.sheet.meicmn.mei.gestural.att.ornam.Ges
    with _root_.sheet.meicmn.mei.shared.att.ornam.Log
    with _root_.sheet.meicmn.mei.visual.att.ornam.Vis
    with _root_.sheet.meicmn.mei.shared.model.ControlEventLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "accidlower",
      "accidlower.ges",
      "accidupper",
      "accidupper.ges",
      "altsym",
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
      "enclose",
      "endho",
      "endid",
      "endto",
      "evaluate",
      "facs",
      "follows",
      "ho",
      "label",
      "layer",
      "n",
      "next",
      "part",
      "partstaff",
      "place",
      "plist",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "staff",
      "startho",
      "startid",
      "startto",
      "synch",
      "to",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "tstamp2",
      "tstamp2.ges",
      "tstamp2.real",
      "type",
      "vgrp",
      "vo",
      "when",
      "x",
      "xml:base",
      "xml:id",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_ornam)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Ornam {
  def apply(
      `accidlower`: java.lang.String = null,
      `accidlower.ges`: java.lang.String = null,
      `accidupper`: java.lang.String = null,
      `accidupper.ges`: java.lang.String = null,
      `altsym`: java.lang.String = null,
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
      `enclose`: java.lang.String = null,
      `endho`: java.lang.String = null,
      `endid`: java.lang.String = null,
      `endto`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `part`: java.lang.String = null,
      `partstaff`: java.lang.String = null,
      `place`: java.lang.String = null,
      `plist`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `startho`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `startto`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `to`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `tstamp2`: java.lang.String = null,
      `tstamp2.ges`: java.lang.String = null,
      `tstamp2.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vgrp`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `when`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): Ornam = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`accidlower` != null) Some("accidlower" -> `accidlower`) else None,
            if (`accidlower.ges` != null) Some("accidlower.ges" -> `accidlower.ges`) else None,
            if (`accidupper` != null) Some("accidupper" -> `accidupper`) else None,
            if (`accidupper.ges` != null) Some("accidupper.ges" -> `accidupper.ges`) else None,
            if (`altsym` != null) Some("altsym" -> `altsym`) else None,
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
            if (`enclose` != null) Some("enclose" -> `enclose`) else None,
            if (`endho` != null) Some("endho" -> `endho`) else None,
            if (`endid` != null) Some("endid" -> `endid`) else None,
            if (`endto` != null) Some("endto" -> `endto`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`part` != null) Some("part" -> `part`) else None,
            if (`partstaff` != null) Some("partstaff" -> `partstaff`) else None,
            if (`place` != null) Some("place" -> `place`) else None,
            if (`plist` != null) Some("plist" -> `plist`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`startho` != null) Some("startho" -> `startho`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`startto` != null) Some("startto" -> `startto`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`tstamp2` != null) Some("tstamp2" -> `tstamp2`) else None,
            if (`tstamp2.ges` != null) Some("tstamp2.ges" -> `tstamp2.ges`) else None,
            if (`tstamp2.real` != null) Some("tstamp2.real" -> `tstamp2.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`vgrp` != null) Some("vgrp" -> `vgrp`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
          )
          .flatten,
      )
    new Ornam(elements.toList, attributes)
  }
}

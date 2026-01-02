package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Text and symbols descriptive of tempo, mood, or style,e.g., "allarg.", "a tempo", "cantabile", "Moderato", "♩=60", "Moderato ♩ =60").
  *
  * Ident: tempo
  * Module: MEI.shared
  */
case class Tempo(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.analytical.att.tempo.Anl
    with _root_.sheet.meicmn.mei.gestural.att.tempo.Ges
    with _root_.sheet.meicmn.mei.shared.att.tempo.Log
    with _root_.sheet.meicmn.mei.visual.att.tempo.Vis
    with _root_.sheet.meicmn.mei.shared.model.ControlEventLike
    with _root_.sheet.meicmn.mei.header.model.WorkIdent {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "analog",
      "class",
      "color",
      "copyof",
      "corresp",
      "endho",
      "endid",
      "endto",
      "evaluate",
      "extender",
      "facs",
      "follows",
      "func",
      "ho",
      "label",
      "layer",
      "lendsym",
      "lendsym.size",
      "lform",
      "lsegs",
      "lstartsym",
      "lstartsym.size",
      "lwidth",
      "midi.bpm",
      "midi.mspb",
      "mm",
      "mm.dots",
      "mm.unit",
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
      "translit",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "tstamp2",
      "type",
      "vgrp",
      "vo",
      "when",
      "x",
      "xml:base",
      "xml:id",
      "xml:lang",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_tempo)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Tempo {
  def apply(
      `analog`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `endho`: java.lang.String = null,
      `endid`: java.lang.String = null,
      `endto`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `extender`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `func`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `lendsym`: java.lang.String = null,
      `lendsym.size`: java.lang.String = null,
      `lform`: java.lang.String = null,
      `lsegs`: java.lang.String = null,
      `lstartsym`: java.lang.String = null,
      `lstartsym.size`: java.lang.String = null,
      `lwidth`: java.lang.String = null,
      `midi.bpm`: java.lang.String = null,
      `midi.mspb`: java.lang.String = null,
      `mm`: java.lang.String = null,
      `mm.dots`: java.lang.String = null,
      `mm.unit`: java.lang.String = null,
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
      `translit`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `tstamp2`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vgrp`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `when`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): Tempo = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`endho` != null) Some("endho" -> `endho`) else None,
            if (`endid` != null) Some("endid" -> `endid`) else None,
            if (`endto` != null) Some("endto" -> `endto`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`extender` != null) Some("extender" -> `extender`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`func` != null) Some("func" -> `func`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`lendsym` != null) Some("lendsym" -> `lendsym`) else None,
            if (`lendsym.size` != null) Some("lendsym.size" -> `lendsym.size`) else None,
            if (`lform` != null) Some("lform" -> `lform`) else None,
            if (`lsegs` != null) Some("lsegs" -> `lsegs`) else None,
            if (`lstartsym` != null) Some("lstartsym" -> `lstartsym`) else None,
            if (`lstartsym.size` != null) Some("lstartsym.size" -> `lstartsym.size`) else None,
            if (`lwidth` != null) Some("lwidth" -> `lwidth`) else None,
            if (`midi.bpm` != null) Some("midi.bpm" -> `midi.bpm`) else None,
            if (`midi.mspb` != null) Some("midi.mspb" -> `midi.mspb`) else None,
            if (`mm` != null) Some("mm" -> `mm`) else None,
            if (`mm.dots` != null) Some("mm.dots" -> `mm.dots`) else None,
            if (`mm.unit` != null) Some("mm.unit" -> `mm.unit`) else None,
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
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`tstamp2` != null) Some("tstamp2" -> `tstamp2`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`vgrp` != null) Some("vgrp" -> `vgrp`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`xml:lang` != null) Some("xml:lang" -> `xml:lang`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
          )
          .flatten,
      )
    new Tempo(elements.toList, attributes)
  }
}

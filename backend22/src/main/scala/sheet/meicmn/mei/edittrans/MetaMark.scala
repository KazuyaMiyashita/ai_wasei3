package sheet.meicmn.mei.edittrans

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A graphical or textual statement with additional / explanatory information about the musical text. The textual consequences of this intervention are encoded independently via other means; that is, with elements such asadd,del, etc.
  *
  * Ident: metaMark
  * Module: MEI.edittrans
  */
case class MetaMark(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.edittrans.att.metamark.Log
    with _root_.sheet.meicmn.mei.visual.att.metamark.Vis
    with _root_.sheet.meicmn.mei.gestural.att.metamark.Ges
    with _root_.sheet.meicmn.mei.analytical.att.metamark.Anl
    with _root_.sheet.meicmn.mei.shared.att.Pointing
    with _root_.sheet.meicmn.mei.shared.model.ControlEventLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "cert",
      "class",
      "color",
      "copyof",
      "corresp",
      "decls",
      "dots.ges",
      "dur.ges",
      "dur.metrical",
      "dur.ppq",
      "dur.real",
      "dur.recip",
      "endid",
      "evaluate",
      "evidence",
      "facs",
      "follows",
      "function",
      "hand",
      "instant",
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
      "seq",
      "source",
      "staff",
      "startid",
      "state",
      "synch",
      "target",
      "targettype",
      "translit",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "tstamp2",
      "tstamp2.ges",
      "tstamp2.real",
      "type",
      "vgrp",
      "when",
      "xlink:actuate",
      "xlink:role",
      "xlink:show",
      "xml:base",
      "xml:id",
      "xml:lang",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_metaMark)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object MetaMark {
  def apply(
      `cert`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `dots.ges`: java.lang.String = null,
      `dur.ges`: java.lang.String = null,
      `dur.metrical`: java.lang.String = null,
      `dur.ppq`: java.lang.String = null,
      `dur.real`: java.lang.String = null,
      `dur.recip`: java.lang.String = null,
      `endid`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `function`: java.lang.String = null,
      `hand`: java.lang.String = null,
      `instant`: java.lang.String = null,
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
      `seq`: java.lang.String = null,
      `source`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `state`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `target`: java.lang.String = null,
      `targettype`: java.lang.String = null,
      `translit`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `tstamp2`: java.lang.String = null,
      `tstamp2.ges`: java.lang.String = null,
      `tstamp2.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vgrp`: java.lang.String = null,
      `when`: java.lang.String = null,
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
  )(elements: Element*): MetaMark = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`dots.ges` != null) Some("dots.ges" -> `dots.ges`) else None,
            if (`dur.ges` != null) Some("dur.ges" -> `dur.ges`) else None,
            if (`dur.metrical` != null) Some("dur.metrical" -> `dur.metrical`) else None,
            if (`dur.ppq` != null) Some("dur.ppq" -> `dur.ppq`) else None,
            if (`dur.real` != null) Some("dur.real" -> `dur.real`) else None,
            if (`dur.recip` != null) Some("dur.recip" -> `dur.recip`) else None,
            if (`endid` != null) Some("endid" -> `endid`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`function` != null) Some("function" -> `function`) else None,
            if (`hand` != null) Some("hand" -> `hand`) else None,
            if (`instant` != null) Some("instant" -> `instant`) else None,
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
            if (`seq` != null) Some("seq" -> `seq`) else None,
            if (`source` != null) Some("source" -> `source`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`state` != null) Some("state" -> `state`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`target` != null) Some("target" -> `target`) else None,
            if (`targettype` != null) Some("targettype" -> `targettype`) else None,
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`tstamp2` != null) Some("tstamp2" -> `tstamp2`) else None,
            if (`tstamp2.ges` != null) Some("tstamp2.ges" -> `tstamp2.ges`) else None,
            if (`tstamp2.real` != null) Some("tstamp2.real" -> `tstamp2.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`vgrp` != null) Some("vgrp" -> `vgrp`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`xlink:actuate` != null) Some("xlink:actuate" -> `xlink:actuate`) else None,
            if (`xlink:role` != null) Some("xlink:role" -> `xlink:role`) else None,
            if (`xlink:show` != null) Some("xlink:show" -> `xlink:show`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`xml:lang` != null) Some("xml:lang" -> `xml:lang`) else None,
          )
          .flatten,
      )
    new MetaMark(elements.toList, attributes)
  }
}

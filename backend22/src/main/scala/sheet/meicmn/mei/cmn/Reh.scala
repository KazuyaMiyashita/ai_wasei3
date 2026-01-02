package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** In an orchestral score and its corresponding parts, a mark indicating a convenient point from which to resume rehearsal after a break.
  *
  * Ident: reh
  * Module: MEI.cmn
  */
case class Reh(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.cmn.att.reh.Log
    with _root_.sheet.meicmn.mei.visual.att.reh.Vis
    with _root_.sheet.meicmn.mei.gestural.att.reh.Ges
    with _root_.sheet.meicmn.mei.analytical.att.reh.Anl
    with _root_.sheet.meicmn.mei.cmn.model.controleventlike.Cmn {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "color",
      "copyof",
      "corresp",
      "facs",
      "follows",
      "fontfam",
      "fontname",
      "fontsize",
      "fontstyle",
      "fontweight",
      "ho",
      "label",
      "letterspacing",
      "lineheight",
      "n",
      "next",
      "part",
      "partstaff",
      "place",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "staff",
      "startid",
      "synch",
      "to",
      "translit",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "type",
      "vo",
      "when",
      "x",
      "xml:base",
      "xml:id",
      "xml:lang",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_reh)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Reh {
  def apply(
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `fontfam`: java.lang.String = null,
      `fontname`: java.lang.String = null,
      `fontsize`: java.lang.String = null,
      `fontstyle`: java.lang.String = null,
      `fontweight`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `letterspacing`: java.lang.String = null,
      `lineheight`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `part`: java.lang.String = null,
      `partstaff`: java.lang.String = null,
      `place`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `to`: java.lang.String = null,
      `translit`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `when`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): Reh = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`fontfam` != null) Some("fontfam" -> `fontfam`) else None,
            if (`fontname` != null) Some("fontname" -> `fontname`) else None,
            if (`fontsize` != null) Some("fontsize" -> `fontsize`) else None,
            if (`fontstyle` != null) Some("fontstyle" -> `fontstyle`) else None,
            if (`fontweight` != null) Some("fontweight" -> `fontweight`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`letterspacing` != null) Some("letterspacing" -> `letterspacing`) else None,
            if (`lineheight` != null) Some("lineheight" -> `lineheight`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`part` != null) Some("part" -> `part`) else None,
            if (`partstaff` != null) Some("partstaff" -> `partstaff`) else None,
            if (`place` != null) Some("place" -> `place`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
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
    new Reh(elements.toList, attributes)
  }
}

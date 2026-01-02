package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Multiple full measure rests compressed into a single bar, frequently found in performer parts.
  *
  * Ident: multiRest
  * Module: MEI.cmn
  */
case class MultiRest(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.cmn.att.multirest.Log
    with _root_.sheet.meicmn.mei.visual.att.multirest.Vis
    with _root_.sheet.meicmn.mei.gestural.att.multirest.Ges
    with _root_.sheet.meicmn.mei.analytical.att.multirest.Anl
    with _root_.sheet.meicmn.mei.cmn.model.eventlike.MeasureFilling {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "altsym",
      "block",
      "class",
      "color",
      "copyof",
      "corresp",
      "dots.ges",
      "dur.ges",
      "dur.metrical",
      "dur.ppq",
      "dur.real",
      "dur.recip",
      "facs",
      "follows",
      "fontfam",
      "fontname",
      "fontsize",
      "fontstyle",
      "fontweight",
      "glyph.auth",
      "glyph.name",
      "glyph.num",
      "glyph.uri",
      "label",
      "layer",
      "letterspacing",
      "lineheight",
      "loc",
      "n",
      "next",
      "num",
      "num.place",
      "num.visible",
      "oloc",
      "ploc",
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
      "visible",
      "when",
      "width",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_multiRest)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object MultiRest {
  def apply(
      `altsym`: java.lang.String = null,
      `block`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `dots.ges`: java.lang.String = null,
      `dur.ges`: java.lang.String = null,
      `dur.metrical`: java.lang.String = null,
      `dur.ppq`: java.lang.String = null,
      `dur.real`: java.lang.String = null,
      `dur.recip`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `fontfam`: java.lang.String = null,
      `fontname`: java.lang.String = null,
      `fontsize`: java.lang.String = null,
      `fontstyle`: java.lang.String = null,
      `fontweight`: java.lang.String = null,
      `glyph.auth`: java.lang.String = null,
      `glyph.name`: java.lang.String = null,
      `glyph.num`: java.lang.String = null,
      `glyph.uri`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `letterspacing`: java.lang.String = null,
      `lineheight`: java.lang.String = null,
      `loc`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `num`: java.lang.String = null,
      `num.place`: java.lang.String = null,
      `num.visible`: java.lang.String = null,
      `oloc`: java.lang.String = null,
      `ploc`: java.lang.String = null,
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
      `visible`: java.lang.String = null,
      `when`: java.lang.String = null,
      `width`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): MultiRest = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`altsym` != null) Some("altsym" -> `altsym`) else None,
            if (`block` != null) Some("block" -> `block`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`dots.ges` != null) Some("dots.ges" -> `dots.ges`) else None,
            if (`dur.ges` != null) Some("dur.ges" -> `dur.ges`) else None,
            if (`dur.metrical` != null) Some("dur.metrical" -> `dur.metrical`) else None,
            if (`dur.ppq` != null) Some("dur.ppq" -> `dur.ppq`) else None,
            if (`dur.real` != null) Some("dur.real" -> `dur.real`) else None,
            if (`dur.recip` != null) Some("dur.recip" -> `dur.recip`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`fontfam` != null) Some("fontfam" -> `fontfam`) else None,
            if (`fontname` != null) Some("fontname" -> `fontname`) else None,
            if (`fontsize` != null) Some("fontsize" -> `fontsize`) else None,
            if (`fontstyle` != null) Some("fontstyle" -> `fontstyle`) else None,
            if (`fontweight` != null) Some("fontweight" -> `fontweight`) else None,
            if (`glyph.auth` != null) Some("glyph.auth" -> `glyph.auth`) else None,
            if (`glyph.name` != null) Some("glyph.name" -> `glyph.name`) else None,
            if (`glyph.num` != null) Some("glyph.num" -> `glyph.num`) else None,
            if (`glyph.uri` != null) Some("glyph.uri" -> `glyph.uri`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`letterspacing` != null) Some("letterspacing" -> `letterspacing`) else None,
            if (`lineheight` != null) Some("lineheight" -> `lineheight`) else None,
            if (`loc` != null) Some("loc" -> `loc`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`num` != null) Some("num" -> `num`) else None,
            if (`num.place` != null) Some("num.place" -> `num.place`) else None,
            if (`num.visible` != null) Some("num.visible" -> `num.visible`) else None,
            if (`oloc` != null) Some("oloc" -> `oloc`) else None,
            if (`ploc` != null) Some("ploc" -> `ploc`) else None,
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
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new MultiRest(elements.toList, attributes)
  }
}

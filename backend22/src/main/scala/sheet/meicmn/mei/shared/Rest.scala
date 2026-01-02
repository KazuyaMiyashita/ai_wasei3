package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A non-sounding event found in the source being transcribed.
  *
  * Ident: rest
  * Module: MEI.shared
  */
case class Rest(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.analytical.att.rest.Anl
    with _root_.sheet.meicmn.mei.gestural.att.rest.Ges
    with _root_.sheet.meicmn.mei.shared.att.rest.Log
    with _root_.sheet.meicmn.mei.visual.att.rest.Vis
    with _root_.sheet.meicmn.mei.shared.model.EventLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "altsym",
      "beam",
      "breaksec",
      "class",
      "color",
      "copyof",
      "corresp",
      "cue",
      "dots",
      "dots.ges",
      "dur",
      "dur.ges",
      "dur.metrical",
      "dur.ppq",
      "dur.real",
      "dur.recip",
      "enclose",
      "facs",
      "fermata",
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
      "ho",
      "label",
      "layer",
      "letterspacing",
      "lineheight",
      "loc",
      "n",
      "next",
      "oloc",
      "ploc",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "staff",
      "synch",
      "to",
      "tstamp",
      "tstamp.ges",
      "tstamp.real",
      "tuplet",
      "type",
      "vo",
      "when",
      "x",
      "xml:base",
      "xml:id",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_rest)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Rest {
  def apply(
      `altsym`: java.lang.String = null,
      `beam`: java.lang.String = null,
      `breaksec`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `cue`: java.lang.String = null,
      `dots`: java.lang.String = null,
      `dots.ges`: java.lang.String = null,
      `dur`: java.lang.String = null,
      `dur.ges`: java.lang.String = null,
      `dur.metrical`: java.lang.String = null,
      `dur.ppq`: java.lang.String = null,
      `dur.real`: java.lang.String = null,
      `dur.recip`: java.lang.String = null,
      `enclose`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `fermata`: java.lang.String = null,
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
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
      `letterspacing`: java.lang.String = null,
      `lineheight`: java.lang.String = null,
      `loc`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `oloc`: java.lang.String = null,
      `ploc`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `staff`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `to`: java.lang.String = null,
      `tstamp`: java.lang.String = null,
      `tstamp.ges`: java.lang.String = null,
      `tstamp.real`: java.lang.String = null,
      `tuplet`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `when`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): Rest = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`altsym` != null) Some("altsym" -> `altsym`) else None,
            if (`beam` != null) Some("beam" -> `beam`) else None,
            if (`breaksec` != null) Some("breaksec" -> `breaksec`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`cue` != null) Some("cue" -> `cue`) else None,
            if (`dots` != null) Some("dots" -> `dots`) else None,
            if (`dots.ges` != null) Some("dots.ges" -> `dots.ges`) else None,
            if (`dur` != null) Some("dur" -> `dur`) else None,
            if (`dur.ges` != null) Some("dur.ges" -> `dur.ges`) else None,
            if (`dur.metrical` != null) Some("dur.metrical" -> `dur.metrical`) else None,
            if (`dur.ppq` != null) Some("dur.ppq" -> `dur.ppq`) else None,
            if (`dur.real` != null) Some("dur.real" -> `dur.real`) else None,
            if (`dur.recip` != null) Some("dur.recip" -> `dur.recip`) else None,
            if (`enclose` != null) Some("enclose" -> `enclose`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`fermata` != null) Some("fermata" -> `fermata`) else None,
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
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
            if (`letterspacing` != null) Some("letterspacing" -> `letterspacing`) else None,
            if (`lineheight` != null) Some("lineheight" -> `lineheight`) else None,
            if (`loc` != null) Some("loc" -> `loc`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`oloc` != null) Some("oloc" -> `oloc`) else None,
            if (`ploc` != null) Some("ploc" -> `ploc`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`staff` != null) Some("staff" -> `staff`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`tstamp` != null) Some("tstamp" -> `tstamp`) else None,
            if (`tstamp.ges` != null) Some("tstamp.ges" -> `tstamp.ges`) else None,
            if (`tstamp.real` != null) Some("tstamp.real" -> `tstamp.real`) else None,
            if (`tuplet` != null) Some("tuplet" -> `tuplet`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
          )
          .flatten,
      )
    new Rest(elements.toList, attributes)
  }
}

package sheet.meicmn.mei.stringtab

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A visual indication of the duration of atabGrp.
  *
  * Ident: tabDurSym
  * Module: MEI.stringtab
  */
case class TabDurSym(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.stringtab.att.Stringtab
    with _root_.sheet.meicmn.mei.stringtab.att.tabdursym.Log
    with _root_.sheet.meicmn.mei.visual.att.tabdursym.Vis
    with _root_.sheet.meicmn.mei.gestural.att.tabdursym.Ges
    with _root_.sheet.meicmn.mei.analytical.att.tabdursym.Anl {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "altsym",
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
      "glyph.auth",
      "glyph.name",
      "glyph.num",
      "glyph.uri",
      "ho",
      "label",
      "layer",
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
      "synch",
      "tab.course",
      "tab.fing",
      "tab.fret",
      "tab.line",
      "tab.string",
      "to",
      "type",
      "visible",
      "vo",
      "x",
      "xml:base",
      "xml:id",
      "y",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_tabDurSym)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object TabDurSym {
  def apply(
      `altsym`: java.lang.String = null,
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
      `glyph.auth`: java.lang.String = null,
      `glyph.name`: java.lang.String = null,
      `glyph.num`: java.lang.String = null,
      `glyph.uri`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `layer`: java.lang.String = null,
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
      `synch`: java.lang.String = null,
      `tab.course`: java.lang.String = null,
      `tab.fing`: java.lang.String = null,
      `tab.fret`: java.lang.String = null,
      `tab.line`: java.lang.String = null,
      `tab.string`: java.lang.String = null,
      `to`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `x`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `y`: java.lang.String = null,
  )(elements: Element*): TabDurSym = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`altsym` != null) Some("altsym" -> `altsym`) else None,
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
            if (`glyph.auth` != null) Some("glyph.auth" -> `glyph.auth`) else None,
            if (`glyph.name` != null) Some("glyph.name" -> `glyph.name`) else None,
            if (`glyph.num` != null) Some("glyph.num" -> `glyph.num`) else None,
            if (`glyph.uri` != null) Some("glyph.uri" -> `glyph.uri`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`layer` != null) Some("layer" -> `layer`) else None,
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
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`tab.course` != null) Some("tab.course" -> `tab.course`) else None,
            if (`tab.fing` != null) Some("tab.fing" -> `tab.fing`) else None,
            if (`tab.fret` != null) Some("tab.fret" -> `tab.fret`) else None,
            if (`tab.line` != null) Some("tab.line" -> `tab.line`) else None,
            if (`tab.string` != null) Some("tab.string" -> `tab.string`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
          )
          .flatten,
      )
    new TabDurSym(elements.toList, attributes)
  }
}

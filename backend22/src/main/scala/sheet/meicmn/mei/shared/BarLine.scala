package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Vertical line drawn through one or more staves that divides musical notation into metrical units.
  *
  * Ident: barLine
  * Module: MEI.shared
  */
case class BarLine(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Pointing
    with _root_.sheet.meicmn.mei.shared.att.TargetEval
    with _root_.sheet.meicmn.mei.analytical.att.barline.Anl
    with _root_.sheet.meicmn.mei.gestural.att.barline.Ges
    with _root_.sheet.meicmn.mei.shared.att.barline.Log
    with _root_.sheet.meicmn.mei.visual.att.barline.Vis
    with _root_.sheet.meicmn.mei.shared.model.EventLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "altsym",
      "class",
      "color",
      "control",
      "copyof",
      "corresp",
      "evaluate",
      "facs",
      "follows",
      "form",
      "glyph.auth",
      "glyph.name",
      "glyph.num",
      "glyph.uri",
      "label",
      "len",
      "metcon",
      "method",
      "n",
      "next",
      "place",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "target",
      "targettype",
      "type",
      "visible",
      "width",
      "xlink:actuate",
      "xlink:role",
      "xlink:show",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_barLine)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object BarLine {
  def apply(
      `altsym`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `control`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `form`: java.lang.String = null,
      `glyph.auth`: java.lang.String = null,
      `glyph.name`: java.lang.String = null,
      `glyph.num`: java.lang.String = null,
      `glyph.uri`: java.lang.String = null,
      `label`: java.lang.String = null,
      `len`: java.lang.String = null,
      `metcon`: java.lang.String = null,
      `method`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `place`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `target`: java.lang.String = null,
      `targettype`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `width`: java.lang.String = null,
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): BarLine = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`altsym` != null) Some("altsym" -> `altsym`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`control` != null) Some("control" -> `control`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`form` != null) Some("form" -> `form`) else None,
            if (`glyph.auth` != null) Some("glyph.auth" -> `glyph.auth`) else None,
            if (`glyph.name` != null) Some("glyph.name" -> `glyph.name`) else None,
            if (`glyph.num` != null) Some("glyph.num" -> `glyph.num`) else None,
            if (`glyph.uri` != null) Some("glyph.uri" -> `glyph.uri`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`len` != null) Some("len" -> `len`) else None,
            if (`metcon` != null) Some("metcon" -> `metcon`) else None,
            if (`method` != null) Some("method" -> `method`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`place` != null) Some("place" -> `place`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`target` != null) Some("target" -> `target`) else None,
            if (`targettype` != null) Some("targettype" -> `targettype`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`xlink:actuate` != null) Some("xlink:actuate" -> `xlink:actuate`) else None,
            if (`xlink:role` != null) Some("xlink:role" -> `xlink:role`) else None,
            if (`xlink:show` != null) Some("xlink:show" -> `xlink:show`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new BarLine(elements.toList, attributes)
  }
}

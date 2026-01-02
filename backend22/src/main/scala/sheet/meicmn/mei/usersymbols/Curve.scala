package sheet.meicmn.mei.usersymbols

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A curved line that cannot be represented by a more specific element, such as a slur.
  *
  * Ident: curve
  * Module: MEI.usersymbols
  */
case class Curve(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.analytical.att.curve.Anl
    with _root_.sheet.meicmn.mei.gestural.att.curve.Ges
    with _root_.sheet.meicmn.mei.usersymbols.att.curve.Log
    with _root_.sheet.meicmn.mei.visual.att.curve.Vis
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "bezier",
      "bulge",
      "class",
      "color",
      "copyof",
      "corresp",
      "curvedir",
      "endho",
      "endid",
      "endto",
      "endvo",
      "facs",
      "follows",
      "func",
      "ho",
      "label",
      "lform",
      "lsegs",
      "lwidth",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "startho",
      "startid",
      "startto",
      "startvo",
      "synch",
      "to",
      "type",
      "vo",
      "x",
      "x2",
      "xml:base",
      "xml:id",
      "y",
      "y2",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_curve)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Curve {
  def apply(
      `bezier`: java.lang.String = null,
      `bulge`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `curvedir`: java.lang.String = null,
      `endho`: java.lang.String = null,
      `endid`: java.lang.String = null,
      `endto`: java.lang.String = null,
      `endvo`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `func`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `lform`: java.lang.String = null,
      `lsegs`: java.lang.String = null,
      `lwidth`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `startho`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `startto`: java.lang.String = null,
      `startvo`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `to`: java.lang.String = null,
      `type`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `x`: java.lang.String = null,
      `x2`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `y`: java.lang.String = null,
      `y2`: java.lang.String = null,
  )(elements: Element*): Curve = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`bezier` != null) Some("bezier" -> `bezier`) else None,
            if (`bulge` != null) Some("bulge" -> `bulge`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`curvedir` != null) Some("curvedir" -> `curvedir`) else None,
            if (`endho` != null) Some("endho" -> `endho`) else None,
            if (`endid` != null) Some("endid" -> `endid`) else None,
            if (`endto` != null) Some("endto" -> `endto`) else None,
            if (`endvo` != null) Some("endvo" -> `endvo`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`func` != null) Some("func" -> `func`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`lform` != null) Some("lform" -> `lform`) else None,
            if (`lsegs` != null) Some("lsegs" -> `lsegs`) else None,
            if (`lwidth` != null) Some("lwidth" -> `lwidth`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`startho` != null) Some("startho" -> `startho`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`startto` != null) Some("startto" -> `startto`) else None,
            if (`startvo` != null) Some("startvo" -> `startvo`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`x` != null) Some("x" -> `x`) else None,
            if (`x2` != null) Some("x2" -> `x2`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`y` != null) Some("y" -> `y`) else None,
            if (`y2` != null) Some("y2" -> `y2`) else None,
          )
          .flatten,
      )
    new Curve(elements.toList, attributes)
  }
}

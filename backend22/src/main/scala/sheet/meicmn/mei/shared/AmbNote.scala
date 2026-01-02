package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Highest or lowest pitch in a score, staff, or layer.
  *
  * Ident: ambNote
  * Module: MEI.shared
  */
case class AmbNote(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.analytical.att.ambnote.Anl
    with _root_.sheet.meicmn.mei.gestural.att.ambnote.Ges
    with _root_.sheet.meicmn.mei.shared.att.ambnote.Log
    with _root_.sheet.meicmn.mei.visual.att.ambnote.Vis {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "accid",
      "class",
      "color",
      "colored",
      "copyof",
      "corresp",
      "dur",
      "enclose",
      "facs",
      "follows",
      "fontfam",
      "fontname",
      "fontsize",
      "fontstyle",
      "fontweight",
      "head.altsym",
      "head.auth",
      "head.color",
      "head.fill",
      "head.fillcolor",
      "head.mod",
      "head.rotation",
      "head.shape",
      "head.visible",
      "label",
      "letterspacing",
      "lineheight",
      "loc",
      "n",
      "next",
      "oct",
      "pname",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "stem.dir",
      "stem.len",
      "stem.mod",
      "stem.pos",
      "stem.sameas",
      "stem.visible",
      "stem.with",
      "stem.x",
      "stem.y",
      "synch",
      "type",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_ambNote)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object AmbNote {
  def apply(
      `accid`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `colored`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `dur`: java.lang.String = null,
      `enclose`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `fontfam`: java.lang.String = null,
      `fontname`: java.lang.String = null,
      `fontsize`: java.lang.String = null,
      `fontstyle`: java.lang.String = null,
      `fontweight`: java.lang.String = null,
      `head.altsym`: java.lang.String = null,
      `head.auth`: java.lang.String = null,
      `head.color`: java.lang.String = null,
      `head.fill`: java.lang.String = null,
      `head.fillcolor`: java.lang.String = null,
      `head.mod`: java.lang.String = null,
      `head.rotation`: java.lang.String = null,
      `head.shape`: java.lang.String = null,
      `head.visible`: java.lang.String = null,
      `label`: java.lang.String = null,
      `letterspacing`: java.lang.String = null,
      `lineheight`: java.lang.String = null,
      `loc`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `oct`: java.lang.String = null,
      `pname`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `stem.dir`: java.lang.String = null,
      `stem.len`: java.lang.String = null,
      `stem.mod`: java.lang.String = null,
      `stem.pos`: java.lang.String = null,
      `stem.sameas`: java.lang.String = null,
      `stem.visible`: java.lang.String = null,
      `stem.with`: java.lang.String = null,
      `stem.x`: java.lang.String = null,
      `stem.y`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): AmbNote = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`accid` != null) Some("accid" -> `accid`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`colored` != null) Some("colored" -> `colored`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`dur` != null) Some("dur" -> `dur`) else None,
            if (`enclose` != null) Some("enclose" -> `enclose`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`fontfam` != null) Some("fontfam" -> `fontfam`) else None,
            if (`fontname` != null) Some("fontname" -> `fontname`) else None,
            if (`fontsize` != null) Some("fontsize" -> `fontsize`) else None,
            if (`fontstyle` != null) Some("fontstyle" -> `fontstyle`) else None,
            if (`fontweight` != null) Some("fontweight" -> `fontweight`) else None,
            if (`head.altsym` != null) Some("head.altsym" -> `head.altsym`) else None,
            if (`head.auth` != null) Some("head.auth" -> `head.auth`) else None,
            if (`head.color` != null) Some("head.color" -> `head.color`) else None,
            if (`head.fill` != null) Some("head.fill" -> `head.fill`) else None,
            if (`head.fillcolor` != null) Some("head.fillcolor" -> `head.fillcolor`) else None,
            if (`head.mod` != null) Some("head.mod" -> `head.mod`) else None,
            if (`head.rotation` != null) Some("head.rotation" -> `head.rotation`) else None,
            if (`head.shape` != null) Some("head.shape" -> `head.shape`) else None,
            if (`head.visible` != null) Some("head.visible" -> `head.visible`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`letterspacing` != null) Some("letterspacing" -> `letterspacing`) else None,
            if (`lineheight` != null) Some("lineheight" -> `lineheight`) else None,
            if (`loc` != null) Some("loc" -> `loc`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`oct` != null) Some("oct" -> `oct`) else None,
            if (`pname` != null) Some("pname" -> `pname`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`stem.dir` != null) Some("stem.dir" -> `stem.dir`) else None,
            if (`stem.len` != null) Some("stem.len" -> `stem.len`) else None,
            if (`stem.mod` != null) Some("stem.mod" -> `stem.mod`) else None,
            if (`stem.pos` != null) Some("stem.pos" -> `stem.pos`) else None,
            if (`stem.sameas` != null) Some("stem.sameas" -> `stem.sameas`) else None,
            if (`stem.visible` != null) Some("stem.visible" -> `stem.visible`) else None,
            if (`stem.with` != null) Some("stem.with" -> `stem.with`) else None,
            if (`stem.x` != null) Some("stem.x" -> `stem.x`) else None,
            if (`stem.y` != null) Some("stem.y" -> `stem.y`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new AmbNote(elements.toList, attributes)
  }
}

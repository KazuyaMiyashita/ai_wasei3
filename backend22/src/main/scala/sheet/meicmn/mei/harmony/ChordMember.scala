package sheet.meicmn.mei.harmony

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** An individual pitch in a chord defined by achordDefelement.
  *
  * Ident: chordMember
  * Module: MEI.harmony
  */
case class ChordMember(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.analytical.att.chordmember.Anl
    with _root_.sheet.meicmn.mei.gestural.att.chordmember.Ges
    with _root_.sheet.meicmn.mei.harmony.att.chordmember.Log
    with _root_.sheet.meicmn.mei.visual.att.chordmember.Vis {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "accid.ges",
      "class",
      "copyof",
      "corresp",
      "follows",
      "inth",
      "label",
      "n",
      "next",
      "oct",
      "pname",
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
      "type",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_chordMember)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object ChordMember {
  def apply(
      `accid.ges`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `inth`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `oct`: java.lang.String = null,
      `pname`: java.lang.String = null,
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
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): ChordMember = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`accid.ges` != null) Some("accid.ges" -> `accid.ges`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`inth` != null) Some("inth" -> `inth`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`oct` != null) Some("oct" -> `oct`) else None,
            if (`pname` != null) Some("pname" -> `pname`) else None,
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
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new ChordMember(elements.toList, attributes)
  }
}

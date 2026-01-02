package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Alternative ending for a repeated passage of music;i.e., prima volta, seconda volta, etc.
  *
  * Ident: ending
  * Module: MEI.shared
  */
case class Ending(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Pointing
    with _root_.sheet.meicmn.mei.shared.att.TargetEval
    with _root_.sheet.meicmn.mei.analytical.att.ending.Anl
    with _root_.sheet.meicmn.mei.gestural.att.ending.Ges
    with _root_.sheet.meicmn.mei.shared.att.ending.Log
    with _root_.sheet.meicmn.mei.visual.att.ending.Vis
    with _root_.sheet.meicmn.mei.shared.model.EndingLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "copyof",
      "corresp",
      "evaluate",
      "facs",
      "follows",
      "label",
      "lendsym",
      "lendsym.size",
      "lform",
      "lsegs",
      "lstartsym",
      "lstartsym.size",
      "lwidth",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "target",
      "targettype",
      "type",
      "when",
      "xlink:actuate",
      "xlink:role",
      "xlink:show",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_ending)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Ending {
  def apply(
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `lendsym`: java.lang.String = null,
      `lendsym.size`: java.lang.String = null,
      `lform`: java.lang.String = null,
      `lsegs`: java.lang.String = null,
      `lstartsym`: java.lang.String = null,
      `lstartsym.size`: java.lang.String = null,
      `lwidth`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `target`: java.lang.String = null,
      `targettype`: java.lang.String = null,
      `type`: java.lang.String = null,
      `when`: java.lang.String = null,
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Ending = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`lendsym` != null) Some("lendsym" -> `lendsym`) else None,
            if (`lendsym.size` != null) Some("lendsym.size" -> `lendsym.size`) else None,
            if (`lform` != null) Some("lform" -> `lform`) else None,
            if (`lsegs` != null) Some("lsegs" -> `lsegs`) else None,
            if (`lstartsym` != null) Some("lstartsym" -> `lstartsym`) else None,
            if (`lstartsym.size` != null) Some("lstartsym.size" -> `lstartsym.size`) else None,
            if (`lwidth` != null) Some("lwidth" -> `lwidth`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`target` != null) Some("target" -> `target`) else None,
            if (`targettype` != null) Some("targettype" -> `targettype`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`when` != null) Some("when" -> `when`) else None,
            if (`xlink:actuate` != null) Some("xlink:actuate" -> `xlink:actuate`) else None,
            if (`xlink:role` != null) Some("xlink:role" -> `xlink:role`) else None,
            if (`xlink:show` != null) Some("xlink:show" -> `xlink:show`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Ending(elements.toList, attributes)
  }
}

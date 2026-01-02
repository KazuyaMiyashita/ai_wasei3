package sheet.meicmn.mei.frbr

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Single instance or exemplar of a source/manifestation.
  *
  * Ident: item
  * Module: MEI.frbr
  */
case class Item(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Authorized
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.shared.att.DataPointing
    with _root_.sheet.meicmn.mei.shared.att.Pointing
    with _root_.sheet.meicmn.mei.shared.att.TargetEval
    with _root_.sheet.meicmn.mei.frbr.model.ItemLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "analog",
      "auth",
      "auth.uri",
      "class",
      "codedval",
      "copyof",
      "corresp",
      "data",
      "evaluate",
      "follows",
      "label",
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
      "xlink:actuate",
      "xlink:role",
      "xlink:show",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_item)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Item {
  def apply(
      `analog`: java.lang.String = null,
      `auth`: java.lang.String = null,
      `auth.uri`: java.lang.String = null,
      `class`: java.lang.String = null,
      `codedval`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `data`: java.lang.String = null,
      `evaluate`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
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
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Item = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`auth` != null) Some("auth" -> `auth`) else None,
            if (`auth.uri` != null) Some("auth.uri" -> `auth.uri`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`codedval` != null) Some("codedval" -> `codedval`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`data` != null) Some("data" -> `data`) else None,
            if (`evaluate` != null) Some("evaluate" -> `evaluate`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
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
            if (`xlink:actuate` != null) Some("xlink:actuate" -> `xlink:actuate`) else None,
            if (`xlink:role` != null) Some("xlink:role" -> `xlink:role`) else None,
            if (`xlink:show` != null) Some("xlink:show" -> `xlink:show`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Item(elements.toList, attributes)
  }
}

package sheet.meicmn.mei.genetic

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Describes a distinctive state in the textual development of a work.
  *
  * Ident: genState
  * Module: MEI.genetic
  */
case class GenState(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.shared.att.Datable
    with _root_.sheet.meicmn.mei.shared.att.DataPointing
    with _root_.sheet.meicmn.mei.shared.att.MetadataPointing
    with _root_.sheet.meicmn.mei.shared.att.Pointing {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "analog",
      "class",
      "copyof",
      "corresp",
      "data",
      "decls",
      "enddate",
      "follows",
      "isodate",
      "label",
      "n",
      "next",
      "notafter",
      "notbefore",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "startdate",
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
    val elementsValid   = Validator.validate(elements, Schemas.schema_genState)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object GenState {
  def apply(
      `analog`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `data`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `enddate`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `isodate`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `notafter`: java.lang.String = null,
      `notbefore`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `startdate`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `target`: java.lang.String = null,
      `targettype`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): GenState = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`data` != null) Some("data" -> `data`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`enddate` != null) Some("enddate" -> `enddate`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`isodate` != null) Some("isodate" -> `isodate`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`notafter` != null) Some("notafter" -> `notafter`) else None,
            if (`notbefore` != null) Some("notbefore" -> `notbefore`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`startdate` != null) Some("startdate" -> `startdate`) else None,
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
    new GenState(elements.toList, attributes)
  }
}

package sheet.meicmn.mei.namesdates

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Contains a name component which indicates that the referent has a particular role or position in society, such as an official title or rank.
  *
  * Ident: roleName
  * Module: MEI.namesdates
  */
case class RoleName(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.edittrans.att.Edit
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.shared.att.Name
    with _root_.sheet.meicmn.mei.namesdates.model.PersNamePart {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "analog",
      "auth",
      "auth.uri",
      "cert",
      "class",
      "codedval",
      "copyof",
      "corresp",
      "enddate",
      "evidence",
      "facs",
      "follows",
      "isodate",
      "label",
      "n",
      "next",
      "nonfiling",
      "notafter",
      "notbefore",
      "nymref",
      "precedes",
      "prev",
      "resp",
      "role",
      "sameas",
      "source",
      "startdate",
      "synch",
      "translit",
      "type",
      "xml:base",
      "xml:id",
      "xml:lang",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_roleName)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object RoleName {
  def apply(
      `analog`: java.lang.String = null,
      `auth`: java.lang.String = null,
      `auth.uri`: java.lang.String = null,
      `cert`: java.lang.String = null,
      `class`: java.lang.String = null,
      `codedval`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `enddate`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `isodate`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `nonfiling`: java.lang.String = null,
      `notafter`: java.lang.String = null,
      `notbefore`: java.lang.String = null,
      `nymref`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `role`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `source`: java.lang.String = null,
      `startdate`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `translit`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
  )(elements: Element*): RoleName = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`auth` != null) Some("auth" -> `auth`) else None,
            if (`auth.uri` != null) Some("auth.uri" -> `auth.uri`) else None,
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`codedval` != null) Some("codedval" -> `codedval`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`enddate` != null) Some("enddate" -> `enddate`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`isodate` != null) Some("isodate" -> `isodate`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`nonfiling` != null) Some("nonfiling" -> `nonfiling`) else None,
            if (`notafter` != null) Some("notafter" -> `notafter`) else None,
            if (`notbefore` != null) Some("notbefore" -> `notbefore`) else None,
            if (`nymref` != null) Some("nymref" -> `nymref`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`role` != null) Some("role" -> `role`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`source` != null) Some("source" -> `source`) else None,
            if (`startdate` != null) Some("startdate" -> `startdate`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`xml:lang` != null) Some("xml:lang" -> `xml:lang`) else None,
          )
          .flatten,
      )
    new RoleName(elements.toList, attributes)
  }
}

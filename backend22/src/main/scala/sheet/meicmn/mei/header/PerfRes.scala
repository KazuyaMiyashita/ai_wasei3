package sheet.meicmn.mei.header

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Name of an instrument on which a performer plays, a performer's voice range, or a standard performing ensemble designation.
  *
  * Ident: perfRes
  * Module: MEI.header
  */
case class PerfRes(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Authorized
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.edittrans.att.Edit
    with _root_.sheet.meicmn.mei.shared.att.Lang
    with _root_.sheet.meicmn.mei.header.att.PerfRes {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "adlib",
      "analog",
      "auth",
      "auth.uri",
      "cert",
      "class",
      "codedval",
      "copyof",
      "corresp",
      "count",
      "evidence",
      "follows",
      "label",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "solo",
      "source",
      "synch",
      "trans.diat",
      "trans.semi",
      "translit",
      "type",
      "xml:base",
      "xml:id",
      "xml:lang",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_perfRes)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object PerfRes {
  def apply(
      `adlib`: java.lang.String = null,
      `analog`: java.lang.String = null,
      `auth`: java.lang.String = null,
      `auth.uri`: java.lang.String = null,
      `cert`: java.lang.String = null,
      `class`: java.lang.String = null,
      `codedval`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `count`: java.lang.String = null,
      `evidence`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `solo`: java.lang.String = null,
      `source`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `trans.diat`: java.lang.String = null,
      `trans.semi`: java.lang.String = null,
      `translit`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
      `xml:lang`: java.lang.String = null,
  )(elements: Element*): PerfRes = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`adlib` != null) Some("adlib" -> `adlib`) else None,
            if (`analog` != null) Some("analog" -> `analog`) else None,
            if (`auth` != null) Some("auth" -> `auth`) else None,
            if (`auth.uri` != null) Some("auth.uri" -> `auth.uri`) else None,
            if (`cert` != null) Some("cert" -> `cert`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`codedval` != null) Some("codedval" -> `codedval`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`count` != null) Some("count" -> `count`) else None,
            if (`evidence` != null) Some("evidence" -> `evidence`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`solo` != null) Some("solo" -> `solo`) else None,
            if (`source` != null) Some("source" -> `source`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`trans.diat` != null) Some("trans.diat" -> `trans.diat`) else None,
            if (`trans.semi` != null) Some("trans.semi" -> `trans.semi`) else None,
            if (`translit` != null) Some("translit" -> `translit`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
            if (`xml:lang` != null) Some("xml:lang" -> `xml:lang`) else None,
          )
          .flatten,
      )
    new PerfRes(elements.toList, attributes)
  }
}

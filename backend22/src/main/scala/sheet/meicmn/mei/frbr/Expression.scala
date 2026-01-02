package sheet.meicmn.mei.frbr

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Intellectual or artistic realization of a work.
  *
  * Ident: expression
  * Module: MEI.frbr
  */
case class Expression(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.Authorized
    with _root_.sheet.meicmn.mei.shared.att.Bibl
    with _root_.sheet.meicmn.mei.shared.att.DataPointing
    with _root_.sheet.meicmn.mei.frbr.model.ExpressionLike {
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
      "follows",
      "label",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "type",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_expression)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Expression {
  def apply(
      `analog`: java.lang.String = null,
      `auth`: java.lang.String = null,
      `auth.uri`: java.lang.String = null,
      `class`: java.lang.String = null,
      `codedval`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `data`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Expression = {
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
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Expression(elements.toList, attributes)
  }
}

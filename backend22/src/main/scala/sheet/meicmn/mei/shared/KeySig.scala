package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Written key signature.
  *
  * Ident: keySig
  * Module: MEI.shared
  */
case class KeySig(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.analytical.att.keysig.Anl
    with _root_.sheet.meicmn.mei.gestural.att.keysig.Ges
    with _root_.sheet.meicmn.mei.shared.att.keysig.Log
    with _root_.sheet.meicmn.mei.visual.att.keysig.Vis
    with _root_.sheet.meicmn.mei.shared.model.KeySigLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "accid",
      "cancelaccid",
      "class",
      "color",
      "copyof",
      "corresp",
      "facs",
      "follows",
      "label",
      "mode",
      "n",
      "next",
      "pname",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "sig",
      "synch",
      "type",
      "visible",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_keySig)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object KeySig {
  def apply(
      `accid`: java.lang.String = null,
      `cancelaccid`: java.lang.String = null,
      `class`: java.lang.String = null,
      `color`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `mode`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `pname`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `sig`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): KeySig = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`accid` != null) Some("accid" -> `accid`) else None,
            if (`cancelaccid` != null) Some("cancelaccid" -> `cancelaccid`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`color` != null) Some("color" -> `color`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`mode` != null) Some("mode" -> `mode`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`pname` != null) Some("pname" -> `pname`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`sig` != null) Some("sig" -> `sig`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new KeySig(elements.toList, attributes)
  }
}

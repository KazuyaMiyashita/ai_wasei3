package sheet.meicmn.mei.cmn

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** A layer that contains an alternative to material in another layer.
  *
  * Ident: oLayer
  * Module: MEI.cmn
  */
case class OLayer(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Basic
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.Labelled
    with _root_.sheet.meicmn.mei.shared.att.Linking
    with _root_.sheet.meicmn.mei.shared.att.MetadataPointing
    with _root_.sheet.meicmn.mei.shared.att.NInteger
    with _root_.sheet.meicmn.mei.shared.att.Responsibility
    with _root_.sheet.meicmn.mei.shared.att.Typed
    with _root_.sheet.meicmn.mei.shared.att.layer.Log
    with _root_.sheet.meicmn.mei.visual.att.layer.Vis
    with _root_.sheet.meicmn.mei.gestural.att.layer.Ges
    with _root_.sheet.meicmn.mei.analytical.att.layer.Anl {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "copyof",
      "corresp",
      "cue",
      "decls",
      "def",
      "facs",
      "follows",
      "label",
      "metcon",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "type",
      "visible",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_oLayer)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object OLayer {
  def apply(
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `cue`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `def`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `metcon`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): OLayer = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`cue` != null) Some("cue" -> `cue`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`def` != null) Some("def" -> `def`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`metcon` != null) Some("metcon" -> `metcon`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new OLayer(elements.toList, attributes)
  }
}

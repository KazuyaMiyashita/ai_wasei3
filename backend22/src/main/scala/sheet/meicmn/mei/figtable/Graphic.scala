package sheet.meicmn.mei.figtable

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Indicates the location of an inline graphic.
  *
  * Ident: graphic
  * Module: MEI.figtable
  */
case class Graphic(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Common
    with _root_.sheet.meicmn.mei.shared.att.coordinated.Ul
    with _root_.sheet.meicmn.mei.shared.att.Dimensions
    with _root_.sheet.meicmn.mei.shared.att.InternetMedia
    with _root_.sheet.meicmn.mei.facsimile.att.Facsimile
    with _root_.sheet.meicmn.mei.shared.att.MetadataPointing
    with _root_.sheet.meicmn.mei.shared.att.Pointing
    with _root_.sheet.meicmn.mei.shared.att.StartId
    with _root_.sheet.meicmn.mei.shared.att.VisualOffset
    with _root_.sheet.meicmn.mei.figtable.model.GraphicLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "class",
      "copyof",
      "corresp",
      "decls",
      "facs",
      "follows",
      "height",
      "ho",
      "label",
      "mimetype",
      "n",
      "next",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "startid",
      "synch",
      "target",
      "targettype",
      "to",
      "type",
      "ulx",
      "uly",
      "vo",
      "width",
      "xlink:actuate",
      "xlink:role",
      "xlink:show",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_graphic)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object Graphic {
  def apply(
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `facs`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `height`: java.lang.String = null,
      `ho`: java.lang.String = null,
      `label`: java.lang.String = null,
      `mimetype`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `startid`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `target`: java.lang.String = null,
      `targettype`: java.lang.String = null,
      `to`: java.lang.String = null,
      `type`: java.lang.String = null,
      `ulx`: java.lang.String = null,
      `uly`: java.lang.String = null,
      `vo`: java.lang.String = null,
      `width`: java.lang.String = null,
      `xlink:actuate`: java.lang.String = null,
      `xlink:role`: java.lang.String = null,
      `xlink:show`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): Graphic = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`facs` != null) Some("facs" -> `facs`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`height` != null) Some("height" -> `height`) else None,
            if (`ho` != null) Some("ho" -> `ho`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`mimetype` != null) Some("mimetype" -> `mimetype`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`startid` != null) Some("startid" -> `startid`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`target` != null) Some("target" -> `target`) else None,
            if (`targettype` != null) Some("targettype" -> `targettype`) else None,
            if (`to` != null) Some("to" -> `to`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`ulx` != null) Some("ulx" -> `ulx`) else None,
            if (`uly` != null) Some("uly" -> `uly`) else None,
            if (`vo` != null) Some("vo" -> `vo`) else None,
            if (`width` != null) Some("width" -> `width`) else None,
            if (`xlink:actuate` != null) Some("xlink:actuate" -> `xlink:actuate`) else None,
            if (`xlink:role` != null) Some("xlink:role" -> `xlink:role`) else None,
            if (`xlink:show` != null) Some("xlink:show" -> `xlink:show`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new Graphic(elements.toList, attributes)
  }
}

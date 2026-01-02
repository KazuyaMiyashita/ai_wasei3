package sheet.meicmn.mei.shared

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** Container for layer meta-information.
  *
  * Ident: layerDef
  * Module: MEI.shared
  */
case class LayerDef(
    elements: scala.List[Element],
    attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String],
) extends sheet.meicmn.Element
    with _root_.sheet.meicmn.mei.shared.att.Basic
    with _root_.sheet.meicmn.mei.shared.att.Labelled
    with _root_.sheet.meicmn.mei.shared.att.Linking
    with _root_.sheet.meicmn.mei.shared.att.MetadataPointing
    with _root_.sheet.meicmn.mei.shared.att.NInteger
    with _root_.sheet.meicmn.mei.shared.att.Responsibility
    with _root_.sheet.meicmn.mei.shared.att.Typed
    with _root_.sheet.meicmn.mei.analytical.att.layerdef.Anl
    with _root_.sheet.meicmn.mei.gestural.att.layerdef.Ges
    with _root_.sheet.meicmn.mei.shared.att.layerdef.Log
    with _root_.sheet.meicmn.mei.visual.att.layerdef.Vis
    with _root_.sheet.meicmn.mei.shared.model.LayerDefLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "beam.color",
      "beam.group",
      "beam.rend",
      "beam.rests",
      "beam.slope",
      "class",
      "copyof",
      "corresp",
      "decls",
      "dur.default",
      "follows",
      "instr",
      "label",
      "n",
      "next",
      "num.default",
      "numbase.default",
      "oct.default",
      "precedes",
      "prev",
      "resp",
      "sameas",
      "synch",
      "text.fam",
      "text.name",
      "text.size",
      "text.style",
      "text.weight",
      "trans.diat",
      "trans.semi",
      "tune.Hz",
      "tune.pname",
      "tune.temper",
      "type",
      "visible",
      "xml:base",
      "xml:id",
    )
    val attributesValid = attributes.keys.forall(allowedAttrs.contains)
    val elementsValid   = Validator.validate(elements, Schemas.schema_layerDef)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object LayerDef {
  def apply(
      `beam.color`: java.lang.String = null,
      `beam.group`: java.lang.String = null,
      `beam.rend`: java.lang.String = null,
      `beam.rests`: java.lang.String = null,
      `beam.slope`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `dur.default`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `instr`: java.lang.String = null,
      `label`: java.lang.String = null,
      `n`: java.lang.String = null,
      `next`: java.lang.String = null,
      `num.default`: java.lang.String = null,
      `numbase.default`: java.lang.String = null,
      `oct.default`: java.lang.String = null,
      `precedes`: java.lang.String = null,
      `prev`: java.lang.String = null,
      `resp`: java.lang.String = null,
      `sameas`: java.lang.String = null,
      `synch`: java.lang.String = null,
      `text.fam`: java.lang.String = null,
      `text.name`: java.lang.String = null,
      `text.size`: java.lang.String = null,
      `text.style`: java.lang.String = null,
      `text.weight`: java.lang.String = null,
      `trans.diat`: java.lang.String = null,
      `trans.semi`: java.lang.String = null,
      `tune.Hz`: java.lang.String = null,
      `tune.pname`: java.lang.String = null,
      `tune.temper`: java.lang.String = null,
      `type`: java.lang.String = null,
      `visible`: java.lang.String = null,
      `xml:base`: java.lang.String = null,
      `xml:id`: java.lang.String = null,
  )(elements: Element*): LayerDef = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`beam.color` != null) Some("beam.color" -> `beam.color`) else None,
            if (`beam.group` != null) Some("beam.group" -> `beam.group`) else None,
            if (`beam.rend` != null) Some("beam.rend" -> `beam.rend`) else None,
            if (`beam.rests` != null) Some("beam.rests" -> `beam.rests`) else None,
            if (`beam.slope` != null) Some("beam.slope" -> `beam.slope`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`dur.default` != null) Some("dur.default" -> `dur.default`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`instr` != null) Some("instr" -> `instr`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`n` != null) Some("n" -> `n`) else None,
            if (`next` != null) Some("next" -> `next`) else None,
            if (`num.default` != null) Some("num.default" -> `num.default`) else None,
            if (`numbase.default` != null) Some("numbase.default" -> `numbase.default`) else None,
            if (`oct.default` != null) Some("oct.default" -> `oct.default`) else None,
            if (`precedes` != null) Some("precedes" -> `precedes`) else None,
            if (`prev` != null) Some("prev" -> `prev`) else None,
            if (`resp` != null) Some("resp" -> `resp`) else None,
            if (`sameas` != null) Some("sameas" -> `sameas`) else None,
            if (`synch` != null) Some("synch" -> `synch`) else None,
            if (`text.fam` != null) Some("text.fam" -> `text.fam`) else None,
            if (`text.name` != null) Some("text.name" -> `text.name`) else None,
            if (`text.size` != null) Some("text.size" -> `text.size`) else None,
            if (`text.style` != null) Some("text.style" -> `text.style`) else None,
            if (`text.weight` != null) Some("text.weight" -> `text.weight`) else None,
            if (`trans.diat` != null) Some("trans.diat" -> `trans.diat`) else None,
            if (`trans.semi` != null) Some("trans.semi" -> `trans.semi`) else None,
            if (`tune.Hz` != null) Some("tune.Hz" -> `tune.Hz`) else None,
            if (`tune.pname` != null) Some("tune.pname" -> `tune.pname`) else None,
            if (`tune.temper` != null) Some("tune.temper" -> `tune.temper`) else None,
            if (`type` != null) Some("type" -> `type`) else None,
            if (`visible` != null) Some("visible" -> `visible`) else None,
            if (`xml:base` != null) Some("xml:base" -> `xml:base`) else None,
            if (`xml:id` != null) Some("xml:id" -> `xml:id`) else None,
          )
          .flatten,
      )
    new LayerDef(elements.toList, attributes)
  }
}

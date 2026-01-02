package sheet.meicmn.mei.midi

import sheet.meicmn.Element
import sheet.meicmn.Schemas
import sheet.meicmn.Validator

/** MIDI instrument declaration.
  *
  * Ident: instrDef
  * Module: MEI.midi
  */
case class InstrDef(
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
    with _root_.sheet.meicmn.mei.analytical.att.instrdef.Anl
    with _root_.sheet.meicmn.mei.gestural.att.instrdef.Ges
    with _root_.sheet.meicmn.mei.midi.att.instrdef.Log
    with _root_.sheet.meicmn.mei.visual.att.instrdef.Vis
    with _root_.sheet.meicmn.mei.shared.model.InstrDefLike {
  override def validate(): Boolean = {
    val allowedAttrs = Set(
      "azimuth",
      "class",
      "copyof",
      "corresp",
      "decls",
      "elevation",
      "follows",
      "label",
      "midi.channel",
      "midi.duty",
      "midi.instrname",
      "midi.instrnum",
      "midi.pan",
      "midi.patchname",
      "midi.patchnum",
      "midi.port",
      "midi.track",
      "midi.volume",
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
    val elementsValid   = Validator.validate(elements, Schemas.schema_instrDef)
    attributesValid && elementsValid
  }
  override def children: scala.List[Element]                           = elements
  override def withChildren(newChildren: scala.List[Element]): Element = copy(elements = newChildren)
}

object InstrDef {
  def apply(
      `azimuth`: java.lang.String = null,
      `class`: java.lang.String = null,
      `copyof`: java.lang.String = null,
      `corresp`: java.lang.String = null,
      `decls`: java.lang.String = null,
      `elevation`: java.lang.String = null,
      `follows`: java.lang.String = null,
      `label`: java.lang.String = null,
      `midi.channel`: java.lang.String = null,
      `midi.duty`: java.lang.String = null,
      `midi.instrname`: java.lang.String = null,
      `midi.instrnum`: java.lang.String = null,
      `midi.pan`: java.lang.String = null,
      `midi.patchname`: java.lang.String = null,
      `midi.patchnum`: java.lang.String = null,
      `midi.port`: java.lang.String = null,
      `midi.track`: java.lang.String = null,
      `midi.volume`: java.lang.String = null,
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
  )(elements: Element*): InstrDef = {
    val attributes: scala.collection.immutable.SeqMap[java.lang.String, java.lang.String] =
      scala.collection.immutable.SeqMap.from(
        scala
          .List(
            if (`azimuth` != null) Some("azimuth" -> `azimuth`) else None,
            if (`class` != null) Some("class" -> `class`) else None,
            if (`copyof` != null) Some("copyof" -> `copyof`) else None,
            if (`corresp` != null) Some("corresp" -> `corresp`) else None,
            if (`decls` != null) Some("decls" -> `decls`) else None,
            if (`elevation` != null) Some("elevation" -> `elevation`) else None,
            if (`follows` != null) Some("follows" -> `follows`) else None,
            if (`label` != null) Some("label" -> `label`) else None,
            if (`midi.channel` != null) Some("midi.channel" -> `midi.channel`) else None,
            if (`midi.duty` != null) Some("midi.duty" -> `midi.duty`) else None,
            if (`midi.instrname` != null) Some("midi.instrname" -> `midi.instrname`) else None,
            if (`midi.instrnum` != null) Some("midi.instrnum" -> `midi.instrnum`) else None,
            if (`midi.pan` != null) Some("midi.pan" -> `midi.pan`) else None,
            if (`midi.patchname` != null) Some("midi.patchname" -> `midi.patchname`) else None,
            if (`midi.patchnum` != null) Some("midi.patchnum" -> `midi.patchnum`) else None,
            if (`midi.port` != null) Some("midi.port" -> `midi.port`) else None,
            if (`midi.track` != null) Some("midi.track" -> `midi.track`) else None,
            if (`midi.volume` != null) Some("midi.volume" -> `midi.volume`) else None,
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
    new InstrDef(elements.toList, attributes)
  }
}

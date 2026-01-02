package sheet.meicmn.mei.gestural.att.note

/** Gestural domain attributes.
  *
  * Ident: att.note.ges
  * Module: MEI.gestural
  */
trait Ges
    extends _root_.sheet.meicmn.mei.gestural.att.accidental.Ges
    with _root_.sheet.meicmn.mei.gestural.att.articulation.Ges
    with _root_.sheet.meicmn.mei.gestural.att.duration.Ges
    with _root_.sheet.meicmn.mei.midi.att.InstrumentIdent
    with _root_.sheet.meicmn.mei.midi.att.MidiVelocity
    with _root_.sheet.meicmn.mei.gestural.att.pitch.Ges
    with _root_.sheet.meicmn.mei.stringtab.att.Stringtab {}

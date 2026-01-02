package sheet.meicmn.mei.midi.att.midi

/** Attributes common to MIDI events.
  *
  * Ident: att.midi.event
  * Module: MEI.midi
  */
trait Event
    extends _root_.sheet.meicmn.mei.shared.att.LayerIdent
    with _root_.sheet.meicmn.mei.shared.att.PartIdent
    with _root_.sheet.meicmn.mei.shared.att.StaffIdent
    with _root_.sheet.meicmn.mei.shared.att.timestamp.Log
    with _root_.sheet.meicmn.mei.gestural.att.timestamp.Ges {}

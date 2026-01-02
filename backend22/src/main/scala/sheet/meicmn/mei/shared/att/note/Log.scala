package sheet.meicmn.mei.shared.att.note

/** Logical domain attributes.
  *
  * Ident: att.note.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.AugmentDots
    with _root_.sheet.meicmn.mei.shared.att.Coloration
    with _root_.sheet.meicmn.mei.shared.att.Cue
    with _root_.sheet.meicmn.mei.shared.att.duration.Log
    with _root_.sheet.meicmn.mei.shared.att.Event
    with _root_.sheet.meicmn.mei.cmn.att.note.log.Cmn
    with _root_.sheet.meicmn.mei.shared.att.Pitched {}

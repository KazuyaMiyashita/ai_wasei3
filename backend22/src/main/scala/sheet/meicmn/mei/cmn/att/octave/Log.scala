package sheet.meicmn.mei.cmn.att.octave

/** Logical domain attributes.
  *
  * Ident: att.octave.log
  * Module: MEI.cmn
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.OctaveDisplacement
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

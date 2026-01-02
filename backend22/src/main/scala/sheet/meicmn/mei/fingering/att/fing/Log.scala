package sheet.meicmn.mei.fingering.att.fing

/** Logical domain attributes.
  *
  * Ident: att.fing.log
  * Module: MEI.fingering
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

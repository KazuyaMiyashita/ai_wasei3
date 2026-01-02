package sheet.meicmn.mei.cmn.att.slur

/** Logical domain attributes.
  *
  * Ident: att.slur.log
  * Module: MEI.cmn
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

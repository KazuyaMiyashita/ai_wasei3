package sheet.meicmn.mei.shared.att.dir

/** Logical domain attributes.
  *
  * Ident: att.dir.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

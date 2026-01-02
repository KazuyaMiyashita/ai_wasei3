package sheet.meicmn.mei.harmony.att.harm

/** Logical domain attributes.
  *
  * Ident: att.harm.log
  * Module: MEI.harmony
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

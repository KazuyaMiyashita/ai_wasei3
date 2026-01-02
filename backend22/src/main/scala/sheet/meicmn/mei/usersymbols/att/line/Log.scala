package sheet.meicmn.mei.usersymbols.att.line

/** Attributes for describing the logical behavior of a line.
  *
  * Ident: att.line.log
  * Module: MEI.usersymbols
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

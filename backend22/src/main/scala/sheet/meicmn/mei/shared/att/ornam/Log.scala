package sheet.meicmn.mei.shared.att.ornam

/** Logical domain attributes.
  *
  * Ident: att.ornam.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.cmnornaments.att.OrnamentAccid
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

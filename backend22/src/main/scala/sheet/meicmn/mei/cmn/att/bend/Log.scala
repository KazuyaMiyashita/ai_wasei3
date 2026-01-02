package sheet.meicmn.mei.cmn.att.bend

/** Logical domain attributes.
  *
  * Ident: att.bend.log
  * Module: MEI.cmn
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

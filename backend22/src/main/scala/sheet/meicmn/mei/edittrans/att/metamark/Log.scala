package sheet.meicmn.mei.edittrans.att.metamark

/** Logical domain attributes.
  *
  * Ident: att.metaMark.log
  * Module: MEI.edittrans
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log
    with _root_.sheet.meicmn.mei.edittrans.att.Edit
    with _root_.sheet.meicmn.mei.edittrans.att.Trans {}

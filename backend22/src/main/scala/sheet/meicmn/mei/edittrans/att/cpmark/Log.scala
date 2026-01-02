package sheet.meicmn.mei.edittrans.att.cpmark

/** Logical domain attributes.
  *
  * Ident: att.cpMark.log
  * Module: MEI.edittrans
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.ControlEvent
    with _root_.sheet.meicmn.mei.shared.att.origin.timestamp.Log
    with _root_.sheet.meicmn.mei.shared.att.origin.StaffIdent
    with _root_.sheet.meicmn.mei.shared.att.origin.LayerIdent
    with _root_.sheet.meicmn.mei.shared.att.origin.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.OctaveDisplacement
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

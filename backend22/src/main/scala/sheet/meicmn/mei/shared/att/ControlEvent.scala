package sheet.meicmn.mei.shared.att

/** Attributes shared by events which rely on other events for their existence. For example, a slur/phrase marking must be drawn between or over a group of notes. The slur is therefore a control event.
  *
  * Ident: att.controlEvent
  * Module: MEI.shared
  */
trait ControlEvent
    extends _root_.sheet.meicmn.mei.performance.att.Alignment
    with _root_.sheet.meicmn.mei.shared.att.LayerIdent
    with _root_.sheet.meicmn.mei.shared.att.PartIdent
    with _root_.sheet.meicmn.mei.shared.att.Plist
    with _root_.sheet.meicmn.mei.shared.att.StaffIdent
    with _root_.sheet.meicmn.mei.shared.att.TargetEval
    with _root_.sheet.meicmn.mei.shared.att.timestamp.Log
    with _root_.sheet.meicmn.mei.gestural.att.timestamp.Ges {}

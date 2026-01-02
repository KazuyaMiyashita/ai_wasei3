package sheet.meicmn.mei.shared.att.annot

/** Logical domain attributes for annot. Values for the type attribute can be taken from any convenient typology of annotation suitable to the work in hand;e.g., annotation, gloss, citation, digression, preliminary, temporary, etc.
  *
  * Ident: att.annot.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.performance.att.Alignment
    with _root_.sheet.meicmn.mei.shared.att.duration.Additive
    with _root_.sheet.meicmn.mei.shared.att.LayerIdent
    with _root_.sheet.meicmn.mei.shared.att.PartIdent
    with _root_.sheet.meicmn.mei.shared.att.StaffIdent
    with _root_.sheet.meicmn.mei.shared.att.StartEndId
    with _root_.sheet.meicmn.mei.shared.att.timestamp.Log
    with _root_.sheet.meicmn.mei.shared.att.timestamp2.Log {}

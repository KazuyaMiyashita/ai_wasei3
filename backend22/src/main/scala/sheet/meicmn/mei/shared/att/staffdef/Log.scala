package sheet.meicmn.mei.shared.att.staffdef

/** Logical domain attributes for staffDef.
  *
  * Ident: att.staffDef.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.cleffing.Log
    with _root_.sheet.meicmn.mei.shared.att.duration.Default
    with _root_.sheet.meicmn.mei.shared.att.keysigdefault.Log
    with _root_.sheet.meicmn.mei.shared.att.metersigdefault.Log
    with _root_.sheet.meicmn.mei.att.NotationType
    with _root_.sheet.meicmn.mei.shared.att.OctaveDefault
    with _root_.sheet.meicmn.mei.shared.att.Transposition
    with _root_.sheet.meicmn.mei.cmn.att.staffdef.log.Cmn {}

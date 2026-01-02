package sheet.meicmn.mei.shared.att.scoredef

/** Logical domain attributes for scoreDef in the CMN repertoire. The values set in these attributes act as score-wide defaults for attributes that are not set in descendant elements.
  *
  * Ident: att.scoreDef.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.cleffing.Log
    with _root_.sheet.meicmn.mei.shared.att.duration.Default
    with _root_.sheet.meicmn.mei.shared.att.keysigdefault.Log
    with _root_.sheet.meicmn.mei.shared.att.metersigdefault.Log
    with _root_.sheet.meicmn.mei.shared.att.OctaveDefault
    with _root_.sheet.meicmn.mei.shared.att.Transposition
    with _root_.sheet.meicmn.mei.cmn.att.scoredef.log.Cmn {}

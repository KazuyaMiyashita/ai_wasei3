package sheet.meicmn.mei.visual.att.chord

/** Visual domain attributes for chord. The slur, slur.dir, slur.rend, tie, tie.dir, and tie.rend attributes here are syntactic sugar for these attributes on each of the chord's individual notes. The values here apply to all the notes in the chord. If some notes are slurred or tied while others aren't, then the individual note attributes must be used.
  *
  * Ident: att.chord.vis
  * Module: MEI.visual
  */
trait Vis
    extends _root_.sheet.meicmn.mei.usersymbols.att.AltSym
    with _root_.sheet.meicmn.mei.shared.att.Color
    with _root_.sheet.meicmn.mei.shared.att.EnclosingChars
    with _root_.sheet.meicmn.mei.externalsymbols.att.ExtSym
    with _root_.sheet.meicmn.mei.shared.att.Stems
    with _root_.sheet.meicmn.mei.shared.att.Typography
    with _root_.sheet.meicmn.mei.shared.att.Visibility
    with _root_.sheet.meicmn.mei.shared.att.visualoffset.Ho
    with _root_.sheet.meicmn.mei.shared.att.visualoffset.To
    with _root_.sheet.meicmn.mei.shared.att.Xy
    with _root_.sheet.meicmn.mei.cmn.att.chord.vis.Cmn {}

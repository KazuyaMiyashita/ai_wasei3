package sheet.meicmn.mei.cmn.att.chord.vis

/** Visual domain attributes for chord. The slur, slur.dir, slur.rend, tie, tie.dir, and tie.rend attributes here are "syntactic sugar" for these attributes on each of the chord's individual notes. The values here apply to all the notes in the chord. If some notes are slurred or tied while others aren't, then the individual note attributes must be used.
  *
  * Ident: att.chord.vis.cmn
  * Module: MEI.cmn
  */
trait Cmn extends _root_.sheet.meicmn.mei.cmn.att.BeamSecondary {}

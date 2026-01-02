package sheet.meicmn.mei.analytical.att.note

/** Analytical domain attributes.
  *
  * Ident: att.note.anl
  * Module: MEI.analytical
  */
trait Anl
    extends _root_.sheet.meicmn.mei.shared.att.Accidental
    with _root_.sheet.meicmn.mei.shared.att.Articulation
    with _root_.sheet.meicmn.mei.analytical.att.HarmonicFunction
    with _root_.sheet.meicmn.mei.analytical.att.IntervalMelodic
    with _root_.sheet.meicmn.mei.analytical.att.MelodicFunction
    with _root_.sheet.meicmn.mei.cmn.att.note.anl.Cmn
    with _root_.sheet.meicmn.mei.shared.att.FermataPresent
    with _root_.sheet.meicmn.mei.analytical.att.PitchClass
    with _root_.sheet.meicmn.mei.analytical.att.Solfa {}

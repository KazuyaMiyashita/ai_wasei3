package sheet.meicmn.mei.shared.att.chord

/** Logical domain attributes for chord. The artic, dots, and dur attributes encode the written articulations, augmentation dots, and duration values. The beam, fermata, lv, slur, syl, tie, and tuplet attributes may be used to indicate the attachment of these things to this chord. If visual information about these things needs to be recorded, then either the elements corresponding to these attributes or the attributes available in the att.vis.chord class should be employed.
  *
  * Ident: att.chord.log
  * Module: MEI.shared
  */
trait Log
    extends _root_.sheet.meicmn.mei.shared.att.Articulation
    with _root_.sheet.meicmn.mei.shared.att.AugmentDots
    with _root_.sheet.meicmn.mei.cmn.att.chord.log.Cmn
    with _root_.sheet.meicmn.mei.shared.att.Cue
    with _root_.sheet.meicmn.mei.shared.att.duration.Log
    with _root_.sheet.meicmn.mei.shared.att.Event
    with _root_.sheet.meicmn.mei.shared.att.SylText {}

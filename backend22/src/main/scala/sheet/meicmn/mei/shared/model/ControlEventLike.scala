package sheet.meicmn.mei.shared.model

/** Groups elements, such as dynamics, ties, phrase marks, pedal marks, etc., which depend upon other events, such as notes or rests, for their existence.
  *
  * Ident: model.controlEventLike
  * Module: MEI.shared
  */
trait ControlEventLike
    extends _root_.sheet.meicmn.mei.cmn.model.MeasurePart
    with _root_.sheet.meicmn.mei.critapp.model.rdgpart.Music
    with _root_.sheet.meicmn.mei.edittrans.model.edittranspart.Music {}

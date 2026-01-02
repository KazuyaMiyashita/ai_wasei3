package sheet.meicmn.mei.shared.att

/** Visual offset attributes. Some items may have their location recorded in terms of offsets from their programmatically-determined location. The ho attribute records the horizontal offset while vo records the vertical. The to attribute holds a timestamp offset, the most common use of which is as an alternative to the ho attribute.
  *
  * Ident: att.visualOffset
  * Module: MEI.shared
  */
trait VisualOffset
    extends _root_.sheet.meicmn.mei.shared.att.visualoffset.Ho
    with _root_.sheet.meicmn.mei.shared.att.visualoffset.To
    with _root_.sheet.meicmn.mei.shared.att.visualoffset.Vo {}

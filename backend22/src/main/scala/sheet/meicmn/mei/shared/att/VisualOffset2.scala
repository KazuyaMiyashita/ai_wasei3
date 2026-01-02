package sheet.meicmn.mei.shared.att

/** Visual offset attributes. Some items may have their location recorded in terms of pairs of offsets from their programmatically-determined location. The startho and endho attributes record the horizontal offsets of the start and end points of the item, respectively. Similarly, the startvo and endvo attributes record the vertical offsets of the start and end points of the item. The startto and endto attributes hold timestamp offsets, the most common use of which is as alternatives to the ho attributes.
  *
  * Ident: att.visualOffset2
  * Module: MEI.shared
  */
trait VisualOffset2
    extends _root_.sheet.meicmn.mei.shared.att.visualoffset2.Ho
    with _root_.sheet.meicmn.mei.shared.att.visualoffset2.To
    with _root_.sheet.meicmn.mei.shared.att.visualoffset2.Vo {}

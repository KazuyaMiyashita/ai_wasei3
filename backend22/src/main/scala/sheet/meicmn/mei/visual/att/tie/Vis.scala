package sheet.meicmn.mei.visual.att.tie

/** Visual domain attributes. The vo attribute is the vertical offset (from its normal position) of the entire rendered tie. The startho, startvo, endho, and endvo attributes describe the horizontal and vertical offsets of the start and end points of the tie in terms of staff interline distance; that is, in units of 1/2 the distance between adjacent staff lines. Startto and endto describe the start and end points in terms of time; that is, beats.
  *
  * Ident: att.tie.vis
  * Module: MEI.visual
  */
trait Vis
    extends _root_.sheet.meicmn.mei.shared.att.Color
    with _root_.sheet.meicmn.mei.shared.att.Curvature
    with _root_.sheet.meicmn.mei.shared.att.linerend.Base
    with _root_.sheet.meicmn.mei.shared.att.VisualOffset
    with _root_.sheet.meicmn.mei.shared.att.VisualOffset2
    with _root_.sheet.meicmn.mei.shared.att.Xy
    with _root_.sheet.meicmn.mei.shared.att.Xy2 {}

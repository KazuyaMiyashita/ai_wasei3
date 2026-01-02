package sheet.meicmn.mei.shared.model.textphraselike

/** Groups textual elements that occur at the level of individual words or phrases. This class is equivalent to the model.textPhraseLike class without the pb element.
  *
  * Ident: model.textPhraseLike.limited
  * Module: MEI.shared
  */
trait Limited
    extends _root_.sheet.meicmn.mei.critapp.model.rdgpart.Text
    with _root_.sheet.meicmn.mei.edittrans.model.edittranspart.Text
    with _root_.sheet.meicmn.mei.shared.model.TextPhraseLike {}

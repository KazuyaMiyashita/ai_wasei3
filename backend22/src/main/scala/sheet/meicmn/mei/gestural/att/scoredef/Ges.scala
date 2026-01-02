package sheet.meicmn.mei.gestural.att.scoredef

/** Gestural domain attributes for scoreDef. The values set in these attributes act as score-wide defaults for attributes that are not set in descendant elements. For example, the grace attribute value here applies to all the grace attribute values in the score (or, more accurately, until the nextscoreDefelement) without having to individually set each note’s grace attribute value. The midi.* attributes function as default values when creating sounding output. The tune.* attributes provide the capability of recording a tuning reference pitch.
  *
  * Ident: att.scoreDef.ges
  * Module: MEI.gestural
  */
trait Ges
    extends _root_.sheet.meicmn.mei.midi.att.Channelized
    with _root_.sheet.meicmn.mei.midi.att.TimeBase
    with _root_.sheet.meicmn.mei.shared.att.Tuning
    with _root_.sheet.meicmn.mei.midi.att.MidiTempo
    with _root_.sheet.meicmn.mei.shared.att.MmTempo {}

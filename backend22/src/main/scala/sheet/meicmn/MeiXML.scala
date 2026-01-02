package sheet.meicmn

import scala.xml.*

object MeiXML {

  def load(xml: Elem): _root_.sheet.meicmn.mei.shared.Mei = {
    loadElement(xml) match {
      case m: _root_.sheet.meicmn.mei.shared.Mei => m
      case other                                 =>
        throw new IllegalArgumentException(s"Root element must be 'mei', but got ${other.getClass.getSimpleName}")
    }
  }

  def loadElement(node: Node): sheet.meicmn.Element = node match {
    case e: Elem =>
      val attributes: scala.collection.immutable.SeqMap[String, String] = scala.collection.immutable.SeqMap.from(
        e.attributes.map {
          case p: scala.xml.PrefixedAttribute   => (s"${p.pre}:${p.key}", p.value.text)
          case u: scala.xml.UnprefixedAttribute => (u.key, u.value.text)
          case m                                => (m.key, m.value.text)
        },
      )
      val children = e.child.flatMap { n =>
        loadElement(n) match {
          case t: sheet.meicmn.Text if t.value.trim.isEmpty => None // No blank Text nodes are created
          case other                                        => Some(other)
        }
      }.toList

      e.label match {
        case "abbr"              => _root_.sheet.meicmn.mei.edittrans.Abbr(children, attributes)
        case "accMat"            => _root_.sheet.meicmn.mei.msdesc.AccMat(children, attributes)
        case "accessRestrict"    => _root_.sheet.meicmn.mei.header.AccessRestrict(children, attributes)
        case "accid"             => _root_.sheet.meicmn.mei.shared.Accid(children, attributes)
        case "acquisition"       => _root_.sheet.meicmn.mei.header.Acquisition(children, attributes)
        case "actor"             => _root_.sheet.meicmn.mei.shared.Actor(children, attributes)
        case "add"               => _root_.sheet.meicmn.mei.edittrans.Add(children, attributes)
        case "addDesc"           => _root_.sheet.meicmn.mei.msdesc.AddDesc(children, attributes)
        case "addName"           => _root_.sheet.meicmn.mei.namesdates.AddName(children, attributes)
        case "addrLine"          => _root_.sheet.meicmn.mei.shared.AddrLine(children, attributes)
        case "address"           => _root_.sheet.meicmn.mei.shared.Address(children, attributes)
        case "altId"             => _root_.sheet.meicmn.mei.header.AltId(children, attributes)
        case "ambNote"           => _root_.sheet.meicmn.mei.shared.AmbNote(children, attributes)
        case "ambitus"           => _root_.sheet.meicmn.mei.shared.Ambitus(children, attributes)
        case "analytic"          => _root_.sheet.meicmn.mei.shared.Analytic(children, attributes)
        case "anchoredText"      => _root_.sheet.meicmn.mei.usersymbols.AnchoredText(children, attributes)
        case "annot"             => _root_.sheet.meicmn.mei.shared.Annot(children, attributes)
        case "app"               => _root_.sheet.meicmn.mei.critapp.App(children, attributes)
        case "appInfo"           => _root_.sheet.meicmn.mei.header.AppInfo(children, attributes)
        case "application"       => _root_.sheet.meicmn.mei.header.Application(children, attributes)
        case "argument"          => _root_.sheet.meicmn.mei.text.Argument(children, attributes)
        case "arpeg"             => _root_.sheet.meicmn.mei.cmn.Arpeg(children, attributes)
        case "arranger"          => _root_.sheet.meicmn.mei.shared.Arranger(children, attributes)
        case "artic"             => _root_.sheet.meicmn.mei.shared.Artic(children, attributes)
        case "attUsage"          => _root_.sheet.meicmn.mei.header.AttUsage(children, attributes)
        case "attacca"           => _root_.sheet.meicmn.mei.cmn.Attacca(children, attributes)
        case "audience"          => _root_.sheet.meicmn.mei.header.Audience(children, attributes)
        case "author"            => _root_.sheet.meicmn.mei.shared.Author(children, attributes)
        case "avFile"            => _root_.sheet.meicmn.mei.performance.AvFile(children, attributes)
        case "availability"      => _root_.sheet.meicmn.mei.header.Availability(children, attributes)
        case "bTrem"             => _root_.sheet.meicmn.mei.cmn.BTrem(children, attributes)
        case "back"              => _root_.sheet.meicmn.mei.text.Back(children, attributes)
        case "barLine"           => _root_.sheet.meicmn.mei.shared.BarLine(children, attributes)
        case "barre"             => _root_.sheet.meicmn.mei.stringtab.Barre(children, attributes)
        case "beam"              => _root_.sheet.meicmn.mei.cmn.Beam(children, attributes)
        case "beamSpan"          => _root_.sheet.meicmn.mei.cmn.BeamSpan(children, attributes)
        case "beatRpt"           => _root_.sheet.meicmn.mei.cmn.BeatRpt(children, attributes)
        case "bend"              => _root_.sheet.meicmn.mei.cmn.Bend(children, attributes)
        case "bibl"              => _root_.sheet.meicmn.mei.shared.Bibl(children, attributes)
        case "biblList"          => _root_.sheet.meicmn.mei.shared.BiblList(children, attributes)
        case "biblScope"         => _root_.sheet.meicmn.mei.shared.BiblScope(children, attributes)
        case "biblStruct"        => _root_.sheet.meicmn.mei.shared.BiblStruct(children, attributes)
        case "bifolium"          => _root_.sheet.meicmn.mei.header.Bifolium(children, attributes)
        case "binding"           => _root_.sheet.meicmn.mei.msdesc.Binding(children, attributes)
        case "bindingDesc"       => _root_.sheet.meicmn.mei.msdesc.BindingDesc(children, attributes)
        case "bloc"              => _root_.sheet.meicmn.mei.namesdates.Bloc(children, attributes)
        case "body"              => _root_.sheet.meicmn.mei.shared.Body(children, attributes)
        case "bracketSpan"       => _root_.sheet.meicmn.mei.cmn.BracketSpan(children, attributes)
        case "breath"            => _root_.sheet.meicmn.mei.cmn.Breath(children, attributes)
        case "byline"            => _root_.sheet.meicmn.mei.header.Byline(children, attributes)
        case "caesura"           => _root_.sheet.meicmn.mei.shared.Caesura(children, attributes)
        case "caption"           => _root_.sheet.meicmn.mei.shared.Caption(children, attributes)
        case "captureMode"       => _root_.sheet.meicmn.mei.header.CaptureMode(children, attributes)
        case "carrierForm"       => _root_.sheet.meicmn.mei.header.CarrierForm(children, attributes)
        case "castGrp"           => _root_.sheet.meicmn.mei.shared.CastGrp(children, attributes)
        case "castItem"          => _root_.sheet.meicmn.mei.shared.CastItem(children, attributes)
        case "castList"          => _root_.sheet.meicmn.mei.shared.CastList(children, attributes)
        case "catRel"            => _root_.sheet.meicmn.mei.header.CatRel(children, attributes)
        case "catchwords"        => _root_.sheet.meicmn.mei.msdesc.Catchwords(children, attributes)
        case "category"          => _root_.sheet.meicmn.mei.header.Category(children, attributes)
        case "cb"                => _root_.sheet.meicmn.mei.shared.Cb(children, attributes)
        case "cc"                => _root_.sheet.meicmn.mei.midi.Cc(children, attributes)
        case "chan"              => _root_.sheet.meicmn.mei.midi.Chan(children, attributes)
        case "chanPr"            => _root_.sheet.meicmn.mei.midi.ChanPr(children, attributes)
        case "change"            => _root_.sheet.meicmn.mei.header.Change(children, attributes)
        case "changeDesc"        => _root_.sheet.meicmn.mei.header.ChangeDesc(children, attributes)
        case "choice"            => _root_.sheet.meicmn.mei.edittrans.Choice(children, attributes)
        case "chord"             => _root_.sheet.meicmn.mei.shared.Chord(children, attributes)
        case "chordDef"          => _root_.sheet.meicmn.mei.harmony.ChordDef(children, attributes)
        case "chordMember"       => _root_.sheet.meicmn.mei.harmony.ChordMember(children, attributes)
        case "chordTable"        => _root_.sheet.meicmn.mei.harmony.ChordTable(children, attributes)
        case "classDecls"        => _root_.sheet.meicmn.mei.header.ClassDecls(children, attributes)
        case "classification"    => _root_.sheet.meicmn.mei.header.Classification(children, attributes)
        case "clef"              => _root_.sheet.meicmn.mei.shared.Clef(children, attributes)
        case "clefGrp"           => _root_.sheet.meicmn.mei.shared.ClefGrp(children, attributes)
        case "clip"              => _root_.sheet.meicmn.mei.performance.Clip(children, attributes)
        case "colLayout"         => _root_.sheet.meicmn.mei.shared.ColLayout(children, attributes)
        case "collation"         => _root_.sheet.meicmn.mei.msdesc.Collation(children, attributes)
        case "colophon"          => _root_.sheet.meicmn.mei.msdesc.Colophon(children, attributes)
        case "componentList"     => _root_.sheet.meicmn.mei.header.ComponentList(children, attributes)
        case "composer"          => _root_.sheet.meicmn.mei.shared.Composer(children, attributes)
        case "condition"         => _root_.sheet.meicmn.mei.header.Condition(children, attributes)
        case "contentItem"       => _root_.sheet.meicmn.mei.header.ContentItem(children, attributes)
        case "contents"          => _root_.sheet.meicmn.mei.header.Contents(children, attributes)
        case "context"           => _root_.sheet.meicmn.mei.header.Context(children, attributes)
        case "contributor"       => _root_.sheet.meicmn.mei.shared.Contributor(children, attributes)
        case "corpName"          => _root_.sheet.meicmn.mei.namesdates.CorpName(children, attributes)
        case "corr"              => _root_.sheet.meicmn.mei.edittrans.Corr(children, attributes)
        case "correction"        => _root_.sheet.meicmn.mei.header.Correction(children, attributes)
        case "country"           => _root_.sheet.meicmn.mei.namesdates.Country(children, attributes)
        case "course"            => _root_.sheet.meicmn.mei.stringtab.Course(children, attributes)
        case "cpMark"            => _root_.sheet.meicmn.mei.edittrans.CpMark(children, attributes)
        case "creation"          => _root_.sheet.meicmn.mei.shared.Creation(children, attributes)
        case "cue"               => _root_.sheet.meicmn.mei.midi.Cue(children, attributes)
        case "curve"             => _root_.sheet.meicmn.mei.usersymbols.Curve(children, attributes)
        case "custos"            => _root_.sheet.meicmn.mei.shared.Custos(children, attributes)
        case "cutout"            => _root_.sheet.meicmn.mei.header.Cutout(children, attributes)
        case "damage"            => _root_.sheet.meicmn.mei.edittrans.Damage(children, attributes)
        case "date"              => _root_.sheet.meicmn.mei.shared.Date(children, attributes)
        case "decoDesc"          => _root_.sheet.meicmn.mei.msdesc.DecoDesc(children, attributes)
        case "decoNote"          => _root_.sheet.meicmn.mei.msdesc.DecoNote(children, attributes)
        case "dedicatee"         => _root_.sheet.meicmn.mei.shared.Dedicatee(children, attributes)
        case "dedication"        => _root_.sheet.meicmn.mei.header.Dedication(children, attributes)
        case "del"               => _root_.sheet.meicmn.mei.edittrans.Del(children, attributes)
        case "depth"             => _root_.sheet.meicmn.mei.shared.Depth(children, attributes)
        case "desc"              => _root_.sheet.meicmn.mei.shared.Desc(children, attributes)
        case "dim"               => _root_.sheet.meicmn.mei.shared.Dim(children, attributes)
        case "dimensions"        => _root_.sheet.meicmn.mei.shared.Dimensions(children, attributes)
        case "dir"               => _root_.sheet.meicmn.mei.shared.Dir(children, attributes)
        case "distributor"       => _root_.sheet.meicmn.mei.shared.Distributor(children, attributes)
        case "district"          => _root_.sheet.meicmn.mei.namesdates.District(children, attributes)
        case "div"               => _root_.sheet.meicmn.mei.shared.Div(children, attributes)
        case "domainsDecl"       => _root_.sheet.meicmn.mei.header.DomainsDecl(children, attributes)
        case "dot"               => _root_.sheet.meicmn.mei.shared.Dot(children, attributes)
        case "dynam"             => _root_.sheet.meicmn.mei.shared.Dynam(children, attributes)
        case "edition"           => _root_.sheet.meicmn.mei.shared.Edition(children, attributes)
        case "editionStmt"       => _root_.sheet.meicmn.mei.header.EditionStmt(children, attributes)
        case "editor"            => _root_.sheet.meicmn.mei.shared.Editor(children, attributes)
        case "editorialDecl"     => _root_.sheet.meicmn.mei.header.EditorialDecl(children, attributes)
        case "encodingDesc"      => _root_.sheet.meicmn.mei.header.EncodingDesc(children, attributes)
        case "ending"            => _root_.sheet.meicmn.mei.shared.Ending(children, attributes)
        case "epigraph"          => _root_.sheet.meicmn.mei.text.Epigraph(children, attributes)
        case "event"             => _root_.sheet.meicmn.mei.shared.Event(children, attributes)
        case "eventList"         => _root_.sheet.meicmn.mei.shared.EventList(children, attributes)
        case "exhibHist"         => _root_.sheet.meicmn.mei.header.ExhibHist(children, attributes)
        case "expan"             => _root_.sheet.meicmn.mei.edittrans.Expan(children, attributes)
        case "expansion"         => _root_.sheet.meicmn.mei.shared.Expansion(children, attributes)
        case "explicit"          => _root_.sheet.meicmn.mei.msdesc.Explicit(children, attributes)
        case "expression"        => _root_.sheet.meicmn.mei.frbr.Expression(children, attributes)
        case "expressionList"    => _root_.sheet.meicmn.mei.frbr.ExpressionList(children, attributes)
        case "extData"           => _root_.sheet.meicmn.mei.shared.ExtData(children, attributes)
        case "extMeta"           => _root_.sheet.meicmn.mei.header.ExtMeta(children, attributes)
        case "extent"            => _root_.sheet.meicmn.mei.shared.Extent(children, attributes)
        case "f"                 => _root_.sheet.meicmn.mei.harmony.F(children, attributes)
        case "fTrem"             => _root_.sheet.meicmn.mei.cmn.FTrem(children, attributes)
        case "facsimile"         => _root_.sheet.meicmn.mei.facsimile.Facsimile(children, attributes)
        case "famName"           => _root_.sheet.meicmn.mei.namesdates.FamName(children, attributes)
        case "fb"                => _root_.sheet.meicmn.mei.harmony.Fb(children, attributes)
        case "fermata"           => _root_.sheet.meicmn.mei.cmn.Fermata(children, attributes)
        case "fig"               => _root_.sheet.meicmn.mei.figtable.Fig(children, attributes)
        case "figDesc"           => _root_.sheet.meicmn.mei.figtable.FigDesc(children, attributes)
        case "fileChar"          => _root_.sheet.meicmn.mei.header.FileChar(children, attributes)
        case "fileDesc"          => _root_.sheet.meicmn.mei.header.FileDesc(children, attributes)
        case "fing"              => _root_.sheet.meicmn.mei.fingering.Fing(children, attributes)
        case "fingGrp"           => _root_.sheet.meicmn.mei.fingering.FingGrp(children, attributes)
        case "foliaDesc"         => _root_.sheet.meicmn.mei.header.FoliaDesc(children, attributes)
        case "foliation"         => _root_.sheet.meicmn.mei.msdesc.Foliation(children, attributes)
        case "folium"            => _root_.sheet.meicmn.mei.header.Folium(children, attributes)
        case "foreName"          => _root_.sheet.meicmn.mei.namesdates.ForeName(children, attributes)
        case "front"             => _root_.sheet.meicmn.mei.text.Front(children, attributes)
        case "funder"            => _root_.sheet.meicmn.mei.shared.Funder(children, attributes)
        case "gap"               => _root_.sheet.meicmn.mei.edittrans.Gap(children, attributes)
        case "genDesc"           => _root_.sheet.meicmn.mei.genetic.GenDesc(children, attributes)
        case "genName"           => _root_.sheet.meicmn.mei.namesdates.GenName(children, attributes)
        case "genState"          => _root_.sheet.meicmn.mei.genetic.GenState(children, attributes)
        case "genre"             => _root_.sheet.meicmn.mei.shared.Genre(children, attributes)
        case "geogFeat"          => _root_.sheet.meicmn.mei.namesdates.GeogFeat(children, attributes)
        case "geogName"          => _root_.sheet.meicmn.mei.namesdates.GeogName(children, attributes)
        case "gliss"             => _root_.sheet.meicmn.mei.cmn.Gliss(children, attributes)
        case "graceGrp"          => _root_.sheet.meicmn.mei.cmn.GraceGrp(children, attributes)
        case "graphic"           => _root_.sheet.meicmn.mei.figtable.Graphic(children, attributes)
        case "group"             => _root_.sheet.meicmn.mei.shared.Group(children, attributes)
        case "grpSym"            => _root_.sheet.meicmn.mei.shared.GrpSym(children, attributes)
        case "hairpin"           => _root_.sheet.meicmn.mei.cmn.Hairpin(children, attributes)
        case "halfmRpt"          => _root_.sheet.meicmn.mei.cmn.HalfmRpt(children, attributes)
        case "hand"              => _root_.sheet.meicmn.mei.header.Hand(children, attributes)
        case "handList"          => _root_.sheet.meicmn.mei.header.HandList(children, attributes)
        case "handShift"         => _root_.sheet.meicmn.mei.edittrans.HandShift(children, attributes)
        case "harm"              => _root_.sheet.meicmn.mei.harmony.Harm(children, attributes)
        case "harpPedal"         => _root_.sheet.meicmn.mei.cmn.HarpPedal(children, attributes)
        case "head"              => _root_.sheet.meicmn.mei.shared.Head(children, attributes)
        case "height"            => _root_.sheet.meicmn.mei.shared.Height(children, attributes)
        case "heraldry"          => _root_.sheet.meicmn.mei.msdesc.Heraldry(children, attributes)
        case "hex"               => _root_.sheet.meicmn.mei.midi.Hex(children, attributes)
        case "history"           => _root_.sheet.meicmn.mei.header.History(children, attributes)
        case "identifier"        => _root_.sheet.meicmn.mei.shared.Identifier(children, attributes)
        case "imprimatur"        => _root_.sheet.meicmn.mei.text.Imprimatur(children, attributes)
        case "imprint"           => _root_.sheet.meicmn.mei.shared.Imprint(children, attributes)
        case "incip"             => _root_.sheet.meicmn.mei.shared.Incip(children, attributes)
        case "incipCode"         => _root_.sheet.meicmn.mei.header.IncipCode(children, attributes)
        case "incipText"         => _root_.sheet.meicmn.mei.header.IncipText(children, attributes)
        case "inscription"       => _root_.sheet.meicmn.mei.header.Inscription(children, attributes)
        case "instrDef"          => _root_.sheet.meicmn.mei.midi.InstrDef(children, attributes)
        case "instrGrp"          => _root_.sheet.meicmn.mei.midi.InstrGrp(children, attributes)
        case "interpretation"    => _root_.sheet.meicmn.mei.header.Interpretation(children, attributes)
        case "item"              => _root_.sheet.meicmn.mei.frbr.Item(children, attributes)
        case "itemList"          => _root_.sheet.meicmn.mei.frbr.ItemList(children, attributes)
        case "key"               => _root_.sheet.meicmn.mei.header.Key(children, attributes)
        case "keyAccid"          => _root_.sheet.meicmn.mei.shared.KeyAccid(children, attributes)
        case "keySig"            => _root_.sheet.meicmn.mei.shared.KeySig(children, attributes)
        case "l"                 => _root_.sheet.meicmn.mei.text.L(children, attributes)
        case "label"             => _root_.sheet.meicmn.mei.shared.Label(children, attributes)
        case "labelAbbr"         => _root_.sheet.meicmn.mei.shared.LabelAbbr(children, attributes)
        case "langUsage"         => _root_.sheet.meicmn.mei.header.LangUsage(children, attributes)
        case "language"          => _root_.sheet.meicmn.mei.header.Language(children, attributes)
        case "layer"             => _root_.sheet.meicmn.mei.shared.Layer(children, attributes)
        case "layerDef"          => _root_.sheet.meicmn.mei.shared.LayerDef(children, attributes)
        case "layout"            => _root_.sheet.meicmn.mei.msdesc.Layout(children, attributes)
        case "layoutDesc"        => _root_.sheet.meicmn.mei.msdesc.LayoutDesc(children, attributes)
        case "lb"                => _root_.sheet.meicmn.mei.shared.Lb(children, attributes)
        case "lem"               => _root_.sheet.meicmn.mei.critapp.Lem(children, attributes)
        case "lg"                => _root_.sheet.meicmn.mei.shared.Lg(children, attributes)
        case "li"                => _root_.sheet.meicmn.mei.text.Li(children, attributes)
        case "librettist"        => _root_.sheet.meicmn.mei.shared.Librettist(children, attributes)
        case "line"              => _root_.sheet.meicmn.mei.usersymbols.Line(children, attributes)
        case "list"              => _root_.sheet.meicmn.mei.text.List(children, attributes)
        case "locus"             => _root_.sheet.meicmn.mei.msdesc.Locus(children, attributes)
        case "locusGrp"          => _root_.sheet.meicmn.mei.msdesc.LocusGrp(children, attributes)
        case "lv"                => _root_.sheet.meicmn.mei.cmn.Lv(children, attributes)
        case "lyricist"          => _root_.sheet.meicmn.mei.shared.Lyricist(children, attributes)
        case "mNum"              => _root_.sheet.meicmn.mei.cmn.MNum(children, attributes)
        case "mRest"             => _root_.sheet.meicmn.mei.cmn.MRest(children, attributes)
        case "mRpt"              => _root_.sheet.meicmn.mei.cmn.MRpt(children, attributes)
        case "mRpt2"             => _root_.sheet.meicmn.mei.cmn.MRpt2(children, attributes)
        case "mSpace"            => _root_.sheet.meicmn.mei.cmn.MSpace(children, attributes)
        case "manifestation"     => _root_.sheet.meicmn.mei.frbr.Manifestation(children, attributes)
        case "manifestationList" => _root_.sheet.meicmn.mei.frbr.ManifestationList(children, attributes)
        case "mapping"           => _root_.sheet.meicmn.mei.usersymbols.Mapping(children, attributes)
        case "marker"            => _root_.sheet.meicmn.mei.midi.Marker(children, attributes)
        case "mdiv"              => _root_.sheet.meicmn.mei.shared.Mdiv(children, attributes)
        case "measure"           => _root_.sheet.meicmn.mei.cmn.Measure(children, attributes)
        case "mei"               => _root_.sheet.meicmn.mei.shared.Mei(children, attributes)
        case "meiCorpus"         => _root_.sheet.meicmn.mei.corpus.MeiCorpus(children, attributes)
        case "meiHead"           => _root_.sheet.meicmn.mei.header.MeiHead(children, attributes)
        case "mensuration"       => _root_.sheet.meicmn.mei.header.Mensuration(children, attributes)
        case "metaMark"          => _root_.sheet.meicmn.mei.edittrans.MetaMark(children, attributes)
        case "metaText"          => _root_.sheet.meicmn.mei.midi.MetaText(children, attributes)
        case "meter"             => _root_.sheet.meicmn.mei.header.Meter(children, attributes)
        case "meterSig"          => _root_.sheet.meicmn.mei.cmn.MeterSig(children, attributes)
        case "meterSigGrp"       => _root_.sheet.meicmn.mei.cmn.MeterSigGrp(children, attributes)
        case "midi"              => _root_.sheet.meicmn.mei.midi.Midi(children, attributes)
        case "monogr"            => _root_.sheet.meicmn.mei.shared.Monogr(children, attributes)
        case "mordent"           => _root_.sheet.meicmn.mei.cmnornaments.Mordent(children, attributes)
        case "multiRest"         => _root_.sheet.meicmn.mei.cmn.MultiRest(children, attributes)
        case "multiRpt"          => _root_.sheet.meicmn.mei.cmn.MultiRpt(children, attributes)
        case "music"             => _root_.sheet.meicmn.mei.shared.Music(children, attributes)
        case "name"              => _root_.sheet.meicmn.mei.shared.Name(children, attributes)
        case "nameLink"          => _root_.sheet.meicmn.mei.namesdates.NameLink(children, attributes)
        case "namespace"         => _root_.sheet.meicmn.mei.header.Namespace(children, attributes)
        case "normalization"     => _root_.sheet.meicmn.mei.header.Normalization(children, attributes)
        case "note"              => _root_.sheet.meicmn.mei.shared.Note(children, attributes)
        case "noteOff"           => _root_.sheet.meicmn.mei.midi.NoteOff(children, attributes)
        case "noteOn"            => _root_.sheet.meicmn.mei.midi.NoteOn(children, attributes)
        case "notesStmt"         => _root_.sheet.meicmn.mei.header.NotesStmt(children, attributes)
        case "num"               => _root_.sheet.meicmn.mei.shared.Num(children, attributes)
        case "oLayer"            => _root_.sheet.meicmn.mei.cmn.OLayer(children, attributes)
        case "oStaff"            => _root_.sheet.meicmn.mei.cmn.OStaff(children, attributes)
        case "octave"            => _root_.sheet.meicmn.mei.cmn.Octave(children, attributes)
        case "orig"              => _root_.sheet.meicmn.mei.edittrans.Orig(children, attributes)
        case "ornam"             => _root_.sheet.meicmn.mei.shared.Ornam(children, attributes)
        case "ossia"             => _root_.sheet.meicmn.mei.cmn.Ossia(children, attributes)
        case "otherChar"         => _root_.sheet.meicmn.mei.header.OtherChar(children, attributes)
        case "p"                 => _root_.sheet.meicmn.mei.shared.P(children, attributes)
        case "pad"               => _root_.sheet.meicmn.mei.shared.Pad(children, attributes)
        case "part"              => _root_.sheet.meicmn.mei.shared.Part(children, attributes)
        case "parts"             => _root_.sheet.meicmn.mei.shared.Parts(children, attributes)
        case "patch"             => _root_.sheet.meicmn.mei.header.Patch(children, attributes)
        case "pb"                => _root_.sheet.meicmn.mei.shared.Pb(children, attributes)
        case "pedal"             => _root_.sheet.meicmn.mei.cmn.Pedal(children, attributes)
        case "perfDuration"      => _root_.sheet.meicmn.mei.header.PerfDuration(children, attributes)
        case "perfMedium"        => _root_.sheet.meicmn.mei.header.PerfMedium(children, attributes)
        case "perfRes"           => _root_.sheet.meicmn.mei.header.PerfRes(children, attributes)
        case "perfResList"       => _root_.sheet.meicmn.mei.header.PerfResList(children, attributes)
        case "performance"       => _root_.sheet.meicmn.mei.performance.Performance(children, attributes)
        case "periodName"        => _root_.sheet.meicmn.mei.namesdates.PeriodName(children, attributes)
        case "persName"          => _root_.sheet.meicmn.mei.namesdates.PersName(children, attributes)
        case "pgDesc"            => _root_.sheet.meicmn.mei.shared.PgDesc(children, attributes)
        case "pgFoot"            => _root_.sheet.meicmn.mei.shared.PgFoot(children, attributes)
        case "pgHead"            => _root_.sheet.meicmn.mei.shared.PgHead(children, attributes)
        case "phrase"            => _root_.sheet.meicmn.mei.shared.Phrase(children, attributes)
        case "physDesc"          => _root_.sheet.meicmn.mei.header.PhysDesc(children, attributes)
        case "physLoc"           => _root_.sheet.meicmn.mei.shared.PhysLoc(children, attributes)
        case "physMedium"        => _root_.sheet.meicmn.mei.header.PhysMedium(children, attributes)
        case "plateNum"          => _root_.sheet.meicmn.mei.header.PlateNum(children, attributes)
        case "playingSpeed"      => _root_.sheet.meicmn.mei.header.PlayingSpeed(children, attributes)
        case "port"              => _root_.sheet.meicmn.mei.midi.Port(children, attributes)
        case "postBox"           => _root_.sheet.meicmn.mei.namesdates.PostBox(children, attributes)
        case "postCode"          => _root_.sheet.meicmn.mei.namesdates.PostCode(children, attributes)
        case "price"             => _root_.sheet.meicmn.mei.header.Price(children, attributes)
        case "prog"              => _root_.sheet.meicmn.mei.midi.Prog(children, attributes)
        case "projectDesc"       => _root_.sheet.meicmn.mei.header.ProjectDesc(children, attributes)
        case "propName"          => _root_.sheet.meicmn.mei.usersymbols.PropName(children, attributes)
        case "propValue"         => _root_.sheet.meicmn.mei.usersymbols.PropValue(children, attributes)
        case "provenance"        => _root_.sheet.meicmn.mei.header.Provenance(children, attributes)
        case "ptr"               => _root_.sheet.meicmn.mei.ptrref.Ptr(children, attributes)
        case "pubPlace"          => _root_.sheet.meicmn.mei.shared.PubPlace(children, attributes)
        case "pubStmt"           => _root_.sheet.meicmn.mei.header.PubStmt(children, attributes)
        case "publisher"         => _root_.sheet.meicmn.mei.shared.Publisher(children, attributes)
        case "q"                 => _root_.sheet.meicmn.mei.text.Q(children, attributes)
        case "quote"             => _root_.sheet.meicmn.mei.text.Quote(children, attributes)
        case "rdg"               => _root_.sheet.meicmn.mei.critapp.Rdg(children, attributes)
        case "recipient"         => _root_.sheet.meicmn.mei.shared.Recipient(children, attributes)
        case "recording"         => _root_.sheet.meicmn.mei.performance.Recording(children, attributes)
        case "ref"               => _root_.sheet.meicmn.mei.ptrref.Ref(children, attributes)
        case "refrain"           => _root_.sheet.meicmn.mei.lyrics.Refrain(children, attributes)
        case "reg"               => _root_.sheet.meicmn.mei.edittrans.Reg(children, attributes)
        case "region"            => _root_.sheet.meicmn.mei.namesdates.Region(children, attributes)
        case "reh"               => _root_.sheet.meicmn.mei.cmn.Reh(children, attributes)
        case "relatedItem"       => _root_.sheet.meicmn.mei.shared.RelatedItem(children, attributes)
        case "relation"          => _root_.sheet.meicmn.mei.shared.Relation(children, attributes)
        case "relationList"      => _root_.sheet.meicmn.mei.shared.RelationList(children, attributes)
        case "rend"              => _root_.sheet.meicmn.mei.shared.Rend(children, attributes)
        case "repeatMark"        => _root_.sheet.meicmn.mei.cmn.RepeatMark(children, attributes)
        case "repository"        => _root_.sheet.meicmn.mei.shared.Repository(children, attributes)
        case "resp"              => _root_.sheet.meicmn.mei.shared.Resp(children, attributes)
        case "respStmt"          => _root_.sheet.meicmn.mei.shared.RespStmt(children, attributes)
        case "rest"              => _root_.sheet.meicmn.mei.shared.Rest(children, attributes)
        case "restore"           => _root_.sheet.meicmn.mei.edittrans.Restore(children, attributes)
        case "revisionDesc"      => _root_.sheet.meicmn.mei.header.RevisionDesc(children, attributes)
        case "role"              => _root_.sheet.meicmn.mei.shared.Role(children, attributes)
        case "roleDesc"          => _root_.sheet.meicmn.mei.shared.RoleDesc(children, attributes)
        case "roleName"          => _root_.sheet.meicmn.mei.namesdates.RoleName(children, attributes)
        case "rubric"            => _root_.sheet.meicmn.mei.msdesc.Rubric(children, attributes)
        case "samplingDecl"      => _root_.sheet.meicmn.mei.header.SamplingDecl(children, attributes)
        case "sb"                => _root_.sheet.meicmn.mei.shared.Sb(children, attributes)
        case "score"             => _root_.sheet.meicmn.mei.shared.Score(children, attributes)
        case "scoreDef"          => _root_.sheet.meicmn.mei.shared.ScoreDef(children, attributes)
        case "scoreFormat"       => _root_.sheet.meicmn.mei.header.ScoreFormat(children, attributes)
        case "scriptDesc"        => _root_.sheet.meicmn.mei.msdesc.ScriptDesc(children, attributes)
        case "scriptNote"        => _root_.sheet.meicmn.mei.msdesc.ScriptNote(children, attributes)
        case "seal"              => _root_.sheet.meicmn.mei.msdesc.Seal(children, attributes)
        case "sealDesc"          => _root_.sheet.meicmn.mei.msdesc.SealDesc(children, attributes)
        case "secFolio"          => _root_.sheet.meicmn.mei.msdesc.SecFolio(children, attributes)
        case "section"           => _root_.sheet.meicmn.mei.shared.Section(children, attributes)
        case "seg"               => _root_.sheet.meicmn.mei.text.Seg(children, attributes)
        case "segmentation"      => _root_.sheet.meicmn.mei.header.Segmentation(children, attributes)
        case "seqNum"            => _root_.sheet.meicmn.mei.midi.SeqNum(children, attributes)
        case "series"            => _root_.sheet.meicmn.mei.shared.Series(children, attributes)
        case "seriesStmt"        => _root_.sheet.meicmn.mei.header.SeriesStmt(children, attributes)
        case "settlement"        => _root_.sheet.meicmn.mei.namesdates.Settlement(children, attributes)
        case "sic"               => _root_.sheet.meicmn.mei.edittrans.Sic(children, attributes)
        case "signatures"        => _root_.sheet.meicmn.mei.msdesc.Signatures(children, attributes)
        case "slur"              => _root_.sheet.meicmn.mei.cmn.Slur(children, attributes)
        case "soundChan"         => _root_.sheet.meicmn.mei.header.SoundChan(children, attributes)
        case "source"            => _root_.sheet.meicmn.mei.header.Source(children, attributes)
        case "sourceDesc"        => _root_.sheet.meicmn.mei.header.SourceDesc(children, attributes)
        case "sp"                => _root_.sheet.meicmn.mei.drama.Sp(children, attributes)
        case "space"             => _root_.sheet.meicmn.mei.shared.Space(children, attributes)
        case "speaker"           => _root_.sheet.meicmn.mei.shared.Speaker(children, attributes)
        case "specRepro"         => _root_.sheet.meicmn.mei.header.SpecRepro(children, attributes)
        case "sponsor"           => _root_.sheet.meicmn.mei.shared.Sponsor(children, attributes)
        case "stack"             => _root_.sheet.meicmn.mei.shared.Stack(children, attributes)
        case "staff"             => _root_.sheet.meicmn.mei.shared.Staff(children, attributes)
        case "staffDef"          => _root_.sheet.meicmn.mei.shared.StaffDef(children, attributes)
        case "staffGrp"          => _root_.sheet.meicmn.mei.shared.StaffGrp(children, attributes)
        case "stageDir"          => _root_.sheet.meicmn.mei.drama.StageDir(children, attributes)
        case "stamp"             => _root_.sheet.meicmn.mei.msdesc.Stamp(children, attributes)
        case "stdVals"           => _root_.sheet.meicmn.mei.header.StdVals(children, attributes)
        case "street"            => _root_.sheet.meicmn.mei.namesdates.Street(children, attributes)
        case "string"            => _root_.sheet.meicmn.mei.stringtab.String(children, attributes)
        case "styleName"         => _root_.sheet.meicmn.mei.namesdates.StyleName(children, attributes)
        case "subst"             => _root_.sheet.meicmn.mei.edittrans.Subst(children, attributes)
        case "supplied"          => _root_.sheet.meicmn.mei.edittrans.Supplied(children, attributes)
        case "support"           => _root_.sheet.meicmn.mei.msdesc.Support(children, attributes)
        case "supportDesc"       => _root_.sheet.meicmn.mei.msdesc.SupportDesc(children, attributes)
        case "surface"           => _root_.sheet.meicmn.mei.facsimile.Surface(children, attributes)
        case "syl"               => _root_.sheet.meicmn.mei.shared.Syl(children, attributes)
        case "symName"           => _root_.sheet.meicmn.mei.usersymbols.SymName(children, attributes)
        case "symProp"           => _root_.sheet.meicmn.mei.usersymbols.SymProp(children, attributes)
        case "symbol"            => _root_.sheet.meicmn.mei.shared.Symbol(children, attributes)
        case "symbolDef"         => _root_.sheet.meicmn.mei.usersymbols.SymbolDef(children, attributes)
        case "symbolTable"       => _root_.sheet.meicmn.mei.usersymbols.SymbolTable(children, attributes)
        case "sysReq"            => _root_.sheet.meicmn.mei.header.SysReq(children, attributes)
        case "tabDurSym"         => _root_.sheet.meicmn.mei.stringtab.TabDurSym(children, attributes)
        case "tabGrp"            => _root_.sheet.meicmn.mei.stringtab.TabGrp(children, attributes)
        case "table"             => _root_.sheet.meicmn.mei.figtable.Table(children, attributes)
        case "tagUsage"          => _root_.sheet.meicmn.mei.header.TagUsage(children, attributes)
        case "tagsDecl"          => _root_.sheet.meicmn.mei.header.TagsDecl(children, attributes)
        case "taxonomy"          => _root_.sheet.meicmn.mei.header.Taxonomy(children, attributes)
        case "td"                => _root_.sheet.meicmn.mei.figtable.Td(children, attributes)
        case "tempo"             => _root_.sheet.meicmn.mei.shared.Tempo(children, attributes)
        case "term"              => _root_.sheet.meicmn.mei.shared.Term(children, attributes)
        case "termList"          => _root_.sheet.meicmn.mei.header.TermList(children, attributes)
        case "textLang"          => _root_.sheet.meicmn.mei.shared.TextLang(children, attributes)
        case "th"                => _root_.sheet.meicmn.mei.figtable.Th(children, attributes)
        case "tie"               => _root_.sheet.meicmn.mei.cmn.Tie(children, attributes)
        case "title"             => _root_.sheet.meicmn.mei.shared.Title(children, attributes)
        case "titlePage"         => _root_.sheet.meicmn.mei.shared.TitlePage(children, attributes)
        case "titlePart"         => _root_.sheet.meicmn.mei.shared.TitlePart(children, attributes)
        case "titleStmt"         => _root_.sheet.meicmn.mei.header.TitleStmt(children, attributes)
        case "tr"                => _root_.sheet.meicmn.mei.figtable.Tr(children, attributes)
        case "trackConfig"       => _root_.sheet.meicmn.mei.header.TrackConfig(children, attributes)
        case "treatHist"         => _root_.sheet.meicmn.mei.header.TreatHist(children, attributes)
        case "treatSched"        => _root_.sheet.meicmn.mei.header.TreatSched(children, attributes)
        case "trill"             => _root_.sheet.meicmn.mei.cmnornaments.Trill(children, attributes)
        case "trkName"           => _root_.sheet.meicmn.mei.midi.TrkName(children, attributes)
        case "tuning"            => _root_.sheet.meicmn.mei.shared.Tuning(children, attributes)
        case "tuplet"            => _root_.sheet.meicmn.mei.cmn.Tuplet(children, attributes)
        case "tupletSpan"        => _root_.sheet.meicmn.mei.cmn.TupletSpan(children, attributes)
        case "turn"              => _root_.sheet.meicmn.mei.cmnornaments.Turn(children, attributes)
        case "typeDesc"          => _root_.sheet.meicmn.mei.msdesc.TypeDesc(children, attributes)
        case "typeNote"          => _root_.sheet.meicmn.mei.msdesc.TypeNote(children, attributes)
        case "unclear"           => _root_.sheet.meicmn.mei.edittrans.Unclear(children, attributes)
        case "unpub"             => _root_.sheet.meicmn.mei.header.Unpub(children, attributes)
        case "useRestrict"       => _root_.sheet.meicmn.mei.header.UseRestrict(children, attributes)
        case "vel"               => _root_.sheet.meicmn.mei.midi.Vel(children, attributes)
        case "verse"             => _root_.sheet.meicmn.mei.lyrics.Verse(children, attributes)
        case "volta"             => _root_.sheet.meicmn.mei.lyrics.Volta(children, attributes)
        case "watermark"         => _root_.sheet.meicmn.mei.header.Watermark(children, attributes)
        case "when"              => _root_.sheet.meicmn.mei.performance.When(children, attributes)
        case "width"             => _root_.sheet.meicmn.mei.shared.Width(children, attributes)
        case "work"              => _root_.sheet.meicmn.mei.header.Work(children, attributes)
        case "workList"          => _root_.sheet.meicmn.mei.header.WorkList(children, attributes)
        case "zone"              => _root_.sheet.meicmn.mei.facsimile.Zone(children, attributes)
        case _                   => throw new IllegalArgumentException(s"Unknown tag: ${e.label}")
      }

    case t: scala.xml.Text => sheet.meicmn.Text(t.text)
    case _                 => null // Ignore comments, etc.
  }

  private def mkAttributes(attributes: scala.collection.immutable.SeqMap[String, String]): MetaData = {
    attributes.foldRight[MetaData](Null) { case ((k, v), acc) =>
      if (k.contains(":")) {
        val parts = k.split(":", 2)
        new PrefixedAttribute(parts(0), parts(1), v, acc)
      } else {
        new UnprefixedAttribute(k, v, acc)
      }
    }
  }

  private val meiScope = scala.xml.NamespaceBinding(null, "http://www.music-encoding.org/ns/mei", scala.xml.TopScope)

  def toXml(element: sheet.meicmn.Element): Node = toXmlWithScope(element, meiScope)

  private def toXmlWithScope(element: sheet.meicmn.Element, scope: NamespaceBinding): Node = element match {
    case sheet.meicmn.Text(value)                  => scala.xml.Text(value)
    case e: _root_.sheet.meicmn.mei.edittrans.Abbr =>
      new scala.xml.Elem(
        null,
        "abbr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.AccMat =>
      new scala.xml.Elem(
        null,
        "accMat",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.AccessRestrict =>
      new scala.xml.Elem(
        null,
        "accessRestrict",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Accid =>
      new scala.xml.Elem(
        null,
        "accid",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Acquisition =>
      new scala.xml.Elem(
        null,
        "acquisition",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Actor =>
      new scala.xml.Elem(
        null,
        "actor",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Add =>
      new scala.xml.Elem(
        null,
        "add",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.AddDesc =>
      new scala.xml.Elem(
        null,
        "addDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.AddName =>
      new scala.xml.Elem(
        null,
        "addName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.AddrLine =>
      new scala.xml.Elem(
        null,
        "addrLine",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Address =>
      new scala.xml.Elem(
        null,
        "address",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.AltId =>
      new scala.xml.Elem(
        null,
        "altId",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.AmbNote =>
      new scala.xml.Elem(
        null,
        "ambNote",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Ambitus =>
      new scala.xml.Elem(
        null,
        "ambitus",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Analytic =>
      new scala.xml.Elem(
        null,
        "analytic",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.AnchoredText =>
      new scala.xml.Elem(
        null,
        "anchoredText",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Annot =>
      new scala.xml.Elem(
        null,
        "annot",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.critapp.App =>
      new scala.xml.Elem(
        null,
        "app",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.AppInfo =>
      new scala.xml.Elem(
        null,
        "appInfo",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Application =>
      new scala.xml.Elem(
        null,
        "application",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Argument =>
      new scala.xml.Elem(
        null,
        "argument",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Arpeg =>
      new scala.xml.Elem(
        null,
        "arpeg",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Arranger =>
      new scala.xml.Elem(
        null,
        "arranger",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Artic =>
      new scala.xml.Elem(
        null,
        "artic",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.AttUsage =>
      new scala.xml.Elem(
        null,
        "attUsage",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Attacca =>
      new scala.xml.Elem(
        null,
        "attacca",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Audience =>
      new scala.xml.Elem(
        null,
        "audience",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Author =>
      new scala.xml.Elem(
        null,
        "author",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.performance.AvFile =>
      new scala.xml.Elem(
        null,
        "avFile",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Availability =>
      new scala.xml.Elem(
        null,
        "availability",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.BTrem =>
      new scala.xml.Elem(
        null,
        "bTrem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Back =>
      new scala.xml.Elem(
        null,
        "back",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.BarLine =>
      new scala.xml.Elem(
        null,
        "barLine",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.stringtab.Barre =>
      new scala.xml.Elem(
        null,
        "barre",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Beam =>
      new scala.xml.Elem(
        null,
        "beam",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.BeamSpan =>
      new scala.xml.Elem(
        null,
        "beamSpan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.BeatRpt =>
      new scala.xml.Elem(
        null,
        "beatRpt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Bend =>
      new scala.xml.Elem(
        null,
        "bend",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Bibl =>
      new scala.xml.Elem(
        null,
        "bibl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.BiblList =>
      new scala.xml.Elem(
        null,
        "biblList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.BiblScope =>
      new scala.xml.Elem(
        null,
        "biblScope",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.BiblStruct =>
      new scala.xml.Elem(
        null,
        "biblStruct",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Bifolium =>
      new scala.xml.Elem(
        null,
        "bifolium",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Binding =>
      new scala.xml.Elem(
        null,
        "binding",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.BindingDesc =>
      new scala.xml.Elem(
        null,
        "bindingDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.Bloc =>
      new scala.xml.Elem(
        null,
        "bloc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Body =>
      new scala.xml.Elem(
        null,
        "body",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.BracketSpan =>
      new scala.xml.Elem(
        null,
        "bracketSpan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Breath =>
      new scala.xml.Elem(
        null,
        "breath",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Byline =>
      new scala.xml.Elem(
        null,
        "byline",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Caesura =>
      new scala.xml.Elem(
        null,
        "caesura",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Caption =>
      new scala.xml.Elem(
        null,
        "caption",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.CaptureMode =>
      new scala.xml.Elem(
        null,
        "captureMode",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.CarrierForm =>
      new scala.xml.Elem(
        null,
        "carrierForm",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.CastGrp =>
      new scala.xml.Elem(
        null,
        "castGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.CastItem =>
      new scala.xml.Elem(
        null,
        "castItem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.CastList =>
      new scala.xml.Elem(
        null,
        "castList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.CatRel =>
      new scala.xml.Elem(
        null,
        "catRel",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Catchwords =>
      new scala.xml.Elem(
        null,
        "catchwords",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Category =>
      new scala.xml.Elem(
        null,
        "category",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Cb =>
      new scala.xml.Elem(
        null,
        "cb",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Cc =>
      new scala.xml.Elem(
        null,
        "cc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Chan =>
      new scala.xml.Elem(
        null,
        "chan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.ChanPr =>
      new scala.xml.Elem(
        null,
        "chanPr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Change =>
      new scala.xml.Elem(
        null,
        "change",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ChangeDesc =>
      new scala.xml.Elem(
        null,
        "changeDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Choice =>
      new scala.xml.Elem(
        null,
        "choice",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Chord =>
      new scala.xml.Elem(
        null,
        "chord",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.ChordDef =>
      new scala.xml.Elem(
        null,
        "chordDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.ChordMember =>
      new scala.xml.Elem(
        null,
        "chordMember",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.ChordTable =>
      new scala.xml.Elem(
        null,
        "chordTable",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ClassDecls =>
      new scala.xml.Elem(
        null,
        "classDecls",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Classification =>
      new scala.xml.Elem(
        null,
        "classification",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Clef =>
      new scala.xml.Elem(
        null,
        "clef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.ClefGrp =>
      new scala.xml.Elem(
        null,
        "clefGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.performance.Clip =>
      new scala.xml.Elem(
        null,
        "clip",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.ColLayout =>
      new scala.xml.Elem(
        null,
        "colLayout",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Collation =>
      new scala.xml.Elem(
        null,
        "collation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Colophon =>
      new scala.xml.Elem(
        null,
        "colophon",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ComponentList =>
      new scala.xml.Elem(
        null,
        "componentList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Composer =>
      new scala.xml.Elem(
        null,
        "composer",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Condition =>
      new scala.xml.Elem(
        null,
        "condition",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ContentItem =>
      new scala.xml.Elem(
        null,
        "contentItem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Contents =>
      new scala.xml.Elem(
        null,
        "contents",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Context =>
      new scala.xml.Elem(
        null,
        "context",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Contributor =>
      new scala.xml.Elem(
        null,
        "contributor",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.CorpName =>
      new scala.xml.Elem(
        null,
        "corpName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Corr =>
      new scala.xml.Elem(
        null,
        "corr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Correction =>
      new scala.xml.Elem(
        null,
        "correction",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.Country =>
      new scala.xml.Elem(
        null,
        "country",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.stringtab.Course =>
      new scala.xml.Elem(
        null,
        "course",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.CpMark =>
      new scala.xml.Elem(
        null,
        "cpMark",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Creation =>
      new scala.xml.Elem(
        null,
        "creation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Cue =>
      new scala.xml.Elem(
        null,
        "cue",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.Curve =>
      new scala.xml.Elem(
        null,
        "curve",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Custos =>
      new scala.xml.Elem(
        null,
        "custos",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Cutout =>
      new scala.xml.Elem(
        null,
        "cutout",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Damage =>
      new scala.xml.Elem(
        null,
        "damage",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Date =>
      new scala.xml.Elem(
        null,
        "date",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.DecoDesc =>
      new scala.xml.Elem(
        null,
        "decoDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.DecoNote =>
      new scala.xml.Elem(
        null,
        "decoNote",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dedicatee =>
      new scala.xml.Elem(
        null,
        "dedicatee",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Dedication =>
      new scala.xml.Elem(
        null,
        "dedication",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Del =>
      new scala.xml.Elem(
        null,
        "del",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Depth =>
      new scala.xml.Elem(
        null,
        "depth",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Desc =>
      new scala.xml.Elem(
        null,
        "desc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dim =>
      new scala.xml.Elem(
        null,
        "dim",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dimensions =>
      new scala.xml.Elem(
        null,
        "dimensions",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dir =>
      new scala.xml.Elem(
        null,
        "dir",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Distributor =>
      new scala.xml.Elem(
        null,
        "distributor",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.District =>
      new scala.xml.Elem(
        null,
        "district",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Div =>
      new scala.xml.Elem(
        null,
        "div",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.DomainsDecl =>
      new scala.xml.Elem(
        null,
        "domainsDecl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dot =>
      new scala.xml.Elem(
        null,
        "dot",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Dynam =>
      new scala.xml.Elem(
        null,
        "dynam",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Edition =>
      new scala.xml.Elem(
        null,
        "edition",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.EditionStmt =>
      new scala.xml.Elem(
        null,
        "editionStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Editor =>
      new scala.xml.Elem(
        null,
        "editor",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.EditorialDecl =>
      new scala.xml.Elem(
        null,
        "editorialDecl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.EncodingDesc =>
      new scala.xml.Elem(
        null,
        "encodingDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Ending =>
      new scala.xml.Elem(
        null,
        "ending",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Epigraph =>
      new scala.xml.Elem(
        null,
        "epigraph",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Event =>
      new scala.xml.Elem(
        null,
        "event",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.EventList =>
      new scala.xml.Elem(
        null,
        "eventList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ExhibHist =>
      new scala.xml.Elem(
        null,
        "exhibHist",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Expan =>
      new scala.xml.Elem(
        null,
        "expan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Expansion =>
      new scala.xml.Elem(
        null,
        "expansion",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Explicit =>
      new scala.xml.Elem(
        null,
        "explicit",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.Expression =>
      new scala.xml.Elem(
        null,
        "expression",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.ExpressionList =>
      new scala.xml.Elem(
        null,
        "expressionList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.ExtData =>
      new scala.xml.Elem(
        null,
        "extData",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ExtMeta =>
      new scala.xml.Elem(
        null,
        "extMeta",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Extent =>
      new scala.xml.Elem(
        null,
        "extent",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.F =>
      new scala.xml.Elem(
        null,
        "f",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.FTrem =>
      new scala.xml.Elem(
        null,
        "fTrem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.facsimile.Facsimile =>
      new scala.xml.Elem(
        null,
        "facsimile",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.FamName =>
      new scala.xml.Elem(
        null,
        "famName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.Fb =>
      new scala.xml.Elem(
        null,
        "fb",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Fermata =>
      new scala.xml.Elem(
        null,
        "fermata",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Fig =>
      new scala.xml.Elem(
        null,
        "fig",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.FigDesc =>
      new scala.xml.Elem(
        null,
        "figDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.FileChar =>
      new scala.xml.Elem(
        null,
        "fileChar",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.FileDesc =>
      new scala.xml.Elem(
        null,
        "fileDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.fingering.Fing =>
      new scala.xml.Elem(
        null,
        "fing",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.fingering.FingGrp =>
      new scala.xml.Elem(
        null,
        "fingGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.FoliaDesc =>
      new scala.xml.Elem(
        null,
        "foliaDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Foliation =>
      new scala.xml.Elem(
        null,
        "foliation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Folium =>
      new scala.xml.Elem(
        null,
        "folium",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.ForeName =>
      new scala.xml.Elem(
        null,
        "foreName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Front =>
      new scala.xml.Elem(
        null,
        "front",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Funder =>
      new scala.xml.Elem(
        null,
        "funder",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Gap =>
      new scala.xml.Elem(
        null,
        "gap",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.genetic.GenDesc =>
      new scala.xml.Elem(
        null,
        "genDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.GenName =>
      new scala.xml.Elem(
        null,
        "genName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.genetic.GenState =>
      new scala.xml.Elem(
        null,
        "genState",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Genre =>
      new scala.xml.Elem(
        null,
        "genre",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.GeogFeat =>
      new scala.xml.Elem(
        null,
        "geogFeat",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.GeogName =>
      new scala.xml.Elem(
        null,
        "geogName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Gliss =>
      new scala.xml.Elem(
        null,
        "gliss",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.GraceGrp =>
      new scala.xml.Elem(
        null,
        "graceGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Graphic =>
      new scala.xml.Elem(
        null,
        "graphic",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Group =>
      new scala.xml.Elem(
        null,
        "group",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.GrpSym =>
      new scala.xml.Elem(
        null,
        "grpSym",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Hairpin =>
      new scala.xml.Elem(
        null,
        "hairpin",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.HalfmRpt =>
      new scala.xml.Elem(
        null,
        "halfmRpt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Hand =>
      new scala.xml.Elem(
        null,
        "hand",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.HandList =>
      new scala.xml.Elem(
        null,
        "handList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.HandShift =>
      new scala.xml.Elem(
        null,
        "handShift",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.harmony.Harm =>
      new scala.xml.Elem(
        null,
        "harm",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.HarpPedal =>
      new scala.xml.Elem(
        null,
        "harpPedal",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Head =>
      new scala.xml.Elem(
        null,
        "head",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Height =>
      new scala.xml.Elem(
        null,
        "height",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Heraldry =>
      new scala.xml.Elem(
        null,
        "heraldry",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Hex =>
      new scala.xml.Elem(
        null,
        "hex",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.History =>
      new scala.xml.Elem(
        null,
        "history",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Identifier =>
      new scala.xml.Elem(
        null,
        "identifier",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Imprimatur =>
      new scala.xml.Elem(
        null,
        "imprimatur",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Imprint =>
      new scala.xml.Elem(
        null,
        "imprint",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Incip =>
      new scala.xml.Elem(
        null,
        "incip",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.IncipCode =>
      new scala.xml.Elem(
        null,
        "incipCode",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.IncipText =>
      new scala.xml.Elem(
        null,
        "incipText",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Inscription =>
      new scala.xml.Elem(
        null,
        "inscription",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.InstrDef =>
      new scala.xml.Elem(
        null,
        "instrDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.InstrGrp =>
      new scala.xml.Elem(
        null,
        "instrGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Interpretation =>
      new scala.xml.Elem(
        null,
        "interpretation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.Item =>
      new scala.xml.Elem(
        null,
        "item",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.ItemList =>
      new scala.xml.Elem(
        null,
        "itemList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Key =>
      new scala.xml.Elem(
        null,
        "key",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.KeyAccid =>
      new scala.xml.Elem(
        null,
        "keyAccid",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.KeySig =>
      new scala.xml.Elem(
        null,
        "keySig",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.L =>
      new scala.xml.Elem(
        null,
        "l",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Label =>
      new scala.xml.Elem(
        null,
        "label",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.LabelAbbr =>
      new scala.xml.Elem(
        null,
        "labelAbbr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.LangUsage =>
      new scala.xml.Elem(
        null,
        "langUsage",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Language =>
      new scala.xml.Elem(
        null,
        "language",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Layer =>
      new scala.xml.Elem(
        null,
        "layer",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.LayerDef =>
      new scala.xml.Elem(
        null,
        "layerDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Layout =>
      new scala.xml.Elem(
        null,
        "layout",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.LayoutDesc =>
      new scala.xml.Elem(
        null,
        "layoutDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Lb =>
      new scala.xml.Elem(
        null,
        "lb",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.critapp.Lem =>
      new scala.xml.Elem(
        null,
        "lem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Lg =>
      new scala.xml.Elem(
        null,
        "lg",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Li =>
      new scala.xml.Elem(
        null,
        "li",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Librettist =>
      new scala.xml.Elem(
        null,
        "librettist",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.Line =>
      new scala.xml.Elem(
        null,
        "line",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.List =>
      new scala.xml.Elem(
        null,
        "list",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Locus =>
      new scala.xml.Elem(
        null,
        "locus",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.LocusGrp =>
      new scala.xml.Elem(
        null,
        "locusGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Lv =>
      new scala.xml.Elem(
        null,
        "lv",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Lyricist =>
      new scala.xml.Elem(
        null,
        "lyricist",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MNum =>
      new scala.xml.Elem(
        null,
        "mNum",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MRest =>
      new scala.xml.Elem(
        null,
        "mRest",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MRpt =>
      new scala.xml.Elem(
        null,
        "mRpt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MRpt2 =>
      new scala.xml.Elem(
        null,
        "mRpt2",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MSpace =>
      new scala.xml.Elem(
        null,
        "mSpace",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.Manifestation =>
      new scala.xml.Elem(
        null,
        "manifestation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.frbr.ManifestationList =>
      new scala.xml.Elem(
        null,
        "manifestationList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.Mapping =>
      new scala.xml.Elem(
        null,
        "mapping",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Marker =>
      new scala.xml.Elem(
        null,
        "marker",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Mdiv =>
      new scala.xml.Elem(
        null,
        "mdiv",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Measure =>
      new scala.xml.Elem(
        null,
        "measure",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Mei =>
      new scala.xml.Elem(
        null,
        "mei",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.corpus.MeiCorpus =>
      new scala.xml.Elem(
        null,
        "meiCorpus",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.MeiHead =>
      new scala.xml.Elem(
        null,
        "meiHead",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Mensuration =>
      new scala.xml.Elem(
        null,
        "mensuration",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.MetaMark =>
      new scala.xml.Elem(
        null,
        "metaMark",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.MetaText =>
      new scala.xml.Elem(
        null,
        "metaText",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Meter =>
      new scala.xml.Elem(
        null,
        "meter",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MeterSig =>
      new scala.xml.Elem(
        null,
        "meterSig",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MeterSigGrp =>
      new scala.xml.Elem(
        null,
        "meterSigGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Midi =>
      new scala.xml.Elem(
        null,
        "midi",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Monogr =>
      new scala.xml.Elem(
        null,
        "monogr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmnornaments.Mordent =>
      new scala.xml.Elem(
        null,
        "mordent",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MultiRest =>
      new scala.xml.Elem(
        null,
        "multiRest",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.MultiRpt =>
      new scala.xml.Elem(
        null,
        "multiRpt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Music =>
      new scala.xml.Elem(
        null,
        "music",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Name =>
      new scala.xml.Elem(
        null,
        "name",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.NameLink =>
      new scala.xml.Elem(
        null,
        "nameLink",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Namespace =>
      new scala.xml.Elem(
        null,
        "namespace",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Normalization =>
      new scala.xml.Elem(
        null,
        "normalization",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Note =>
      new scala.xml.Elem(
        null,
        "note",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.NoteOff =>
      new scala.xml.Elem(
        null,
        "noteOff",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.NoteOn =>
      new scala.xml.Elem(
        null,
        "noteOn",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.NotesStmt =>
      new scala.xml.Elem(
        null,
        "notesStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Num =>
      new scala.xml.Elem(
        null,
        "num",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.OLayer =>
      new scala.xml.Elem(
        null,
        "oLayer",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.OStaff =>
      new scala.xml.Elem(
        null,
        "oStaff",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Octave =>
      new scala.xml.Elem(
        null,
        "octave",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Orig =>
      new scala.xml.Elem(
        null,
        "orig",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Ornam =>
      new scala.xml.Elem(
        null,
        "ornam",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Ossia =>
      new scala.xml.Elem(
        null,
        "ossia",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.OtherChar =>
      new scala.xml.Elem(
        null,
        "otherChar",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.P =>
      new scala.xml.Elem(
        null,
        "p",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Pad =>
      new scala.xml.Elem(
        null,
        "pad",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Part =>
      new scala.xml.Elem(
        null,
        "part",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Parts =>
      new scala.xml.Elem(
        null,
        "parts",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Patch =>
      new scala.xml.Elem(
        null,
        "patch",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Pb =>
      new scala.xml.Elem(
        null,
        "pb",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Pedal =>
      new scala.xml.Elem(
        null,
        "pedal",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PerfDuration =>
      new scala.xml.Elem(
        null,
        "perfDuration",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PerfMedium =>
      new scala.xml.Elem(
        null,
        "perfMedium",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PerfRes =>
      new scala.xml.Elem(
        null,
        "perfRes",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PerfResList =>
      new scala.xml.Elem(
        null,
        "perfResList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.performance.Performance =>
      new scala.xml.Elem(
        null,
        "performance",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.PeriodName =>
      new scala.xml.Elem(
        null,
        "periodName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.PersName =>
      new scala.xml.Elem(
        null,
        "persName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.PgDesc =>
      new scala.xml.Elem(
        null,
        "pgDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.PgFoot =>
      new scala.xml.Elem(
        null,
        "pgFoot",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.PgHead =>
      new scala.xml.Elem(
        null,
        "pgHead",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Phrase =>
      new scala.xml.Elem(
        null,
        "phrase",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PhysDesc =>
      new scala.xml.Elem(
        null,
        "physDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.PhysLoc =>
      new scala.xml.Elem(
        null,
        "physLoc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PhysMedium =>
      new scala.xml.Elem(
        null,
        "physMedium",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PlateNum =>
      new scala.xml.Elem(
        null,
        "plateNum",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PlayingSpeed =>
      new scala.xml.Elem(
        null,
        "playingSpeed",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Port =>
      new scala.xml.Elem(
        null,
        "port",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.PostBox =>
      new scala.xml.Elem(
        null,
        "postBox",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.PostCode =>
      new scala.xml.Elem(
        null,
        "postCode",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Price =>
      new scala.xml.Elem(
        null,
        "price",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Prog =>
      new scala.xml.Elem(
        null,
        "prog",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ProjectDesc =>
      new scala.xml.Elem(
        null,
        "projectDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.PropName =>
      new scala.xml.Elem(
        null,
        "propName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.PropValue =>
      new scala.xml.Elem(
        null,
        "propValue",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Provenance =>
      new scala.xml.Elem(
        null,
        "provenance",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.ptrref.Ptr =>
      new scala.xml.Elem(
        null,
        "ptr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.PubPlace =>
      new scala.xml.Elem(
        null,
        "pubPlace",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.PubStmt =>
      new scala.xml.Elem(
        null,
        "pubStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Publisher =>
      new scala.xml.Elem(
        null,
        "publisher",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Q =>
      new scala.xml.Elem(
        null,
        "q",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Quote =>
      new scala.xml.Elem(
        null,
        "quote",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.critapp.Rdg =>
      new scala.xml.Elem(
        null,
        "rdg",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Recipient =>
      new scala.xml.Elem(
        null,
        "recipient",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.performance.Recording =>
      new scala.xml.Elem(
        null,
        "recording",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.ptrref.Ref =>
      new scala.xml.Elem(
        null,
        "ref",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.lyrics.Refrain =>
      new scala.xml.Elem(
        null,
        "refrain",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Reg =>
      new scala.xml.Elem(
        null,
        "reg",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.Region =>
      new scala.xml.Elem(
        null,
        "region",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Reh =>
      new scala.xml.Elem(
        null,
        "reh",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.RelatedItem =>
      new scala.xml.Elem(
        null,
        "relatedItem",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Relation =>
      new scala.xml.Elem(
        null,
        "relation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.RelationList =>
      new scala.xml.Elem(
        null,
        "relationList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Rend =>
      new scala.xml.Elem(
        null,
        "rend",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.RepeatMark =>
      new scala.xml.Elem(
        null,
        "repeatMark",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Repository =>
      new scala.xml.Elem(
        null,
        "repository",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Resp =>
      new scala.xml.Elem(
        null,
        "resp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.RespStmt =>
      new scala.xml.Elem(
        null,
        "respStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Rest =>
      new scala.xml.Elem(
        null,
        "rest",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Restore =>
      new scala.xml.Elem(
        null,
        "restore",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.RevisionDesc =>
      new scala.xml.Elem(
        null,
        "revisionDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Role =>
      new scala.xml.Elem(
        null,
        "role",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.RoleDesc =>
      new scala.xml.Elem(
        null,
        "roleDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.RoleName =>
      new scala.xml.Elem(
        null,
        "roleName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Rubric =>
      new scala.xml.Elem(
        null,
        "rubric",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SamplingDecl =>
      new scala.xml.Elem(
        null,
        "samplingDecl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Sb =>
      new scala.xml.Elem(
        null,
        "sb",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Score =>
      new scala.xml.Elem(
        null,
        "score",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.ScoreDef =>
      new scala.xml.Elem(
        null,
        "scoreDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.ScoreFormat =>
      new scala.xml.Elem(
        null,
        "scoreFormat",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.ScriptDesc =>
      new scala.xml.Elem(
        null,
        "scriptDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.ScriptNote =>
      new scala.xml.Elem(
        null,
        "scriptNote",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Seal =>
      new scala.xml.Elem(
        null,
        "seal",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.SealDesc =>
      new scala.xml.Elem(
        null,
        "sealDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.SecFolio =>
      new scala.xml.Elem(
        null,
        "secFolio",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Section =>
      new scala.xml.Elem(
        null,
        "section",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.text.Seg =>
      new scala.xml.Elem(
        null,
        "seg",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Segmentation =>
      new scala.xml.Elem(
        null,
        "segmentation",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.SeqNum =>
      new scala.xml.Elem(
        null,
        "seqNum",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Series =>
      new scala.xml.Elem(
        null,
        "series",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SeriesStmt =>
      new scala.xml.Elem(
        null,
        "seriesStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.Settlement =>
      new scala.xml.Elem(
        null,
        "settlement",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Sic =>
      new scala.xml.Elem(
        null,
        "sic",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Signatures =>
      new scala.xml.Elem(
        null,
        "signatures",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Slur =>
      new scala.xml.Elem(
        null,
        "slur",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SoundChan =>
      new scala.xml.Elem(
        null,
        "soundChan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Source =>
      new scala.xml.Elem(
        null,
        "source",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SourceDesc =>
      new scala.xml.Elem(
        null,
        "sourceDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.drama.Sp =>
      new scala.xml.Elem(
        null,
        "sp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Space =>
      new scala.xml.Elem(
        null,
        "space",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Speaker =>
      new scala.xml.Elem(
        null,
        "speaker",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SpecRepro =>
      new scala.xml.Elem(
        null,
        "specRepro",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Sponsor =>
      new scala.xml.Elem(
        null,
        "sponsor",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Stack =>
      new scala.xml.Elem(
        null,
        "stack",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Staff =>
      new scala.xml.Elem(
        null,
        "staff",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.StaffDef =>
      new scala.xml.Elem(
        null,
        "staffDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.StaffGrp =>
      new scala.xml.Elem(
        null,
        "staffGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.drama.StageDir =>
      new scala.xml.Elem(
        null,
        "stageDir",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Stamp =>
      new scala.xml.Elem(
        null,
        "stamp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.StdVals =>
      new scala.xml.Elem(
        null,
        "stdVals",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.Street =>
      new scala.xml.Elem(
        null,
        "street",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.stringtab.String =>
      new scala.xml.Elem(
        null,
        "string",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.namesdates.StyleName =>
      new scala.xml.Elem(
        null,
        "styleName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Subst =>
      new scala.xml.Elem(
        null,
        "subst",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Supplied =>
      new scala.xml.Elem(
        null,
        "supplied",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.Support =>
      new scala.xml.Elem(
        null,
        "support",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.SupportDesc =>
      new scala.xml.Elem(
        null,
        "supportDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.facsimile.Surface =>
      new scala.xml.Elem(
        null,
        "surface",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Syl =>
      new scala.xml.Elem(
        null,
        "syl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.SymName =>
      new scala.xml.Elem(
        null,
        "symName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.SymProp =>
      new scala.xml.Elem(
        null,
        "symProp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Symbol =>
      new scala.xml.Elem(
        null,
        "symbol",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.SymbolDef =>
      new scala.xml.Elem(
        null,
        "symbolDef",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.usersymbols.SymbolTable =>
      new scala.xml.Elem(
        null,
        "symbolTable",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.SysReq =>
      new scala.xml.Elem(
        null,
        "sysReq",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.stringtab.TabDurSym =>
      new scala.xml.Elem(
        null,
        "tabDurSym",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.stringtab.TabGrp =>
      new scala.xml.Elem(
        null,
        "tabGrp",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Table =>
      new scala.xml.Elem(
        null,
        "table",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TagUsage =>
      new scala.xml.Elem(
        null,
        "tagUsage",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TagsDecl =>
      new scala.xml.Elem(
        null,
        "tagsDecl",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Taxonomy =>
      new scala.xml.Elem(
        null,
        "taxonomy",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Td =>
      new scala.xml.Elem(
        null,
        "td",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Tempo =>
      new scala.xml.Elem(
        null,
        "tempo",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Term =>
      new scala.xml.Elem(
        null,
        "term",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TermList =>
      new scala.xml.Elem(
        null,
        "termList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.TextLang =>
      new scala.xml.Elem(
        null,
        "textLang",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Th =>
      new scala.xml.Elem(
        null,
        "th",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Tie =>
      new scala.xml.Elem(
        null,
        "tie",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Title =>
      new scala.xml.Elem(
        null,
        "title",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.TitlePage =>
      new scala.xml.Elem(
        null,
        "titlePage",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.TitlePart =>
      new scala.xml.Elem(
        null,
        "titlePart",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TitleStmt =>
      new scala.xml.Elem(
        null,
        "titleStmt",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.figtable.Tr =>
      new scala.xml.Elem(
        null,
        "tr",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TrackConfig =>
      new scala.xml.Elem(
        null,
        "trackConfig",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TreatHist =>
      new scala.xml.Elem(
        null,
        "treatHist",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.TreatSched =>
      new scala.xml.Elem(
        null,
        "treatSched",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmnornaments.Trill =>
      new scala.xml.Elem(
        null,
        "trill",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.TrkName =>
      new scala.xml.Elem(
        null,
        "trkName",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Tuning =>
      new scala.xml.Elem(
        null,
        "tuning",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.Tuplet =>
      new scala.xml.Elem(
        null,
        "tuplet",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmn.TupletSpan =>
      new scala.xml.Elem(
        null,
        "tupletSpan",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.cmnornaments.Turn =>
      new scala.xml.Elem(
        null,
        "turn",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.TypeDesc =>
      new scala.xml.Elem(
        null,
        "typeDesc",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.msdesc.TypeNote =>
      new scala.xml.Elem(
        null,
        "typeNote",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.edittrans.Unclear =>
      new scala.xml.Elem(
        null,
        "unclear",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Unpub =>
      new scala.xml.Elem(
        null,
        "unpub",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.UseRestrict =>
      new scala.xml.Elem(
        null,
        "useRestrict",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.midi.Vel =>
      new scala.xml.Elem(
        null,
        "vel",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.lyrics.Verse =>
      new scala.xml.Elem(
        null,
        "verse",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.lyrics.Volta =>
      new scala.xml.Elem(
        null,
        "volta",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Watermark =>
      new scala.xml.Elem(
        null,
        "watermark",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.performance.When =>
      new scala.xml.Elem(
        null,
        "when",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.shared.Width =>
      new scala.xml.Elem(
        null,
        "width",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.Work =>
      new scala.xml.Elem(
        null,
        "work",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.header.WorkList =>
      new scala.xml.Elem(
        null,
        "workList",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case e: _root_.sheet.meicmn.mei.facsimile.Zone =>
      new scala.xml.Elem(
        null,
        "zone",
        mkAttributes(e.attributes),
        scope,
        true,
        e.elements.map(el => toXmlWithScope(el, scope))*,
      )
    case _ => throw new IllegalArgumentException(s"Unknown element type: ${element.getClass.getName}")
  }
}

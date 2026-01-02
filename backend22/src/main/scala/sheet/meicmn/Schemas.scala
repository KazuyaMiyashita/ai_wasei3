package sheet.meicmn

trait SchemaNode
case object SEmpty                                extends SchemaNode
case object SText                                 extends SchemaNode
case class SRef(clazz: Class[?])                  extends SchemaNode
case class SChoice(nodes: scala.List[SchemaNode]) extends SchemaNode
case class SSeq(nodes: scala.List[SchemaNode])    extends SchemaNode
case class SZeroOrMore(node: SchemaNode)          extends SchemaNode
case class SOneOrMore(node: SchemaNode)           extends SchemaNode
case class SOptional(node: SchemaNode)            extends SchemaNode
case class SMacroRef(lazyNode: () => SchemaNode)  extends SchemaNode

object Validator {
  def validate(elements: scala.List[Element], schema: SchemaNode): Boolean = {
    matchSchema(elements, schema).exists(_.isEmpty)
  }

  private def matchSchema(elems: scala.List[Element], node: SchemaNode): Option[scala.List[Element]] = node match {
    case SEmpty => Some(elems)
    case SText  =>
      if (elems.headOption.exists(_.isInstanceOf[Text])) Some(elems.tail) else None

    case SRef(clazz) =>
      elems.headOption.filter(e => clazz.isAssignableFrom(e.getClass)).map(_ => elems.tail)

    case SSeq(nodes) =>
      nodes.foldLeft(Option(elems)) { (acc, n) => acc.flatMap(es => matchSchema(es, n)) }

    case SChoice(nodes) =>
      nodes.view.map(n => matchSchema(elems, n)).find(_.isDefined).flatten

    case SZeroOrMore(child) =>
      var current                                = elems
      var lastMatch: Option[scala.List[Element]] = Some(current)
      while (lastMatch.isDefined) {
        matchSchema(current, child) match {
          case Some(next) if next != current =>
            current = next
            lastMatch = Some(current)
          case _ =>
            lastMatch = None
        }
      }
      Some(current)

    case SOneOrMore(child) =>
      matchSchema(elems, child).flatMap(rest => matchSchema(rest, SZeroOrMore(child)))

    case SOptional(child) =>
      matchSchema(elems, child).orElse(Some(elems))

    case SMacroRef(lazyNode) =>
      matchSchema(elems, lazyNode())
  }
}

object Schemas {
  lazy val schema_abbr: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_accMat: SchemaNode         = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_accessRestrict: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_accid: SchemaNode          = SEmpty
  lazy val schema_acquisition: SchemaNode    = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_actor: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_add: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_addDesc: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_addName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_addrLine: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_address: SchemaNode = SChoice(
    scala.List(
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.AddrLine])),
      SOneOrMore(SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.AddressPart])))),
    ),
  )
  lazy val schema_altId: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RendLike]),
      ),
    ),
  )
  lazy val schema_ambNote: SchemaNode = SEmpty
  lazy val schema_ambitus: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.shared.AmbNote]),
      SRef(classOf[_root_.sheet.meicmn.mei.shared.AmbNote]),
    ),
  )
  lazy val schema_analytic: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Title]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblScope])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Identifier])),
    ),
  )
  lazy val schema_anchoredText: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_annot: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike])),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SText,
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_app: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.critapp.Lem])),
      SRef(classOf[_root_.sheet.meicmn.mei.critapp.Rdg]),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.critapp.Rdg])),
    ),
  )
  lazy val schema_appInfo: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Application])),
    ),
  )
  lazy val schema_application: SchemaNode = SSeq(
    scala.List(
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Name])),
      SChoice(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.ptrref.model.LocrefLike])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
        ),
      ),
    ),
  )
  lazy val schema_argument: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.drama.Sp]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_arpeg: SchemaNode    = SEmpty
  lazy val schema_arranger: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_artic: SchemaNode                      = SEmpty
  lazy val schema_att_accid_anl: SchemaNode              = SEmpty
  lazy val schema_att_accid_ges: SchemaNode              = SEmpty
  lazy val schema_att_accid_log: SchemaNode              = SEmpty
  lazy val schema_att_accid_vis: SchemaNode              = SEmpty
  lazy val schema_att_accidental: SchemaNode             = SEmpty
  lazy val schema_att_accidental_ges: SchemaNode         = SEmpty
  lazy val schema_att_adlibitum: SchemaNode              = SEmpty
  lazy val schema_att_agentIdent: SchemaNode             = SEmpty
  lazy val schema_att_alignment: SchemaNode              = SEmpty
  lazy val schema_att_altSym: SchemaNode                 = SEmpty
  lazy val schema_att_ambNote_anl: SchemaNode            = SEmpty
  lazy val schema_att_ambNote_ges: SchemaNode            = SEmpty
  lazy val schema_att_ambNote_log: SchemaNode            = SEmpty
  lazy val schema_att_ambNote_vis: SchemaNode            = SEmpty
  lazy val schema_att_ambitus_anl: SchemaNode            = SEmpty
  lazy val schema_att_ambitus_ges: SchemaNode            = SEmpty
  lazy val schema_att_ambitus_log: SchemaNode            = SEmpty
  lazy val schema_att_ambitus_vis: SchemaNode            = SEmpty
  lazy val schema_att_anchoredText_anl: SchemaNode       = SEmpty
  lazy val schema_att_anchoredText_ges: SchemaNode       = SEmpty
  lazy val schema_att_anchoredText_log: SchemaNode       = SEmpty
  lazy val schema_att_anchoredText_vis: SchemaNode       = SEmpty
  lazy val schema_att_annot_anl: SchemaNode              = SEmpty
  lazy val schema_att_annot_ges: SchemaNode              = SEmpty
  lazy val schema_att_annot_log: SchemaNode              = SEmpty
  lazy val schema_att_annot_vis: SchemaNode              = SEmpty
  lazy val schema_att_arpeg_anl: SchemaNode              = SEmpty
  lazy val schema_att_arpeg_ges: SchemaNode              = SEmpty
  lazy val schema_att_arpeg_log: SchemaNode              = SEmpty
  lazy val schema_att_arpeg_vis: SchemaNode              = SEmpty
  lazy val schema_att_artic_anl: SchemaNode              = SEmpty
  lazy val schema_att_artic_ges: SchemaNode              = SEmpty
  lazy val schema_att_artic_log: SchemaNode              = SEmpty
  lazy val schema_att_artic_vis: SchemaNode              = SEmpty
  lazy val schema_att_articulation: SchemaNode           = SEmpty
  lazy val schema_att_articulation_ges: SchemaNode       = SEmpty
  lazy val schema_att_attacca_anl: SchemaNode            = SEmpty
  lazy val schema_att_attacca_ges: SchemaNode            = SEmpty
  lazy val schema_att_attacca_log: SchemaNode            = SEmpty
  lazy val schema_att_attacca_vis: SchemaNode            = SEmpty
  lazy val schema_att_attacking: SchemaNode              = SEmpty
  lazy val schema_att_audience: SchemaNode               = SEmpty
  lazy val schema_att_augmentDots: SchemaNode            = SEmpty
  lazy val schema_att_authorized: SchemaNode             = SEmpty
  lazy val schema_att_bTrem_anl: SchemaNode              = SEmpty
  lazy val schema_att_bTrem_ges: SchemaNode              = SEmpty
  lazy val schema_att_bTrem_log: SchemaNode              = SEmpty
  lazy val schema_att_bTrem_vis: SchemaNode              = SEmpty
  lazy val schema_att_barLine_anl: SchemaNode            = SEmpty
  lazy val schema_att_barLine_ges: SchemaNode            = SEmpty
  lazy val schema_att_barLine_log: SchemaNode            = SEmpty
  lazy val schema_att_barLine_vis: SchemaNode            = SEmpty
  lazy val schema_att_barring: SchemaNode                = SEmpty
  lazy val schema_att_basic: SchemaNode                  = SEmpty
  lazy val schema_att_beam_anl: SchemaNode               = SEmpty
  lazy val schema_att_beam_ges: SchemaNode               = SEmpty
  lazy val schema_att_beam_log: SchemaNode               = SEmpty
  lazy val schema_att_beam_vis: SchemaNode               = SEmpty
  lazy val schema_att_beamPresent: SchemaNode            = SEmpty
  lazy val schema_att_beamRend: SchemaNode               = SEmpty
  lazy val schema_att_beamSecondary: SchemaNode          = SEmpty
  lazy val schema_att_beamSpan_anl: SchemaNode           = SEmpty
  lazy val schema_att_beamSpan_ges: SchemaNode           = SEmpty
  lazy val schema_att_beamSpan_log: SchemaNode           = SEmpty
  lazy val schema_att_beamSpan_vis: SchemaNode           = SEmpty
  lazy val schema_att_beamedWith: SchemaNode             = SEmpty
  lazy val schema_att_beaming_log: SchemaNode            = SEmpty
  lazy val schema_att_beaming_vis: SchemaNode            = SEmpty
  lazy val schema_att_beatRpt_anl: SchemaNode            = SEmpty
  lazy val schema_att_beatRpt_ges: SchemaNode            = SEmpty
  lazy val schema_att_beatRpt_log: SchemaNode            = SEmpty
  lazy val schema_att_beatRpt_vis: SchemaNode            = SEmpty
  lazy val schema_att_bend_anl: SchemaNode               = SEmpty
  lazy val schema_att_bend_ges: SchemaNode               = SEmpty
  lazy val schema_att_bend_log: SchemaNode               = SEmpty
  lazy val schema_att_bend_vis: SchemaNode               = SEmpty
  lazy val schema_att_bibl: SchemaNode                   = SEmpty
  lazy val schema_att_bifoliumSurfaces: SchemaNode       = SEmpty
  lazy val schema_att_bracketSpan_anl: SchemaNode        = SEmpty
  lazy val schema_att_bracketSpan_ges: SchemaNode        = SEmpty
  lazy val schema_att_bracketSpan_log: SchemaNode        = SEmpty
  lazy val schema_att_bracketSpan_vis: SchemaNode        = SEmpty
  lazy val schema_att_breath_anl: SchemaNode             = SEmpty
  lazy val schema_att_breath_ges: SchemaNode             = SEmpty
  lazy val schema_att_breath_log: SchemaNode             = SEmpty
  lazy val schema_att_breath_vis: SchemaNode             = SEmpty
  lazy val schema_att_caesura_anl: SchemaNode            = SEmpty
  lazy val schema_att_caesura_ges: SchemaNode            = SEmpty
  lazy val schema_att_caesura_log: SchemaNode            = SEmpty
  lazy val schema_att_caesura_vis: SchemaNode            = SEmpty
  lazy val schema_att_calendared: SchemaNode             = SEmpty
  lazy val schema_att_canonical: SchemaNode              = SEmpty
  lazy val schema_att_channelized: SchemaNode            = SEmpty
  lazy val schema_att_chord_anl: SchemaNode              = SEmpty
  lazy val schema_att_chord_anl_cmn: SchemaNode          = SEmpty
  lazy val schema_att_chord_ges: SchemaNode              = SEmpty
  lazy val schema_att_chord_ges_cmn: SchemaNode          = SEmpty
  lazy val schema_att_chord_log: SchemaNode              = SEmpty
  lazy val schema_att_chord_log_cmn: SchemaNode          = SEmpty
  lazy val schema_att_chord_vis: SchemaNode              = SEmpty
  lazy val schema_att_chord_vis_cmn: SchemaNode          = SEmpty
  lazy val schema_att_chordDef_anl: SchemaNode           = SEmpty
  lazy val schema_att_chordDef_ges: SchemaNode           = SEmpty
  lazy val schema_att_chordDef_log: SchemaNode           = SEmpty
  lazy val schema_att_chordDef_vis: SchemaNode           = SEmpty
  lazy val schema_att_chordMember_anl: SchemaNode        = SEmpty
  lazy val schema_att_chordMember_ges: SchemaNode        = SEmpty
  lazy val schema_att_chordMember_log: SchemaNode        = SEmpty
  lazy val schema_att_chordMember_vis: SchemaNode        = SEmpty
  lazy val schema_att_classed: SchemaNode                = SEmpty
  lazy val schema_att_clef_anl: SchemaNode               = SEmpty
  lazy val schema_att_clef_ges: SchemaNode               = SEmpty
  lazy val schema_att_clef_log: SchemaNode               = SEmpty
  lazy val schema_att_clef_vis: SchemaNode               = SEmpty
  lazy val schema_att_clefGrp_anl: SchemaNode            = SEmpty
  lazy val schema_att_clefGrp_ges: SchemaNode            = SEmpty
  lazy val schema_att_clefGrp_log: SchemaNode            = SEmpty
  lazy val schema_att_clefGrp_vis: SchemaNode            = SEmpty
  lazy val schema_att_clefShape: SchemaNode              = SEmpty
  lazy val schema_att_cleffing_log: SchemaNode           = SEmpty
  lazy val schema_att_cleffing_vis: SchemaNode           = SEmpty
  lazy val schema_att_color: SchemaNode                  = SEmpty
  lazy val schema_att_coloration: SchemaNode             = SEmpty
  lazy val schema_att_common: SchemaNode                 = SEmpty
  lazy val schema_att_componentType: SchemaNode          = SEmpty
  lazy val schema_att_contemporary: SchemaNode           = SEmpty
  lazy val schema_att_controlEvent: SchemaNode           = SEmpty
  lazy val schema_att_coordinated: SchemaNode            = SEmpty
  lazy val schema_att_coordinated_ul: SchemaNode         = SEmpty
  lazy val schema_att_course_anl: SchemaNode             = SEmpty
  lazy val schema_att_course_ges: SchemaNode             = SEmpty
  lazy val schema_att_course_log: SchemaNode             = SEmpty
  lazy val schema_att_course_vis: SchemaNode             = SEmpty
  lazy val schema_att_cpMark_anl: SchemaNode             = SEmpty
  lazy val schema_att_cpMark_ges: SchemaNode             = SEmpty
  lazy val schema_att_cpMark_log: SchemaNode             = SEmpty
  lazy val schema_att_cpMark_vis: SchemaNode             = SEmpty
  lazy val schema_att_crit: SchemaNode                   = SEmpty
  lazy val schema_att_cue: SchemaNode                    = SEmpty
  lazy val schema_att_curvature: SchemaNode              = SEmpty
  lazy val schema_att_curve_anl: SchemaNode              = SEmpty
  lazy val schema_att_curve_ges: SchemaNode              = SEmpty
  lazy val schema_att_curve_log: SchemaNode              = SEmpty
  lazy val schema_att_curve_vis: SchemaNode              = SEmpty
  lazy val schema_att_custos_anl: SchemaNode             = SEmpty
  lazy val schema_att_custos_ges: SchemaNode             = SEmpty
  lazy val schema_att_custos_log: SchemaNode             = SEmpty
  lazy val schema_att_custos_vis: SchemaNode             = SEmpty
  lazy val schema_att_cutout: SchemaNode                 = SEmpty
  lazy val schema_att_dataPointing: SchemaNode           = SEmpty
  lazy val schema_att_dataSelecting: SchemaNode          = SEmpty
  lazy val schema_att_datable: SchemaNode                = SEmpty
  lazy val schema_att_dimensions: SchemaNode             = SEmpty
  lazy val schema_att_dir_anl: SchemaNode                = SEmpty
  lazy val schema_att_dir_ges: SchemaNode                = SEmpty
  lazy val schema_att_dir_log: SchemaNode                = SEmpty
  lazy val schema_att_dir_vis: SchemaNode                = SEmpty
  lazy val schema_att_distances: SchemaNode              = SEmpty
  lazy val schema_att_dot_anl: SchemaNode                = SEmpty
  lazy val schema_att_dot_ges: SchemaNode                = SEmpty
  lazy val schema_att_dot_log: SchemaNode                = SEmpty
  lazy val schema_att_dot_vis: SchemaNode                = SEmpty
  lazy val schema_att_duration_additive: SchemaNode      = SEmpty
  lazy val schema_att_duration_default: SchemaNode       = SEmpty
  lazy val schema_att_duration_ges: SchemaNode           = SEmpty
  lazy val schema_att_duration_log: SchemaNode           = SEmpty
  lazy val schema_att_duration_ratio: SchemaNode         = SEmpty
  lazy val schema_att_dynam_anl: SchemaNode              = SEmpty
  lazy val schema_att_dynam_ges: SchemaNode              = SEmpty
  lazy val schema_att_dynam_log: SchemaNode              = SEmpty
  lazy val schema_att_dynam_vis: SchemaNode              = SEmpty
  lazy val schema_att_edit: SchemaNode                   = SEmpty
  lazy val schema_att_enclosingChars: SchemaNode         = SEmpty
  lazy val schema_att_ending_anl: SchemaNode             = SEmpty
  lazy val schema_att_ending_ges: SchemaNode             = SEmpty
  lazy val schema_att_ending_log: SchemaNode             = SEmpty
  lazy val schema_att_ending_vis: SchemaNode             = SEmpty
  lazy val schema_att_endings: SchemaNode                = SEmpty
  lazy val schema_att_event: SchemaNode                  = SEmpty
  lazy val schema_att_evidence: SchemaNode               = SEmpty
  lazy val schema_att_expandable: SchemaNode             = SEmpty
  lazy val schema_att_extSym: SchemaNode                 = SEmpty
  lazy val schema_att_extSym_auth: SchemaNode            = SEmpty
  lazy val schema_att_extSym_names: SchemaNode           = SEmpty
  lazy val schema_att_extender: SchemaNode               = SEmpty
  lazy val schema_att_extent: SchemaNode                 = SEmpty
  lazy val schema_att_f_anl: SchemaNode                  = SEmpty
  lazy val schema_att_f_ges: SchemaNode                  = SEmpty
  lazy val schema_att_f_log: SchemaNode                  = SEmpty
  lazy val schema_att_f_vis: SchemaNode                  = SEmpty
  lazy val schema_att_fTrem_anl: SchemaNode              = SEmpty
  lazy val schema_att_fTrem_ges: SchemaNode              = SEmpty
  lazy val schema_att_fTrem_log: SchemaNode              = SEmpty
  lazy val schema_att_fTrem_vis: SchemaNode              = SEmpty
  lazy val schema_att_facsimile: SchemaNode              = SEmpty
  lazy val schema_att_fermata_anl: SchemaNode            = SEmpty
  lazy val schema_att_fermata_ges: SchemaNode            = SEmpty
  lazy val schema_att_fermata_log: SchemaNode            = SEmpty
  lazy val schema_att_fermata_vis: SchemaNode            = SEmpty
  lazy val schema_att_fermataPresent: SchemaNode         = SEmpty
  lazy val schema_att_filing: SchemaNode                 = SEmpty
  lazy val schema_att_fing_anl: SchemaNode               = SEmpty
  lazy val schema_att_fing_ges: SchemaNode               = SEmpty
  lazy val schema_att_fing_log: SchemaNode               = SEmpty
  lazy val schema_att_fing_vis: SchemaNode               = SEmpty
  lazy val schema_att_fingGrp_anl: SchemaNode            = SEmpty
  lazy val schema_att_fingGrp_ges: SchemaNode            = SEmpty
  lazy val schema_att_fingGrp_log: SchemaNode            = SEmpty
  lazy val schema_att_fingGrp_vis: SchemaNode            = SEmpty
  lazy val schema_att_foliation_scheme: SchemaNode       = SEmpty
  lazy val schema_att_foliumSurfaces: SchemaNode         = SEmpty
  lazy val schema_att_formework: SchemaNode              = SEmpty
  lazy val schema_att_geneticState: SchemaNode           = SEmpty
  lazy val schema_att_gliss_anl: SchemaNode              = SEmpty
  lazy val schema_att_gliss_ges: SchemaNode              = SEmpty
  lazy val schema_att_gliss_log: SchemaNode              = SEmpty
  lazy val schema_att_gliss_vis: SchemaNode              = SEmpty
  lazy val schema_att_glissPresent: SchemaNode           = SEmpty
  lazy val schema_att_graceGrp_anl: SchemaNode           = SEmpty
  lazy val schema_att_graceGrp_ges: SchemaNode           = SEmpty
  lazy val schema_att_graceGrp_log: SchemaNode           = SEmpty
  lazy val schema_att_graceGrp_vis: SchemaNode           = SEmpty
  lazy val schema_att_graced: SchemaNode                 = SEmpty
  lazy val schema_att_grpSym_anl: SchemaNode             = SEmpty
  lazy val schema_att_grpSym_ges: SchemaNode             = SEmpty
  lazy val schema_att_grpSym_log: SchemaNode             = SEmpty
  lazy val schema_att_grpSym_vis: SchemaNode             = SEmpty
  lazy val schema_att_guitarGrid_vis: SchemaNode         = SEmpty
  lazy val schema_att_hairpin_anl: SchemaNode            = SEmpty
  lazy val schema_att_hairpin_ges: SchemaNode            = SEmpty
  lazy val schema_att_hairpin_log: SchemaNode            = SEmpty
  lazy val schema_att_hairpin_vis: SchemaNode            = SEmpty
  lazy val schema_att_halfmRpt_anl: SchemaNode           = SEmpty
  lazy val schema_att_halfmRpt_ges: SchemaNode           = SEmpty
  lazy val schema_att_halfmRpt_log: SchemaNode           = SEmpty
  lazy val schema_att_halfmRpt_vis: SchemaNode           = SEmpty
  lazy val schema_att_handIdent: SchemaNode              = SEmpty
  lazy val schema_att_harm_anl: SchemaNode               = SEmpty
  lazy val schema_att_harm_ges: SchemaNode               = SEmpty
  lazy val schema_att_harm_log: SchemaNode               = SEmpty
  lazy val schema_att_harm_vis: SchemaNode               = SEmpty
  lazy val schema_att_harmonicFunction: SchemaNode       = SEmpty
  lazy val schema_att_harpPedal_anl: SchemaNode          = SEmpty
  lazy val schema_att_harpPedal_ges: SchemaNode          = SEmpty
  lazy val schema_att_harpPedal_log: SchemaNode          = SEmpty
  lazy val schema_att_harpPedal_vis: SchemaNode          = SEmpty
  lazy val schema_att_height: SchemaNode                 = SEmpty
  lazy val schema_att_horizontalAlign: SchemaNode        = SEmpty
  lazy val schema_att_id: SchemaNode                     = SEmpty
  lazy val schema_att_instrDef_anl: SchemaNode           = SEmpty
  lazy val schema_att_instrDef_ges: SchemaNode           = SEmpty
  lazy val schema_att_instrDef_log: SchemaNode           = SEmpty
  lazy val schema_att_instrDef_vis: SchemaNode           = SEmpty
  lazy val schema_att_instrumentIdent: SchemaNode        = SEmpty
  lazy val schema_att_internetMedia: SchemaNode          = SEmpty
  lazy val schema_att_intervalHarmonic: SchemaNode       = SEmpty
  lazy val schema_att_intervalMelodic: SchemaNode        = SEmpty
  lazy val schema_att_joined: SchemaNode                 = SEmpty
  lazy val schema_att_keyAccid_anl: SchemaNode           = SEmpty
  lazy val schema_att_keyAccid_ges: SchemaNode           = SEmpty
  lazy val schema_att_keyAccid_log: SchemaNode           = SEmpty
  lazy val schema_att_keyAccid_vis: SchemaNode           = SEmpty
  lazy val schema_att_keyMode: SchemaNode                = SEmpty
  lazy val schema_att_keySig_anl: SchemaNode             = SEmpty
  lazy val schema_att_keySig_ges: SchemaNode             = SEmpty
  lazy val schema_att_keySig_log: SchemaNode             = SEmpty
  lazy val schema_att_keySig_vis: SchemaNode             = SEmpty
  lazy val schema_att_keySigDefault_anl: SchemaNode      = SEmpty
  lazy val schema_att_keySigDefault_log: SchemaNode      = SEmpty
  lazy val schema_att_keySigDefault_vis: SchemaNode      = SEmpty
  lazy val schema_att_labelled: SchemaNode               = SEmpty
  lazy val schema_att_lang: SchemaNode                   = SEmpty
  lazy val schema_att_layer_anl: SchemaNode              = SEmpty
  lazy val schema_att_layer_ges: SchemaNode              = SEmpty
  lazy val schema_att_layer_log: SchemaNode              = SEmpty
  lazy val schema_att_layer_vis: SchemaNode              = SEmpty
  lazy val schema_att_layerDef_anl: SchemaNode           = SEmpty
  lazy val schema_att_layerDef_ges: SchemaNode           = SEmpty
  lazy val schema_att_layerDef_log: SchemaNode           = SEmpty
  lazy val schema_att_layerDef_log_cmn: SchemaNode       = SEmpty
  lazy val schema_att_layerDef_vis: SchemaNode           = SEmpty
  lazy val schema_att_layerIdent: SchemaNode             = SEmpty
  lazy val schema_att_line_anl: SchemaNode               = SEmpty
  lazy val schema_att_line_ges: SchemaNode               = SEmpty
  lazy val schema_att_line_log: SchemaNode               = SEmpty
  lazy val schema_att_line_vis: SchemaNode               = SEmpty
  lazy val schema_att_lineLoc: SchemaNode                = SEmpty
  lazy val schema_att_lineRend: SchemaNode               = SEmpty
  lazy val schema_att_lineRend_base: SchemaNode          = SEmpty
  lazy val schema_att_linking: SchemaNode                = SEmpty
  lazy val schema_att_lv_anl: SchemaNode                 = SEmpty
  lazy val schema_att_lv_ges: SchemaNode                 = SEmpty
  lazy val schema_att_lv_log: SchemaNode                 = SEmpty
  lazy val schema_att_lv_vis: SchemaNode                 = SEmpty
  lazy val schema_att_lvPresent: SchemaNode              = SEmpty
  lazy val schema_att_lyricStyle: SchemaNode             = SEmpty
  lazy val schema_att_lyrics_anl: SchemaNode             = SEmpty
  lazy val schema_att_lyrics_ges: SchemaNode             = SEmpty
  lazy val schema_att_lyrics_log: SchemaNode             = SEmpty
  lazy val schema_att_lyrics_vis: SchemaNode             = SEmpty
  lazy val schema_att_mNum_anl: SchemaNode               = SEmpty
  lazy val schema_att_mNum_ges: SchemaNode               = SEmpty
  lazy val schema_att_mNum_log: SchemaNode               = SEmpty
  lazy val schema_att_mNum_vis: SchemaNode               = SEmpty
  lazy val schema_att_mRest_anl: SchemaNode              = SEmpty
  lazy val schema_att_mRest_ges: SchemaNode              = SEmpty
  lazy val schema_att_mRest_log: SchemaNode              = SEmpty
  lazy val schema_att_mRest_vis: SchemaNode              = SEmpty
  lazy val schema_att_mRpt_anl: SchemaNode               = SEmpty
  lazy val schema_att_mRpt_ges: SchemaNode               = SEmpty
  lazy val schema_att_mRpt_log: SchemaNode               = SEmpty
  lazy val schema_att_mRpt_vis: SchemaNode               = SEmpty
  lazy val schema_att_mRpt2_anl: SchemaNode              = SEmpty
  lazy val schema_att_mRpt2_ges: SchemaNode              = SEmpty
  lazy val schema_att_mRpt2_log: SchemaNode              = SEmpty
  lazy val schema_att_mRpt2_vis: SchemaNode              = SEmpty
  lazy val schema_att_mSpace_anl: SchemaNode             = SEmpty
  lazy val schema_att_mSpace_ges: SchemaNode             = SEmpty
  lazy val schema_att_mSpace_log: SchemaNode             = SEmpty
  lazy val schema_att_mSpace_vis: SchemaNode             = SEmpty
  lazy val schema_att_mdiv_anl: SchemaNode               = SEmpty
  lazy val schema_att_mdiv_ges: SchemaNode               = SEmpty
  lazy val schema_att_mdiv_log: SchemaNode               = SEmpty
  lazy val schema_att_mdiv_vis: SchemaNode               = SEmpty
  lazy val schema_att_measure_anl: SchemaNode            = SEmpty
  lazy val schema_att_measure_ges: SchemaNode            = SEmpty
  lazy val schema_att_measure_log: SchemaNode            = SEmpty
  lazy val schema_att_measure_vis: SchemaNode            = SEmpty
  lazy val schema_att_measureNumbers: SchemaNode         = SEmpty
  lazy val schema_att_measurement: SchemaNode            = SEmpty
  lazy val schema_att_mediaBounds: SchemaNode            = SEmpty
  lazy val schema_att_medium: SchemaNode                 = SEmpty
  lazy val schema_att_meiVersion: SchemaNode             = SEmpty
  lazy val schema_att_melodicFunction: SchemaNode        = SEmpty
  lazy val schema_att_mensur_log: SchemaNode             = SEmpty
  lazy val schema_att_mensur_vis: SchemaNode             = SEmpty
  lazy val schema_att_metaMark_anl: SchemaNode           = SEmpty
  lazy val schema_att_metaMark_ges: SchemaNode           = SEmpty
  lazy val schema_att_metaMark_log: SchemaNode           = SEmpty
  lazy val schema_att_metaMark_vis: SchemaNode           = SEmpty
  lazy val schema_att_metadataPointing: SchemaNode       = SEmpty
  lazy val schema_att_meterConformance: SchemaNode       = SEmpty
  lazy val schema_att_meterConformance_bar: SchemaNode   = SEmpty
  lazy val schema_att_meterSig_anl: SchemaNode           = SEmpty
  lazy val schema_att_meterSig_ges: SchemaNode           = SEmpty
  lazy val schema_att_meterSig_log: SchemaNode           = SEmpty
  lazy val schema_att_meterSig_vis: SchemaNode           = SEmpty
  lazy val schema_att_meterSigDefault_log: SchemaNode    = SEmpty
  lazy val schema_att_meterSigDefault_vis: SchemaNode    = SEmpty
  lazy val schema_att_meterSigGrp_anl: SchemaNode        = SEmpty
  lazy val schema_att_meterSigGrp_ges: SchemaNode        = SEmpty
  lazy val schema_att_meterSigGrp_log: SchemaNode        = SEmpty
  lazy val schema_att_meterSigGrp_vis: SchemaNode        = SEmpty
  lazy val schema_att_midi_anl: SchemaNode               = SEmpty
  lazy val schema_att_midi_event: SchemaNode             = SEmpty
  lazy val schema_att_midi_ges: SchemaNode               = SEmpty
  lazy val schema_att_midi_log: SchemaNode               = SEmpty
  lazy val schema_att_midiInstrument: SchemaNode         = SEmpty
  lazy val schema_att_midiNumber: SchemaNode             = SEmpty
  lazy val schema_att_midiTempo: SchemaNode              = SEmpty
  lazy val schema_att_midiValue: SchemaNode              = SEmpty
  lazy val schema_att_midiValue2: SchemaNode             = SEmpty
  lazy val schema_att_midiVelocity: SchemaNode           = SEmpty
  lazy val schema_att_mmTempo: SchemaNode                = SEmpty
  lazy val schema_att_mordent_anl: SchemaNode            = SEmpty
  lazy val schema_att_mordent_ges: SchemaNode            = SEmpty
  lazy val schema_att_mordent_log: SchemaNode            = SEmpty
  lazy val schema_att_mordent_vis: SchemaNode            = SEmpty
  lazy val schema_att_multiRest_anl: SchemaNode          = SEmpty
  lazy val schema_att_multiRest_ges: SchemaNode          = SEmpty
  lazy val schema_att_multiRest_log: SchemaNode          = SEmpty
  lazy val schema_att_multiRest_vis: SchemaNode          = SEmpty
  lazy val schema_att_multiRpt_anl: SchemaNode           = SEmpty
  lazy val schema_att_multiRpt_ges: SchemaNode           = SEmpty
  lazy val schema_att_multiRpt_log: SchemaNode           = SEmpty
  lazy val schema_att_multiRpt_vis: SchemaNode           = SEmpty
  lazy val schema_att_multinumMeasures: SchemaNode       = SEmpty
  lazy val schema_att_nInteger: SchemaNode               = SEmpty
  lazy val schema_att_nNumberLike: SchemaNode            = SEmpty
  lazy val schema_att_name: SchemaNode                   = SEmpty
  lazy val schema_att_notationStyle: SchemaNode          = SEmpty
  lazy val schema_att_notationType: SchemaNode           = SEmpty
  lazy val schema_att_note_anl: SchemaNode               = SEmpty
  lazy val schema_att_note_anl_cmn: SchemaNode           = SEmpty
  lazy val schema_att_note_ges: SchemaNode               = SEmpty
  lazy val schema_att_note_log: SchemaNode               = SEmpty
  lazy val schema_att_note_log_cmn: SchemaNode           = SEmpty
  lazy val schema_att_note_vis: SchemaNode               = SEmpty
  lazy val schema_att_note_vis_cmn: SchemaNode           = SEmpty
  lazy val schema_att_noteHeads: SchemaNode              = SEmpty
  lazy val schema_att_numberPlacement: SchemaNode        = SEmpty
  lazy val schema_att_numbered: SchemaNode               = SEmpty
  lazy val schema_att_octave: SchemaNode                 = SEmpty
  lazy val schema_att_octave_anl: SchemaNode             = SEmpty
  lazy val schema_att_octave_ges: SchemaNode             = SEmpty
  lazy val schema_att_octave_log: SchemaNode             = SEmpty
  lazy val schema_att_octave_vis: SchemaNode             = SEmpty
  lazy val schema_att_octaveDefault: SchemaNode          = SEmpty
  lazy val schema_att_octaveDisplacement: SchemaNode     = SEmpty
  lazy val schema_att_oneLineStaff: SchemaNode           = SEmpty
  lazy val schema_att_optimization: SchemaNode           = SEmpty
  lazy val schema_att_origin_layerIdent: SchemaNode      = SEmpty
  lazy val schema_att_origin_staffIdent: SchemaNode      = SEmpty
  lazy val schema_att_origin_startEndId: SchemaNode      = SEmpty
  lazy val schema_att_origin_timestamp_log: SchemaNode   = SEmpty
  lazy val schema_att_ornam_anl: SchemaNode              = SEmpty
  lazy val schema_att_ornam_ges: SchemaNode              = SEmpty
  lazy val schema_att_ornam_log: SchemaNode              = SEmpty
  lazy val schema_att_ornam_vis: SchemaNode              = SEmpty
  lazy val schema_att_ornamPresent: SchemaNode           = SEmpty
  lazy val schema_att_ornamentAccid: SchemaNode          = SEmpty
  lazy val schema_att_ornamentAccid_ges: SchemaNode      = SEmpty
  lazy val schema_att_ossia_anl: SchemaNode              = SEmpty
  lazy val schema_att_ossia_ges: SchemaNode              = SEmpty
  lazy val schema_att_ossia_log: SchemaNode              = SEmpty
  lazy val schema_att_ossia_vis: SchemaNode              = SEmpty
  lazy val schema_att_pad_anl: SchemaNode                = SEmpty
  lazy val schema_att_pad_ges: SchemaNode                = SEmpty
  lazy val schema_att_pad_log: SchemaNode                = SEmpty
  lazy val schema_att_pad_vis: SchemaNode                = SEmpty
  lazy val schema_att_pages: SchemaNode                  = SEmpty
  lazy val schema_att_part_anl: SchemaNode               = SEmpty
  lazy val schema_att_part_ges: SchemaNode               = SEmpty
  lazy val schema_att_part_log: SchemaNode               = SEmpty
  lazy val schema_att_part_vis: SchemaNode               = SEmpty
  lazy val schema_att_partIdent: SchemaNode              = SEmpty
  lazy val schema_att_parts_anl: SchemaNode              = SEmpty
  lazy val schema_att_parts_ges: SchemaNode              = SEmpty
  lazy val schema_att_parts_log: SchemaNode              = SEmpty
  lazy val schema_att_parts_vis: SchemaNode              = SEmpty
  lazy val schema_att_pb_anl: SchemaNode                 = SEmpty
  lazy val schema_att_pb_ges: SchemaNode                 = SEmpty
  lazy val schema_att_pb_log: SchemaNode                 = SEmpty
  lazy val schema_att_pb_vis: SchemaNode                 = SEmpty
  lazy val schema_att_pedal_anl: SchemaNode              = SEmpty
  lazy val schema_att_pedal_ges: SchemaNode              = SEmpty
  lazy val schema_att_pedal_log: SchemaNode              = SEmpty
  lazy val schema_att_pedal_vis: SchemaNode              = SEmpty
  lazy val schema_att_perfRes: SchemaNode                = SEmpty
  lazy val schema_att_perfRes_basic: SchemaNode          = SEmpty
  lazy val schema_att_phrase_anl: SchemaNode             = SEmpty
  lazy val schema_att_phrase_ges: SchemaNode             = SEmpty
  lazy val schema_att_phrase_log: SchemaNode             = SEmpty
  lazy val schema_att_phrase_vis: SchemaNode             = SEmpty
  lazy val schema_att_phrase_vis_cmn: SchemaNode         = SEmpty
  lazy val schema_att_pianoPedals: SchemaNode            = SEmpty
  lazy val schema_att_pitch: SchemaNode                  = SEmpty
  lazy val schema_att_pitch_ges: SchemaNode              = SEmpty
  lazy val schema_att_pitchClass: SchemaNode             = SEmpty
  lazy val schema_att_pitched: SchemaNode                = SEmpty
  lazy val schema_att_placementOnStaff: SchemaNode       = SEmpty
  lazy val schema_att_placementRelEvent: SchemaNode      = SEmpty
  lazy val schema_att_placementRelStaff: SchemaNode      = SEmpty
  lazy val schema_att_plist: SchemaNode                  = SEmpty
  lazy val schema_att_pointing: SchemaNode               = SEmpty
  lazy val schema_att_quantity: SchemaNode               = SEmpty
  lazy val schema_att_ranging: SchemaNode                = SEmpty
  lazy val schema_att_rdg_anl: SchemaNode                = SEmpty
  lazy val schema_att_rdg_ges: SchemaNode                = SEmpty
  lazy val schema_att_rdg_log: SchemaNode                = SEmpty
  lazy val schema_att_rdg_vis: SchemaNode                = SEmpty
  lazy val schema_att_reasonIdent: SchemaNode            = SEmpty
  lazy val schema_att_recordType: SchemaNode             = SEmpty
  lazy val schema_att_refrain_anl: SchemaNode            = SEmpty
  lazy val schema_att_refrain_ges: SchemaNode            = SEmpty
  lazy val schema_att_refrain_log: SchemaNode            = SEmpty
  lazy val schema_att_refrain_vis: SchemaNode            = SEmpty
  lazy val schema_att_regularMethod: SchemaNode          = SEmpty
  lazy val schema_att_reh_anl: SchemaNode                = SEmpty
  lazy val schema_att_reh_ges: SchemaNode                = SEmpty
  lazy val schema_att_reh_log: SchemaNode                = SEmpty
  lazy val schema_att_reh_vis: SchemaNode                = SEmpty
  lazy val schema_att_rehearsal: SchemaNode              = SEmpty
  lazy val schema_att_repeatMark_anl: SchemaNode         = SEmpty
  lazy val schema_att_repeatMark_ges: SchemaNode         = SEmpty
  lazy val schema_att_repeatMark_log: SchemaNode         = SEmpty
  lazy val schema_att_repeatMark_vis: SchemaNode         = SEmpty
  lazy val schema_att_responsibility: SchemaNode         = SEmpty
  lazy val schema_att_rest_anl: SchemaNode               = SEmpty
  lazy val schema_att_rest_anl_cmn: SchemaNode           = SEmpty
  lazy val schema_att_rest_ges: SchemaNode               = SEmpty
  lazy val schema_att_rest_log: SchemaNode               = SEmpty
  lazy val schema_att_rest_log_cmn: SchemaNode           = SEmpty
  lazy val schema_att_rest_vis: SchemaNode               = SEmpty
  lazy val schema_att_rest_vis_cmn: SchemaNode           = SEmpty
  lazy val schema_att_restduration_log: SchemaNode       = SEmpty
  lazy val schema_att_sb_anl: SchemaNode                 = SEmpty
  lazy val schema_att_sb_ges: SchemaNode                 = SEmpty
  lazy val schema_att_sb_log: SchemaNode                 = SEmpty
  lazy val schema_att_sb_vis: SchemaNode                 = SEmpty
  lazy val schema_att_scalable: SchemaNode               = SEmpty
  lazy val schema_att_score_anl: SchemaNode              = SEmpty
  lazy val schema_att_score_ges: SchemaNode              = SEmpty
  lazy val schema_att_score_log: SchemaNode              = SEmpty
  lazy val schema_att_score_vis: SchemaNode              = SEmpty
  lazy val schema_att_scoreDef_anl: SchemaNode           = SEmpty
  lazy val schema_att_scoreDef_ges: SchemaNode           = SEmpty
  lazy val schema_att_scoreDef_log: SchemaNode           = SEmpty
  lazy val schema_att_scoreDef_log_cmn: SchemaNode       = SEmpty
  lazy val schema_att_scoreDef_vis: SchemaNode           = SEmpty
  lazy val schema_att_scoreDef_vis_cmn: SchemaNode       = SEmpty
  lazy val schema_att_scoreDef_vis_tablature: SchemaNode = SEmpty
  lazy val schema_att_section_anl: SchemaNode            = SEmpty
  lazy val schema_att_section_ges: SchemaNode            = SEmpty
  lazy val schema_att_section_log: SchemaNode            = SEmpty
  lazy val schema_att_section_vis: SchemaNode            = SEmpty
  lazy val schema_att_sequence: SchemaNode               = SEmpty
  lazy val schema_att_slashCount: SchemaNode             = SEmpty
  lazy val schema_att_slur_anl: SchemaNode               = SEmpty
  lazy val schema_att_slur_ges: SchemaNode               = SEmpty
  lazy val schema_att_slur_log: SchemaNode               = SEmpty
  lazy val schema_att_slur_vis: SchemaNode               = SEmpty
  lazy val schema_att_slurPresent: SchemaNode            = SEmpty
  lazy val schema_att_slurRend: SchemaNode               = SEmpty
  lazy val schema_att_solfa: SchemaNode                  = SEmpty
  lazy val schema_att_soundLocation: SchemaNode          = SEmpty
  lazy val schema_att_source: SchemaNode                 = SEmpty
  lazy val schema_att_sp_anl: SchemaNode                 = SEmpty
  lazy val schema_att_sp_ges: SchemaNode                 = SEmpty
  lazy val schema_att_sp_log: SchemaNode                 = SEmpty
  lazy val schema_att_sp_vis: SchemaNode                 = SEmpty
  lazy val schema_att_space_anl: SchemaNode              = SEmpty
  lazy val schema_att_space_anl_cmn: SchemaNode          = SEmpty
  lazy val schema_att_space_ges: SchemaNode              = SEmpty
  lazy val schema_att_space_log: SchemaNode              = SEmpty
  lazy val schema_att_space_log_cmn: SchemaNode          = SEmpty
  lazy val schema_att_space_vis: SchemaNode              = SEmpty
  lazy val schema_att_spacing: SchemaNode                = SEmpty
  lazy val schema_att_staff_anl: SchemaNode              = SEmpty
  lazy val schema_att_staff_ges: SchemaNode              = SEmpty
  lazy val schema_att_staff_log: SchemaNode              = SEmpty
  lazy val schema_att_staff_vis: SchemaNode              = SEmpty
  lazy val schema_att_staffDef_anl: SchemaNode           = SEmpty
  lazy val schema_att_staffDef_ges: SchemaNode           = SEmpty
  lazy val schema_att_staffDef_log: SchemaNode           = SEmpty
  lazy val schema_att_staffDef_log_cmn: SchemaNode       = SEmpty
  lazy val schema_att_staffDef_vis: SchemaNode           = SEmpty
  lazy val schema_att_staffDef_vis_cmn: SchemaNode       = SEmpty
  lazy val schema_att_staffDef_vis_tablature: SchemaNode = SEmpty
  lazy val schema_att_staffGroupingSym: SchemaNode       = SEmpty
  lazy val schema_att_staffGrp_anl: SchemaNode           = SEmpty
  lazy val schema_att_staffGrp_ges: SchemaNode           = SEmpty
  lazy val schema_att_staffGrp_log: SchemaNode           = SEmpty
  lazy val schema_att_staffGrp_vis: SchemaNode           = SEmpty
  lazy val schema_att_staffIdent: SchemaNode             = SEmpty
  lazy val schema_att_staffItems: SchemaNode             = SEmpty
  lazy val schema_att_staffLoc: SchemaNode               = SEmpty
  lazy val schema_att_staffLoc_pitched: SchemaNode       = SEmpty
  lazy val schema_att_stageDir_anl: SchemaNode           = SEmpty
  lazy val schema_att_stageDir_ges: SchemaNode           = SEmpty
  lazy val schema_att_stageDir_log: SchemaNode           = SEmpty
  lazy val schema_att_stageDir_vis: SchemaNode           = SEmpty
  lazy val schema_att_startEndId: SchemaNode             = SEmpty
  lazy val schema_att_startId: SchemaNode                = SEmpty
  lazy val schema_att_stems: SchemaNode                  = SEmpty
  lazy val schema_att_stems_cmn: SchemaNode              = SEmpty
  lazy val schema_att_stringtab: SchemaNode              = SEmpty
  lazy val schema_att_stringtab_position: SchemaNode     = SEmpty
  lazy val schema_att_stringtab_tuning: SchemaNode       = SEmpty
  lazy val schema_att_syl_anl: SchemaNode                = SEmpty
  lazy val schema_att_syl_ges: SchemaNode                = SEmpty
  lazy val schema_att_syl_log: SchemaNode                = SEmpty
  lazy val schema_att_syl_vis: SchemaNode                = SEmpty
  lazy val schema_att_sylText: SchemaNode                = SEmpty
  lazy val schema_att_symbol_anl: SchemaNode             = SEmpty
  lazy val schema_att_symbol_ges: SchemaNode             = SEmpty
  lazy val schema_att_symbol_log: SchemaNode             = SEmpty
  lazy val schema_att_symbol_vis: SchemaNode             = SEmpty
  lazy val schema_att_systems: SchemaNode                = SEmpty
  lazy val schema_att_tabDurSym_anl: SchemaNode          = SEmpty
  lazy val schema_att_tabDurSym_ges: SchemaNode          = SEmpty
  lazy val schema_att_tabDurSym_log: SchemaNode          = SEmpty
  lazy val schema_att_tabDurSym_vis: SchemaNode          = SEmpty
  lazy val schema_att_tabGrp_anl: SchemaNode             = SEmpty
  lazy val schema_att_tabGrp_ges: SchemaNode             = SEmpty
  lazy val schema_att_tabGrp_log: SchemaNode             = SEmpty
  lazy val schema_att_tabGrp_vis: SchemaNode             = SEmpty
  lazy val schema_att_tabular: SchemaNode                = SEmpty
  lazy val schema_att_targetEval: SchemaNode             = SEmpty
  lazy val schema_att_tempo_anl: SchemaNode              = SEmpty
  lazy val schema_att_tempo_ges: SchemaNode              = SEmpty
  lazy val schema_att_tempo_log: SchemaNode              = SEmpty
  lazy val schema_att_tempo_vis: SchemaNode              = SEmpty
  lazy val schema_att_textRendition: SchemaNode          = SEmpty
  lazy val schema_att_textStyle: SchemaNode              = SEmpty
  lazy val schema_att_tie_anl: SchemaNode                = SEmpty
  lazy val schema_att_tie_ges: SchemaNode                = SEmpty
  lazy val schema_att_tie_log: SchemaNode                = SEmpty
  lazy val schema_att_tie_vis: SchemaNode                = SEmpty
  lazy val schema_att_tiePresent: SchemaNode             = SEmpty
  lazy val schema_att_tieRend: SchemaNode                = SEmpty
  lazy val schema_att_timeBase: SchemaNode               = SEmpty
  lazy val schema_att_timestamp_ges: SchemaNode          = SEmpty
  lazy val schema_att_timestamp_log: SchemaNode          = SEmpty
  lazy val schema_att_timestamp2_ges: SchemaNode         = SEmpty
  lazy val schema_att_timestamp2_log: SchemaNode         = SEmpty
  lazy val schema_att_trans: SchemaNode                  = SEmpty
  lazy val schema_att_transposition: SchemaNode          = SEmpty
  lazy val schema_att_tremForm: SchemaNode               = SEmpty
  lazy val schema_att_tremMeasured: SchemaNode           = SEmpty
  lazy val schema_att_trill_anl: SchemaNode              = SEmpty
  lazy val schema_att_trill_ges: SchemaNode              = SEmpty
  lazy val schema_att_trill_log: SchemaNode              = SEmpty
  lazy val schema_att_trill_vis: SchemaNode              = SEmpty
  lazy val schema_att_tuning: SchemaNode                 = SEmpty
  lazy val schema_att_tuning_anl: SchemaNode             = SEmpty
  lazy val schema_att_tuning_ges: SchemaNode             = SEmpty
  lazy val schema_att_tuning_log: SchemaNode             = SEmpty
  lazy val schema_att_tuning_vis: SchemaNode             = SEmpty
  lazy val schema_att_tuplet_anl: SchemaNode             = SEmpty
  lazy val schema_att_tuplet_ges: SchemaNode             = SEmpty
  lazy val schema_att_tuplet_log: SchemaNode             = SEmpty
  lazy val schema_att_tuplet_vis: SchemaNode             = SEmpty
  lazy val schema_att_tupletPresent: SchemaNode          = SEmpty
  lazy val schema_att_tupletSpan_anl: SchemaNode         = SEmpty
  lazy val schema_att_tupletSpan_ges: SchemaNode         = SEmpty
  lazy val schema_att_tupletSpan_log: SchemaNode         = SEmpty
  lazy val schema_att_tupletSpan_vis: SchemaNode         = SEmpty
  lazy val schema_att_turn_anl: SchemaNode               = SEmpty
  lazy val schema_att_turn_ges: SchemaNode               = SEmpty
  lazy val schema_att_turn_log: SchemaNode               = SEmpty
  lazy val schema_att_turn_vis: SchemaNode               = SEmpty
  lazy val schema_att_typed: SchemaNode                  = SEmpty
  lazy val schema_att_typography: SchemaNode             = SEmpty
  lazy val schema_att_verse_anl: SchemaNode              = SEmpty
  lazy val schema_att_verse_ges: SchemaNode              = SEmpty
  lazy val schema_att_verse_log: SchemaNode              = SEmpty
  lazy val schema_att_verse_vis: SchemaNode              = SEmpty
  lazy val schema_att_verticalAlign: SchemaNode          = SEmpty
  lazy val schema_att_verticalGroup: SchemaNode          = SEmpty
  lazy val schema_att_visibility: SchemaNode             = SEmpty
  lazy val schema_att_visualOffset: SchemaNode           = SEmpty
  lazy val schema_att_visualOffset_ho: SchemaNode        = SEmpty
  lazy val schema_att_visualOffset_to: SchemaNode        = SEmpty
  lazy val schema_att_visualOffset_vo: SchemaNode        = SEmpty
  lazy val schema_att_visualOffset2: SchemaNode          = SEmpty
  lazy val schema_att_visualOffset2_ho: SchemaNode       = SEmpty
  lazy val schema_att_visualOffset2_to: SchemaNode       = SEmpty
  lazy val schema_att_visualOffset2_vo: SchemaNode       = SEmpty
  lazy val schema_att_volta_anl: SchemaNode              = SEmpty
  lazy val schema_att_volta_ges: SchemaNode              = SEmpty
  lazy val schema_att_volta_log: SchemaNode              = SEmpty
  lazy val schema_att_volta_vis: SchemaNode              = SEmpty
  lazy val schema_att_voltaGroupingSym: SchemaNode       = SEmpty
  lazy val schema_att_whitespace: SchemaNode             = SEmpty
  lazy val schema_att_width: SchemaNode                  = SEmpty
  lazy val schema_att_xy: SchemaNode                     = SEmpty
  lazy val schema_att_xy2: SchemaNode                    = SEmpty
  lazy val schema_attUsage: SchemaNode                   = SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc]))
  lazy val schema_attacca: SchemaNode                    = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_audience: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_author: SchemaNode   = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_avFile: SchemaNode       = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.Clip]))
  lazy val schema_availability: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.header.macro_.AvailabilityPart])
  lazy val schema_bTrem: SchemaNode        = SChoice(
    scala.List(SRef(classOf[_root_.sheet.meicmn.mei.shared.Chord]), SRef(classOf[_root_.sheet.meicmn.mei.shared.Note])),
  )
  lazy val schema_back: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.FrontAndBackPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
      ),
    ),
  )
  lazy val schema_barLine: SchemaNode = SEmpty
  lazy val schema_barre: SchemaNode   = SEmpty
  lazy val schema_beam: SchemaNode    = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EventLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_beamSpan: SchemaNode = SEmpty
  lazy val schema_beatRpt: SchemaNode  = SEmpty
  lazy val schema_bend: SchemaNode     = SEmpty
  lazy val schema_bibl: SchemaNode     = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ImprintPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
      ),
    ),
  )
  lazy val schema_biblList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SSeq(
              scala.List(
                SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label])),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblLike]),
              ),
            ),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_biblScope: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_biblStruct: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Analytic])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Monogr])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Identifier]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.RelatedItem]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_bifolium: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.FoliumLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.BifoliumLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.PaperModLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Del]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Add]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Damage]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Gap]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Restore]),
      ),
    ),
  )
  lazy val schema_binding: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dimensions]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_bindingDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
      SChoice(
        scala.List(
          SSeq(
            scala.List(
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Binding])),
              SZeroOrMore(
                SChoice(
                  scala.List(
                    SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
                    SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
                    SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
                  ),
                ),
              ),
            ),
          ),
          SZeroOrMore(
            SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
          ),
        ),
      ),
    ),
  )
  lazy val schema_bloc: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_body: SchemaNode = SOneOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MdivLike]),
      ),
    ),
  )
  lazy val schema_bracketSpan: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_breath: SchemaNode = SEmpty
  lazy val schema_byline: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
      ),
    ),
  )
  lazy val schema_caesura: SchemaNode = SEmpty
  lazy val schema_caption: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_captureMode: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_carrierForm: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_castGrp: SchemaNode     = SOneOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.CastItem]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.CastGrp]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.RoleDesc]),
      ),
    ),
  )
  lazy val schema_castItem: SchemaNode = SOneOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Role]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.RoleDesc]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Actor]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.PerfRes]),
      ),
    ),
  )
  lazy val schema_castList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.CastItem]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.CastGrp]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_catRel: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
    ),
  )
  lazy val schema_catchwords: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_category: SchemaNode   = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.AltId])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.CatRel])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Category])),
    ),
  )
  lazy val schema_cb: SchemaNode     = SEmpty
  lazy val schema_cc: SchemaNode     = SEmpty
  lazy val schema_chan: SchemaNode   = SEmpty
  lazy val schema_chanPr: SchemaNode = SEmpty
  lazy val schema_change: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt])),
      SRef(classOf[_root_.sheet.meicmn.mei.header.ChangeDesc]),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.DateLike])),
    ),
  )
  lazy val schema_changeDesc: SchemaNode = SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]))
  lazy val schema_choice: SchemaNode     = SZeroOrMore(
    SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.ChoicePart]))),
  )
  lazy val schema_chord: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ChordPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.lyrics.model.VerseLike]),
      ),
    ),
  )
  lazy val schema_chordDef: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.harmony.ChordMember])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.stringtab.Barre])),
    ),
  )
  lazy val schema_chordMember: SchemaNode = SEmpty
  lazy val schema_chordTable: SchemaNode  = SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.harmony.ChordDef]))
  lazy val schema_classDecls: SchemaNode  = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Taxonomy])),
    ),
  )
  lazy val schema_classification: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.header.TermList])))),
    ),
  )
  lazy val schema_clef: SchemaNode    = SEmpty
  lazy val schema_clefGrp: SchemaNode = SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Clef]))
  lazy val schema_clip: SchemaNode    = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.AvFile])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.When])),
    ),
  )
  lazy val schema_colLayout: SchemaNode     = SEmpty
  lazy val schema_collation: SchemaNode     = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_colophon: SchemaNode      = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_componentList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SChoice(
            scala.List(
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.WorkLike])),
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ExpressionLike])),
            ),
          ),
          SChoice(
            scala.List(
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ManifestationLike])),
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ItemLike])),
            ),
          ),
        ),
      ),
    ),
  )
  lazy val schema_composer: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_condition: SchemaNode   = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_contentItem: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_contents: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
          SOneOrMore(SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label]))),
        ),
      ),
    ),
  )
  lazy val schema_context: SchemaNode     = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_contributor: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_corpName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_corr: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_correction: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_country: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_course: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.stringtab.String]))
  lazy val schema_cpMark: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_creation: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SText,
            SRef(classOf[_root_.sheet.meicmn.mei.header.Dedication]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_cue: SchemaNode    = SText
  lazy val schema_curve: SchemaNode  = SEmpty
  lazy val schema_custos: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Accid]))
  lazy val schema_cutout: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.FoliumLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.BifoliumLike]),
      ),
    ),
  )
  lazy val schema_damage: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_data_ACCIDENTAL_GESTURAL: SchemaNode          = SText
  lazy val schema_data_ACCIDENTAL_GESTURAL_basic: SchemaNode    = SText
  lazy val schema_data_ACCIDENTAL_GESTURAL_extended: SchemaNode = SText
  lazy val schema_data_ACCIDENTAL_WRITTEN: SchemaNode           = SText
  lazy val schema_data_ACCIDENTAL_WRITTEN_basic: SchemaNode     = SText
  lazy val schema_data_ACCIDENTAL_WRITTEN_extended: SchemaNode  = SText
  lazy val schema_data_ACCIDENTAL_aeu: SchemaNode               = SText
  lazy val schema_data_ACCIDENTAL_persian: SchemaNode           = SText
  lazy val schema_data_ARTICULATION: SchemaNode                 = SText
  lazy val schema_data_AUGMENTDOT: SchemaNode                   = SText
  lazy val schema_data_BARMETHOD: SchemaNode                    = SText
  lazy val schema_data_BARRENDITION: SchemaNode                 = SText
  lazy val schema_data_BEAM: SchemaNode                         = SText
  lazy val schema_data_BEAMPLACE: SchemaNode                    = SText
  lazy val schema_data_BEAT: SchemaNode                         = SText
  lazy val schema_data_BEATRPT_REND: SchemaNode                 = SChoice(scala.List(SText, SText))
  lazy val schema_data_BEND_AMOUNT: SchemaNode                  = SChoice(scala.List(SText, SText, SText))
  lazy val schema_data_BETYPE: SchemaNode                       = SText
  lazy val schema_data_BOOLEAN: SchemaNode                      = SText
  lazy val schema_data_CANCELACCID: SchemaNode                  = SText
  lazy val schema_data_CERTAINTY: SchemaNode                    = SText
  lazy val schema_data_CLEFLINE: SchemaNode                     = SText
  lazy val schema_data_CLEFSHAPE: SchemaNode                    = SText
  lazy val schema_data_CLUSTER: SchemaNode                      = SText
  lazy val schema_data_COLOR: SchemaNode                        = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.COLORNAMES]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.COLORVALUES]),
    ),
  )
  lazy val schema_data_COLORNAMES: SchemaNode       = SText
  lazy val schema_data_COLORVALUES: SchemaNode      = SChoice(scala.List(SText, SText, SText, SText, SText, SText))
  lazy val schema_data_COMPASSDIRECTION: SchemaNode = SText
  lazy val schema_data_COMPASSDIRECTION_basic: SchemaNode    = SText
  lazy val schema_data_COMPASSDIRECTION_extended: SchemaNode = SText
  lazy val schema_data_CONFIDENCE: SchemaNode                = SText
  lazy val schema_data_COURSENUMBER: SchemaNode              = SText
  lazy val schema_data_COURSETUNING: SchemaNode              = SText
  lazy val schema_data_DEGREES: SchemaNode                   = SText
  lazy val schema_data_DURATION: SchemaNode                  = SChoice(
    scala.List(SRef(classOf[_root_.sheet.meicmn.mei.cmn.data.duration.Cmn]), SRef(classOf[sheet.meicmn.Element])),
  )
  lazy val schema_data_DURATION_GESTURAL: SchemaNode = SChoice(
    scala.List(SRef(classOf[_root_.sheet.meicmn.mei.cmn.data.duration.Cmn]), SRef(classOf[sheet.meicmn.Element])),
  )
  lazy val schema_data_DURATION_cmn: SchemaNode  = SText
  lazy val schema_data_DURATIONRESTS: SchemaNode = SChoice(
    scala.List(SRef(classOf[_root_.sheet.meicmn.mei.cmn.data.duration.Cmn]), SRef(classOf[sheet.meicmn.Element])),
  )
  lazy val schema_data_ENCLOSURE: SchemaNode         = SText
  lazy val schema_data_EVENTREL: SchemaNode          = SText
  lazy val schema_data_EVENTREL_basic: SchemaNode    = SText
  lazy val schema_data_EVENTREL_extended: SchemaNode = SText
  lazy val schema_data_FILL: SchemaNode              = SText
  lazy val schema_data_FINGER_FRET: SchemaNode       = SChoice(scala.List(SText, SText))
  lazy val schema_data_FONTFAMILY: SchemaNode        = SText
  lazy val schema_data_FONTNAME: SchemaNode          = SText
  lazy val schema_data_FONTSIZE: SchemaNode          = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.FONTSIZENUMERIC]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.FONTSIZETERM]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.PERCENT]),
    ),
  )
  lazy val schema_data_FONTSIZENUMERIC: SchemaNode   = SText
  lazy val schema_data_FONTSIZESCALE: SchemaNode     = SText
  lazy val schema_data_FONTSIZETERM: SchemaNode      = SText
  lazy val schema_data_FONTSTYLE: SchemaNode         = SText
  lazy val schema_data_FONTWEIGHT: SchemaNode        = SText
  lazy val schema_data_FRBRRELATIONSHIP: SchemaNode  = SText
  lazy val schema_data_FRETNUMBER: SchemaNode        = SText
  lazy val schema_data_GLISSANDO: SchemaNode         = SText
  lazy val schema_data_GRACE: SchemaNode             = SText
  lazy val schema_data_HARPPEDALPOSITION: SchemaNode = SText
  lazy val schema_data_HEADSHAPE: SchemaNode         = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.headshape.List]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.HEXNUM]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.NMTOKEN]),
    ),
  )
  lazy val schema_data_HEADSHAPE_list: SchemaNode      = SText
  lazy val schema_data_HEXNUM: SchemaNode              = SText
  lazy val schema_data_HORIZONTALALIGNMENT: SchemaNode = SText
  lazy val schema_data_INTERVAL_HARMONIC: SchemaNode   = SChoice(scala.List(SText))
  lazy val schema_data_INTERVAL_MELODIC: SchemaNode    = SChoice(scala.List(SText, SText, SText))
  lazy val schema_data_ISODATE: SchemaNode             = SChoice(
    scala.List(SText, SText, SText, SText, SText, SText, SText, SText, SText),
  )
  lazy val schema_data_ISOTIME: SchemaNode            = SText
  lazy val schema_data_KEYFIFTHS: SchemaNode          = SText
  lazy val schema_data_LAYERSCHEME: SchemaNode        = SText
  lazy val schema_data_LIGATUREFORM: SchemaNode       = SText
  lazy val schema_data_LINEFORM: SchemaNode           = SText
  lazy val schema_data_LINESTARTENDSYMBOL: SchemaNode = SText
  lazy val schema_data_LINEWIDTH: SchemaNode          = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.LINEWIDTHTERM]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.MEASUREMENTUNSIGNED]),
    ),
  )
  lazy val schema_data_LINEWIDTHTERM: SchemaNode               = SText
  lazy val schema_data_MEASUREBEAT: SchemaNode                 = SText
  lazy val schema_data_MEASUREBEATOFFSET: SchemaNode           = SText
  lazy val schema_data_MEASUREMENTFONTSIGNED: SchemaNode       = SText
  lazy val schema_data_MEASUREMENTFONTUNSIGNED: SchemaNode     = SText
  lazy val schema_data_MEASUREMENTSIGNED: SchemaNode           = SText
  lazy val schema_data_MEASUREMENTTYPOGRAPHYSIGNED: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.MEASUREMENTFONTSIGNED]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.MEASUREMENTSIGNED]),
    ),
  )
  lazy val schema_data_MEASUREMENTTYPOGRAPHYUNSIGNED: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.MEASUREMENTFONTUNSIGNED]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.MEASUREMENTUNSIGNED]),
    ),
  )
  lazy val schema_data_MEASUREMENTUNSIGNED: SchemaNode = SText
  lazy val schema_data_MELODICFUNCTION: SchemaNode     = SText
  lazy val schema_data_MENSURATIONSIGN: SchemaNode     = SText
  lazy val schema_data_METERFORM: SchemaNode           = SText
  lazy val schema_data_METERSIGN: SchemaNode           = SText
  lazy val schema_data_MIDIBPM: SchemaNode             = SText
  lazy val schema_data_MIDICHANNEL: SchemaNode         = SText
  lazy val schema_data_MIDIMSPB: SchemaNode            = SText
  lazy val schema_data_MIDINAMES: SchemaNode           = SText
  lazy val schema_data_MIDIVALUE: SchemaNode           = SText
  lazy val schema_data_MIDIVALUE_NAME: SchemaNode      = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.MIDIVALUE]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.NCNAME]),
    ),
  )
  lazy val schema_data_MIDIVALUE_PAN: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.MIDIVALUE]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.percent.limited.SIGNED]),
    ),
  )
  lazy val schema_data_MIDIVALUE_PERCENT: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.MIDIVALUE]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.percent.LIMITED]),
    ),
  )
  lazy val schema_data_MODE: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.mode.Cmn]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.mode.Gregorian]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.mode.Extended]),
    ),
  )
  lazy val schema_data_MODE_cmn: SchemaNode         = SText
  lazy val schema_data_MODE_extended: SchemaNode    = SText
  lazy val schema_data_MODE_gregorian: SchemaNode   = SText
  lazy val schema_data_MODSRELATIONSHIP: SchemaNode = SText
  lazy val schema_data_MUSICFONT: SchemaNode        = SText
  lazy val schema_data_NCNAME: SchemaNode           = SText
  lazy val schema_data_NEIGHBORINGLAYER: SchemaNode = SText
  lazy val schema_data_NMTOKEN: SchemaNode          = SText
  lazy val schema_data_NONSTAFFPLACE: SchemaNode    = SText
  lazy val schema_data_NOTATIONTYPE: SchemaNode     = SText
  lazy val schema_data_NOTEHEADMODIFIER: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.noteheadmodifier.List]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.noteheadmodifier.Pat]),
    ),
  )
  lazy val schema_data_NOTEHEADMODIFIER_list: SchemaNode  = SText
  lazy val schema_data_NOTEHEADMODIFIER_pat: SchemaNode   = SChoice(scala.List(SText, SText))
  lazy val schema_data_OCTAVE: SchemaNode                 = SText
  lazy val schema_data_OCTAVE_DIS: SchemaNode             = SText
  lazy val schema_data_ORIENTATION: SchemaNode            = SText
  lazy val schema_data_ORNAM_cmn: SchemaNode              = SText
  lazy val schema_data_PAGE_PANELS: SchemaNode            = SText
  lazy val schema_data_PEDALSTYLE: SchemaNode             = SText
  lazy val schema_data_PERCENT: SchemaNode                = SText
  lazy val schema_data_PERCENT_LIMITED: SchemaNode        = SText
  lazy val schema_data_PERCENT_LIMITED_SIGNED: SchemaNode = SText
  lazy val schema_data_PGFUNC: SchemaNode                 = SText
  lazy val schema_data_PGSCALE: SchemaNode    = SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.data.PERCENT])))
  lazy val schema_data_PITCHCLASS: SchemaNode = SText
  lazy val schema_data_PITCHNAME: SchemaNode  = SText
  lazy val schema_data_PITCHNAME_GESTURAL: SchemaNode = SText
  lazy val schema_data_PITCHNUMBER: SchemaNode        = SText
  lazy val schema_data_PLACEMENT: SchemaNode          = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.STAFFREL]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.NONSTAFFPLACE]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.NMTOKEN]),
    ),
  )
  lazy val schema_data_RELATIONSHIP: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.frbr.data.FRBRRELATIONSHIP]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.MODSRELATIONSHIP]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.NMTOKEN]),
    ),
  )
  lazy val schema_data_ROTATION: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.DEGREES]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.ROTATIONDIRECTION]),
    ),
  )
  lazy val schema_data_ROTATIONDIRECTION: SchemaNode = SText
  lazy val schema_data_SCALEDEGREE: SchemaNode       = SText
  lazy val schema_data_SLASH: SchemaNode             = SText
  lazy val schema_data_SLUR: SchemaNode              = SText
  lazy val schema_data_STAFFITEM: SchemaNode         = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.staffitem.Basic]),
      SRef(classOf[_root_.sheet.meicmn.mei.cmn.data.staffitem.Cmn]),
      SRef(classOf[sheet.meicmn.Element]),
      SRef(classOf[sheet.meicmn.Element]),
    ),
  )
  lazy val schema_data_STAFFITEM_basic: SchemaNode = SText
  lazy val schema_data_STAFFITEM_cmn: SchemaNode   = SText
  lazy val schema_data_STAFFLOC: SchemaNode        = SText
  lazy val schema_data_STAFFREL: SchemaNode        = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.staffrel.Basic]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.staffrel.Extended]),
    ),
  )
  lazy val schema_data_STAFFREL_basic: SchemaNode    = SText
  lazy val schema_data_STAFFREL_extended: SchemaNode = SText
  lazy val schema_data_STEMDIRECTION: SchemaNode     = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.stemdirection.Basic]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.stemdirection.Extended]),
    ),
  )
  lazy val schema_data_STEMDIRECTION_basic: SchemaNode    = SText
  lazy val schema_data_STEMDIRECTION_extended: SchemaNode = SText
  lazy val schema_data_STEMMODIFIER: SchemaNode           = SText
  lazy val schema_data_STEMPOSITION: SchemaNode           = SText
  lazy val schema_data_STRINGNUMBER: SchemaNode           = SText
  lazy val schema_data_TEMPERAMENT: SchemaNode            = SText
  lazy val schema_data_TEMPOVALUE: SchemaNode             = SText
  lazy val schema_data_TEXTRENDITION: SchemaNode          = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.data.TEXTRENDITIONLIST]),
      SRef(classOf[_root_.sheet.meicmn.mei.data.TEXTRENDITIONPAR]),
    ),
  )
  lazy val schema_data_TEXTRENDITIONLIST: SchemaNode = SText
  lazy val schema_data_TEXTRENDITIONPAR: SchemaNode  = SText
  lazy val schema_data_TIE: SchemaNode               = SText
  lazy val schema_data_TSTAMPOFFSET: SchemaNode      = SText
  lazy val schema_data_TUPLET: SchemaNode            = SText
  lazy val schema_data_URI: SchemaNode               = SText
  lazy val schema_data_VERTICALALIGNMENT: SchemaNode = SText
  lazy val schema_data_WORD: SchemaNode              = SText
  lazy val schema_date: SchemaNode                   = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]))),
  )
  lazy val schema_decoDesc: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
                SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_decoNote: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
                SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_dedicatee: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_dedication: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike])),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_del: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_depth: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_desc: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_dim: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_dimensions: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_dir: SchemaNode        = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_distributor: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_district: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_div: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.drama.Sp]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_domainsDecl: SchemaNode = SEmpty
  lazy val schema_dot: SchemaNode         = SEmpty
  lazy val schema_dynam: SchemaNode       = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_edition: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Editor]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.TextLang]),
      ),
    ),
  )
  lazy val schema_editionStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EditionLike])),
    ),
  )
  lazy val schema_editor: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_editorialDecl: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
          SSeq(
            scala.List(
              SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.EditorialDeclPart])),
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
            ),
          ),
        ),
      ),
    ),
  )
  lazy val schema_encodingDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.AppInfo])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.EditorialDecl])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ProjectDesc])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.SamplingDecl])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.DomainsDecl])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.TagsDecl])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ClassDecls])),
    ),
  )
  lazy val schema_ending: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Expansion])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.cmn.model.sectionpart.Cmn]),
            SRef(classOf[sheet.meicmn.Element]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_epigraph: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ParacontentPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
      ),
    ),
  )
  lazy val schema_event: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.header.model.EventPart]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.CastList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
              ),
            ),
          ),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
                SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.TableLike]),
                SRef(classOf[_root_.sheet.meicmn.mei.text.List]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList])),
    ),
  )
  lazy val schema_eventList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SSeq(
          scala.List(
            SOptional(
              SChoice(
                scala.List(
                  SRef(classOf[_root_.sheet.meicmn.mei.shared.Name]),
                  SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AddressLike]),
                  SRef(classOf[_root_.sheet.meicmn.mei.shared.model.DateLike]),
                  SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.namelike.Agent]),
                  SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.namelike.GeogName]),
                ),
              ),
            ),
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.Event]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList])),
    ),
  )
  lazy val schema_exhibHist: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_expan: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_expansion: SchemaNode  = SEmpty
  lazy val schema_explicit: SchemaNode   = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_expression: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.WorkIdent])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.OtherChar])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Creation])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.History])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.LangUsage])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PerfMedium])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PerfDuration])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Extent])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ScoreFormat])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Contents])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Context])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.NotesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Classification])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ComponentList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.RelationList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.ExtMeta])),
    ),
  )
  lazy val schema_expressionList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ExpressionLike])),
    ),
  )
  lazy val schema_extData: SchemaNode = SZeroOrMore(
    SSeq(scala.List(SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.AnyXML]))))),
  )
  lazy val schema_extMeta: SchemaNode = SZeroOrMore(
    SSeq(scala.List(SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.AnyXML]))))),
  )
  lazy val schema_extent: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_f: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_fTrem: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SRef(classOf[_root_.sheet.meicmn.mei.shared.Chord]),
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Clef])),
          SChoice(
            scala.List(
              SRef(classOf[_root_.sheet.meicmn.mei.shared.Chord]),
              SRef(classOf[_root_.sheet.meicmn.mei.shared.Note]),
            ),
          ),
        ),
      ),
      SSeq(
        scala.List(
          SRef(classOf[_root_.sheet.meicmn.mei.shared.Note]),
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Clef])),
          SChoice(
            scala.List(
              SRef(classOf[_root_.sheet.meicmn.mei.shared.Chord]),
              SRef(classOf[_root_.sheet.meicmn.mei.shared.Note]),
            ),
          ),
        ),
      ),
    ),
  )
  lazy val schema_facsimile: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.Graphic])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.facsimile.Surface])),
    ),
  )
  lazy val schema_famName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.namesdates.NameLink]),
      ),
    ),
  )
  lazy val schema_fb: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_fermata: SchemaNode = SEmpty
  lazy val schema_fig: SchemaNode     = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.figtable.FigDesc]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.CaptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.GraphicLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreLike]),
      ),
    ),
  )
  lazy val schema_figDesc: SchemaNode = SChoice(
    scala.List(
      SZeroOrMore(SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike])))),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_fileChar: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_fileDesc: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.header.TitleStmt]),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.EditionStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Extent])),
      SRef(classOf[_root_.sheet.meicmn.mei.header.PubStmt]),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.SeriesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.NotesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.SourceDesc])),
    ),
  )
  lazy val schema_fing: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_fingGrp: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.fingering.model.FingeringLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_foliaDesc: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.FoliumLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.BifoliumLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Del]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Add]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Damage]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Gap]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.Restore]),
      ),
    ),
  )
  lazy val schema_foliation: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_folium: SchemaNode    = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.PaperModLike]))
  lazy val schema_foreName: SchemaNode  = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_front: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.model.FrontAndBackPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
      ),
    ),
  )
  lazy val schema_funder: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_gap: SchemaNode     = SEmpty
  lazy val schema_genDesc: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.genetic.GenDesc]),
        SRef(classOf[_root_.sheet.meicmn.mei.genetic.GenState]),
      ),
    ),
  )
  lazy val schema_genName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_genState: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.DateLike])),
    ),
  )
  lazy val schema_genre: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_geogFeat: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_geogName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_gliss: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_graceGrp: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EventLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_graphic: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.facsimile.Zone]))
  lazy val schema_group: SchemaNode   = SSeq(
    scala.List(
      SChoice(
        scala.List(
          SRef(classOf[_root_.sheet.meicmn.mei.shared.Music]),
          SRef(classOf[_root_.sheet.meicmn.mei.shared.Group]),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Music]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Group]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_grpSym: SchemaNode   = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LabelLike]))
  lazy val schema_hairpin: SchemaNode  = SEmpty
  lazy val schema_halfmRpt: SchemaNode = SEmpty
  lazy val schema_hand: SchemaNode     = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_handList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label]))),
    ),
  )
  lazy val schema_handShift: SchemaNode = SEmpty
  lazy val schema_harm: SchemaNode      = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FigbassLike]),
      ),
    ),
  )
  lazy val schema_harpPedal: SchemaNode = SEmpty
  lazy val schema_head: SchemaNode      = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_height: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_heraldry: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_hex: SchemaNode      = SText
  lazy val schema_history: SchemaNode  = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.header.Acquisition]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.ExhibHist]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.Provenance]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.TreatHist]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.TreatSched]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_identifier: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_imprimatur: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ParacontentPart]))),
  )
  lazy val schema_imprint: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ImprintPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
      ),
    ),
  )
  lazy val schema_incip: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Tempo])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Role])),
      SOptional(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Clef]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.ClefGrp]),
          ),
        ),
      ),
      SOptional(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.header.PerfRes]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.PerfResList]),
          ),
        ),
      ),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Key])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Meter])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Mensuration])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.GraphicLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.IncipCode]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.IncipText]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_incipCode: SchemaNode = SText
  lazy val schema_incipText: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.LgLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_inscription: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_instrDef: SchemaNode    = SEmpty
  lazy val schema_instrGrp: SchemaNode    = SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.InstrDefLike]))
  lazy val schema_interpretation: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_item: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Availability])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PhysDesc])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.PhysLoc])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.History])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.NotesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Classification])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ComponentList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.RelationList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.ExtMeta])),
    ),
  )
  lazy val schema_itemList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ItemLike])),
    ),
  )
  lazy val schema_key: SchemaNode      = SText
  lazy val schema_keyAccid: SchemaNode = SEmpty
  lazy val schema_keySig: SchemaNode   = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_l: SchemaNode = SChoice(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SText,
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_label: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_labelAbbr: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_langUsage: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Language])),
    ),
  )
  lazy val schema_language: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_layer: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[sheet.meicmn.Element]),
      ),
    ),
  )
  lazy val schema_layerDef: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LabelLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.InstrDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MeterSigLike]),
          ),
        ),
      ),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Ambitus])),
    ),
  )
  lazy val schema_layout: SchemaNode     = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_layoutDesc: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
                SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Layout]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_lb: SchemaNode  = SEmpty
  lazy val schema_lem: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.RdgPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Expansion]),
      ),
    ),
  )
  lazy val schema_lg: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SRef(classOf[_root_.sheet.meicmn.mei.text.model.LLike]),
          SRef(classOf[_root_.sheet.meicmn.mei.text.model.LgLike]),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.LLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.LgLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_li: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_librettist: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_line: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_list: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label]))),
    ),
  )
  lazy val schema_locus: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Locus]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Rend]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Symbol]),
      ),
    ),
  )
  lazy val schema_locusGrp: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Locus]))
  lazy val schema_lv: SchemaNode       = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.Curve]))
  lazy val schema_lyricist: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_mNum: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RendLike]),
      ),
    ),
  )
  lazy val schema_mRest: SchemaNode                  = SEmpty
  lazy val schema_mRpt: SchemaNode                   = SEmpty
  lazy val schema_mRpt2: SchemaNode                  = SEmpty
  lazy val schema_mSpace: SchemaNode                 = SEmpty
  lazy val schema_macro_anyXML: SchemaNode           = SEmpty
  lazy val schema_macro_availabilityPart: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.header.AccessRestrict]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.Distributor]),
                SRef(classOf[_root_.sheet.meicmn.mei.header.Price]),
                SRef(classOf[_root_.sheet.meicmn.mei.header.SysReq]),
                SRef(classOf[_root_.sheet.meicmn.mei.header.UseRestrict]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AddressLike]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.DateLike]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
              ),
            ),
          ),
        ),
      ),
      SText,
    ),
  )
  lazy val schema_macro_bibldescPart: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.EditionStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PubStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Availability])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.PhysDesc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.PhysLoc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.SeriesStmt])),
    ),
  )
  lazy val schema_macro_metaLike_page: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.PgHead])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.PgFoot])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.PgDesc])),
    ),
  )
  lazy val schema_macro_musicPart: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.text.model.FrontLike])),
      SOptional(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Body]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Group]),
          ),
        ),
      ),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.text.model.BackLike])),
    ),
  )
  lazy val schema_macro_struc_unstrucContent: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_macro_titlePart: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AddressLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.AddressPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.DateLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EditorialLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.ptrref.model.LocrefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MeasurementLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.NameLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.namelike.Label]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PbLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RendLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Symbol]),
      ),
    ),
  )
  lazy val schema_manifestation: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Locus]),
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.LocusGrp]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.TitleStmt])),
      SRef(classOf[_root_.sheet.meicmn.mei.header.macro_.BibldescPart]),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Creation])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.History])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.LangUsage])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Contents])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.NotesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Classification])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.frbr.ItemList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ComponentList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.RelationList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.ExtMeta])),
    ),
  )
  lazy val schema_manifestationList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.frbr.model.ManifestationLike])),
    ),
  )
  lazy val schema_mapping: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.Symbol]))),
  )
  lazy val schema_marker: SchemaNode = SText
  lazy val schema_mdiv: SchemaNode   = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreLike])),
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PartsLike])),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MdivLike])),
    ),
  )
  lazy val schema_measure: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.cmn.MNum])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.cmn.model.MeasurePart]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_mei: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.header.MeiHead]),
      SRef(classOf[_root_.sheet.meicmn.mei.shared.Music]),
    ),
  )
  lazy val schema_meiCorpus: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.header.MeiHead]),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Mei])),
    ),
  )
  lazy val schema_meiHead: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.AltId])),
      SRef(classOf[_root_.sheet.meicmn.mei.header.FileDesc]),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.EncodingDesc])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.WorkList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.frbr.ManifestationList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.ExtMeta])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.RevisionDesc])),
    ),
  )
  lazy val schema_mensuration: SchemaNode = SText
  lazy val schema_metaMark: SchemaNode    = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_metaText: SchemaNode    = SText
  lazy val schema_meter: SchemaNode       = SText
  lazy val schema_meterSig: SchemaNode    = SEmpty
  lazy val schema_meterSigGrp: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MeterSigLike]),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MeterSigLike])),
    ),
  )
  lazy val schema_midi: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Cc]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Chan]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.ChanPr]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Cue]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Hex]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Marker]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.MetaText]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.NoteOff]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.NoteOn]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Port]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Prog]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.SeqNum]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.TrkName]),
        SRef(classOf[_root_.sheet.meicmn.mei.midi.Vel]),
      ),
    ),
  )
  lazy val schema_model_addressLike: SchemaNode              = SEmpty
  lazy val schema_model_addressPart: SchemaNode              = SEmpty
  lazy val schema_model_annotLike: SchemaNode                = SEmpty
  lazy val schema_model_appLike: SchemaNode                  = SEmpty
  lazy val schema_model_backLike: SchemaNode                 = SEmpty
  lazy val schema_model_biblLike: SchemaNode                 = SEmpty
  lazy val schema_model_biblPart: SchemaNode                 = SEmpty
  lazy val schema_model_bifoliumLike: SchemaNode             = SEmpty
  lazy val schema_model_captionLike: SchemaNode              = SEmpty
  lazy val schema_model_choicePart: SchemaNode               = SEmpty
  lazy val schema_model_chordPart: SchemaNode                = SEmpty
  lazy val schema_model_chordTableLike: SchemaNode           = SEmpty
  lazy val schema_model_controlEventLike: SchemaNode         = SEmpty
  lazy val schema_model_controlEventLike_cmn: SchemaNode     = SEmpty
  lazy val schema_model_controlEventLike_harmony: SchemaNode = SEmpty
  lazy val schema_model_dateLike: SchemaNode                 = SEmpty
  lazy val schema_model_dimLike: SchemaNode                  = SEmpty
  lazy val schema_model_divLike: SchemaNode                  = SEmpty
  lazy val schema_model_editLike: SchemaNode                 = SEmpty
  lazy val schema_model_editTransPart: SchemaNode            = SEmpty
  lazy val schema_model_editTransPart_music: SchemaNode      = SEmpty
  lazy val schema_model_editTransPart_text: SchemaNode       = SEmpty
  lazy val schema_model_editionLike: SchemaNode              = SEmpty
  lazy val schema_model_editorialDeclPart: SchemaNode        = SEmpty
  lazy val schema_model_editorialLike: SchemaNode            = SEmpty
  lazy val schema_model_endingLike: SchemaNode               = SEmpty
  lazy val schema_model_eventLike: SchemaNode                = SEmpty
  lazy val schema_model_eventLike_cmn: SchemaNode            = SEmpty
  lazy val schema_model_eventLike_measureFilling: SchemaNode = SEmpty
  lazy val schema_model_eventPart: SchemaNode                = SEmpty
  lazy val schema_model_expressionLike: SchemaNode           = SEmpty
  lazy val schema_model_fLike: SchemaNode                    = SEmpty
  lazy val schema_model_figDescLike: SchemaNode              = SEmpty
  lazy val schema_model_figbassLike: SchemaNode              = SEmpty
  lazy val schema_model_figureLike: SchemaNode               = SEmpty
  lazy val schema_model_fingeringLike: SchemaNode            = SEmpty
  lazy val schema_model_foliumLike: SchemaNode               = SEmpty
  lazy val schema_model_frontAndBackPart: SchemaNode         = SEmpty
  lazy val schema_model_frontLike: SchemaNode                = SEmpty
  lazy val schema_model_geogNamePart: SchemaNode             = SEmpty
  lazy val schema_model_graphicLike: SchemaNode              = SEmpty
  lazy val schema_model_graphicPrimitiveLike: SchemaNode     = SEmpty
  lazy val schema_model_harmLike: SchemaNode                 = SEmpty
  lazy val schema_model_headLike: SchemaNode                 = SEmpty
  lazy val schema_model_identifierLike: SchemaNode           = SEmpty
  lazy val schema_model_imprintPart: SchemaNode              = SEmpty
  lazy val schema_model_incipLike: SchemaNode                = SEmpty
  lazy val schema_model_instrDefLike: SchemaNode             = SEmpty
  lazy val schema_model_itemLike: SchemaNode                 = SEmpty
  lazy val schema_model_keyAccidLike: SchemaNode             = SEmpty
  lazy val schema_model_keySigLike: SchemaNode               = SEmpty
  lazy val schema_model_lLike: SchemaNode                    = SEmpty
  lazy val schema_model_labelLike: SchemaNode                = SEmpty
  lazy val schema_model_layerDefLike: SchemaNode             = SEmpty
  lazy val schema_model_layerLike: SchemaNode                = SEmpty
  lazy val schema_model_layerPart: SchemaNode                = SEmpty
  lazy val schema_model_layerPart_cmn: SchemaNode            = SEmpty
  lazy val schema_model_lbLike: SchemaNode                   = SEmpty
  lazy val schema_model_lgLike: SchemaNode                   = SEmpty
  lazy val schema_model_listLike: SchemaNode                 = SEmpty
  lazy val schema_model_locrefLike: SchemaNode               = SEmpty
  lazy val schema_model_manifestationLike: SchemaNode        = SEmpty
  lazy val schema_model_mdivLike: SchemaNode                 = SEmpty
  lazy val schema_model_measureLike: SchemaNode              = SEmpty
  lazy val schema_model_measurePart: SchemaNode              = SEmpty
  lazy val schema_model_measurementLike: SchemaNode          = SEmpty
  lazy val schema_model_meterSigLike: SchemaNode             = SEmpty
  lazy val schema_model_midiLike: SchemaNode                 = SEmpty
  lazy val schema_model_milestoneLike_music: SchemaNode      = SEmpty
  lazy val schema_model_milestoneLike_text: SchemaNode       = SEmpty
  lazy val schema_model_msInline: SchemaNode                 = SEmpty
  lazy val schema_model_nameLike: SchemaNode                 = SEmpty
  lazy val schema_model_nameLike_agent: SchemaNode           = SEmpty
  lazy val schema_model_nameLike_geogName: SchemaNode        = SEmpty
  lazy val schema_model_nameLike_label: SchemaNode           = SEmpty
  lazy val schema_model_nameLike_place: SchemaNode           = SEmpty
  lazy val schema_model_noteModifierLike: SchemaNode         = SEmpty
  lazy val schema_model_numLike: SchemaNode                  = SEmpty
  lazy val schema_model_ornamentLike_cmn: SchemaNode         = SEmpty
  lazy val schema_model_ossiaLike: SchemaNode                = SEmpty
  lazy val schema_model_pLike: SchemaNode                    = SEmpty
  lazy val schema_model_paperModLike: SchemaNode             = SEmpty
  lazy val schema_model_paracontentPart: SchemaNode          = SEmpty
  lazy val schema_model_partLike: SchemaNode                 = SEmpty
  lazy val schema_model_partsLike: SchemaNode                = SEmpty
  lazy val schema_model_pbLike: SchemaNode                   = SEmpty
  lazy val schema_model_persNamePart: SchemaNode             = SEmpty
  lazy val schema_model_physDescPart: SchemaNode             = SEmpty
  lazy val schema_model_pubStmtPart: SchemaNode              = SEmpty
  lazy val schema_model_qLike: SchemaNode                    = SEmpty
  lazy val schema_model_quoteLike: SchemaNode                = SEmpty
  lazy val schema_model_rdgPart: SchemaNode                  = SEmpty
  lazy val schema_model_rdgPart_music: SchemaNode            = SEmpty
  lazy val schema_model_rdgPart_text: SchemaNode             = SEmpty
  lazy val schema_model_relationLike: SchemaNode             = SEmpty
  lazy val schema_model_rendLike: SchemaNode                 = SEmpty
  lazy val schema_model_repositoryLike: SchemaNode           = SEmpty
  lazy val schema_model_resourceLike: SchemaNode             = SEmpty
  lazy val schema_model_respLike: SchemaNode                 = SEmpty
  lazy val schema_model_respLikePart: SchemaNode             = SEmpty
  lazy val schema_model_scoreDefLike: SchemaNode             = SEmpty
  lazy val schema_model_scoreLike: SchemaNode                = SEmpty
  lazy val schema_model_scorePart: SchemaNode                = SEmpty
  lazy val schema_model_sectionLike: SchemaNode              = SEmpty
  lazy val schema_model_sectionPart: SchemaNode              = SEmpty
  lazy val schema_model_sectionPart_cmn: SchemaNode          = SEmpty
  lazy val schema_model_staffDefLike: SchemaNode             = SEmpty
  lazy val schema_model_staffDefPart: SchemaNode             = SEmpty
  lazy val schema_model_staffGrpLike: SchemaNode             = SEmpty
  lazy val schema_model_staffLike: SchemaNode                = SEmpty
  lazy val schema_model_staffPart: SchemaNode                = SEmpty
  lazy val schema_model_stageDirLike: SchemaNode             = SEmpty
  lazy val schema_model_sylLike: SchemaNode                  = SEmpty
  lazy val schema_model_symbolTableLike: SchemaNode          = SEmpty
  lazy val schema_model_tableLike: SchemaNode                = SEmpty
  lazy val schema_model_textComponentLike: SchemaNode        = SEmpty
  lazy val schema_model_textPhraseLike: SchemaNode           = SEmpty
  lazy val schema_model_textPhraseLike_limited: SchemaNode   = SEmpty
  lazy val schema_model_titleLike: SchemaNode                = SEmpty
  lazy val schema_model_titlePagePart: SchemaNode            = SEmpty
  lazy val schema_model_transcriptionLike: SchemaNode        = SEmpty
  lazy val schema_model_tuningPart: SchemaNode               = SEmpty
  lazy val schema_model_verseLike: SchemaNode                = SEmpty
  lazy val schema_model_workIdent: SchemaNode                = SEmpty
  lazy val schema_model_workLike: SchemaNode                 = SEmpty
  lazy val schema_monogr: SchemaNode                         = SSeq(
    scala.List(
      SOptional(
        SChoice(
          scala.List(
            SSeq(
              scala.List(
                SChoice(
                  scala.List(
                    SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
                    SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
                  ),
                ),
                SZeroOrMore(
                  SChoice(
                    scala.List(
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
                    ),
                  ),
                ),
                SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
                SZeroOrMore(
                  SChoice(
                    scala.List(
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.Editor]),
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
                    ),
                  ),
                ),
              ),
            ),
            SSeq(
              scala.List(
                SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
                SZeroOrMore(
                  SChoice(
                    scala.List(
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
                      SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.namesdates.CorpName]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EditionLike])),
      SRef(classOf[_root_.sheet.meicmn.mei.shared.Imprint]),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Imprint]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Extent]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblScope]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_mordent: SchemaNode   = SEmpty
  lazy val schema_multiRest: SchemaNode = SEmpty
  lazy val schema_multiRpt: SchemaNode  = SEmpty
  lazy val schema_music: SchemaNode     = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ResourceLike])),
      SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.MusicPart]),
    ),
  )
  lazy val schema_name: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_nameLink: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_namespace: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SChoice(
        scala.List(
          SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.TagUsage])),
          SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.AttUsage])),
        ),
      ),
    ),
  )
  lazy val schema_normalization: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_note: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.NoteModifierLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.lyrics.model.VerseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
      ),
    ),
  )
  lazy val schema_noteOff: SchemaNode   = SEmpty
  lazy val schema_noteOn: SchemaNode    = SEmpty
  lazy val schema_notesStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike])),
    ),
  )
  lazy val schema_num: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_oLayer: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_oStaff: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_octave: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_orig: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_ornam: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_ossia: SchemaNode     = SChoice(scala.List())
  lazy val schema_otherChar: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_p: SchemaNode         = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ParacontentPart]))),
  )
  lazy val schema_pad: SchemaNode  = SEmpty
  lazy val schema_part: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScorePart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_parts: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PartLike]))
  lazy val schema_patch: SchemaNode = SChoice(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.header.model.FoliumLike]),
      SRef(classOf[_root_.sheet.meicmn.mei.header.model.BifoliumLike]),
    ),
  )
  lazy val schema_pb: SchemaNode           = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.metalike.Page])
  lazy val schema_pedal: SchemaNode        = SEmpty
  lazy val schema_perfDuration: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_perfMedium: SchemaNode   = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.CastList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PerfResList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike])),
    ),
  )
  lazy val schema_perfRes: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.header.PerfRes]),
      ),
    ),
  )
  lazy val schema_perfResList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Annot]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.PerfRes]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.PerfResList]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_performance: SchemaNode = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.Recording]))
  lazy val schema_periodName: SchemaNode  = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_persName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.PersNamePart]),
      ),
    ),
  )
  lazy val schema_pgDesc: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.ptrref.model.LocrefLike]),
      ),
    ),
  )
  lazy val schema_pgFoot: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.AnchoredText]),
      ),
    ),
  )
  lazy val schema_pgHead: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.AnchoredText]),
      ),
    ),
  )
  lazy val schema_phrase: SchemaNode   = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.Curve]))
  lazy val schema_physDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.PhysDescPart])),
    ),
  )
  lazy val schema_physLoc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SSeq(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RepositoryLike]),
            SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike])),
          ),
        ),
      ),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.History])),
    ),
  )
  lazy val schema_physMedium: SchemaNode   = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_plateNum: SchemaNode     = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_playingSpeed: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_port: SchemaNode         = SEmpty
  lazy val schema_postBox: SchemaNode      = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_postCode: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_price: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_prog: SchemaNode        = SEmpty
  lazy val schema_projectDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_propName: SchemaNode   = SText
  lazy val schema_propValue: SchemaNode  = SText
  lazy val schema_provenance: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_ptr: SchemaNode      = SEmpty
  lazy val schema_pubPlace: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_pubStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SChoice(
        scala.List(
          SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Unpub])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.PubStmtPart])),
        ),
      ),
    ),
  )
  lazy val schema_publisher: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_q: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]))),
  )
  lazy val schema_quote: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ParacontentPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.drama.Sp]),
      ),
    ),
  )
  lazy val schema_rdg: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.RdgPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Expansion]),
      ),
    ),
  )
  lazy val schema_recipient: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_recording: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.AvFile])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.When])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.performance.Clip])),
    ),
  )
  lazy val schema_ref: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]))),
  )
  lazy val schema_refrain: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dir]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dynam]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Tempo]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Space]),
          ),
        ),
      ),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.lyrics.Volta]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike])),
    ),
  )
  lazy val schema_reg: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_region: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_reh: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RendLike]),
      ),
    ),
  )
  lazy val schema_relatedItem: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblLike]))),
  )
  lazy val schema_relation: SchemaNode     = SEmpty
  lazy val schema_relationList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike])),
    ),
  )
  lazy val schema_rend: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_repeatMark: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_repository: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_resp: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_respStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Name]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Resp]),
            SRef(classOf[_root_.sheet.meicmn.mei.namesdates.model.namelike.Agent]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_rest: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Dot]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_restore: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_revisionDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Change])),
    ),
  )
  lazy val schema_role: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_roleDesc: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_roleName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_rubric: SchemaNode       = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_samplingDecl: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_sb: SchemaNode    = SEmpty
  lazy val schema_score: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScoreDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ScorePart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_scoreDef: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.ChordTableLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.SymbolTableLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeySigLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.MeterSigLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.PgHead])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.PgFoot])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.midi.InstrGrp])),
      SChoice(scala.List(SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike])))),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.GrpSym])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Ambitus])),
    ),
  )
  lazy val schema_scoreFormat: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_scriptDesc: SchemaNode  = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.P])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.ScriptNote])),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_scriptNote: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_seal: SchemaNode       = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
                SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_sealDesc: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SChoice(
            scala.List(
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Seal])),
              SZeroOrMore(
                SChoice(
                  scala.List(
                    SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
                    SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
                    SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_secFolio: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_section: SchemaNode  = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Expansion])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_seg: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_segmentation: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_seqNum: SchemaNode = SEmpty
  lazy val schema_series: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.ptrref.model.LocrefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Editor]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.Extent]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.TextLang]),
      ),
    ),
  )
  lazy val schema_seriesStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.RespStmt]),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblScope]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.Contents]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.SeriesStmt]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_settlement: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_sic: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_signatures: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_slur: SchemaNode       = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.Curve]))
  lazy val schema_soundChan: SchemaNode  = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_source: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Locus]),
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.LocusGrp]),
          ),
        ),
      ),
      SZeroOrMore(SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblLike])))),
    ),
  )
  lazy val schema_sourceDesc: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Source])),
    ),
  )
  lazy val schema_sp: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Speaker])),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.LgLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.LLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.model.QuoteLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.drama.model.StageDirLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.text.List]),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_space: SchemaNode   = SEmpty
  lazy val schema_speaker: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_specRepro: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_sponsor: SchemaNode   = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_stack: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_staff: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RelationLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_staffDef: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LabelLike])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.InstrDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerDefLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefPart]),
          ),
        ),
      ),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Ambitus])),
    ),
  )
  lazy val schema_staffGrp: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.GrpSym])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LabelLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.InstrDefLike])),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffDefLike]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.GrpSym])),
    ),
  )
  lazy val schema_stageDir: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_stamp: SchemaNode   = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_stdVals: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike])),
    ),
  )
  lazy val schema_street: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_string: SchemaNode    = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.stringtab.String]))
  lazy val schema_styleName: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_subst: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike])),
    ),
  )
  lazy val schema_supplied: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_support: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.header.Condition]),
            SRef(classOf[_root_.sheet.meicmn.mei.msdesc.DecoNote]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dimensions]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.P]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_supportDesc: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SChoice(
            scala.List(
              SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.P])),
              SSeq(
                scala.List(
                  SOptional(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Support])),
                  SOptional(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Foliation])),
                  SOptional(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.Collation])),
                  SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Condition])),
                ),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_surface: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigDescLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.GraphicLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.facsimile.Zone])),
    ),
  )
  lazy val schema_syl: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_symName: SchemaNode = SText
  lazy val schema_symProp: SchemaNode = SSeq(
    scala.List(
      SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.PropName]),
      SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.PropValue]),
    ),
  )
  lazy val schema_symbol: SchemaNode    = SEmpty
  lazy val schema_symbolDef: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.SymName])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.SymProp])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.Mapping])),
      SChoice(
        scala.List(
          SOptional(SRef(classOf[sheet.meicmn.Element])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.Symbol]),
                SRef(classOf[_root_.sheet.meicmn.mei.figtable.Graphic]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.AnnotLike])),
    ),
  )
  lazy val schema_symbolTable: SchemaNode = SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.SymbolDef]))
  lazy val schema_sysReq: SchemaNode      = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_tabDurSym: SchemaNode   = SEmpty
  lazy val schema_tabGrp: SchemaNode      = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.stringtab.TabDurSym])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Note]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Rest]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_table: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.CaptionLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.Tr])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.CaptionLike])),
    ),
  )
  lazy val schema_tagUsage: SchemaNode = SSeq(
    scala.List(
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.AttUsage])),
    ),
  )
  lazy val schema_tagsDecl: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Namespace])),
    ),
  )
  lazy val schema_taxonomy: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.BiblLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Desc])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.header.Category]),
            SRef(classOf[_root_.sheet.meicmn.mei.header.Taxonomy]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_td: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_tempo: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_term: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_termList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label]))),
    ),
  )
  lazy val schema_textLang: SchemaNode = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_th: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextPhraseLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_tie: SchemaNode   = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.Curve]))
  lazy val schema_title: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.TitlePart]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.TitlePart]),
      ),
    ),
  )
  lazy val schema_titlePage: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigureLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.ImprintPart]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Text]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TextComponentLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitlePagePart]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
          ),
        ),
      ),
    ),
  )
  lazy val schema_titlePart: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.TitlePart]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_titleStmt: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
      SZeroOrMore(SChoice(scala.List(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLike])))),
    ),
  )
  lazy val schema_tr: SchemaNode = SZeroOrMore(
    SChoice(
      scala.List(SRef(classOf[_root_.sheet.meicmn.mei.figtable.Th]), SRef(classOf[_root_.sheet.meicmn.mei.figtable.Td])),
    ),
  )
  lazy val schema_trackConfig: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_treatHist: SchemaNode   = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_treatSched: SchemaNode = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
          SZeroOrMore(
            SChoice(
              scala.List(
                SRef(classOf[_root_.sheet.meicmn.mei.shared.EventList]),
                SRef(classOf[_root_.sheet.meicmn.mei.shared.model.PLike]),
              ),
            ),
          ),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_trill: SchemaNode   = SEmpty
  lazy val schema_trkName: SchemaNode = SText
  lazy val schema_tuning: SchemaNode  = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TuningPart]))
  lazy val schema_tuplet: SchemaNode  = SZeroOrMore(
    SChoice(
      scala.List(
        SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.EventLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.cmn.model.eventlike.Cmn]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_tupletSpan: SchemaNode = SEmpty
  lazy val schema_turn: SchemaNode       = SEmpty
  lazy val schema_typeDesc: SchemaNode   = SChoice(
    scala.List(
      SSeq(
        scala.List(
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Head])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.P])),
          SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.msdesc.TypeNote])),
        ),
      ),
      SZeroOrMore(
        SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
      ),
    ),
  )
  lazy val schema_typeNote: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_unclear: SchemaNode  = SZeroOrMore(
    SChoice(
      scala.List(
        SText,
        SRef(classOf[_root_.sheet.meicmn.mei.text.model.DivLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditTransPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.harmony.model.FLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.usersymbols.model.GraphicPrimitiveLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.KeyAccidLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LayerPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.milestonelike.Music]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[sheet.meicmn.Element]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SectionPart]),
        SRef(classOf[_root_.sheet.meicmn.mei.shared.model.StaffGrpLike]),
        SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.TranscriptionLike]),
      ),
    ),
  )
  lazy val schema_unpub: SchemaNode       = SText
  lazy val schema_useRestrict: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_vel: SchemaNode         = SEmpty
  lazy val schema_verse: SchemaNode       = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.Label])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.LabelAbbr])),
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dir]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dynam]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Tempo]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Space]),
          ),
        ),
      ),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.lyrics.Volta]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike])),
    ),
  )
  lazy val schema_volta: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dir]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Dynam]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Tempo]),
            SRef(classOf[_root_.sheet.meicmn.mei.shared.Space]),
          ),
        ),
      ),
      SOneOrMore(
        SChoice(
          scala.List(
            SRef(classOf[_root_.sheet.meicmn.mei.shared.model.SylLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.critapp.model.AppLike]),
            SRef(classOf[_root_.sheet.meicmn.mei.edittrans.model.EditLike]),
          ),
        ),
      ),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.LbLike])),
    ),
  )
  lazy val schema_watermark: SchemaNode = SRef(classOf[_root_.sheet.meicmn.mei.shared.macro_.Struc_unstrucContent])
  lazy val schema_when: SchemaNode      = SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.ExtData]))
  lazy val schema_width: SchemaNode     = SZeroOrMore(
    SChoice(scala.List(SText, SRef(classOf[_root_.sheet.meicmn.mei.shared.model.textphraselike.Limited]))),
  )
  lazy val schema_work: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.IdentifierLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.TitleLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.RespLikePart])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.model.WorkIdent])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.OtherChar])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.Creation])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.History])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.LangUsage])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PerfMedium])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.PerfDuration])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Audience])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Contents])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Context])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.BiblList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.NotesStmt])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.Classification])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.frbr.ExpressionList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.header.ComponentList])),
      SOptional(SRef(classOf[_root_.sheet.meicmn.mei.shared.RelationList])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.ExtMeta])),
    ),
  )
  lazy val schema_workList: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.shared.model.HeadLike])),
      SOneOrMore(SRef(classOf[_root_.sheet.meicmn.mei.header.Work])),
    ),
  )
  lazy val schema_zone: SchemaNode = SSeq(
    scala.List(
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.FigDescLike])),
      SZeroOrMore(SRef(classOf[_root_.sheet.meicmn.mei.figtable.model.GraphicLike])),
    ),
  )
}

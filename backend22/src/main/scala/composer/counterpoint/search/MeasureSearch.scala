package composer.counterpoint.search

import composer.counterpoint.model.{AnnotatedMeasure, AnnotatedNote, MeasureRythmnPattern, NoteAnnotation, ToneType}
import _root_.model.elements.{Degree, Key, Pitch, Part}
import _root_.model.elements.Interval.IntervalStep
import composer.counterpoint.search.PitchApplyer.PitchMeasureStepSequence
import composer.counterpoint.search.MeasureStepSequence.inversionNormalized
import _root_.model.elements.Pitch.NoteName
import model.containers.{Melody, Note}
import model.elements.Duration

case class MeasureSearchResult(
    measure: AnnotatedMeasure,
    nextMeasureStartPitch: Pitch,
    rythmnPattern: MeasureRythmnPattern,
)

class MeasureSearch(
    sequences: List[MeasureStepSequence],
    allRythmnPatterns: List[MeasureRythmnPattern],
) {
  private val indexer = new MeasureStepSequenceIndexer(sequences, allRythmnPatterns)

  /** 指定された条件に合致する旋律を返す。
    *
    * @param startPitch
    *   旋律の開始音
    * @param startHarmonicPitch
    *   旋律が最初に利用する和声音。 startPitch と IntervalStep の差の絶対値が
    *   1以下(0-indexed)のものを指定する必要がある。 startPitch と一致する場合は和声音から始まる旋律が返され、異なる場合は掛留音から始まる旋律が返される。
    *   全音符単位で骨格となる和声音を実施した後、旋律を埋めるといった探索を行う場合、その和声音が指定される。
    * @param nextMeasureStartHarmonicPitch
    *   次の小節で最初に利用する和声音。 生成した旋律の nextMeasureStartPitch はこの音と同じか、 または IntervalStep の差の絶対値が
    *   1以下(0-indexed)のものになる。 全音符単位で骨格となる和声音を実施した後、旋律を埋めるといった探索を行う場合、次の小節の和声音が指定される。
    * @param harmonicNoteNames
    *   和声音として利用できる音名の一覧 結果の旋律に含まれる和声音はここで指定した一覧の一部が利用される。
    *   調に含まれる三和音の構成音を想定しており、それ以外を指定した場合は結果が空になったり不適切になる可能性がある。
    * @param key
    *   調
    * @param measureRythmnPatterns
    *   結果に含めたいリズムパターン 結果の旋律の形状や非和声音の扱い方に応じてリズムパターンが適切かどうかが検証される。
    * @param pitchRange
    *   旋律の音域
    */
  def search(
      startPitch: Pitch,
      startHarmonicPitch: Pitch,
      nextMeasureStartHarmonicPitch: Pitch,
      harmonicNoteNames: Set[NoteName],
      key: Key,
      measureRythmnPatterns: Set[MeasureRythmnPattern],
      pitchRange: (Pitch, Pitch),
  ): List[MeasureSearchResult] = {

    val firstNoteIntervalStep = (startPitch - startHarmonicPitch).step
    require(Set(IntervalStep(-1), IntervalStep(0), IntervalStep(1)).contains(firstNoteIntervalStep))

    val availableHarmonicSteps: Set[IntervalStep] = harmonicNoteNames.map { hn =>
      (Pitch.of(0, hn.value) - startHarmonicPitch).step.inversionNormalized
    }

    val nextMeasureStep          = (nextMeasureStartHarmonicPitch - startHarmonicPitch).step
    val nextMeasureAdjacentSteps = List(nextMeasureStep + IntervalStep(-1), nextMeasureStep + IntervalStep(1))

    val nextMeasureCondition = Q(SearchField.NEXT_MEASURE_STEP).equal(nextMeasureStep)
    val adjacentCondition    = Q(SearchField.NEXT_MEASURE_STEP)
      .isIn(nextMeasureAdjacentSteps)
      .and(Q(SearchField.IS_TIED_TO_NEXT_MEASURE_REQUIRED).equal(true))

    val (minPitch, maxPitch) = pitchRange
    val minStep              = (minPitch - startHarmonicPitch).step
    val maxStep              = (maxPitch - startHarmonicPitch).step
    val pitchCondition       = Q(SearchField.MIN_STEP).ge(minStep).and(Q(SearchField.MAX_STEP).le(maxStep))

    val condition = Q(SearchField.FIRST_NOTE_INTERVAL_STEP)
      .equal(firstNoteIntervalStep)
      .and(Q(SearchField.USED_HARMONIC_STEPS).isSubsetOf(availableHarmonicSteps))
      .and(Q(SearchField.RYTHMN_PATTERNS).isIn(measureRythmnPatterns))
      .and(nextMeasureCondition.or(adjacentCondition))
      .and(pitchCondition)

    val candidates   = indexer.find(condition)
    val chordDegrees = harmonicNoteNames.map(nn => Degree.fromNoteNameKey(nn, key))

    candidates.flatMap { candidate =>
      toMeasureSearchResult(
        candidate,
        startHarmonicPitch,
        key,
        measureRythmnPatterns,
        chordDegrees,
        nextMeasureStartHarmonicPitch,
        pitchRange,
      )
    }
  }

  private def toMeasureSearchResult(
      measureStepSequence: MeasureStepSequence,
      startPitch: Pitch,
      key: Key,
      measureRythmnPatterns: Set[MeasureRythmnPattern],
      chordDegrees: Set[Degree],
      nextMeasureStartHarmonicPitch: Pitch,
      pitchRange: (Pitch, Pitch),
  ): List[MeasureSearchResult] = {
    val pitchCandidates = PitchApplyer.applyPitchCandidates(
      key,
      chordDegrees,
      startPitch,
      measureStepSequence,
    )

    val filteredCandidates = PitchFilter.filterPitchSequences(
      pitchCandidates,
      nextMeasureStartHarmonicPitch,
      pitchRange,
      key,
    )

    val compatiblePatterns = indexer.getCompatibleRythmnPatterns(measureStepSequence)
    val targetPatterns     = measureRythmnPatterns.intersect(compatiblePatterns)

    val results = scala.collection.mutable.ListBuffer[MeasureSearchResult]()

    for (pitchMeasureSequence <- filteredCandidates) {
      for (rythmnPattern <- targetPatterns) {

        val appliedMelody = applyRythmnToPitch(pitchMeasureSequence, rythmnPattern)

        results += MeasureSearchResult(
          appliedMelody,
          pitchMeasureSequence.nextMeasureStartPitch,
          rythmnPattern,
        )
      }
    }
    results.toList
  }

  private def applyRythmnToPitch(
      seq: PitchMeasureStepSequence,
      rythmnPattern: MeasureRythmnPattern,
  ): AnnotatedMeasure = {
    val rythmn    = rythmnPattern.measureRythmn
    val durations = rythmn.durations
    val seqNotes  = seq.measure.elems

    var notes: List[Note[AnnotatedNote]] = Nil

    if (rythmn.initRestDuration > Duration.of(0)) {
      notes ::= Note(
        AnnotatedNote(None, NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)),
        rythmn.initRestDuration,
        Part.Root,
      )
    }

    for (i <- seqNotes.indices) {
      val note     = seqNotes(i) // Note[AnnotatedNote]
      val duration = durations(i)

      // isTiedToNextMeasureRequired logic
      val lastPitch      = seqNotes.last.value.value
      val nextPitch      = Some(seq.nextMeasureStartPitch)
      val isTiedRequired = lastPitch == nextPitch

      val isLastNote = i == seqNotes.length - 1

      val newAnnotation = NoteAnnotation(
        isTiedStart = isTiedRequired && isLastNote,
        toneType = note.value.annotation.toneType,
      )

      notes ::= Note(
        AnnotatedNote(note.value.value, newAnnotation),
        duration,
        Part.Root,
      )
    }

    Melody(notes.reverse)
  }
}

object MeasureSearch {
  def default: MeasureSearch = {
    val sequences = MeasureStepSequenceGenerator.generate()
    new MeasureSearch(sequences, MeasureRythmnPattern.values.toList)
  }
}

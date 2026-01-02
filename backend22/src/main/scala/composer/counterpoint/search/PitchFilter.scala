package composer.counterpoint.search

import composer.counterpoint.model.ToneType
import composer.counterpoint.search.PitchApplyer.PitchMeasureStepSequence
import model.elements.{Degree, Interval, Key, Pitch}
import model.elements.Interval.IntervalStep
import model.elements.Key.Mode

object PitchFilter {

  /** 生成されたピッチ列の候補から、不適切なものを除外する。
    *
    * (ここにどこまでルールを詰め込むのかは悩みどころである)
    */
  def filterPitchSequences(
      candidates: List[PitchMeasureStepSequence],
      nextMeasureStartHarmonicPitch: Pitch,
      pitchRange: (Pitch, Pitch),
      key: Key,
  ): List[PitchMeasureStepSequence] = {

    val results = scala.collection.mutable.ListBuffer[PitchMeasureStepSequence]()

    for (candidate <- candidates) {
      // Check relationship with next_measure_start_harmonic_pitch
      val nextMeasureDiff = candidate.nextMeasureStartPitch - nextMeasureStartHarmonicPitch
      val nextStepAbs     = nextMeasureDiff.step.abs.toIdx1

      var isValidNext = false
      if (nextMeasureDiff == Interval.P1 || nextStepAbs == 2) {
        isValidNext = true
      } else if (nextMeasureDiff == Interval.A1 || nextMeasureDiff == Interval.d1) {
        isValidNext = false
      } else {
        throw new RuntimeException(
          s"unexpected interval between candidate.nextMeasureStep and nextMeasureStartHarmonicPitch: $nextMeasureDiff",
        )
      }

      if (isValidNext) {
        // candidate.measure is AnnotatedMeasure (Melody[AnnotatedNote, Unit, ...])
        // note.value is AnnotatedNote(value: Option[Pitch], annotation: NoteAnnotation)
        val pitches = candidate.measure.elems.flatMap(_.value.value) :+ candidate.nextMeasureStartPitch
        val degrees = pitches.map(p => Degree.fromNoteNameKey(p.noteName, key))

        // Suspension check
        var isSuspensionValid = true
        val firstNote         = candidate.measure.elems.head
        // firstNote.value is AnnotatedNote. annotation.toneType
        if (firstNote.value.annotation.toneType == ToneType.SUSPENDED_TONE) {
          val resolvePitchOpt =
            candidate.measure.elems.find(_.value.annotation.toneType == ToneType.HARMONIC_TONE).flatMap(_.value.value)
          if (resolvePitchOpt.isDefined) {
            val resolvePitch        = resolvePitchOpt.get
            val resolveIntervalStep = (resolvePitch - firstNote.value.value.get).step
            if (resolveIntervalStep == IntervalStep.idx_1(2)) {
              // Upward resolution
              val resolveDegree = Degree.fromNoteNameKey(resolvePitch.noteName, key)
              if (key.mode == Mode.Minor && resolveDegree == Degree.idx1(1, 0)) {
                // OK (vii -> i in minor)
              } else {
                isSuspensionValid = false
              }
            } else if (resolveIntervalStep == IntervalStep.idx_1(-2)) {
              // Downward resolution OK
            } else {
              throw new RuntimeException(s"invalid suspension resolution step: $resolveIntervalStep")
            }
          }
        }

        // Range check
        val (minP, maxP) = pitchRange
        val isRangeValid = pitches.forall(p => p.num.value >= minP.num.value && p.num.value <= maxP.num.value)

        // Interval check between adjacent notes
        var isIntervalValid = true
        val pairs           = pitches.zip(degrees).sliding(2)
        for (pair <- pairs) {
          val (p1, d1) = pair(0)
          val (p2, _)  = pair(1)
          val interval = (p1 - p2).abs

          if (Set(Interval.d4, Interval.A4, Interval.d5, Interval.A5, Interval.M6).contains(interval)) {
            isIntervalValid = false
          } else if (interval == Interval.A2) {
            throw new RuntimeException("unexpected interval A2")
          } else if (interval == Interval.P8) {
            // Leading tone octave leap check
            val isLeadingTone = (key.mode == Mode.Major && d1 == Degree.idx1(7, 0)) ||
              (key.mode == Mode.Minor && d1 == Degree.idx1(7, 1))
            if (isLeadingTone) isIntervalValid = false
          }
        }

        if (isSuspensionValid && isRangeValid && isIntervalValid) {
          results += candidate
        }
      }
    }
    results.toList
  }
}

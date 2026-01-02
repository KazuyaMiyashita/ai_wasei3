package composer.counterpoint.search

import composer.counterpoint.model.{MeasureRythmnPattern, NoteAnnotation, ToneType}
import model.containers.{Melody, Note}
import model.elements.{Duration, Offset, Part}
import model.elements.Interval.IntervalStep

object RythmnApplyer {

  type AppliedNote   = Note[Option[IntervalStep], NoteAnnotation]
  type AppliedMelody = Melody[Option[IntervalStep], NoteAnnotation, AppliedNote]

  /** MeasureStepSequence に対して MeasureRythmnPattern を適用できるかを判断する。
    * 可能なら適用した Melody を返し、不可能なら None を返す。
    */
  def tryApplyRythmn(
      stepSequence: MeasureStepSequence,
      rythmnPattern: MeasureRythmnPattern,
  ): Option[AppliedMelody] = {
    val rythmn = rythmnPattern.measureRythmn

    if (stepSequence.numNotesInMeasure != rythmn.numDurations) return None
    if (stepSequence.isTiedToNextMeasureRequired != rythmn.isNextTied) return None

    val melody      = applyRythmn(stepSequence, rythmnPattern)
    val offsetNotes = calculateOffsetNotes(melody)

    // Next tie check
    if (stepSequence.isTiedToNextMeasureRequired) {
      val noteAtBeat3 = offsetNotes.get(Offset.idx1(3))
      val isValid     = noteAtBeat3.exists { n =>
        n.duration == Duration.of(2) &&
        n == melody.elems.last &&
        n.meta.toneType == ToneType.HARMONIC_TONE
      }
      if (!isValid) return None
    }

    // Suspension check
    if (melody.elems.head.meta.toneType == ToneType.SUSPENDED_TONE) {
      if (!rythmn.isPreviousTied) return None

      val offset3Note = offsetNotes.get(Offset.idx1(3))
      if (offset3Note.isEmpty) return None
      if (offset3Note.get.meta.toneType != ToneType.HARMONIC_TONE) return None

      // Should not resolve before beat 3
      val resolvedTooEarly = offsetNotes.exists { case (offset, note) =>
        offset > Offset.of(0) && offset < Offset.idx1(3) && note.meta.toneType == ToneType.HARMONIC_TONE
      }
      if (resolvedTooEarly) return None
    }

    // 8th note non-stepwise motion check
    // Create dummy next note
    val nextNoteDummy = Note(
      Option(stepSequence.nextMeasureStep),
      Duration.of(1),
      Part.Root,
      NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE),
    )
    val notesToCheck = melody.elems :+ nextNoteDummy

    val pairs        = notesToCheck.zip(notesToCheck.tail)
    val hasViolation = pairs.exists { case (currentNote, nextNote) =>
      if (currentNote.value.isDefined && currentNote.duration == Duration.of(1, 2)) {
        if (nextNote.value.isDefined) {
          val diff = (currentNote.value.get - nextNote.value.get).abs
          diff >= IntervalStep.idx_1(3)
        } else false
      } else false
    }

    if (hasViolation) None else Some(melody)
  }

  /** 任意の AbstractMeasureStepSequence に対して MeasureRythmnPattern を適用した Melody を返す。
    * ここではリズムの検証は行われない。事前に検証済みの内容に関して利用すること。
    */
  def applyRythmn(
      stepSequence: AbstractMeasureStepSequence[IntervalStep],
      rythmnPattern: MeasureRythmnPattern,
  ): AppliedMelody = {
    val rythmn    = rythmnPattern.measureRythmn
    val durations = rythmn.durations

    var notes: List[AppliedNote] = Nil

    if (rythmn.initRestDuration > Duration.of(0)) {
      notes ::= Note(
        None,
        rythmn.initRestDuration,
        Part.Root,
        NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE),
      )
    }

    val seqNotes = stepSequence.measureNotes
    for (i <- seqNotes.indices) {
      val note       = seqNotes(i)
      val duration   = durations(i)
      val isLastNote = i == stepSequence.numNotesInMeasure - 1

      notes ::= Note(
        Some(note.value),
        duration,
        Part.Root,
        NoteAnnotation(
          isTiedStart = stepSequence.isTiedToNextMeasureRequired && isLastNote,
          toneType = note.meta,
        ),
      )
    }

    Melody(notes.reverse, NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)) // Meta for Melody is dummy
  }

  def calculateOffsetNotes(melody: AppliedMelody): Map[Offset, AppliedNote] = {
    var curr = Offset.of(0)
    melody.elems.map { note =>
      val entry = curr -> note
      curr = curr + note.duration
      entry
    }.toMap
  }
}

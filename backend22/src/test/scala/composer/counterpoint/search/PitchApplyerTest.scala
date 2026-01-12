package composer.counterpoint.search

import composer.counterpoint.model.{AnnotatedNote, NoteAnnotation, ToneType}
import model.containers.{Melody, Note}
import model.elements.{Degree, Duration, Key, Part, Pitch}
import model.elements.Interval.IntervalStep
import org.scalatest.funsuite.AnyFunSuite
import composer.counterpoint.search.PitchApplyer.PitchMeasureStepSequence

class PitchApplyerTest extends AnyFunSuite {

  // Access private[search] method
  test("applyPitchDiatonic simple") {
    val key           = Key.parse("G Minor")
    val startPitch    = Pitch.parse("D4")
    val intervalSteps = (0 until 8).map(i => IntervalStep(i)).toList
    val expected      = List(
      Pitch.parse("D4"),
      Pitch.parse("Eb4"),
      Pitch.parse("F4"),
      Pitch.parse("G4"),
      Pitch.parse("A4"),
      Pitch.parse("Bb4"),
      Pitch.parse("C5"),
      Pitch.parse("D5"),
    )
    val result = PitchApplyer.applyPitchDiatonic(key, startPitch, intervalSteps)
    assert(result == expected)
  }

  test("applyPitchCandidates minor leading tone") {
    val key          = Key.parse("E Minor")
    val chordDegrees = Set(
      Degree.idx1(5, 0),
      Degree.idx1(7, 1), // vii raised
      Degree.idx1(2, 0),
    )
    val startPitch = Pitch.parse("B4")

    val measureNotes = List(
      AnnotatedIntervalStep(IntervalStep(0), Duration.of(1), ToneType.HARMONIC_TONE),
      AnnotatedIntervalStep(IntervalStep(2), Duration.of(1), ToneType.HARMONIC_TONE),
      AnnotatedIntervalStep(IntervalStep(4), Duration.of(1), ToneType.HARMONIC_TONE),
    )
    val mss = MeasureStepSequence(measureNotes, IntervalStep(0))

    val expected = List(
      PitchMeasureStepSequence(
        Melody(
          List(
            Note(
              AnnotatedNote(Some(Pitch.parse("B4")), NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)),
              Duration.of(1),
              Part.Root,
            ),
            Note(
              AnnotatedNote(Some(Pitch.parse("D#5")), NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)),
              Duration.of(1),
              Part.Root,
            ),
            Note(
              AnnotatedNote(Some(Pitch.parse("F#5")), NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)),
              Duration.of(1),
              Part.Root,
            ),
          ),
        ),
        Pitch.parse("B4"),
      ),
    )

    val result = PitchApplyer.applyPitchCandidates(key, chordDegrees, startPitch, mss)
    assert(result == expected)
  }
}

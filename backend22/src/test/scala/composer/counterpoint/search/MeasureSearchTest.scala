package composer.counterpoint.search

import composer.counterpoint.model.{AnnotatedNote, MeasureRythmnPattern, NoteAnnotation, ToneType}
import model.containers.{Melody, Note}
import model.elements.{Duration, Key, Part, Pitch}
import model.elements.Pitch.NoteName
import org.scalatest.funsuite.AnyFunSuite

class MeasureSearchTest extends AnyFunSuite {

  val sampleSequences: List[MeasureStepSequence] = {
    val pattern = List(
      "-1r,0,-1br,0|-1",
      "-1r,0,-1br,0|-2",
      "-1r,0,-1br,0|-3",
      "-1r,0,-1br,0|-4",
      "-1r,0,-1br,0|-5",
      "-1r,0,-1br,0|-7",
      "0|1",
      "0|2",
      "0|3",
      "0,1p|0",
      "0,1p|2",
      "0,1p|4",
      "0,1p,2p,3|2",
      "0,1p,2p,3|4",
      "0,2|3",
      "0,3|2",
      "0,3|3",
      "0,3|4",
      "1r,0|1",
    )
    pattern.map(MeasureStepSequence.parse)
  }

  val rythmnPatterns = List(
    MeasureRythmnPattern.R_1,
    MeasureRythmnPattern.R_22,
    MeasureRythmnPattern.R_22t,
    MeasureRythmnPattern.R_t22,
    MeasureRythmnPattern.R_244,
    MeasureRythmnPattern.R_4444,
    MeasureRythmnPattern.R_4882,
  )

  def createMeasureSearch(): MeasureSearch = {
    new MeasureSearch(sampleSequences, rythmnPatterns)
  }

  test("search returns results 1") {
    val measureSearch = createMeasureSearch()
    val results       = measureSearch.search(
      startPitch = Pitch.parse("C4"),
      startHarmonicPitch = Pitch.parse("C4"),
      nextMeasureStartHarmonicPitch = Pitch.parse("E4"),
      harmonicNoteNames = Set(NoteName.parse("C"), NoteName.parse("E"), NoteName.parse("G")),
      key = Key.parse("C Major"),
      measureRythmnPatterns = Set(MeasureRythmnPattern.R_1),
      pitchRange = (Pitch.parse("G3"), Pitch.parse("D5")),
    )

    val expectedMeasure = Melody(
      List(
        Note(
          AnnotatedNote(Some(Pitch.parse("C4")), NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE)),
          Duration.of(4),
          Part.Root,
        ),
      ),
    )
    val expectedResult = MeasureSearchResult(
      expectedMeasure,
      Pitch.parse("E4"),
      MeasureRythmnPattern.R_1,
    )

    assert(results.contains(expectedResult))
  }

  test("search returns results 2") {
    val measureSearch = createMeasureSearch()
    val results       = measureSearch.search(
      startPitch = Pitch.parse("G4"),
      startHarmonicPitch = Pitch.parse("G4"),
      nextMeasureStartHarmonicPitch = Pitch.parse("B4"),
      harmonicNoteNames = Set(NoteName.parse("C"), NoteName.parse("E"), NoteName.parse("G")),
      key = Key.parse("C Major"),
      measureRythmnPatterns = Set(
        MeasureRythmnPattern.R_22,
        MeasureRythmnPattern.R_22t,
        MeasureRythmnPattern.R_244,
        MeasureRythmnPattern.R_4444,
      ),
      pitchRange = (Pitch.parse("G3"), Pitch.parse("D5")),
    )

    // [G4(d=2) A4(d=2, p) | B4; R_22]
    val exp1 = MeasureSearchResult(
      Melody(
        List(
          Note(
            AnnotatedNote(Some(Pitch.parse("G4")), NoteAnnotation(false, ToneType.HARMONIC_TONE)),
            Duration.of(2),
            Part.Root,
          ),
          Note(
            AnnotatedNote(Some(Pitch.parse("A4")), NoteAnnotation(false, ToneType.PASSING_TONE)),
            Duration.of(2),
            Part.Root,
          ),
        ),
      ),
      Pitch.parse("B4"),
      MeasureRythmnPattern.R_22,
    )
    assert(results.contains(exp1))

    // [G4(d=2) C5(d=2, tied) | C5; R_22t]
    val exp2 = MeasureSearchResult(
      Melody(
        List(
          Note(
            AnnotatedNote(Some(Pitch.parse("G4")), NoteAnnotation(false, ToneType.HARMONIC_TONE)),
            Duration.of(2),
            Part.Root,
          ),
          Note(
            AnnotatedNote(Some(Pitch.parse("C5")), NoteAnnotation(true, ToneType.HARMONIC_TONE)),
            Duration.of(2),
            Part.Root,
          ),
        ),
      ),
      Pitch.parse("C5"),
      MeasureRythmnPattern.R_22t,
    )
    assert(results.contains(exp2))
  }

  test("search returns results 3") {
    val measureSearch = createMeasureSearch()
    val results       = measureSearch.search(
      startPitch = Pitch.parse("C5"),
      startHarmonicPitch = Pitch.parse("B4"),
      nextMeasureStartHarmonicPitch = Pitch.parse("C5"),
      harmonicNoteNames = Set(NoteName.parse("G"), NoteName.parse("B"), NoteName.parse("D")),
      key = Key.parse("C Major"),
      measureRythmnPatterns = Set(MeasureRythmnPattern.R_t22),
      pitchRange = (Pitch.parse("G3"), Pitch.parse("D5")),
    )

    // [C5(d=2, r) B4(d=2) | C5; R_t22]
    val exp1 = MeasureSearchResult(
      Melody(
        List(
          Note(
            AnnotatedNote(Some(Pitch.parse("C5")), NoteAnnotation(false, ToneType.SUSPENDED_TONE)),
            Duration.of(2),
            Part.Root,
          ),
          Note(
            AnnotatedNote(Some(Pitch.parse("B4")), NoteAnnotation(false, ToneType.HARMONIC_TONE)),
            Duration.of(2),
            Part.Root,
          ),
        ),
      ),
      Pitch.parse("C5"),
      MeasureRythmnPattern.R_t22,
    )
    assert(results.contains(exp1))
  }
}

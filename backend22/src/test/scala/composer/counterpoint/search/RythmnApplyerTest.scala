package composer.counterpoint.search

import composer.counterpoint.model.{MeasureRythmnPattern, NoteAnnotation, ToneType}
import model.containers.{Melody, Note}
import model.elements.{Duration, Part}
import model.elements.Interval.IntervalStep
import org.scalatest.funsuite.AnyFunSuite

class RythmnApplyerTest extends AnyFunSuite {

  test("successful application") {
    val seq     = MeasureStepSequence.parse("0,1p,2p,3|0")
    val pattern = MeasureRythmnPattern.R_4444
    val result  = RythmnApplyer.tryApplyRythmn(seq, pattern)

    assert(result.isDefined)

    val expectedMelody = Melody(
      List(
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(0)),
            NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE),
          ),
          Duration.of(1),
          Part.Root,
        ),
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(1)),
            NoteAnnotation(isTiedStart = false, ToneType.PASSING_TONE),
          ),
          Duration.of(1),
          Part.Root,
        ),
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(2)),
            NoteAnnotation(isTiedStart = false, ToneType.PASSING_TONE),
          ),
          Duration.of(1),
          Part.Root,
        ),
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(3)),
            NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE),
          ),
          Duration.of(1),
          Part.Root,
        ),
      ),
    )

    assert(result.get == expectedMelody)
  }

  test("note count mismatch") {
    val seq     = MeasureStepSequence.parse("0,1p,2|0") // 3 notes
    val pattern = MeasureRythmnPattern.R_4444           // 4 notes
    val result  = RythmnApplyer.tryApplyRythmn(seq, pattern)
    assert(result.isEmpty)
  }

  test("tie mismatch") {
    // Sequence requires tie, but pattern does not have it
    val seq1     = MeasureStepSequence.parse("0,1p,2|2") // isTiedToNextMeasureRequired is True
    val pattern1 = MeasureRythmnPattern.R_244            // isNextTied is False
    val result1  = RythmnApplyer.tryApplyRythmn(seq1, pattern1)
    assert(result1.isEmpty)

    // Pattern has tie, but sequence does not require it
    val seq2     = MeasureStepSequence.parse("0,1p,2|3") // isTiedToNextMeasureRequired is False
    val pattern2 = MeasureRythmnPattern.R_244t           // isNextTied is True
    val result2  = RythmnApplyer.tryApplyRythmn(seq2, pattern2)
    assert(result2.isEmpty)
  }

  test("suspension resolution") {
    // 正常系: R_t22 は前にタイが付く。2分音符2つなので、3拍目に解決音が来る
    val seq1     = MeasureStepSequence.parse("1r,0|1")
    val pattern1 = MeasureRythmnPattern.R_t22
    val result1  = RythmnApplyer.tryApplyRythmn(seq1, pattern1)

    assert(result1.isDefined)

    val expectedMelody = Melody(
      List(
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(1)),
            NoteAnnotation(isTiedStart = false, ToneType.SUSPENDED_TONE),
          ),
          Duration.of(2),
          Part.Root,
        ),
        Note(
          RythmnApplyer.AppliedIntervalStep(
            Some(IntervalStep(0)),
            NoteAnnotation(isTiedStart = false, ToneType.HARMONIC_TONE),
          ),
          Duration.of(2),
          Part.Root,
        ),
      ),
    )
    assert(result1.get == expectedMelody)

    // R_22 は前にタイが付かないのでNG
    val pattern2 = MeasureRythmnPattern.R_22
    val result2  = RythmnApplyer.tryApplyRythmn(seq1, pattern2)
    assert(result2.isEmpty)

    // 3拍目で解決しないのでNG
    val seq3     = MeasureStepSequence.parse("1r,2srh,0|-1")
    val pattern3 = MeasureRythmnPattern.R_t244
    val result3  = RythmnApplyer.tryApplyRythmn(seq3, pattern3)
    assert(result3.isEmpty)
  }
}

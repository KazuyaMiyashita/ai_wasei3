package composer.counterpoint.search

import model.elements.Interval.IntervalStep
import org.scalatest.funsuite.AnyFunSuite

class MeasureStepSequenceTest extends AnyFunSuite {

  test("usedHarmonicSteps returns correct set of harmonic steps") {
    val testCases = List(
      ("0|2", Set(IntervalStep(0))),
      ("0,3|3", Set(IntervalStep(0), IntervalStep(3))),
      ("0|4", Set(IntervalStep(0))),
      ("-1r,0,2|5", Set(IntervalStep(0), IntervalStep(2))),
      ("0,2|2", Set(IntervalStep(0), IntervalStep(2))),
      ("0,3,5,3|4", Set(IntervalStep(0), IntervalStep(3), IntervalStep(5))),
      ("1r,0,2|1", Set(IntervalStep(0), IntervalStep(2))),
      ("0,1p,2|3", Set(IntervalStep(0), IntervalStep(2))),
      ("0,1br,0|-1", Set(IntervalStep(0))),
      // inversion_normalized check
      ("0,10|1", Set(IntervalStep(0), IntervalStep(3))), // 10 % 7 == 3
    )

    for ((seqStr, expectedSteps) <- testCases) {
      val seq = MeasureStepSequence.parse(seqStr)
      assert(seq.usedHarmonicSteps == expectedSteps, s"Failed for sequence: $seqStr")
    }
  }

  test("parse and name roundtrip") {
    val sequencesStr = List(
      "0|2",
      "0,3|3",
      "-1r,0,2|5",
      "0,1p,2|3",
      "0,1br,0|-1",
    )
    for (sStr <- sequencesStr) {
      val seq = MeasureStepSequence.parse(sStr)
      assert(seq.name == sStr)
    }
  }
}

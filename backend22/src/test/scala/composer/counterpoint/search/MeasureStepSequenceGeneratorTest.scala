package composer.counterpoint.search

import org.scalatest.funsuite.AnyFunSuite

class MeasureStepSequenceGeneratorTest extends AnyFunSuite {

  test("generate simple sequences") {
    val sequences = MeasureStepSequenceGenerator.generate()
    assert(sequences.nonEmpty)

    val expectedSequences = List(
      "0,1p|2",
      "0,2|3",
      "0,3|1",
      "0,3|3",
      "0,1p,2|3",
      "0,1p,2p,3|2",
      "0,1p,2p,3p|4",
      "0,1br,0|-1",
      "1r,0|2",
      "0,2|2",
      "0,-2|-2",
    )

    val noExpectedSequences = List(
      "0,1|1", // 和音交代を伴うため。 H = {0, 1} のパターンは出現しない
    )

    for (expected <- expectedSequences) {
      assert(sequences.exists(_.name == expected), s"Expected sequence '$expected' was not generated.")
    }

    for (unexpected <- noExpectedSequences) {
      assert(!sequences.exists(_.name == unexpected), s"Unexpected sequence '$unexpected' was generated.")
    }
  }

  test("no consecutive same pitch in measure") {
    val sequences = MeasureStepSequenceGenerator.generate()
    for (seq <- sequences) {
      val notes = seq.measureNotes
      for (i <- 0 until notes.length - 1) {
        assert(notes(i).value != notes(i + 1).value, s"Consecutive same pitch found in sequence: ${seq.name}")
      }
    }
  }
}

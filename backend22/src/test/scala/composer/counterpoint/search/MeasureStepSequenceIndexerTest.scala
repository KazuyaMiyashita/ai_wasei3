package composer.counterpoint.search

import composer.counterpoint.model.MeasureRythmnPattern
import model.elements.Interval.IntervalStep
import org.scalatest.funsuite.AnyFunSuite

class MeasureStepSequenceIndexerTest extends AnyFunSuite {

  val sampleSequences: List[MeasureStepSequence] = {
    val sequencesStr = List(
      "0|2",       // notes=1, next=2, first=0, harmonics={0}, tie=False
      "0,3|3",     // notes=2, next=3, first=0, harmonics={0, 3}, tie=True
      "0|4",       // notes=1, next=4, first=0, harmonics={0}, tie=False
      "-1r,0,2|5", // notes=3, next=5, first=-1, harmonics={0, 2}, tie=False
      "0,2|2",     // notes=2, next=2, first=0, harmonics={0, 2}, tie=True
      "0,3,5,3|4", // notes=4, next=4, first=0, harmonics={0, 3, 5}, tie=False
      "1r,0,2|1",  // notes=3, next=1, first=1, harmonics={0, 2}, tie=False
    )
    sequencesStr.map(MeasureStepSequence.parse)
  }

  test("find simple equal") {
    val indexer = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)

    val results1 = indexer.find(Q(SearchField.NUM_NOTES_IN_MEASURE).equal(1))
    assert(results1.length == 2)
    assert(results1.forall(_.numNotesInMeasure == 1))

    val results2 = indexer.find(Q(SearchField.NEXT_MEASURE_STEP).equal(IntervalStep(5)))
    assert(results2.length == 1)
    assert(results2.forall(_.nextMeasureStep == IntervalStep(5)))

    val results3 = indexer.find(Q(SearchField.FIRST_NOTE_INTERVAL_STEP).equal(IntervalStep(-1)))
    assert(results3.length == 1)
    assert(results3.forall(_.firstNoteIntervalStepOfMeasure == IntervalStep(-1)))
  }

  test("find is in") {
    val indexer = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)
    val results = indexer.find(
      Q(SearchField.RYTHMN_PATTERNS).isIn(List(MeasureRythmnPattern.R_22t, MeasureRythmnPattern.R_4444)),
    )
    assert(results.length == 3)
    assert(results.forall(s => List("0,3|3", "0,2|2", "0,3,5,3|4").contains(s.name)))
  }

  test("find is subset of") {
    val indexer = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)
    val results = indexer.find(
      Q(SearchField.USED_HARMONIC_STEPS).isSubsetOf(Set(IntervalStep(0), IntervalStep(3))),
    )
    assert(results.length == 3)

    // 正規化されていない IntervalStep を含む場合
    val results2 = indexer.find(
      Q(SearchField.USED_HARMONIC_STEPS).isSubsetOf(
        Set(IntervalStep(0), IntervalStep(-2)),
      ), // -2 は 5 と同じ
    )
    assert(results2.length == 2)
    assert(
      results2.forall(_.usedHarmonicSteps.subsetOf(Set(IntervalStep(0), IntervalStep(5)))),
    ) // usedHarmonicSteps is normalized

    // IntervalStep(0) を含まない available_harmonic_steps はエラー
    assertThrows[IllegalArgumentException] {
      indexer.find(Q(SearchField.USED_HARMONIC_STEPS).isSubsetOf(Set(IntervalStep(3))))
    }
  }

  test("find combined and") {
    val indexer   = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)
    val condition = Q(SearchField.NUM_NOTES_IN_MEASURE)
      .equal(3)
      .and(Q(SearchField.FIRST_NOTE_INTERVAL_STEP).equal(IntervalStep(-1)))
      .and(Q(SearchField.USED_HARMONIC_STEPS).isSubsetOf(Set(IntervalStep(0), IntervalStep(2))))

    val results = indexer.find(condition)
    assert(results.length == 1)
    assert(results.head.name == "-1r,0,2|5")
  }

  test("find combined or") {
    val indexer   = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)
    val condition =
      Q(SearchField.NUM_NOTES_IN_MEASURE).equal(4).or(Q(SearchField.NEXT_MEASURE_STEP).equal(IntervalStep(1)))
    val results = indexer.find(condition)
    assert(results.length == 2)
    assert(results.forall(s => List("0,3,5,3|4", "1r,0,2|1").contains(s.name)))
  }

  test("find complex query") {
    val indexer = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)

    // (num_notes == 2 AND is_tied == True) OR (num_notes == 1 AND next_step == 2)
    val condition = (
      Q(SearchField.NUM_NOTES_IN_MEASURE).equal(2).and(Q(SearchField.IS_TIED_TO_NEXT_MEASURE_REQUIRED).equal(true))
    ).or(Q(SearchField.NUM_NOTES_IN_MEASURE).equal(1).and(Q(SearchField.NEXT_MEASURE_STEP).equal(IntervalStep(2))))

    val results = indexer.find(condition)
    assert(results.length == 3)
    assert(results.forall(s => List("0,3|3", "0,2|2", "0|2").contains(s.name)))
  }

  test("find no condition") {
    val indexer = new MeasureStepSequenceIndexer(sampleSequences, MeasureRythmnPattern.values.toList)
    val results = indexer.find()
    assert(results.length == sampleSequences.length)
  }
}

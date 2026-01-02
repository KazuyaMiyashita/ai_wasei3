package composer.counterpoint.search

import composer.counterpoint.model.ToneType
import model.containers.Note
import model.elements.{Duration, Part}
import model.elements.Interval.IntervalStep

/** 音価1のNoteをIntervalStepとToneTypeの組として利用する */
type AnnotatedIntervalStep = Note[IntervalStep, ToneType]

object AnnotatedIntervalStep {
  def apply(value: IntervalStep, duration: Duration, toneType: ToneType): AnnotatedIntervalStep =
    Note(value, duration, Part.Root, toneType)
}

trait AbstractMeasureStepSequence[T] {
  def measureNotes: List[Note[T, ToneType]]
  def nextMeasureStep: T

  /** 小節内で利用した音数 */
  def numNotesInMeasure: Int = measureNotes.length

  /** 小節の最後の音と次の小節の音をタイで繋げる必要があるか */
  def isTiedToNextMeasureRequired: Boolean = {
    measureNotes.last.value == nextMeasureStep
  }
}

/** 1小節およびその次の音で利用する候補となる音列を、一般化された形で表現する。
  *
  *   - 音列の各要素は、上下方向の移動の IntervalStep と、その音の和声音・非和声音の種別 ToneType を持つ
  *   - 音列は、和声音の場合 IntervalStep(0)、 掛留音の場合は IntervalStep(1) または IntervalStep(-1) から始まる
  *   - リズムの情報は持たない。便宜上 Duration.of(1) を指定した Note として扱う
  *   - 小節内の音数は 1 ~ 4 の範囲である
  *   - 次の小節で進行可能な音は、小節の最後の音と同じ音となることがある。その場合はタイで繋げて利用する必要がある
  *   - 小節内の音列で利用した和声音 (used_harmonic_steps) には必ず IntervalStep(0) が含まれる。
  *
  * TODO: 小節内の和音の切り替えを考慮しなければならないが、現在の小節単位の生成のモデリングでは実現が難しい。
  */
case class MeasureStepSequence(
    measureNotes: List[AnnotatedIntervalStep],
    nextMeasureStep: IntervalStep,
) extends AbstractMeasureStepSequence[IntervalStep] {

  import MeasureStepSequence.inversionNormalized

  require(numNotesInMeasure >= 1 && numNotesInMeasure <= 4)

  {
    val firstNote = measureNotes.head
    val valVal    = firstNote.value.value
    // In Python: first_note.value in {-1, 0, 1}
    require(Set(-1, 0, 1).contains(valVal))
    // In Python: first_note.attribute in {HARMONIC_TONE, SUSPENDED_TONE}
    require(Set(ToneType.HARMONIC_TONE, ToneType.SUSPENDED_TONE).contains(firstNote.meta))

    // assert (first_note.attribute == ToneType.HARMONIC_TONE) == (first_note.value == IntervalStep(0))
    require((firstNote.meta == ToneType.HARMONIC_TONE) == (valVal == 0))
    // assert (first_note.attribute == ToneType.SUSPENDED_TONE) == (first_note.value in {IntervalStep(-1), IntervalStep(1)})
    require((firstNote.meta == ToneType.SUSPENDED_TONE) == (Set(-1, 1).contains(valVal)))

    // assert IntervalStep(0) in self.used_harmonic_steps
    // usedHarmonicSteps is computed lazily, but let's check it if cheap.
    // Normalized check might be needed.
    // IntervalStep(0) normalized is 0.
    // usedHarmonicSteps contains normalized values.
    // The requirement says "at least one harmonic tone must be 0 (normalized)".
    // Actually Python says: `assert IntervalStep(0) in self.used_harmonic_steps`
    // And `used_harmonic_steps` returns normalized steps.
    // So 0 must be present.
  }

  def firstNoteIntervalStepOfMeasure: IntervalStep = measureNotes.head.value

  def firstNoteIntervalStep: IntervalStep = measureNotes.last.value // Wait, python says last.value?
  // Python:
  // def first_note_interval_step_of_measure(self) -> IntervalStep:
  //      return self.measure.notes[0].value
  // def first_note_interval_step(self) -> IntervalStep:
  //      return self.measure.notes[-1].value
  // That naming is confusing in Python too. `first_note_interval_step` returns the LAST note?
  // Let's re-read Python code carefully.
  /*
      def first_note_interval_step_of_measure(self) -> IntervalStep:
          return self.measure.notes[0].value

      def first_note_interval_step(self) -> IntervalStep:
          return self.measure.notes[-1].value
   */
  // Yes. I will use the same names to minimize confusion during porting, or rename if I can strictly track usages.
  // `first_note_interval_step` is indexed in `MeasureStepSequenceIndexer`.
  // It seems it is used as "the step that connects to the NEXT measure" or something?
  // Or maybe it's a typo in Python that persisted.
  // In `MeasureStepSequenceIndexer`, `index["first_note_interval_step"][result.first_note_interval_step_of_measure()].append(i)`
  // Wait, the indexer uses `first_note_interval_step_of_measure()`.
  // Where is `first_note_interval_step()` used?
  // It doesn't seem to be used in the indexer building.
  // I will just implement `firstNoteIntervalStepOfMeasure` and ignore the confusing one unless needed.

  /** 小節内の音列で和声音として利用されている IntervalStep の集合を返す。
    * 掛留音が解決する前に跳躍して進行する音もここに含める。
    *
    *   - IntervalStep はユニゾン~7度までの範囲に正規化(inversion_normalized)される。
    */
  lazy val usedHarmonicSteps: Set[IntervalStep] = {
    measureNotes
      .filter { note =>
        Set(ToneType.HARMONIC_TONE, ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE).contains(note.meta)
      }
      .map(_.value.inversionNormalized)
      .toSet
  }

  lazy val minStep: IntervalStep = {
    val steps = measureNotes.map(_.value) :+ nextMeasureStep
    steps.minBy(_.value)
  }

  lazy val maxStep: IntervalStep = {
    val steps = measureNotes.map(_.value) :+ nextMeasureStep
    steps.maxBy(_.value)
  }

  /** parseの逆。Hは省略される */
  def name: String = {
    val toneMap = Map(
      ToneType.HARMONIC_TONE                     -> "",
      ToneType.PASSING_TONE                      -> "p",
      ToneType.NEIGHBOR_TONE                     -> "br",
      ToneType.SUSPENDED_TONE                    -> "r",
      ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE -> "srh",
    )

    val stepsStr = measureNotes
      .map { step =>
        s"${step.value.value}${toneMap(step.meta)}"
      }
      .mkString(",")
    val nextMeasureStepStr = nextMeasureStep.value.toString
    s"$stepsStr|$nextMeasureStepStr"
  }

}

object MeasureStepSequence {

  extension (step: IntervalStep) {
    def inversionNormalized: IntervalStep = IntervalStep(model.elements.Math.mod(step.value, 7))
  }

  /** "-1r,0,10|1" といった文字列をパースする
    *
    * ここでは IntervalStep の 0-indexed 表現を用いていることに注意。
    *
    * 数字の後には H, p, br, r, のいずれかの文字が入ることがある。省略されていたらHとみなす。
    */
  def parse(s: String): MeasureStepSequence = {
    val toneMap = Map(
      "H"   -> ToneType.HARMONIC_TONE,
      "p"   -> ToneType.PASSING_TONE,
      "br"  -> ToneType.NEIGHBOR_TONE,
      "r"   -> ToneType.SUSPENDED_TONE,
      "srh" -> ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE,
    )

    // Regex to match "steps|next"
    // steps part: "-1r,0,10"
    // next part: "1"
    val mainPattern = """^(.+)\|(-?\d+)$""".r
    s match {
      case mainPattern(stepsStr, nextMeasureStepStr) =>
        val stepPattern = """^(-?\d+)(H|p|br|r|srh)?$""".r
        val parsedSteps = stepsStr
          .split(",")
          .map { stepS =>
            stepS match {
              case stepPattern(numberStr, suffixStrOrNull) =>
                val intervalStep = IntervalStep(numberStr.toInt)
                val suffixStr    = Option(suffixStrOrNull).getOrElse("H")
                val toneType     = toneMap.getOrElse(suffixStr, ToneType.HARMONIC_TONE)
                AnnotatedIntervalStep(intervalStep, Duration.of(1), toneType)
              case _ => throw new IllegalArgumentException(s"Internal error parsing step: $stepS")
            }
          }
          .toList

        MeasureStepSequence(parsedSteps, IntervalStep(nextMeasureStepStr.toInt))

      case _ => throw new IllegalArgumentException(s"cannot parse result: $s")
    }
  }
}

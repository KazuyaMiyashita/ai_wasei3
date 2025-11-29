// 音高・音程・音度といった基本要素を定義する。

package model

import scala.math.abs
import Math.{mod, Rational}

/** 国際式音名。C#4のような step, alter, octave の3つ組みの情報を扱う。
  *
  * Pitch や NoteName の parse, toString のために用いる。
  */
case class InternationalPitch(
    step: InternationalPitch.Step,
    alter: InternationalPitch.Alter,
    octave: InternationalPitch.Octave,
) {
  override def toString: String = {
    val accidental = if (alter.value > 0) "#" * alter.value else "b" * (-alter.value)
    s"${step}${accidental}${octave.value}"
  }

  def toPitch: Pitch = {
    val noteName   = InternationalPitch.toNoteName(step, alter)
    val baseOctave = step.basePitch.octave.value
    val octaveVal  = baseOctave + alter.value * -4 + octave.value - 4
    Pitch(Pitch.Octave(octaveVal), noteName)
  }
}

object InternationalPitch {

  enum Step(val basePitch: Pitch) {
    case C extends Step(Pitch.of(0, 0))
    case D extends Step(Pitch.of(-1, 2))
    case E extends Step(Pitch.of(-2, 4))
    case F extends Step(Pitch.of(1, -1))
    case G extends Step(Pitch.of(0, 1))
    case A extends Step(Pitch.of(-1, 3))
    case B extends Step(Pitch.of(-2, 5))
  }
  object Step {
    private val BASE_NOTE_NAME_TO_STEP: Map[Pitch.NoteName, Step] =
      Step.values.map(s => s.basePitch.noteName -> s).toMap
    def fromBaseNoteName(baseNoteName: Pitch.NoteName): Step = BASE_NOTE_NAME_TO_STEP(baseNoteName)
  }
  case class Alter(value: Int)
  case class Octave(value: Int)

  def fromNoteName(noteName: Pitch.NoteName): (Step, Alter) = {
    // F(-1) to B(5)
    val bases = List(-1, 0, 1, 2, 3, 4, 5).map(Pitch.NoteName.apply)
    bases.find(b => (noteName - b).value % 7 == 0) match {
      case Some(baseNoteName) =>
        val alter = (noteName - baseNoteName).value / 7
        val step  = Step.fromBaseNoteName(baseNoteName)
        (step, Alter(alter))
      case None => throw new RuntimeException("Unreachable")
    }
  }

  def toNoteName(step: Step, alter: Alter): Pitch.NoteName = {
    val baseNoteName = step.basePitch.noteName
    Pitch.NoteName(baseNoteName.value + alter.value * 7)
  }

  def stepAlterName(step: Step, alter: Alter): String = {
    val accidental = if (alter.value > 0) "#" * alter.value else "b" * (-alter.value)
    s"$step$accidental"
  }

  def fromPitch(pitch: Pitch): InternationalPitch = {
    val (step, alter) = fromNoteName(pitch.noteName)
    val baseOctave    = step.basePitch.octave.value
    val octave        = pitch.octave.value - baseOctave + 4 * alter.value + 4
    InternationalPitch(step, alter, Octave(octave))
  }

  private val stepAlterRegex = """^([A-G])([#b]*)$""".r

  def parseStepAlter(name: String): (Step, Alter) = name match {
    case stepAlterRegex(stepStr, accidentalStr) =>
      val alter = accidentalStr.count(_ == '#') - accidentalStr.count(_ == 'b')
      (InternationalPitch.Step.valueOf(stepStr), InternationalPitch.Alter(alter))
    case _ => throw new IllegalArgumentException(s"Invalid note name: $name")
  }

  private val regex = """^([A-G][#b]*)(\d+)$""".r

  def parse(name: String): InternationalPitch = name match {
    case regex(noteNameStr, octaveStr) =>
      val (step, alter) = parseStepAlter(noteNameStr)
      val octaveNum     = octaveStr.toInt
      InternationalPitch(step, alter, InternationalPitch.Octave(octaveNum))
    case _ => throw new IllegalArgumentException(s"Invalid pitch name: $name")
  }

}

/** 音高。
  *
  * F#4 のように表される情報を Octave と NoteName の2つの組みによって中央ハ音からの移動回数で表す。
  */
case class Pitch(octave: Pitch.Octave, noteName: Pitch.NoteName) {

  import Pitch.{Octave, NoteName}

  def +(interval: Interval): Pitch =
    Pitch(Octave(this.octave.value + interval.octave), NoteName(this.noteName.value + interval.fifth))

  def -(interval: Interval): Pitch =
    Pitch(Octave(this.octave.value - interval.octave), NoteName(this.noteName.value - interval.fifth))

  def -(that: Pitch): Interval =
    Interval(this.octave.value - that.octave.value, this.noteName.value - that.noteName.value)

  def internationalPitchNotation: InternationalPitch = InternationalPitch.fromPitch(this)

  def num: PitchNumber = PitchNumber(noteName.value * 7 + octave.value * 12)

  override def toString: String = internationalPitchNotation.toString
}

object Pitch {

  /** 音名。
    *
    * F# のように表される情報を五度圏における#方向の位置で表す
    */
  case class NoteName(value: Int) extends Ordered[NoteName] {
    require(value >= -15 && value <= 19, "NoteName must be between -15 and 19.")

    def +(that: NoteName): NoteName           = NoteName(this.value + that.value)
    def -(that: NoteName): NoteName           = NoteName(this.value - that.value)
    override def compare(that: NoteName): Int = this.value compare that.value

    def internationalPitchNotation: (InternationalPitch.Step, InternationalPitch.Alter) =
      InternationalPitch.fromNoteName(this)

    override def toString: String = {
      InternationalPitch.stepAlterName.tupled(internationalPitchNotation)
    }
  }

  object NoteName {

    def parse(name: String): NoteName = {
      InternationalPitch.toNoteName.tupled(InternationalPitch.parseStepAlter(name))
    }

  }

  /** 音高のオクターブ情報 */
  case class Octave(value: Int) {
    def +(that: Octave): Octave = Octave(this.value + that.value)
    def -(that: Octave): Octave = Octave(this.value - that.value)
  }

  def parse(name: String): Pitch = {
    InternationalPitch.parse(name).toPitch
  }

  def of(octave: Int, noteName: Int): Pitch = Pitch(Octave(octave), NoteName(noteName))
}

/** 音高を半音単位で数えたもの。中央ハ音を 0 とする。 */
case class PitchNumber(value: Int) extends Ordered[PitchNumber] {
  def +(that: IntervalNumber): PitchNumber     = PitchNumber(this.value + that.value)
  def -(that: PitchNumber): IntervalNumber     = IntervalNumber(this.value - that.value)
  override def compare(that: PitchNumber): Int = this.value compare that.value
}

/** 音程
  *
  * 表現方法はPitchと同様で、二つのPitchの各要素の差によって表現する。
  *
  * この表現のほかに、「長3度上」といった情報を「3度上」「長」のように表す IntervalStep, IntervalAlter の組と相互変換が可能である。
  */
case class Interval(octave: Int, fifth: Int) {

  import Interval.{IntervalStep, IntervalAlter}

  def +(that: Interval): Interval = Interval(octave + that.octave, fifth + that.fifth)
  def -(that: Interval): Interval = Interval(octave - that.octave, fifth - that.fifth)

  lazy val stepAlter: (IntervalStep, IntervalAlter) = {
    val step: IntervalStep = IntervalStep(4 * fifth + 7 * octave)

    val absFifth = abs(fifth)
    val stepSgn  = if (step.value < 0) -1 else 1
    val fifthSgn = if (fifth < 0) -1 else 1
    val sgn      = stepSgn * fifthSgn

    val valRes =
      if (absFifth <= 1) 0
      else if (absFifth <= 5) sgn * 1
      else sgn * (2 + ((absFifth - 6) / 7))

    (step, IntervalAlter(valRes))
  }

  def step: IntervalStep   = stepAlter._1
  def alter: IntervalAlter = stepAlter._2

  def num: IntervalNumber = IntervalNumber(fifth * 7 + octave * 12)

  override def toString: String = Interval.stepAlterToString(step, alter)
}

object Interval {

  /** 音程の「⚪︎度上」を表す。ただし 0-indexed で表し、「一度」の値は 0 となる。 */
  case class IntervalStep(value: Int)

  object IntervalStep {

    /** 一般的な1-indexedの度数（"3"度など）から IntervalStep を作成する。 */
    def idx_1(value: Int): IntervalStep = {
      if (value == 0) throw new IllegalArgumentException(s"idx_1 cannot be 0")
      else IntervalStep(if (value >= 1) value - 1 else value * 1)
    }
  }

  /** 音程の「完全」「長」「短」「増」「減」などを表す。 */
  case class IntervalAlter(value: Int)

  /** "P1", "-m3", "A4", "dd5" のような音程の表記を Interval に変換する
    *
    * Format: [sgn][Quality][Number]
    *   sgn: "" | "-"
    *   Quality: P=Perfect, M=Major, m=minor, A=Augmented, d=Diminished,
    *            AA=Double Augumented, dd=Double Diminished
    */
  def parse(name: String): Interval = fromStepAlter.tupled(parseStepAlter(name))

  private val regex = """^([-]?)([PMm]|A+|d+)(\d+)$""".r

  def parseStepAlter(name: String): (IntervalStep, IntervalAlter) = name match {
    case regex(sgnStr, qualStr, numStr) =>
      val num = numStr.toInt
      if (num < 1) throw new IllegalArgumentException(s"Interval degree must be 1 or greater, got $num")
      val sgn     = if (sgnStr == "-") -1 else 1
      val stepVal = (num - 1) * sgn

      val alterVal = qualStr match {
        case "P"                    => 0
        case "M"                    => 1
        case "m"                    => -1
        case q if q.startsWith("A") => q.length + 1
        case q if q.startsWith("d") => -(q.length + 1)
        case _                      => 0
      }
      (IntervalStep(stepVal), IntervalAlter(alterVal))
    case _ => throw new IllegalArgumentException(s"Invalid interval name: $name")
  }

  def stepAlterToString(step: IntervalStep, alter: IntervalAlter): String = {
    val s   = step.value
    val sgn = if (s >= 0) "" else "-"
    val num = abs(s) + 1
    val a   = alter.value

    val qual = a match {
      case 0            => "P"
      case 1            => "M"
      case -1           => "m"
      case x if x >= 2  => "A" * (x - 1)
      case x if x <= -2 => "d" * (-x - 1)
      case _            => "?"
    }
    s"$sgn$qual$num"
  }

  def fromStepAlter(step: IntervalStep, alter: IntervalAlter): Interval = {
    val s = step.value
    val a = alter.value

    val fClass     = mod(2 * s, 7)
    val stepSgn    = if (s < 0) -1 else 1
    val fBaseSharp = mod(fClass - 6, 7) + 6
    val fBaseFlat  = mod(fClass - 2, 7) - 12

    var f = 0
    if (a == 0) { // Perfect
      val fMap = Map(0 -> 0, 1 -> 1, 6 -> -1)
      if (!fMap.contains(fClass)) throw new IllegalArgumentException(s"Invalid Perfect interval step: $s")
      f = fMap(fClass)
    } else if (a == 1) { // Major
      if (!Set(2, 3, 4, 5).contains(fClass)) throw new IllegalArgumentException(s"Invalid Major interval step: $s")
      f = if (stepSgn == 1) fClass else fClass - 7
    } else if (a == -1) { // Minor
      if (!Set(2, 3, 4, 5).contains(fClass)) throw new IllegalArgumentException(s"Invalid Minor interval step: $s")
      f = if (stepSgn == 1) fClass - 7 else fClass
    } else if (a >= 2) { // Augmented
      val k = a - 2
      f = if (stepSgn == 1) fBaseSharp + 7 * k else fBaseFlat - 7 * k
    } else if (a <= -2) { // Diminished
      val k = -a - 2
      f = if (stepSgn == 1) fBaseFlat - 7 * k else fBaseSharp + 7 * k
    }

    val residual = s - 4 * f
    Interval(residual / 7, f)
  }

  lazy val P1 = parse("P1")
  lazy val m3 = parse("m3")
  lazy val M3 = parse("M3")
  lazy val P5 = parse("P5")
  lazy val P8 = parse("P8")
  // Add others as needed
}

/** 音程を半音単位で数えたもの */
case class IntervalNumber(value: Int) extends Ordered[IntervalNumber] {
  def +(that: IntervalNumber): IntervalNumber     = IntervalNumber(this.value + that.value)
  def -(that: IntervalNumber): IntervalNumber     = IntervalNumber(this.value - that.value)
  override def compare(that: IntervalNumber): Int = this.value compare that.value
}

/** 調。主音のNoteNameと旋法の組みで表す */
case class Key(tonic: Pitch.NoteName, mode: Key.Mode) {
  def signatureNum: Int = tonic.value + mode.offset

  def diatonicScalePitch(intervalStep: Interval.IntervalStep): Pitch = {
    val (o, n) = Key.calculateScalePitch(tonic.value, mode.offset, intervalStep.value)
    Pitch(Pitch.Octave(o), Pitch.NoteName(n))
  }
}

object Key {

  /** 旋法。長調と短調。 */
  enum Mode(val offset: Int) {
    case Major extends Mode(0)
    case Minor extends Mode(-3)

    override def toString: String = this match {
      case Major => "Major"
      case Minor => "Minor"
    }
  }

  object Mode {

    def parse(name: String): Mode = name match {
      case "Major" => Major
      case "Minor" => Minor
      case _       => throw new IllegalArgumentException(s"Invalid mode: $name")
    }
  }

  def calculateScalePitch(keyTonicVal: Int, modeOffset: Int, intervalStepVal: Int): (Int, Int) = {
    val signatureNum = keyTonicVal + modeOffset

    val (rootStep, _) = InternationalPitch.fromNoteName(Pitch.NoteName(keyTonicVal))
    val rootStepVal   = rootStep.ordinal
    val actualStepVal = intervalStepVal + rootStepVal

    def getAlters(num: Int): List[Int] = {
      val q = num / 7
      val r = mod(num, 7)
      (0 until 7).map(i => if (i < r) q + 1 else q).toList
    }

    // F, C, G, D, A, E, B -> Steps from C: 3, 0, 4, 1, 5, 2, 6
    val stepsMap = List(3, 0, 4, 1, 5, 2, 6)
    val invStep  = mod(actualStepVal, 7)

    val currentAlters = getAlters(signatureNum)
    // Find index in stepsMap where value == invStep
    val idx         = stepsMap.indexOf(invStep)
    val targetAlter = currentAlters(idx)

    val cMajorFifths      = List(0, 2, 4, -1, 1, 3, 5)    // C D E F G A B
    val cMajorBaseOctaves = List(0, -1, -2, 1, 0, -1, -2) // C D E F G A B

    val baseFifth  = cMajorFifths(invStep)
    val baseOctave = cMajorBaseOctaves(invStep)

    val octaveShift = java.lang.Math.floorDiv(actualStepVal, 7)

    // Equivalent to Interval.A1 * alter
    // A1 is octave=-4, fifth=7
    val finalFifth  = baseFifth + 7 * targetAlter
    val finalOctave = baseOctave + octaveShift + (-4 * targetAlter)

    (finalOctave, finalFifth)
  }
}

/** 音度。調内における相対的な位置を表す。
  * 例: ハ長調における F# は、第4音(Step=3)の半音上げ(Alter=1)。
  */
case class Degree(step: Degree.Step, alter: Degree.Alter) extends Ordered[Degree] {

  def noteName(key: Key): Pitch.NoteName = {
    val (_, diatonicVal) = Key.calculateScalePitch(key.tonic.value, key.mode.offset, step.value)
    Pitch.NoteName(diatonicVal + alter.value * 7)
  }

  override def compare(that: Degree): Int = {
    val s = this.step.compare(that.step)
    if (s != 0) s else this.alter.compare(that.alter)
  }
}

object Degree {

  /** 音度のステップ部分。0-indexedで表す。（第1音=0, 第2音=1...）。 */
  case class Step(value: Int) extends Ordered[Step] {
    require(0 <= value && value <= 6, "Step must be 0-6")

    def +(that: Step): Step = Step((this.value + that.value) % 7)
    def -(that: Step): Step = Step(mod(this.value - that.value, 7))

    override def compare(that: Step): Int = this.value compare that.value

    def toIdx1: Int = value + 1
  }

  object Step {
    def idx1(step: Int): Step = Step(step - 1)
  }

  /** 音度の変化記号部分（変化なし=0）。 */
  case class Alter(value: Int) extends Ordered[Alter] {
    require(-1 <= value && value <= 2, "Alter must be -1 to 2")

    override def compare(that: Alter): Int = this.value compare that.value
  }

  def fromNoteNameKey(noteName: Pitch.NoteName, key: Key): Degree = {
    val (nStep, _) = InternationalPitch.fromNoteName(noteName)
    val (tStep, _) = InternationalPitch.fromNoteName(key.tonic)

    val stepDiff   = mod(nStep.ordinal - tStep.ordinal, 7)
    val degreeStep = Step(stepDiff)

    val (_, diatonicVal) = Key.calculateScalePitch(key.tonic.value, key.mode.offset, degreeStep.value)
    val diff             = noteName.value - diatonicVal

    val alterVal = diff / 7
    Degree(degreeStep, Alter(alterVal))
  }

  def idx1(step: Int, alter: Int): Degree = Degree(Step.idx1(step), Alter(alter))
}

/** 音価。四分音符を 1 として数える。 */
case class Duration(value: Rational) extends Ordered[Duration] {
  def +(that: Duration): Duration           = Duration(this.value + that.value)
  def -(that: Duration): Duration           = Duration(this.value - that.value)
  def *(i: Int): Duration                   = Duration(this.value * i)
  override def compare(that: Duration): Int = this.value.compare(that.value)
  override def toString: String             = s"d=$value"
}

object Duration {
  def of(numerator: Int, denominator: Int = 1): Duration = Duration(Rational(numerator, denominator))
}

/** 小節冒頭などの基準地点からの経過時間を表す。必ず0から数え、1拍目=Offset(0)として利用する。 */
case class Offset(value: Rational) extends Ordered[Offset] {
  def +(that: Offset): Offset             = Offset(this.value + that.value)
  def +(that: Duration): Offset           = Offset(this.value + that.value)
  def -(that: Offset): Offset             = Offset(this.value - that.value)
  def *(i: Int): Offset                   = Offset(this.value * i)
  override def compare(that: Offset): Int = this.value.compare(that.value)
  override def toString: String           = s"Offset($value)"

  def asDuration: Duration = Duration(this.value)
}

object Offset {
  def of(numerator: Int, denominator: Int = 1): Offset = Offset(Rational(numerator, denominator))
}

/** 休符 */
sealed trait Rest
case object Rest extends Rest

package composer.counterpoint.model

import model.elements.Duration
import model.elements.Math.Rational
import model.containers.{Melody, Note}
import model.elements.Pitch

case class AnnotatedNote(value: Option[Pitch], annotation: NoteAnnotation)

type AnnotatedMeasure = Melody[AnnotatedNote, Note[AnnotatedNote]]

enum ToneType {

  /** 和声音。冒頭の休符も便宜上和声音として扱う。 */
  case HARMONIC_TONE

  /** 経過音 */
  case PASSING_TONE

  /** 刺繍音 */
  case NEIGHBOR_TONE

  /** 掛留音 */
  case SUSPENDED_TONE

  /** 掛留音が解決する前に進行する和声構成音や、掛留の先取解決で用いる音 */
  case SUSPENDED_RESOLVING_HARMONIC_TONE
}

case class NoteAnnotation(
    isTiedStart: Boolean,
    toneType: ToneType,
)

/** 課題の類を表す */
enum Species {

  /** 第一類、一音符対一音符 */
  case FIRST_SPECIES

  /** 第二類、二音符対一音符 */
  case SECOND_SPECIES

  /** 第三類、四音符対一音符 */
  case THIRD_SPECIES

  /** 第四類、移勢 */
  case FOURTH_SPECIES

  /** 第五類、華麗 */
  case FIFTH_SPECIES
}

/** 課題全体の小節の位置を表す */
enum MeasurePosition {

  /** 冒頭小節 */
  case FIRST

  /** 途中の小節 */
  case MIDDLE

  /** 最終小節の1小節前 */
  case PENULTIMATE

  /** 最終小節 */
  case LAST
}

/** 一小節のリズムを表す。これらの情報で表されるもののうち実際に利用できるものは MeasureRythmnPattern で定義される。 */
case class MeasureRythmn(
    isPreviousTied: Boolean,
    isNextTied: Boolean,
    durations: List[Duration],
    initRestDuration: Duration,
) {
  require(
    durations.foldLeft(Duration.of(0))(_ + _) + initRestDuration == Duration.of(4),
    s"durations: $durations, initRestDuration: $initRestDuration",
  )
  require(durations.length >= 1 && durations.length <= 4)

  def numDurations: Int = durations.length
}

/** 利用できるリズムの一覧 */
enum MeasureRythmnPattern(val value: String) {
  case R_1     extends MeasureRythmnPattern("1")
  case R_22    extends MeasureRythmnPattern("22")
  case R_t22   extends MeasureRythmnPattern("t22")
  case R_22t   extends MeasureRythmnPattern("22t")
  case R_t22t  extends MeasureRythmnPattern("t22t")
  case R_4444  extends MeasureRythmnPattern("4444")
  case R_244   extends MeasureRythmnPattern("244")
  case R_442   extends MeasureRythmnPattern("442")
  case R_t4444 extends MeasureRythmnPattern("t4444")
  case R_t244  extends MeasureRythmnPattern("t244")
  case R_t442  extends MeasureRythmnPattern("t442")
  case R_4444t extends MeasureRythmnPattern("4444t")
  case R_244t  extends MeasureRythmnPattern("244t")
  case R_2488  extends MeasureRythmnPattern("2488")
  case R_4882  extends MeasureRythmnPattern("4882")
  case R_t2488 extends MeasureRythmnPattern("t2488")
  case R_t4882 extends MeasureRythmnPattern("t4882")
  case R_4882t extends MeasureRythmnPattern("4882t")
  case R_2d4   extends MeasureRythmnPattern("2d4")
  case R_2d88  extends MeasureRythmnPattern("2d88")
  case R_rr2   extends MeasureRythmnPattern("rr2")
  case R_rr2t  extends MeasureRythmnPattern("rr2t")
  case R_r444  extends MeasureRythmnPattern("r444")
  case R_r42   extends MeasureRythmnPattern("r42")

  def measureRythmn: MeasureRythmn = {
    val pattern = """^(r*|t)?(\d+(?:d\d*)*)(t)?$""".r
    value match {
      case pattern(initStr, middle, lastStr) =>
        val initRestDuration = Duration.of(if (initStr != null) initStr.count(_ == 'r') else 0)
        val isPreviousTied   = initStr == "t"
        val isNextTied       = lastStr == "t"

        val notePattern = """(\d)(d)?""".r
        val durations   = notePattern
          .findAllMatchIn(middle)
          .map { m =>
            val durationStr = m.group(1)
            val dottedStr   = m.group(2)
            var duration    = Duration.of(4, durationStr.toInt)
            if (dottedStr != null) {
              duration = duration * Rational(3, 2)
            }
            duration
          }
          .toList

        MeasureRythmn(
          isPreviousTied = isPreviousTied,
          isNextTied = isNextTied,
          durations = durations,
          initRestDuration = initRestDuration,
        )
      case _ => throw new IllegalArgumentException(s"cannot parse pattern: $value")
    }
  }
}

// 楽譜の表現。分析用途の Score クラスよりもより具体的に楽譜を表現する。

package sheet

import model.containers.{Chord, Melody, Score}
import model.elements.Math.Rational
import model.elements.{Duration, Key, Part, Pitch, Rest}

case class Measure(melody: Melody[AttributedValue, Unit, Score[AttributedValue, Unit]]) {
  def duration: Duration                           = melody.duration
  def elements: List[Score[AttributedValue, Unit]] = melody.elems
}

object Measure {
  def of(elements: Score[AttributedValue, Unit]*): Measure = Measure(
    Melody(elements.toList, ()),
  )
}

case class TimeSignature(beats: Int, beatType: Duration) {
  def duration: Rational = beatType.value * beats

  override def toString: String = {
    val denom = Rational(4) / beatType.value
    val d     = if (denom.d == 1) denom.n else denom
    s"$beats/$d"
  }
}

trait HasScoreAttrs {
  def isTiedStart: Boolean
}

case class ScoreAttrs(isTiedStart: Boolean, graces: List[Pitch] = Nil) extends HasScoreAttrs

/**  パートごとに小節のリストを持つ。全パートで小節数が一致している必要がある。
  */
case class PartMapScore(
    parts: Map[Part, List[Measure]],
) {
  require(parts.values.map(_.size).toSet.size <= 1, "The number of measures must be the same for all parts")

  def numMeasures: Int = parts.values.headOption.map(_.size).getOrElse(0)

  def part(id: Part): List[Measure] = parts(id)

  /** 指定されたパートの合計演奏時間を計算する */
  def partTotalDuration(partId: Part): Duration = {
    parts
      .get(partId)
      .map {
        _.map(_.melody.duration).fold(Duration.of(0))(_ + _)
      }
      .getOrElse(Duration.of(0)) // パートが存在しない場合はDuration(0)を返す
  }

  /** 全パートの中で最大の合計演奏時間を取得する */
  def maxTotalDuration: Duration = {
    if (parts.isEmpty) Duration.of(0)
    else parts.keys.map(partTotalDuration).max
  }
}

case class AttributedValue(value: Pitch | Rest, attr: Option[ScoreAttrs])

/** 拍子記号の変更イベント */
case class TimeSignatureEvent(
    measureNumber: Int,
    timeSignature: TimeSignature,
)

/** 調号の変更イベント */
case class KeySignatureEvent(
    measureNumber: Int,
    key: Key,
)

/** 楽譜全体を表すクラス。
  */
case class SheetMusic(
    key: Key,
    timeSignature: TimeSignature,
    timeSignatureEvents: List[TimeSignatureEvent],
    keySignatureEvents: List[KeySignatureEvent],
    body: PartMapScore,
) {

  def toChord: Chord[
    AttributedValue,
    Unit,
    Melody[AttributedValue, Unit, Melody[AttributedValue, Unit, Score[AttributedValue, Unit]]],
  ] = {
    val maxDur       = body.maxTotalDuration
    val missingParts = scala.collection.mutable.ListBuffer[String]()

    val partElems: Seq[Melody[AttributedValue, Unit, Melody[AttributedValue, Unit, Score[AttributedValue, Unit]]]] =
      body.parts.map { case (partId, measures) =>
        val currentPartTotalDur = body.partTotalDuration(partId)
        val measureMelodies: List[Melody[AttributedValue, Unit, Score[AttributedValue, Unit]]] = measures.map(_.melody)

        val fullMelody = Melody(measureMelodies, ())

        if (currentPartTotalDur < maxDur) {
          val missing = maxDur - currentPartTotalDur
          missingParts += s"Part: $partId, Duration: $currentPartTotalDur, Expected: $maxDur, Missing: $missing"
          fullMelody
        } else {
          fullMelody
        }
      }.toSeq

    if (missingParts.nonEmpty) {
      throw new IllegalStateException(s"Part duration mismatch detected:\n${missingParts.mkString("\n")}")
    }

    Chord(partElems.toSet, ())
  }

}

object SheetMusic {

  def fromScore(
      key: Key,
      timeSignature: TimeSignature,
      score: Score[AttributedValue, Unit],
  ): SheetMusic = ??? // normalizeしてよしなに詰め替えればよさそうだが、Blankを含まないようにするのは一工夫必要そうだ

}

// 楽譜の表現。分析用途の Score クラスよりもより具体的に楽譜を表現する。

package model

import model.Math.Rational
import model.HasDuration.HasDurationOps

case class Measure(melody: Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]) {
  def duration: Duration                                  = melody.duration
  def notes: List[Note[Pitch | Rest, Option[ScoreAttrs]]] = melody.elems
}

object Measure {
  def of(elements: Note[Pitch | Rest, Option[ScoreAttrs]]*): Measure = Measure(Melody.of(elements*))
}

case class TimeSignature(beats: Int, beatType: Duration) {
  def duration: Rational = beatType.value * beats

  override def toString: String = {
    val denom = Rational(4) / beatType.value
    val d     = if (denom.denominator == 1) denom.numerator else denom
    s"$beats/$d"
  }
}

enum PartId {
  case Soprano, Alto, Tenor, Bass
}

trait HasScoreAttrs {
  def isTiedStart: Boolean
}

case class ScoreAttrs(isTiedStart: Boolean) extends HasScoreAttrs

/**  パートごとに小節のリストを持つ。全パートで小節数が一致している必要がある。
  */
case class PartMapScore(
    parts: Map[PartId, List[Measure]],
) {
  require(parts.values.map(_.size).toSet.size <= 1, "The number of measures must be the same for all parts")

  def numMeasures: Int = parts.values.headOption.map(_.size).getOrElse(0)

  def part(id: PartId): List[Measure] = parts(id)
}

/** 楽譜全体を表すクラス。
  */
case class SheetMusic(
    key: Key,
    timeSignature: TimeSignature,
    body: PartMapScore,
) {

  def toChord: Chord[PartId, Melody[Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]]] = {
    Chord.identified(
      body.parts.view.mapValues(measures => Melody.of(measures.map(measure => Melody.of(measure.notes*))*)).toSeq*,
    )
  }

}

object SheetMusic {

  def fromChord(
      key: Key,
      timeSignature: TimeSignature,
      chord: Chord[PartId, Melody[Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]]],
  ): SheetMusic = {

    SheetMusic(
      key,
      timeSignature,
      PartMapScore(chord.keyElems.map { case (partId, measures) =>
        partId -> measures.elems.map(melody => Measure.of(melody.elems*))
      }.toMap),
    )
  }

}

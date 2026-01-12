package model.containers

import model.elements.{Duration, Part}

/** 分析用の楽譜の木。Note, Melody, Chord からなる。
  *
  * @tparams A 木の葉であるNoteが持つ要素。Pitchなど
  */
sealed trait Score[A] {

  def duration: Duration

  def part: Part

  def mapValue[A2](f: A => A2): Score[A2]

  /** このScoreと全ての子孫の要素を深さ優先で返すイテレーター */
  def iterator: Iterator[Score[A]]

  assert(
    duration > Duration.of(0),
    s"duration must be positive value. duration: $duration",
  )

}

case class Note[A](
    value: A,
    override val duration: Duration,
    override val part: Part,
) extends Score[A] {
  override def mapValue[A2](f: A => A2): Note[A2] = Note(f(value), duration, part)
  override def iterator: Iterator[Score[A]]       = Iterator(this)
}

case class Melody[A, S <: Score[A]](
    elems: List[S],
) extends Score[A] {
  override def mapValue[A2](f: A => A2): Melody[A2, Score[A2]] = Melody(elems.map(e => e.mapValue(f)))
  override def iterator: Iterator[Score[A]]                    = Iterator(this) ++ elems.iterator.flatMap(_.iterator)
  override def duration: Duration                              = elems.map(_.duration).fold(Duration.of(0))(_ + _)
  override def part: Part                                      = Part.commonAncestor(elems.map(_.part))
}

case class Chord[A, S <: Score[A]](
    elems: Set[S],
) extends Score[A] {
  override def mapValue[A2](f: A => A2): Chord[A2, Score[A2]] = Chord(elems.map(e => e.mapValue(f)))
  override def iterator: Iterator[Score[A]]                   = Iterator(this) ++ elems.iterator.flatMap(_.iterator)
  override def duration: Duration                             = elems.head.duration
  override def part: Part                                     = Part.commonAncestor(elems.map(_.part))

  assert(
    elems.map(_.duration).toSet.size == 1,
    s"All notes in a chord must have the same duration. ${elems.map(e => s"${e.part}: ${e.duration}").mkString(", ")}",
  )

  assert(elems.map(_.part).size == elems.size, "Each element in a chord must belong to a different part.")

}

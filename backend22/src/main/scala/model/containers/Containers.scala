package model.containers

import model.containers.metas.*
import model.elements.{Duration, Offset, Part}
import scala.util.chaining.*

/** 分析用の楽譜の木。Note, Melody, Chord からなる。
  *
  * @tparams A 木の葉であるNoteが持つ要素。Pitch, Blankable[Pitch] など
  * @tparams B 全てのノードが持つ要素。メタ情報を主に扱う。Slice など
  */
sealed trait Score[A, B] {

  def duration: Duration

  def part: Part

  def meta: B

  def mapValue[A2](f: A => A2): Score[A2, B]

  def mapMeta[B2](f: B => B2): Score[A, B2]

  /** このScoreと全ての子孫の要素を深さ優先で返すイテレーター */
  def iterator: Iterator[Score[A, B]]

  def slice(offset: Offset): (Score[A, (B, Slice)], Score[A, (B, Slice)]) =
    ops.slice(this, offset)

  def partwise: PartwiseScore[A, Unit]

  assert(
    duration > Duration.of(0),
    s"duration must be positive value. duration: $duration",
  )

}

type PartwiseScore[A, B] = Chord[Option[A], B, Melody[Option[A], B, Note[Option[A], B]]]

object Score {

  def note[A](value: A, duration: Duration, part: Part): Note[A, Unit] =
    Note(value, duration, part, ())

  def melody[A, S <: Score[A, Unit]](elems: List[S]): Melody[A, Unit, S] =
    Melody(elems, ())

  def chord[A, S <: Score[A, Unit]](elems: Set[S]): Chord[A, Unit, S] =
    Chord(elems, ())

  extension [A, B](score: Score[A, (B, Slice)]) {
    def join: Option[Score[A, B]] = ops.join(score)
  }

}

case class Note[A, B](
    value: A,
    override val duration: Duration,
    override val part: Part,
    override val meta: B,
) extends Score[A, B] {
  override def mapValue[A2](f: A => A2): Note[A2, B] = Note(f(value), duration, part, meta)
  override def mapMeta[B2](f: B => B2): Note[A, B2]  = Note(value, duration, part, f(meta))
  override def iterator: Iterator[Score[A, B]]       = Iterator(this)
  override def partwise: PartwiseScore[A, Unit]      = {
    Chord(Set(Melody(mapValue(Some(_)).mapMeta(_ => ()) :: Nil, ())), ())
  }
}

case class Melody[A, B, S <: Score[A, B]](
    elems: List[S],
    override val meta: B,
) extends Score[A, B] {
  override def mapValue[A2](f: A => A2): Melody[A2, B, Score[A2, B]] = Melody(elems.map(e => e.mapValue(f)), meta)
  override def mapMeta[B2](f: B => B2): Melody[A, B2, Score[A, B2]]  = Melody(elems.map(e => e.mapMeta(f)), f(meta))
  override def iterator: Iterator[Score[A, B]]  = Iterator(this) ++ elems.iterator.flatMap(_.iterator)
  override def partwise: PartwiseScore[A, Unit] = {
    val partwiseElems: List[PartwiseScore[A, Unit]] = elems.map(_.partwise)

    // 1. このMelody全体に登場するすべてのパートを把握する
    val allParts = partwiseElems.flatMap(_.elems.map(_.part)).toSet

    // 2. 各パートについて、全区間をスキャンしてノートを生成する
    val melodies = allParts.map { part =>
      val notesForThisPart = partwiseElems.flatMap { pwChord =>
        // このパートがその区間に存在するか探す
        pwChord.elems.find(_.part == part) match {
          case Some(m) =>
            // 存在すればその音（List[Note]）をそのまま使う
            m.elems
          case None =>
            // 存在しない区間は、その区間の長さ(duration)を持つ「None」のNoteを作る
            List(Note(None, pwChord.duration, part, ()))
        }
      }
      Melody(notesForThisPart, ())
    }
    Chord(melodies, ()).tap { c =>
      assert(c.elems.map(_.part) == c.elems.map(_.iterator).flatten.map(_.part))
    }
  }

  override def duration: Duration = elems.map(_.duration).fold(Duration.of(0))(_ + _)
  override def part: Part         = Part.commonAncestor(elems.map(_.part))
}

case class Chord[A, B, S <: Score[A, B]](
    elems: Set[S],
    override val meta: B,
) extends Score[A, B] {
  override def mapValue[A2](f: A => A2): Chord[A2, B, Score[A2, B]] = Chord(elems.map(e => e.mapValue(f)), meta)
  override def mapMeta[B2](f: B => B2): Chord[A, B2, Score[A, B2]]  = Chord(elems.map(e => e.mapMeta(f)), f(meta))
  override def iterator: Iterator[Score[A, B]]  = Iterator(this) ++ elems.iterator.flatMap(_.iterator)
  override def partwise: PartwiseScore[A, Unit] = {
    // 1. 各要素を partwise (Chord[Melody[Note]]) に変換し、中の Melody をすべて取得
    val allPartMelodies = elems.flatMap(_.partwise.elems)

    // 2. Part ごとに Note のリストをフラットにまとめる
    val partNotes = allPartMelodies.toSeq
      .groupMap(_.part)(_.elems)
      .view
      .mapValues(_.flatten)
      .toMap

    // 3. 各 Part ごとに一つの Melody にまとめ、最後に Chord で包む
    val melodies = partNotes.map { (_, notes) =>
      Melody(notes.toList, ())
    }.toSet

    Chord(melodies, ()).tap { c =>
      assert(c.elems.map(_.part) == c.elems.map(_.iterator).flatten.map(_.part))
    }
  }
  override def duration: Duration = elems.head.duration
  override def part: Part         = Part.commonAncestor(elems.map(_.part))

  assert(
    elems.map(_.duration).toSet.size == 1,
    s"All notes in a chord must have the same duration. ${elems.map(e => s"${e.part}: ${e.duration}").mkString(", ")}",
  )

  assert(elems.map(_.part).size == elems.size, "Each element in a chord must belong to a different part.")

}

/** Score の M の中に入るもの一覧 */
package metas {

  case class Slice(isLeftCutaway: Boolean = false, isRightCutaway: Boolean = false)

}

// ----------- 以下は開発者向けの内部実装 ---------------

package ops {

  import model.containers.metas.Slice

  def slice[A, B](score: Score[A, B], offset: Offset): (Score[A, (B, Slice)], Score[A, (B, Slice)]) = {
    if (offset <= Offset.of(0)) {
      throw new IllegalArgumentException(s"Invalid slice offset: $offset")
    }

    val meta = score.meta

    score match {
      case Note(v, d, p, _) =>
        if (d <= offset.asDuration) {
          throw new IllegalArgumentException(s"Invalid slice offset: $offset for duration: $d")
        }
        val headDur = offset.asDuration
        val tailDur = d - headDur

        val lMeta = (meta, Slice(isLeftCutaway = false, isRightCutaway = true))
        val l     = Note(v, headDur, p, lMeta)

        val rMeta = (meta, Slice(isLeftCutaway = true, isRightCutaway = false))
        val r     = Note(v, tailDur, p, rMeta)

        (l, r)

      case Melody(elems, _) =>
        def loop(
            rem: List[Score[A, B]],
            curr: Offset,
        ): (List[Score[A, (B, Slice)]], List[Score[A, (B, Slice)]]) = rem match {
          case Nil          => (Nil, Nil)
          case head :: tail =>
            val headDur = head.duration

            if (curr.asDuration >= headDur) {
              val processedHead = appendSlice(head, Slice())
              val (l, r)        = loop(tail, curr - headDur)
              (processedHead :: l, r)
            } else if (curr <= Offset.of(0)) {
              val processedHead = appendSlice(head, Slice())
              (Nil, processedHead :: tail.map(appendSlice(_, Slice())))
            } else {
              val (hLeft, hRight) = slice(head, curr)
              (List(hLeft), hRight :: tail.map(appendSlice(_, Slice())))
            }
        }
        val (lElems, rElems) = loop(elems, offset)

        val lMeta = (meta, Slice(isLeftCutaway = false, isRightCutaway = true))
        val l     = Melody(lElems, lMeta)

        val rMeta = (meta, Slice(isLeftCutaway = true, isRightCutaway = false))
        val r     = Melody(rElems, rMeta)

        (l, r)

      case Chord(elems, _) =>
        val sliced = elems.map(child => slice(child, offset))
        val lElems = sliced.map(_._1)
        val rElems = sliced.map(_._2)

        val lMeta = (meta, Slice(isLeftCutaway = false, isRightCutaway = true))
        val l     = Chord(lElems, lMeta)

        val rMeta = (meta, Slice(isLeftCutaway = true, isRightCutaway = false))
        val r     = Chord(rElems, rMeta)

        (l, r)
    }
  }

  private def appendSlice[A, B](score: Score[A, B], s: Slice): Score[A, (B, Slice)] = {
    val newMeta = (score.meta, s)
    score match {
      case Note(v, d, p, _) => Note(v, d, p, newMeta)
      case Melody(elems, _) =>
        val newElems = elems.map(e => appendSlice(e, s))
        Melody(newElems, newMeta)
      case Chord(elems, _) =>
        val newElems = elems.map(e => appendSlice(e, s))
        Chord(newElems, newMeta)
    }
  }

  def joinNext[A, B](s1: Score[A, (B, Slice)], s2: Score[A, (B, Slice)]): Option[Score[A, B]] = {
    val (meta1, slice1) = s1.meta
    val (_, slice2)     = s2.meta

    if (slice1.isRightCutaway && slice2.isLeftCutaway) {
      if (!slice1.isLeftCutaway && !slice2.isRightCutaway) {
        // clean check passed. Result meta is meta1 (which is B).
        // Compatibility check: meta1 should equal meta2?
        // if (meta1 == meta2) {
        (s1, s2) match {
          case (n1: Note[A, (B, Slice)], n2: Note[A, (B, Slice)]) =>
            if (n1.value == n2.value) {
              Some(Note(n1.value, n1.duration + n2.duration, Part.commonAncestor(n1.part :: n2.part :: Nil), meta1))
            } else None

          case (
                m1: Melody[A, (B, Slice), Score[A, (B, Slice)]] @unchecked,
                m2: Melody[A, (B, Slice), Score[A, (B, Slice)]] @unchecked,
              ) =>
            val last1 = m1.elems.last
            val head2 = m2.elems.head

            val boundaryMerged = joinNext(last1, head2)

            val p1ElemsOption = if (boundaryMerged.isDefined) {
              m1.elems.init.map(e => join(e))
            } else {
              m1.elems.map(e => join(e))
            }

            val p2ElemsOption = if (boundaryMerged.isDefined) {
              m2.elems.tail.map(e => join(e))
            } else {
              m2.elems.map(e => join(e))
            }

            if (p1ElemsOption.forall(_.isDefined) && p2ElemsOption.forall(_.isDefined)) {
              val p1Elems = p1ElemsOption.map(_.get)
              val p2Elems = p2ElemsOption.map(_.get)
              val merged  = boundaryMerged.toList

              Some(Melody(p1Elems ++ merged ++ p2Elems, meta1))
            } else None

          case (
                c1: Chord[A, (B, Slice), Score[A, (B, Slice)]] @unchecked,
                c2: Chord[A, (B, Slice), Score[A, (B, Slice)]] @unchecked,
              ) =>
            val joinedElems = c1.elems.flatMap { e1 =>
              c2.elems.map { e2 => joinNext(e1, e2) }.collect { case Some(e) => e }
            }
            if (joinedElems.size == c1.elems.size) {
              Some(Chord(joinedElems, meta1))
            } else None

          case _ => None
        }
        // } else None
      } else None
    } else None
  }

  def join[A, B](s: Score[A, (B, Slice)]): Option[Score[A, B]] = {
    val (meta, slice) = s.meta

    if (!slice.isLeftCutaway && !slice.isRightCutaway) {
      s match {
        case n: Note[A, (B, Slice)] => Some(Note(n.value, n.duration, n.part, meta))

        case m: Melody[A, (B, Slice), Score[A, (B, Slice)]] @unchecked =>
          def loop(
              rem: List[Score[A, (B, Slice)]],
              acc: List[Score[A, B]],
              pending: Option[Score[A, (B, Slice)]],
          ): Option[List[Score[A, B]]] = rem match {
            case Nil =>
              pending match {
                case Some(p) => join(p).map(clean => (clean :: acc).reverse)
                case None    => Some(acc.reverse)
              }
            case head :: tail =>
              pending match {
                case Some(prev) =>
                  joinNext(prev, head) match {
                    case Some(joined) =>
                      loop(tail, joined :: acc, None)
                    case None =>
                      join(prev) match {
                        case Some(p) => loop(tail, p :: acc, Some(head))
                        case None    => None
                      }
                  }
                case None =>
                  loop(tail, acc, Some(head))
              }
          }
          loop(m.elems, Nil, None).map(es => Melody(es, meta))

        case c: Chord[A, (B, Slice), Score[A, (B, Slice)]] @unchecked =>
          val joinedElems = c.elems.map(e => join(e))
          if (joinedElems.forall(_.isDefined)) {
            Some(Chord(joinedElems.map(_.get), meta))
          } else None
      }
    } else None
  }

}

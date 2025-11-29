package model

/** Containers.scala で定義した Note, Melody, Chord に適切な型が付いている時、これらを木構造として扱う
  *
  * この Score が音楽分析の基盤となる。
  */
trait Score[+Id, +A, +Attr]
object Score {

  case class NoteScore[Id, A, Attr](note: Note[A, Attr])                   extends Score[Id, A, Attr]
  case class MelodyScore[Id, A, Attr](melody: Melody[Score[Id, A, Attr]])  extends Score[Id, A, Attr]
  case class ChordScore[Id, A, Attr](chord: Chord[Id, Score[Id, A, Attr]]) extends Score[Id, A, Attr]

  implicit def noteScoreHasDuration[Id, A, Attr]: HasDuration[NoteScore[Id, A, Attr]] =
    new HasDuration[NoteScore[Id, A, Attr]] {
      def getDuration(s: NoteScore[Id, A, Attr]): Duration = s.note.duration
    }

  implicit def scoreHasDuration[Id, A, Attr]: HasDuration[Score[Id, A, Attr]] = new HasDuration[Score[Id, A, Attr]] {
    import HasDuration.HasDurationOps
    def getDuration(s: Score[Id, A, Attr]): Duration = s match {
      case NoteScore(note)     => note.duration
      case MelodyScore(melody) => melody.duration
      case ChordScore(chord)   => chord.duration
    }
  }

}

/** ある要素や Note, Melody, Chord が Score に変換可能であることを表す型クラス */
trait ScoreLike[I, Id, A, Attr] {
  def toScore(input: I): Score[Id, A, Attr]
}

trait ScoreLikeLowPriority {

  /** ある要素に音価がある時、それをNoteに変換して Score (NoteScore) にする。
    * Melody や Chord よりも適用される優先順位が低い。
    * 単純な要素から作られるため、Attribute は Unit となる。
    */
  implicit def elementIsScore[Id, A](implicit ev: HasDuration[A]): ScoreLike[A, Id, A, Unit] =
    new ScoreLike[A, Id, A, Unit] {
      def toScore(a: A): Score[Id, A, Unit] = {
        Score.NoteScore(Note(a, ev.getDuration(a), ()))
      }
    }

  /** Note[I] の中身 I が ScoreLike である場合、その中身を Score に変換して返す (Noteの皮を剥く)。
    * noteIsScore (Note[A] -> Score[Id, A]) との競合を避けるため、低優先度にする。
    * Note自体が持っていた Attribute は捨てられ、中身の Score の Attribute が採用される。
    */
  implicit def noteWrapperIsScore[I, Id, A, Attr, NAttr](implicit
      ev: ScoreLike[I, Id, A, Attr],
  ): ScoreLike[Note[I, NAttr], Id, A, Attr] =
    new ScoreLike[Note[I, NAttr], Id, A, Attr] {
      def toScore(n: Note[I, NAttr]): Score[Id, A, Attr] = ev.toScore(n.value)
    }
}

object ScoreLike extends ScoreLikeLowPriority {

  /** すでに Score であるものはそのまま返す */
  implicit def scoreIsScore[Id, A, Attr]: ScoreLike[Score[Id, A, Attr], Id, A, Attr] =
    new ScoreLike[Score[Id, A, Attr], Id, A, Attr] {
      def toScore(s: Score[Id, A, Attr]): Score[Id, A, Attr] = s
    }

  /** Note[A, Attr] は明示的に NoteScore に変換する。 (elementIsScore よりも優先させる) */
  implicit def noteIsScore[Id, A, Attr]: ScoreLike[Note[A, Attr], Id, A, Attr] =
    new ScoreLike[Note[A, Attr], Id, A, Attr] {
      def toScore(n: Note[A, Attr]): Score[Id, A, Attr] = Score.NoteScore(n)
    }

  /** Melody[I] を再帰的に Score (MelodyScore) に変換する。 */
  implicit def melodyIsScore[I, Id, A, Attr](implicit
      ev: ScoreLike[I, Id, A, Attr],
  ): ScoreLike[Melody[I], Id, A, Attr] =
    new ScoreLike[Melody[I], Id, A, Attr] {
      def toScore(m: Melody[I]): Score[Id, A, Attr] = {
        val scores = m.elems.map(ev.toScore)
        Score.MelodyScore(Melody(scores))
      }
    }

  /** Chord[Id, I] を再帰的に Score (ChordScore) に変換する。 */
  implicit def chordIsScore[I, Id, A, Attr](implicit
      ev: ScoreLike[I, Id, A, Attr],
  ): ScoreLike[Chord[Id, I], Id, A, Attr] =
    new ScoreLike[Chord[Id, I], Id, A, Attr] {
      def toScore(c: Chord[Id, I]): Score[Id, A, Attr] = {
        val newVoices = c.keyElems.map { case (k, v) =>
          (k, ev.toScore(v))
        }
        Score.ChordScore(Chord(newVoices))
      }
    }
}

/** Score型を作成するためのユーティリティ */
object ScoreSyntax {
  implicit class ScoreLikeOps[I](val input: I) extends AnyVal {

    /** 任意の ScoreLike な型を Score[Id, A] に変換する */
    def asScore[Id, A, Attr](implicit ev: ScoreLike[I, Id, A, Attr]): Score[Id, A, Attr] = ev.toScore(input)
  }
}

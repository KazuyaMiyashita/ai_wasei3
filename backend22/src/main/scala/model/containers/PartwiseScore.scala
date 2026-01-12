package model.containers
import scala.util.chaining.*

type PartwiseScore[A] = Chord[Option[A], Melody[Option[A], Note[Option[A]]]]

object PartwiseScore {

  def partwise[A](score: Score[A]): PartwiseScore[A] = {
    score match {
      case note: Note[A] =>
        Chord(Set(Melody(List(note.mapValue(Some(_))))))
      case melody: Melody[A, _] =>
        val elems                                 = melody.elems.asInstanceOf[List[Score[A]]]
        val partwiseElems: List[PartwiseScore[A]] = elems.map(partwise(_))

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
                List(Note(None, pwChord.duration, part))
            }
          }
          Melody(notesForThisPart)
        }
        Chord(melodies).tap { c =>
          assert(c.elems.map(_.part) == c.elems.map(_.iterator).flatten.map(_.part))
        }
      case chord: Chord[A, _] =>
        // 1. 各要素を partwise (Chord[Melody[Note]]) に変換し、中の Melody をすべて取得
        val elems           = chord.elems.asInstanceOf[Set[Score[A]]]
        val allPartMelodies = elems.flatMap(partwise(_).elems)

        // 2. Part ごとに Note のリストをフラットにまとめる
        val partNotes = allPartMelodies.toSeq
          .groupMap(_.part)(_.elems)
          .view
          .mapValues(_.flatten)
          .toMap

        // 3. 各 Part ごとに一つの Melody にまとめ、最後に Chord で包む
        val melodies = partNotes.map { (_, notes) =>
          Melody[Option[A], Note[Option[A]]](notes.toList)
        }.toSet

        Chord(melodies).tap { c =>
          assert(c.elems.map(_.part) == c.elems.map(_.iterator).flatten.map(_.part))
        }
    }
  }

}

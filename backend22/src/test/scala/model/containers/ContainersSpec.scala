package model.containers

import model.containers.Score
import model.containers.Score.{note => mkNote, _}
import model.containers.metas.*
import model.elements.{Duration, Offset, Part}
import org.scalatest.funsuite.AnyFunSuite

class ContainersSpec extends AnyFunSuite {

  test("part") {
    val p_s   = Part.of("Soprano")
    val p_s_1 = Part.of("Soprano", "Voice1")
    val p_a   = Part.of("Alto")

    // メタデータとして Part を直接渡す
    val n1 = mkNote("C", Duration.of(1), p_s_1)
    val n2 = mkNote("D", Duration.of(1), p_a)

    val score = melody(List(n1, n2))

    // n1 (Soprano:Voice1) と n2 (Alto) の共通親 -> Root
    assert(score.part == Part.Root)

    val n3     = mkNote("E", Duration.of(1), p_s)
    val score2 = melody(List(n1, n3))

    assert(score2.part == p_s)
  }

  test("slice") {
    val note = mkNote("C", Duration.of(4), Part.Root)

    // Note Slice
    val (lNote, rNote) = note.slice(Offset.of(1))
    assert(lNote.duration == Duration.of(1))
    assert(rNote.duration == Duration.of(3))

    // Check Slice meta
    // (Unit, Slice)
    val (_, lSlice) = lNote.meta
    val (_, rSlice) = rNote.meta

    assert(!lSlice.isLeftCutaway && lSlice.isRightCutaway)
    assert(rSlice.isLeftCutaway && !rSlice.isRightCutaway)

    // Melody Slice
    val melody             = Melody(List(note), ())
    val (lMelody, rMelody) = melody.slice(Offset.of(1))

    assert(lMelody.duration == Duration.of(1))
    assert(rMelody.duration == Duration.of(3))

    val (_, lMelodySlice) = lMelody.meta
    assert(!lMelodySlice.isLeftCutaway && lMelodySlice.isRightCutaway)
  }

  test("joinNext chord") {
    val p1 = Part.of("p1")
    val p2 = Part.of("p2")

    val n1    = mkNote("C", Duration.of(3), p1)
    val n2    = mkNote("D", Duration.of(3), p2)
    val score = chord(Set(n1, n2))

    val (l, r) = score.slice(Offset.of(2))

    val joined = ops.joinNext(l, r)

    assert(joined.isDefined)
    assert(joined.get == score)
  }

  test("join melody internal") {
    val nOriginal = mkNote("C", Duration.of(4), Part.Root)
    val (l, r)    = nOriginal.slice(Offset.of(2))

    // l and r have meta (Unit, Slice).
    // Melody must have meta (Unit, Slice).
    // clean slice for parent
    val metaWithSlice = ((), Slice(false, false))

    val melody = Melody(List(l, r), metaWithSlice)

    val joined = melody.join

    assert(joined.isDefined)
    assert(joined.get.duration == Duration.of(4))
    assert(joined.get.meta == ()) // Outer slice peeled

    val joinedElems = joined.get.asInstanceOf[Melody[String, Unit, ?]].elems
    assert(joinedElems.size == 1)
    assert(joinedElems.head.duration == Duration.of(4))
    assert(joinedElems.head.asInstanceOf[Note[String, Unit]].value == "C")
  }

  test("partwise") {
    val part_s   = Part.of("Soprano")
    val part_s_1 = part_s.spawn("1")
    val score    = Melody[String, Unit, Score[String, Unit]](
      mkNote("C", Duration.of(1), part_s) :: Chord(
        Set(
          mkNote("D", Duration.of(1), part_s),
          mkNote("E", Duration.of(1), part_s_1),
        ),
        (),
      ) :: Nil,
      (),
    )
    val partwiseScore: PartwiseScore[String, Unit] = score.partwise

    val expected = Chord(
      Set(
        Melody(
          List(
            mkNote(Some("C"), Duration.of(1), part_s),
            mkNote(Some("D"), Duration.of(1), part_s),
          ),
          (),
        ),
        Melody(
          List(
            mkNote(None, Duration.of(1), part_s_1),
            mkNote(Some("E"), Duration.of(1), part_s_1),
          ),
          (),
        ),
      ),
      (),
    )

    assert(partwiseScore == expected)
  }

  test("partwise - part disappears (filling None at the end)") {
    val pA = Part.of("A")
    val pB = Part.of("B")

    // Melody: [Chord(A, B), Note(A)]
    // パートBは後半に存在しないため、Noneで埋められる必要がある
    val score = Score.melody(
      List(
        Score.chord(Set(mkNote("A1", Duration.of(1), pA), mkNote("B1", Duration.of(1), pB))),
        mkNote("A2", Duration.of(1), pA),
      ),
    )

    val result = score.partwise

    val expected = Chord(
      Set(
        Melody(
          List(
            mkNote(Some("A1"), Duration.of(1), pA),
            mkNote(Some("A2"), Duration.of(1), pA),
          ),
          (),
        ),
        Melody(
          List(
            mkNote(Some("B1"), Duration.of(1), pB),
            mkNote(None, Duration.of(1), pB), // ここがNoneで埋まる
          ),
          (),
        ),
      ),
      (),
    )

    assert(result == expected)
  }

  test("partwise - complex nested chord") {
    val pA = Part.of("A")
    val pB = Part.of("B")
    val pC = Part.of("C")

    // Chord(Note(A), Melody(Note(B), Note(C)))
    // Durationは全て 2 である必要がある (Aは2, Bは1, Cは1)
    val score = Score.chord(
      Set(
        mkNote("A", Duration.of(2), pA),
        Score.melody(List(mkNote("B", Duration.of(1), pB), mkNote("C", Duration.of(1), pC))),
      ),
    )

    val result = score.partwise

    // 全てのパートのMelodyの合計Durationが 2 になっているか
    assert(result.elems.forall(_.duration == Duration.of(2)))

    // パートAの検証
    val mB = result.elems.find(_.part == pB).get
    assert(mB.elems.map(_.value) == List(Some("B"), None)) // Bの後はCの区間なのでBパートはNone
  }

  test("partwise - total duration consistency") {
    val pA = Part.of("A")
    val pB = Part.of("B")

    val score = Score.melody(
      List(
        mkNote("A1", Duration.of(3), pA),
        Score.chord(Set(mkNote("A2", Duration.of(2), pA), mkNote("B1", Duration.of(2), pB))),
      ),
    )

    val result = score.partwise

    // 元のスコアの長さ 3 + 2 = 5
    assert(score.duration == Duration.of(5))
    // 変換後も全てのパートが長さ5を持っているか
    result.elems.foreach { m =>
      assert(m.duration == Duration.of(5))
    }
  }

}

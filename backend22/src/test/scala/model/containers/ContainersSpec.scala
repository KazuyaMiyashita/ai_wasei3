package model.containers

import model.elements.{Duration, Part}
import org.scalatest.funsuite.AnyFunSuite

class ContainersSpec extends AnyFunSuite {

  def mkNote[A](value: A, duration: Duration, part: Part): Note[A] = Note(value, duration, part)

  test("part") {
    val p_s   = Part.of("Soprano")
    val p_s_1 = Part.of("Soprano", "Voice1")
    val p_a   = Part.of("Alto")

    val n1 = mkNote("C", Duration.of(1), p_s_1)
    val n2 = mkNote("D", Duration.of(1), p_a)

    val score = Melody(List(n1, n2))

    // n1 (Soprano:Voice1) と n2 (Alto) の共通親 -> Root
    assert(score.part == Part.Root)

    val n3     = mkNote("E", Duration.of(1), p_s)
    val score2 = Melody(List(n1, n3))

    assert(score2.part == p_s)
  }

  test("partwise") {
    val part_s   = Part.of("Soprano")
    val part_s_1 = part_s.spawn("1")
    val score    = Melody(
      mkNote("C", Duration.of(1), part_s) :: Chord(
        Set(
          mkNote("D", Duration.of(1), part_s),
          mkNote("E", Duration.of(1), part_s_1),
        ),
      ) :: Nil,
    )
    val partwiseScore: PartwiseScore[String] = PartwiseScore.partwise(score)

    val expected = Chord(
      Set(
        Melody(
          List(
            mkNote(Some("C"), Duration.of(1), part_s),
            mkNote(Some("D"), Duration.of(1), part_s),
          ),
        ),
        Melody(
          List(
            mkNote(None, Duration.of(1), part_s_1),
            mkNote(Some("E"), Duration.of(1), part_s_1),
          ),
        ),
      ),
    )

    // Using iterator to check equality because order in Set in Chord might differ or be hard to compare directly without ordering
    // But Chord equality check should work if Sets are equal.
    // However, Melodies inside are reference types? No, case classes.
    // Set equality relies on hashCode/equals.
    assert(partwiseScore == expected)
  }

  test("partwise - part disappears (filling None at the end)") {
    val pA = Part.of("A")
    val pB = Part.of("B")

    // Melody: [Chord(A, B), Note(A)]
    // パートBは後半に存在しないため、Noneで埋められる必要がある
    val score = Melody(
      List(
        Chord(Set(mkNote("A1", Duration.of(1), pA), mkNote("B1", Duration.of(1), pB))),
        mkNote("A2", Duration.of(1), pA),
      ),
    )

    val result = PartwiseScore.partwise(score)

    val expected = Chord(
      Set(
        Melody(
          List(
            mkNote(Some("A1"), Duration.of(1), pA),
            mkNote(Some("A2"), Duration.of(1), pA),
          ),
        ),
        Melody(
          List(
            mkNote(Some("B1"), Duration.of(1), pB),
            mkNote(None, Duration.of(1), pB), // ここがNoneで埋まる
          ),
        ),
      ),
    )

    assert(result == expected)
  }

  test("partwise - complex nested chord") {
    val pA = Part.of("A")
    val pB = Part.of("B")
    val pC = Part.of("C")

    // Chord(Note(A), Melody(Note(B), Note(C)))
    // Durationは全て 2 である必要がある (Aは2, Bは1, Cは1)
    val score = Chord(
      Set(
        mkNote("A", Duration.of(2), pA),
        Melody(List(mkNote("B", Duration.of(1), pB), mkNote("C", Duration.of(1), pC))),
      ),
    )

    val result = PartwiseScore.partwise(score)

    // 全てのパートのMelodyの合計Durationが 2 になっているか
    assert(result.elems.forall(_.duration == Duration.of(2)))

    // パートAの検証
    val mB = result.elems.find(_.part == pB).get
    // Check values inside Melody.
    // mB.elems is List[Note[Option[String]]]
    val values = mB.elems.map(_.value)
    assert(values == List(Some("B"), None)) // Bの後はCの区間なのでBパートはNone
  }

  test("partwise - total duration consistency") {
    val pA = Part.of("A")
    val pB = Part.of("B")

    val score = Melody(
      List(
        mkNote("A1", Duration.of(3), pA),
        Chord(Set(mkNote("A2", Duration.of(2), pA), mkNote("B1", Duration.of(2), pB))),
      ),
    )

    val result = PartwiseScore.partwise(score)

    // 元のスコアの長さ 3 + 2 = 5
    assert(score.duration == Duration.of(5))
    // 変換後も全てのパートが長さ5を持っているか
    result.elems.foreach { m =>
      assert(m.duration == Duration.of(5))
    }
  }

}

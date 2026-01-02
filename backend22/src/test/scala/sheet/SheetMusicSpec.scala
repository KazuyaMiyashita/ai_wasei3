package sheet

import model.containers.Note
import model.elements.{Duration, Key, Math, Part, Pitch}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SheetMusicSpec extends AnyFunSpec with Matchers {

  describe("SheetMusic Structures") {
    it("should create Measure with Melody") {
      val note: Note[AttributedValue, Unit] =
        Note(AttributedValue(Pitch.parse("C4"), None), Duration.of(1), Part.Root, ())
      val measure = Measure.of(note)
      measure.duration shouldBe Duration.of(1)
      measure.elements shouldBe List(note)
    }

    it("should calculate TimeSignature duration") {
      val ts = TimeSignature(4, Duration.of(1)) // 4/4
      ts.duration shouldBe Math.Rational(4) // 4 * 1 = 4
      ts.toString shouldBe "4/4"

      val ts2 = TimeSignature(3, Duration.of(1)) // 3/4
      ts2.duration shouldBe Math.Rational(3)
      ts2.toString shouldBe "3/4"

      val ts3 = TimeSignature(6, Duration.of(1, 2)) // 6/8
      ts3.duration shouldBe Math.Rational(3) // 6 * 1/2 = 3
      ts3.toString shouldBe "6/8"
    }
    it("should create PartMapScore and FullScore") {
      val Soprano = Part.of("Soprano")
      val Alto    = Part.of("Alto")

      val score = PartMapScore(
        Map(
          Soprano -> List(Measure.of(Note(AttributedValue(Pitch.parse("C4"), None), Duration.of(1), Soprano, ()))),
          Alto    -> List(Measure.of(Note(AttributedValue(Pitch.parse("F3"), None), Duration.of(1), Alto, ()))),
        ),
      )
      score.numMeasures shouldBe 1

      val sheetMusic = SheetMusic(
        Key(Pitch.NoteName.parse("C"), Key.Mode.Major),
        TimeSignature(4, Duration.of(1, 4)),
        timeSignatureEvents = Nil,
        keySignatureEvents = Nil,
        score,
      )

      // score.notes.head ではなく、elements の中身を見るように修正
      // elements は List[Score[AttributedValue]] なので、型チェックなどが必要
      val element = sheetMusic.body.parts(Soprano).head.elements.head
      element match {
        case n: Note[AttributedValue, Unit] => n.value.value shouldBe Pitch.parse("C4")
        case _                              => fail("First element should be a Note")
      }
    }
  }
}

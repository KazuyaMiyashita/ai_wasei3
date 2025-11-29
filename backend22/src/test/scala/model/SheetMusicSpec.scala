package model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ScoreDataSpec extends AnyFunSpec with Matchers {
  import Key.Mode.*

  describe("ScoreData Structures") {
    it("should create Measure with Melody") {
      val note: Note[Pitch | Rest, Option[ScoreAttrs]] = Note(Pitch.parse("C4"), Duration.of(1), None)
      val measure                                      = Measure.of(note)
      measure.duration shouldBe Duration.of(1)
      measure.notes shouldBe List(note)
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
      val note    = Note[Pitch | Rest, Option[ScoreAttrs]](Pitch.parse("C4"), Duration.of(1), None)
      val measure = Measure.of(note)

      val parts = Map(
        PartId.Soprano -> List(measure),
        PartId.Alto    -> List(measure),
      )

      val score = PartMapScore(parts)
      score.numMeasures shouldBe 1

      val sheetMusic = SheetMusic(
        Key(Pitch.NoteName.parse("C"), Major),
        TimeSignature(4, Duration.of(1, 4)),
        score,
      )

      sheetMusic.body.parts(PartId.Soprano).head.notes.head.value shouldBe Pitch.parse("C4")
    }
  }
}

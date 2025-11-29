package model

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import model.Pitch.NoteName

class DegreeSpec extends AnyFunSpec with Matchers {
  import Key.Mode._

  describe("Degree") {
    it("should calculate correct degree in C Major") {
      val cMajor = Key(NoteName.parse("C"), Major)

      // C (Tonic) -> I (Step 0, Alter 0)
      val c   = NoteName.parse("C")
      val d_c = Degree.fromNoteNameKey(c, cMajor)
      d_c.step.value shouldBe 0
      d_c.alter.value shouldBe 0
      d_c.noteName(cMajor) shouldBe c

      // F (Subdominant) -> IV (Step 3, Alter 0)
      val f   = NoteName.parse("F")
      val d_f = Degree.fromNoteNameKey(f, cMajor)
      d_f.step.value shouldBe 3
      d_f.alter.value shouldBe 0
      d_f.noteName(cMajor) shouldBe f

      // F# -> IV# (Step 3, Alter 1)
      val fs   = NoteName.parse("F#")
      val d_fs = Degree.fromNoteNameKey(fs, cMajor)
      d_fs.step.value shouldBe 3
      d_fs.alter.value shouldBe 1
      d_fs.noteName(cMajor) shouldBe fs
    }

    it("should calculate correct degree in G Major (F# is diatonic)") {
      val gMajor = Key(NoteName.parse("G"), Major)

      // G (Tonic) -> I
      val g = NoteName.parse("G")
      Degree.fromNoteNameKey(g, gMajor).step.value shouldBe 0

      // F# (Leading tone) -> VII (Step 6, Alter 0)
      val fs   = NoteName.parse("F#")
      val d_fs = Degree.fromNoteNameKey(fs, gMajor)
      d_fs.step.value shouldBe 6
      d_fs.alter.value shouldBe 0
      d_fs.noteName(gMajor) shouldBe fs

      // F (Natural) -> VIIb (Step 6, Alter -1)
      val f   = NoteName.parse("F")
      val d_f = Degree.fromNoteNameKey(f, gMajor)
      d_f.step.value shouldBe 6
      d_f.alter.value shouldBe -1
      d_f.noteName(gMajor) shouldBe f
    }

    it("should calculate correct degree in A Minor") {
      val aMinor = Key(NoteName.parse("A"), Minor)

      // A (Tonic) -> I
      val a = NoteName.parse("A")
      Degree.fromNoteNameKey(a, aMinor).step.value shouldBe 0

      // G (7th) -> VII (Step 6, Alter 0) -- Natural Minor scale
      val g   = NoteName.parse("G")
      val d_g = Degree.fromNoteNameKey(g, aMinor)
      d_g.step.value shouldBe 6
      d_g.alter.value shouldBe 0

      // G# (Leading tone in Harmonic Minor) -> VII# (Step 6, Alter 1)
      val gs   = NoteName.parse("G#")
      val d_gs = Degree.fromNoteNameKey(gs, aMinor)
      d_gs.step.value shouldBe 6
      d_gs.alter.value shouldBe 1
    }
  }
}

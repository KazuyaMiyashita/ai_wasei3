package model.elements

import model.elements.{InternationalPitch, Pitch}
import org.scalatest.funsuite.AnyFunSuite

class ModelSpec extends AnyFunSuite {
  test("Pitch parsing and toString") {
    val pitch = Pitch.parse("C4")
    assert(pitch.toString == "C4")

    val pitch2 = Pitch.parse("A#3")
    assert(pitch2.toString == "A#3")

    val pitch3 = Pitch.parse("Eb5")
    assert(pitch3.toString == "Eb5")
  }

  test("NoteName parsing and toString") {
    val note = Pitch.NoteName.parse("C")
    assert(note.toString == "C")

    val note2 = Pitch.NoteName.parse("F#")
    assert(note2.toString == "F#")

    val note3 = Pitch.NoteName.parse("Bb")
    assert(note3.toString == "Bb")
  }

  test("InternationalPitch round trip") {
    val p  = Pitch.parse("C4")
    val ip = InternationalPitch.fromPitch(p)
    assert(ip.step == InternationalPitch.Step.C)
    assert(ip.octave.value == 4)
    assert(ip.alter.value == 0)

    val p2 = ip.toPitch
    assert(p2 == p)
  }
}

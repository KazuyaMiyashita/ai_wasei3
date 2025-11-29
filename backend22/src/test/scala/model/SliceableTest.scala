package model

import org.scalatest.funsuite.AnyFunSuite
import model._

class SliceableTest extends AnyFunSuite {

  test("Sliceable[Note] should slice a note correctly") {
    val note   = Note("C", Duration.of(4, 1), ())
    val offset = Offset.of(3, 1)

    val (slice1, slice2Opt) = implicitly[Sliceable[Note[String, Unit]]].slice(note, offset, isContinued = false)

    assert(slice1.value.duration == Duration.of(3, 1))
    assert(slice1.leftConnected == false)
    assert(slice1.rightConnected == true)

    assert(slice2Opt.isDefined)
    val slice2 = slice2Opt.get
    assert(slice2.duration == Duration.of(1, 1))
  }

  test("Sliceable[Note] should not slice if offset is ge duration") {

    val note = Note("C", Duration.of(4, 1), ())

    val offset = Offset.of(4, 1)

    val (slice1, slice2Opt) = implicitly[Sliceable[Note[String, Unit]]].slice(note, offset, isContinued = false)

    assert(slice1.value.duration == Duration.of(4, 1))

    assert(slice2Opt.isEmpty)

  }

  implicit val stringHasDuration: HasDuration[String] = new HasDuration[String] {

    def getDuration(a: String): Duration = Duration.of(1) // Dummy

  }

  implicit val stringSliceable: Sliceable[String] = new Sliceable[String] {

    def slice(target: String, offset: Offset, isContinued: Boolean): (Slice[String], Option[String]) = {

      // Dummy implementation for String slicing: just return same string

      (Slice(target, isContinued, false), None)

    }

    def join(a: Slice[String], b: Slice[String]): Slice[String] =
      Slice(a.value + b.value, a.leftConnected, b.rightConnected)

    def canJoin(a: String, b: String): Boolean = true

  }

  test("Sliceable[Melody] should slice melody correctly") {

    // Setup

    // Melody: [Note(C, 2), Note(D, 2), Note(E, 2)]

    // Total: 6

    // Slice at 3.0 -> Should split Note(D) in half.

    val n1 = Note("C", Duration.of(2), ())

    val n2 = Note("D", Duration.of(2), ())

    val n3 = Note("E", Duration.of(2), ())

    val melody = Melody.of(n1, n2, n3)

    val offset = Offset.of(3)

    val (s1, s2Opt) = implicitly[Sliceable[Melody[Note[String, Unit]]]].slice(melody, offset, isContinued = false)

    // Check Head Slice (0-3)

    // Should contain: Note(C, 2), Note(D, 1)

    val m1 = s1.value

    assert(m1.elems.size == 2)

    assert(m1.elems(0).value == "C")

    assert(m1.elems(0).duration == Duration.of(2))

    assert(m1.elems(1).value == "D")

    assert(m1.elems(1).duration == Duration.of(1))

    assert(s1.rightConnected == true) // Split happened mid-note

    // Check Tail Slice (3-6)

    // Should contain: Note(D, 1), Note(E, 2)

    assert(s2Opt.isDefined)

    val m2 = s2Opt.get

    assert(m2.elems.size == 2)

    assert(m2.elems(0).value == "D")

    assert(m2.elems(0).duration == Duration.of(1))

    assert(m2.elems(1).value == "E")

    assert(m2.elems(1).duration == Duration.of(2))

  }

}

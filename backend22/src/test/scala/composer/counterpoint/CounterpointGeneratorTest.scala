package composer.counterpoint

import composer.counterpoint.CounterpointGenerator
import composer.counterpoint.model.Species
import _root_.model.elements.{Key, Pitch, Part}
import org.scalatest.funsuite.AnyFunSuite

class CounterpointGeneratorTest extends AnyFunSuite {

  test("counterpoint generator has at least one output") {

    val gen = CounterpointGenerator(
      cantusFirmus = "C4 A3 G3 E3 F3 A3 G3 E3 D3 C3 ".split(" ").map(Pitch.parse).toList,
      cfPart = Part.of("Bass"),
      key = Key.parse("C Major"),
      species = Species.FIFTH_SPECIES,
      part = Part.of("Tenor"),
    )

    val result = gen.generateScores.next

    println(result)

  }

}

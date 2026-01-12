import composer.counterpoint.CounterpointGenerator
import composer.counterpoint.model.Species
import _root_.model.elements.{Key, Pitch}
import model.elements.Part
import sheet.MeiScore
import scala.xml.PrettyPrinter

object Main {

  def main(args: Array[String]): Unit = {
    genCounterPoint()
  }

  def genCounterPoint(): Unit = {
    val gen = CounterpointGenerator(
      cantusFirmus = "C4 A3 G3 E3 F3 A3 G3 E3 D3 C3".split(" ").map(Pitch.parse).toList,
      cfPart = Part.of("Bass"),
      key = Key.parse("C Major"),
      species = Species.FIFTH_SPECIES,
      part = Part.of("Tenor"),
    )

    val printer = new PrettyPrinter(Int.MaxValue, 2)
    gen.generateScores.take(1).zipWithIndex.foreach { case (sheet, _i) =>
      val index = _i + 1
      println(s"試行: $index:")
      val meiXml    = MeiScore.fromSheetMusic(sheet.copy(title = Some(s"generated $index")))
      val resultStr = s"<?xml version='1.0' encoding='UTF-8'?>\n${printer.format(meiXml)}"
      println(resultStr)
      println("\n\n")
    }
  }

}

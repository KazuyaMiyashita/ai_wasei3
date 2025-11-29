package parser.musicxml

import org.scalatest.funsuite.AnyFunSuite
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MusicXMLParserTest extends AnyFunSuite {
  test("parse 268.musicxml") {
    val resource = getClass.getResource("/data/268.musicxml")
    assert(resource != null, "File not found in resources")

    val path       = URLDecoder.decode(resource.getPath, StandardCharsets.UTF_8.name())
    val sheetMusic = MusicXMLParser.parse(path)

    assert(sheetMusic.body.parts.nonEmpty)
    println(s"Parts found: ${sheetMusic.body.parts.keys}")
    println(s"Key: ${sheetMusic.key}")
    println(s"Time Signature: ${sheetMusic.timeSignature}")

    // Check Soprano part presence (P1 is usually Soprano)
    val sopranoPart = sheetMusic.body.parts.get(model.PartId.Soprano)
    if (sopranoPart.isDefined) {
      val measures = sopranoPart.get
      println(s"Soprano measures: ${measures.length}")
      if (measures.nonEmpty) {
        val firstMeasure = measures.head
        println(s"First measure notes: ${firstMeasure.notes.length}")
        firstMeasure.notes.foreach { note =>
          println(s"  $note")
        }
      }
    } else {
      println("Soprano part not found, checking available parts...")
      sheetMusic.body.parts.keys.foreach(println)
    }
  }
}

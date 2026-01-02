package sheet

import org.scalatest.funsuite.AnyFunSuite
// import scala.xml.{PrettyPrinter, XML, TopScope}
import scala.xml.{PrettyPrinter, XML}
import sheet.meicmn.MeiXML
import sheet.meicmn.mei

class MeiScoreSpec extends AnyFunSuite {

  val printer = new PrettyPrinter(Int.MaxValue, 2)

  test("partwise 001.mei") {
    val resource = getClass.getResource("/data/mei/001.mei")
    assert(resource != null, "File not found in resources")

    val xml          = XML.load(resource)
    val meiStructure = MeiXML.load(xml)
    val meiScore     = MeiScore(meiStructure)
    assert(meiScore.toScore.elems.toList.map(_.part).size == 4)

    val partwise = meiScore.partwise
    assert(MeiScore(partwise).toScore.elems.toList.map(_.part).size == 4)

    val m1 = meiStructure.collect { case e: mei.cmn.Measure if e.attributes("n") == "1" => e }.next
    // println(printer.format(MeiXML.toXml(m1, TopScope)))
    // println()

    val m1p = partwise.collect { case e: mei.cmn.Measure if e.attributes("n") == "1" => e }.next
    // println(printer.format(MeiXML.toXml(m1p, TopScope)))

    assert(m1 == m1p)

    val scoreDef = meiStructure.collect { case e: mei.shared.ScoreDef => e }.next
    // println(printer.format(MeiXML.toXml(scoreDef, TopScope)))
    // println()

    val scoreDefP = partwise.collect { case e: mei.shared.ScoreDef => e }.next
    // println(printer.format(MeiXML.toXml(scoreDefP, TopScope)))

    assert(scoreDef == scoreDefP)

    assert(meiStructure.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))
    assert(partwise.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))

  }

  test("partwise 356.mei") {
    val resource = getClass.getResource("/data/mei/356.mei")
    assert(resource != null, "File not found in resources")

    val xml          = XML.load(resource)
    val meiStructure = MeiXML.load(xml)
    val meiScore     = MeiScore(meiStructure)
    assert(meiScore.toScore.elems.toList.map(_.part).size == 4)

    val partwise = meiScore.partwise
    assert(MeiScore(partwise).toScore.elems.toList.map(_.part).size == 4)

    val m1 = meiStructure.collect { case e: mei.cmn.Measure if e.attributes("n") == "1" => e }.next
    // println(printer.format(MeiXML.toXml(m1, TopScope)))
    // println()

    val m1p = partwise.collect { case e: mei.cmn.Measure if e.attributes("n") == "1" => e }.next
    // println(printer.format(MeiXML.toXml(m1p, TopScope)))

    assert(m1 == m1p)

    assert(meiStructure.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))
    assert(partwise.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))

  }

  test("partwise 357.mei") {
    val resource = getClass.getResource("/data/mei/357.mei")
    assert(resource != null, "File not found in resources")

    val xml          = XML.load(resource)
    val meiStructure = MeiXML.load(xml)
    val meiScore     = MeiScore(meiStructure)
    assert(meiScore.toScore.elems.toList.map(_.part).size == 4)

    val partwise = meiScore.partwise
    assert(MeiScore(partwise).toScore.elems.toList.map(_.part).size == 6)

    val staffDefs = meiStructure.collect { case e: mei.shared.StaffDef => e }.map(_.attributes.get("xml:id")).toList
    assert(staffDefs.size == 4)

    val ms = meiStructure
      .collect { case e: mei.cmn.Measure => e }
      .map(_.collect { case e: mei.shared.Staff => e }.size)
      .toList
    assert(ms.forall(_ == 4))

    // val scoreDefP = partwise.collect { case e: mei.shared.ScoreDef => e }.next
    // println(printer.format(MeiXML.toXml(scoreDefP, TopScope)))

    val staffDefsP = partwise.collect { case e: mei.shared.StaffDef => e }.toList
    assert(staffDefsP.size == 6)
    val staffDefsP2 =
      partwise.collect { case e: mei.shared.StaffDef => e }.map(_.attributes.get("xml:id")).distinct.toList
    assert(staffDefsP2.size == 6)

    val ms2 = partwise
      .collect { case e: mei.cmn.Measure => e }
      .map(_.collect { case e: mei.shared.Staff => e }.size)
      .toList
    assert(ms2.forall(_ == 6))

    assert(meiStructure.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).exists(_.isEmpty))
    assert(partwise.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))

  }

  test("partwise 358.mei") {
    val resource = getClass.getResource("/data/mei/358.mei")
    assert(resource != null, "File not found in resources")

    val xml          = XML.load(resource)
    val meiStructure = MeiXML.load(xml)
    val meiScore     = MeiScore(meiStructure)
    assert(meiScore.toScore.elems.toList.map(_.part).size == 4)

    val partwise = meiScore.partwise
    assert(MeiScore(partwise).toScore.elems.toList.map(_.part).size == 5)

    assert(meiStructure.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).exists(_.isEmpty))
    assert(partwise.collect { case e: mei.shared.Note => e }.map(_.attributes.get("dur")).forall(_.isDefined))

  }

}

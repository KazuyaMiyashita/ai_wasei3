import java.net.InetAddress

import scala.xml.{PrettyPrinter, XML}

import com.illposed.osc.transport.OSCPortOut
import model.containers.{Score, PartwiseScore}
import model.elements.Part
import performer.{OscSender, Performer, PerformerEvent}
import sheet.{MeiScore, NoteInfo}
import sheet.meicmn.MeiXML

object Main {

  val printer = new PrettyPrinter(120, 2)

  def main(args: Array[String]): Unit = {
    val resourcePath = s"/data/mei/356.mei"
    val resource     = getClass.getResource(resourcePath)

    if (resource == null) {
      System.err.println(s"Error: Resource not found: $resourcePath")
      return
    }

    println(s"Loading MEI file from: $resource")
    // Load from XML file
    val xml          = XML.load(resource)
    val meiStructure = MeiXML.load(xml)
    val meiScore     = MeiScore(meiStructure)

    val tempo = meiScore.tempo.getOrElse(80.0)

    val score: Score[NoteInfo, Unit] = meiScore.toScore

    val partwizeScore: PartwiseScore[NoteInfo, Unit] = score.partwise

    val soprano = partwizeScore.elems.find(e => e.part == Part("Soprano" :: Nil))
    println("Soprano:")
    soprano.take(10).foreach(println)
    println("...")

    // イベント生成
    val events = Performer.perform(partwizeScore, tempo)
    println(s"生成されたイベント数: ${events.length}")
    events.take(10).foreach(println)
    println("...")
    sendOsc(events)
  }

  def sendOsc(events: List[PerformerEvent]): Unit = {
    val ip   = InetAddress.getByName("127.0.0.1")
    val port = 8000

    val sender = new OSCPortOut(ip, port)

    try {
      println(s"[送信開始] $ip:$port へイベントを生成中...")

      // 送信処理
      OscSender.sendEvents(sender, events)

      println("[完了]")
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      sender.close()
    }
  }

}

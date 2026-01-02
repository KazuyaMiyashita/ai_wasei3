package performer

import com.illposed.osc.transport.OSCPortOut
import com.illposed.osc.{OSCBundle, OSCMessage, OSCPacket}

object OscSender {

  // イベントデータを管理する型エイリアス: (ミリ秒, アドレス, 引数リスト)
  type OscEvent = (Double, String, List[Any])

  def sendEvents(sender: OSCPortOut, events: List[PerformerEvent]): Unit = {
    // 1. バンドル送信 (50件ずつ)
    val bundleSize = 50

    events.grouped(bundleSize).foreach { batch =>
      val bundlePackets = new java.util.ArrayList[OSCPacket]()

      batch.foreach { case e =>
        // ここで直接 JavaOSC の OSCMessage オブジェクトを生成
        bundlePackets.add(createJavaOscMessage(e.address, e.args))
      }

      val bundle = new OSCBundle(bundlePackets)
      sender.send(bundle)

      // 必要であればここで Thread.sleep などで送信ペースを調整
    }
  }

  /** Scalaのリストデータを JavaOSC のメッセージオブジェクトに変換するヘルパーメソッド
    */
  def createJavaOscMessage(address: String, args: List[Any]): OSCMessage = {
    val javaArgs = new java.util.ArrayList[AnyRef]()

    args.foreach {
      case i: Int => javaArgs.add(Integer.valueOf(i))
      // Doubleの値を送信すると 0.0 になってしまう。
      case d: Double => javaArgs.add(java.lang.Float.valueOf(d.toFloat))
      case f: Float  => javaArgs.add(java.lang.Float.valueOf(f))
      case s: String => javaArgs.add(s)
      case other     => javaArgs.add(other.toString)
    }

    new OSCMessage(address, javaArgs)
  }
}

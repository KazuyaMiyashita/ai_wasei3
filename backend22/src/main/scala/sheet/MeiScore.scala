package sheet

import model.containers.{Score, Melody, Chord, Note}
import model.elements.Math.Rational
import model.elements.{Duration, InternationalPitch, Pitch, Rest, Part}
import sheet.meicmn.{Element, Text, mei}
import scala.xml.Elem
import model.elements.Key

case class NoteInfo(
    value: Pitch | Rest,
    id: String,
    isTieStarted: Boolean,
    isTieEnded: Boolean,
)

case class MeiScore(meiStructure: mei.shared.Mei) {

  import MeiScore.*

  /** 楽曲冒頭のテンポを取得する */
  def tempo: Option[Double] = {
    meiStructure
      .collect { case t: mei.shared.Tempo => t }
      .nextOption
      .flatMap(_.attributes.get("midi.bpm").map(_.toDouble))
  }

  private lazy val partMap: Map[String, Part] = {
    val staffDefs = meiStructure.collect { case s: mei.shared.StaffDef => s }
    staffDefs.map { s =>
      val n    = s.attributes("n")
      val name = (for {
        label <- s.children.collectFirst { case l: mei.shared.Label => l }
        text  <- label.children.collectFirst { case t: Text => t }
      } yield text.value).getOrElse(n)
      n -> Part.of(name)
    }.toMap
  }

  private lazy val scoreDef = meiStructure.collect { case s: mei.shared.ScoreDef => s }.nextOption()
  private lazy val meterSig = meiStructure.collect { case s: mei.cmn.MeterSig => s }.nextOption()

  private lazy val meterCount: Int = scoreDef
    .flatMap(_.attributes.get("meter.count"))
    .orElse(meterSig.flatMap(_.attributes.get("count")))
    .orElse(
      meiStructure.collect { case s: mei.shared.StaffDef => s }.nextOption().flatMap(_.attributes.get("meter.count")),
    )
    .map(_.toInt)
    .getOrElse(4)

  private lazy val meterUnit: Int = scoreDef
    .flatMap(_.attributes.get("meter.unit"))
    .orElse(meterSig.flatMap(_.attributes.get("unit")))
    .orElse(
      meiStructure.collect { case s: mei.shared.StaffDef => s }.nextOption().flatMap(_.attributes.get("meter.unit")),
    )
    .map(_.toInt)
    .getOrElse(4)

  private lazy val measureDuration: Duration = Duration.of(meterCount * 4, meterUnit)

  private def getDuration(attributes: Map[String, String]): Duration = {
    val dur   = attributes.get("dur").flatMap(_.toIntOption).getOrElse(4)
    val dots  = attributes.get("dots").flatMap(_.toIntOption).getOrElse(0)
    val denom = 1L << dots
    Duration.of(4, dur) * Rational((denom * 2) - 1, denom)
  }

  private def calculateDuration(e: Element): Duration = {
    e match {
      case n: mei.shared.Note  => getDuration(n.attributes)
      case r: mei.shared.Rest  => getDuration(r.attributes)
      case c: mei.shared.Chord => getDuration(c.attributes)
      case _: mei.cmn.MRest    => measureDuration
      case _: mei.cmn.MSpace   => measureDuration
      case other               =>
        val childDurs = other.children.map(calculateDuration)
        if (childDurs.nonEmpty) childDurs.reduce(_ + _) else Duration.of(0)
    }
  }

  def toScore: Chord[NoteInfo, Score[NoteInfo]] = {
    val measures = meiStructure.collect { case e: mei.cmn.Measure => e }.toList
    val staffs   = measures.flatMap { _.collect { case s: mei.shared.Staff => s } }.toList
    val ties     = getTies(meiStructure)

    val melodies = staffs
      .groupBy(_.attributes.get("n"))
      .collect { case (Some(part), staffs) => (partMap(part), staffs) }
      .map { case (part, staffs) => staffsToNotes(staffs, part, ties) }
      .toSeq

    val maxDuration = melodies.map(_.duration).maxOption.getOrElse(Duration.of(0))

    val adjustedMelodies = melodies.map { m =>
      if (m.duration < maxDuration) {
        val paddingInfo = NoteInfo(Rest, java.util.UUID.randomUUID.toString, isTieStarted = false, isTieEnded = false)
        val padding     = Note(paddingInfo, maxDuration - m.duration, m.part)
        Melody(List(m, padding))
      } else m
    }.toSet

    Chord(adjustedMelodies)
  }

  private def getTies(meiStructure: Element): TiesIndex = {
    val seq = meiStructure
      .collect { case t: mei.cmn.Tie => t }
      .map { t =>
        val startId = t.attributes("startid").drop(1)
        val endId   = t.attributes("endid").drop(1)
        Tie(startId, endId)
      }
      .toSeq
    TiesIndex(seq)
  }

  /** 同じパートのStaffの一覧からNoteの一覧を得る */
  private def staffsToNotes(
      staffs: Seq[mei.shared.Staff],
      part: Part,
      ties: TiesIndex,
  ): Score[NoteInfo] = {

    def getPitch(n: mei.shared.Note): Pitch = {
      val octave = InternationalPitch.Octave(n.attributes.get("oct").flatMap(_.toIntOption).getOrElse(4))
      val step   = n.attributes.get("pname") match {
        case Some("a") => InternationalPitch.Step.A
        case Some("b") => InternationalPitch.Step.B
        case Some("c") => InternationalPitch.Step.C
        case Some("d") => InternationalPitch.Step.D
        case Some("e") => InternationalPitch.Step.E
        case Some("f") => InternationalPitch.Step.F
        case Some("g") => InternationalPitch.Step.G
        case other     => throw new IllegalArgumentException(s"unknown pname: $other")
      }
      val alter = n.attributes
        .get("accid.ges")
        .orElse(
          n.children.collectFirst { case a: mei.shared.Accid => a }.map(_.attributes("accid")),
        ) match {
        case None | Some("n")       => InternationalPitch.Alter(0)
        case Some("s")              => InternationalPitch.Alter(1)
        case Some("x") | Some("ss") => InternationalPitch.Alter(2)
        case Some("f")              => InternationalPitch.Alter(-1)
        case Some("ff")             => InternationalPitch.Alter(-2)
        case other                  => throw new IllegalArgumentException(s"unknown accid: $other; $n")
      }
      InternationalPitch(step, alter, octave).toPitch
    }

    val chordNoteIds = staffs
      .flatMap(_.collect { case c: mei.shared.Chord => c })
      .flatMap(_.elements.collect { case n: mei.shared.Note => n.attributes("xml:id") })
      .toSet

    def parseSafe(container: Element, currentPart: Part): List[Score[NoteInfo]] = {
      container.children.flatMap { child =>
        child match {
          case l: mei.shared.Layer =>
            val n       = l.attributes.getOrElse("n", "1")
            val subPart = if (n == "1") currentPart else currentPart.spawn(n)
            val notes   = parseSafe(l, subPart)
            if (notes.nonEmpty) Some(Melody(notes))
            else {
              val info = NoteInfo(
                Rest,
                l.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
                isTieStarted = false,
                isTieEnded = false,
              )
              Some(Melody(List(Note(info, measureDuration, subPart))))
            }
          case c: mei.shared.Chord =>
            val duration = getDuration(c.attributes)
            val notes    = c.children.zipWithIndex.flatMap {
              case (n: mei.shared.Note, i) =>
                val subPart      = if (i == 0) currentPart else currentPart.spawn((i + 1).toString)
                val id           = n.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString)
                val isTieStarted = ties.getByStartId(id).isDefined
                val isTieEnded   = ties.getByEndId(id).isDefined
                val info         = NoteInfo(getPitch(n), id, isTieStarted = isTieStarted, isTieEnded = isTieEnded)
                Some(Note(info, duration, subPart))
              case _ => None
            }.toSet
            if (notes.nonEmpty) Some(Chord(notes))
            else {
              val info = NoteInfo(
                Rest,
                c.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
                isTieStarted = false,
                isTieEnded = false,
              )
              Some(Note(info, duration, currentPart))
            }
          case n: mei.shared.Note if !chordNoteIds(n.attributes.getOrElse("xml:id", "")) =>
            val id           = n.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString)
            val isTieStarted = ties.getByStartId(id).isDefined
            val isTieEnded   = ties.getByEndId(id).isDefined
            val info         = NoteInfo(getPitch(n), id, isTieStarted = isTieStarted, isTieEnded = isTieEnded)
            Some(Note(info, getDuration(n.attributes), currentPart))
          case r: mei.shared.Rest =>
            val info = NoteInfo(
              Rest,
              r.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(Note(info, getDuration(r.attributes), currentPart))
          case mr: mei.cmn.MRest =>
            val info = NoteInfo(
              Rest,
              mr.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(Note(info, measureDuration, currentPart))
          case ms: mei.cmn.MSpace =>
            val info = NoteInfo(
              Rest,
              ms.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(Note(info, measureDuration, currentPart))
          case other =>
            val dur = calculateDuration(other)
            if (dur > Duration.of(0)) {
              val subNotes = parseSafe(other, currentPart)
              if (subNotes.nonEmpty) Some(Melody(subNotes)) else None
            } else None
        }
      }
    }

    val scores = staffs.flatMap { staff =>
      val elems = parseSafe(staff, part)
      if (elems.isEmpty) {
        val info = NoteInfo(
          Rest,
          staff.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
          isTieStarted = false,
          isTieEnded = false,
        )
        List(Note(info, measureDuration, part))
      } else if (staff.children.exists(_.isInstanceOf[mei.shared.Layer])) {
        val layerMelodies = elems.collect { case s: Melody[_, _] => s.asInstanceOf[Score[NoteInfo]] }
        if (layerMelodies.nonEmpty) List(Chord(layerMelodies.toSet)) else elems
      } else {
        elems
      }
    }
    Melody(scores.toList)
  }

}

object MeiScore {
  case class Tie(startId: String, endId: String)
  case class TiesIndex(ties: Seq[Tie]) {
    private val byStart                       = ties.map(t => t.startId -> t).toMap
    private val byEnd                         = ties.map(t => t.endId -> t).toMap
    def getByStartId(id: String): Option[Tie] = byStart.get(id)
    def getByEndId(id: String): Option[Tie]   = byEnd.get(id)
  }

  def assignPartClef(part: Part): Elem = {
    val partTop = part.hierarchy.headOption
    partTop match {
      case Some(v) if v.contains("Soprano") => <clef shape="C" line="1"/>
      case Some(v) if v.contains("Alto")    => <clef shape="C" line="3"/>
      case Some(v) if v.contains("Tenor")   => <clef shape="C" line="4"/>
      case Some(v) if v.contains("Bass")    => <clef shape="F" line="4"/>
      case _                                => <clef shape="G" line="2"/>
    }
  }

  def toKeySig(key: Key): Elem = {
    val mode = key.mode match {
      case Key.Mode.Major => "major"
      case Key.Mode.Minor => "minor"
    }
    val sig = s"${key.signatureNum}" // 2f などの表記もあるが、-2などもOKらしい
    <keySig mode={mode} sig={sig}/>
  }

  def toMeterSig(timeSignature: TimeSignature): Elem = {
    val count = timeSignature.beats.toString
    val unit  = if (timeSignature.beatType.value.d == 1) {
      timeSignature.beatType.value.n.toString
    } else {
      throw new RuntimeException(s"cannot convert ${timeSignature} to meterSig. denominator of beatType is not 1.")
    }
    <meterSig count={count} unit={unit}/>
  }

  def toNote(note: Note[Pitch | Rest], key: Key, part: Part): Elem = {
    // note.duration は 四分音符が1, 二分音符が2
    // durは四分音符は4, 二分音符は2, ..., 付点は <note dots="1" ...>,  <note dots="2" ...>, ...
    val (durStr, dots) = note.duration.value match {
      case r @ Rational(num, den) =>
        // 1. 分子から2のべき乗を追い出して奇数部分を取り出す
        var oddPart    = num
        var powerOfTwo = 0
        while (oddPart > 0 && oddPart % 2 == 0) {
          oddPart /= 2
          powerOfTwo += 1
        }

        // 2. oddPart が 2^(k+1) - 1 の形かチェック
        // 1 -> k=0 (点なし), 3 -> k=1 (点1), 7 -> k=2 (点2), 15 -> k=3 (点3)
        val k = (Math.log(oddPart + 1) / Math.log(2)).toInt
        if ((1L << k) - 1 != oddPart) {
          throw new RuntimeException(s"Unsupported duration (not a standard dotted note): $r")
        }

        val numDots = k - 1

        // 3. 基底となる音符の長さを計算 (付点分を取り除く)
        // baseDuration = r / (1 + 1/2 + 1/4 + ...)
        // 1.5倍なら 3/2 で割る、1.75倍なら 7/4 で割る
        val dotMultiplier = Rational((1L << k) - 1, 1L << (k - 1))
        val baseDuration  = r / dotMultiplier

        // 4. MEIの文字列にマッピング
        val typeStr = baseDuration match {
          case Rational(16, 1) => "long"
          case Rational(8, 1)  => "breve"
          case Rational(n, 1)  => (4 / n).toString // 4 -> "1", 2 -> "2", 1 -> "4"
          case Rational(1, d)  => (4 * d).toString // 1/2 -> "8", 1/4 -> "16", 1/512 -> "2048"
          case other           => throw new RuntimeException(s"Illegal base duration: $other")
        }

        (typeStr, numDots)
    }

    note.value match {
      case _: Rest  => <rest dur={durStr} dots={if (dots > 0) dots.toString else null}/> // mRestの可能性
      case p: Pitch =>
        val ip: InternationalPitch = p.internationalPitchNotation
        val oct                    = ip.octave.value.toString
        val pname                  = ip.step match {
          case InternationalPitch.Step.C => "c"
          case InternationalPitch.Step.D => "d"
          case InternationalPitch.Step.E => "e"
          case InternationalPitch.Step.F => "f"
          case InternationalPitch.Step.G => "g"
          case InternationalPitch.Step.A => "a"
          case InternationalPitch.Step.B => "b"
        }

        val stemDir = "down" // TODO: partとclefから得る
        <note dur={durStr} dots={if (dots > 0) dots.toString else null} oct={oct} pname={pname} stem.dir={stemDir}/>
      // pnameには臨時記号の情報は一切ない
      // 調号に応じて、実際なる音にシャープなどあれば accid.ges="s" を付与
      // 臨時記号が必要な場合は
      // <note ...>
      //   <accid accid="s"/>
      // </note>
    }

  }

  def fromSheetMusic(
      sheetMusic: SheetMusic,
  ): Elem = {

    val val_title = sheetMusic.title.getOrElse("Empty Title")

    val staffDefs = sheetMusic.body.parts
      .map(_._1)
      .zipWithIndex
      .map { case (part, _i) =>
        val n     = (_i + 1).toString
        val label = part.toString

        <staffDef n={n} lines="5">
        <label>{label}</label>
        {assignPartClef(part)}
        {toKeySig(sheetMusic.key)}
        {toMeterSig(sheetMusic.timeSignature)}
      </staffDef>
      }
      .toList

    val sheetMeasures = sheetMusic.body.toMeasures
    val measuresLen   = sheetMeasures.length

    val measures = sheetMeasures.zipWithIndex.map { case (measure, _mn) =>
      val isLastMeasure = measuresLen == _mn + 1
      val measureNumber = (_mn + 1).toString
      // 最後の小節は <measure right="dbl">
      <measure n={measureNumber} right={if (isLastMeasure) "dbl" else null}>
        {
        measure.toSeq.zipWithIndex.map { case ((part, pMeasure), _pn) =>
          val staffNumber = (_pn + 1).toString
          <staff n={staffNumber}>
            <layer n="1">
              {
            pMeasure.elements.map {
              case n: Note[AttributedValue] => toNote(n.mapValue(_.value), sheetMusic.key, part)
              case other => throw new RuntimeException(s"not supported chord or melody in measure: $other")
            }
          }
            </layer>
          </staff>
        }
      }
      </measure>

    }

    val meiElem = <mei meiversion="5.1" xmlns="http://www.music-encoding.org/ns/mei">
  <meiHead>
    <titleStmt>
      <title>{val_title}</title>
    </titleStmt>
  </meiHead>
  <music>
    <body>
      <mdiv>
        <score>
          <scoreDef>
            <staffGrp>
              <staffGrp bar.thru="true">
                <grpSym symbol="bracket"/>
                {staffDefs}
              </staffGrp>
            </staffGrp>
          </scoreDef>
          <section>
            {measures}
          </section>
        </score>
      </mdiv>
    </body>
  </music>
</mei>

    MeiXmlUtil.assignIds(meiElem)
  }

}

package sheet

import model.containers.{Score, Melody, Chord}
import model.containers.Score.note
import model.elements.Math.Rational
import model.elements.{Duration, InternationalPitch, Pitch, Rest, Part}
import sheet.meicmn.{Element, Text, mei}
import scala.collection.immutable.SeqMap

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

  def toScore: Chord[NoteInfo, Unit, Score[NoteInfo, Unit]] = {
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
        val padding     = note(paddingInfo, maxDuration - m.duration, m.part)
        Melody(List(m, padding), ())
      } else m
    }.toSet

    Chord(adjustedMelodies, ())
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
  ): Score[NoteInfo, Unit] = {

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
        case None | Some("n") => InternationalPitch.Alter(0)
        case Some("s")        => InternationalPitch.Alter(1)
        case Some("x")        => InternationalPitch.Alter(2)
        case Some("f")        => InternationalPitch.Alter(-1)
        case Some("ff")       => InternationalPitch.Alter(-2)
        case other            => throw new IllegalArgumentException(s"unknown accid: $other; $n")
      }
      InternationalPitch(step, alter, octave).toPitch
    }

    val chordNoteIds = staffs
      .flatMap(_.collect { case c: mei.shared.Chord => c })
      .flatMap(_.elements.collect { case n: mei.shared.Note => n.attributes("xml:id") })
      .toSet

    def parseSafe(container: Element, currentPart: Part): List[Score[NoteInfo, Unit]] = {
      container.children.flatMap { child =>
        child match {
          case l: mei.shared.Layer =>
            val n       = l.attributes.getOrElse("n", "1")
            val subPart = if (n == "1") currentPart else currentPart.spawn(n)
            val notes   = parseSafe(l, subPart)
            if (notes.nonEmpty) Some(Melody(notes, ()))
            else {
              val info = NoteInfo(
                Rest,
                l.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
                isTieStarted = false,
                isTieEnded = false,
              )
              Some(Melody(List(note(info, measureDuration, subPart)), ()))
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
                Some(note(info, duration, subPart))
              case _ => None
            }.toSet
            if (notes.nonEmpty) Some(Chord(notes, ()))
            else {
              val info = NoteInfo(
                Rest,
                c.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
                isTieStarted = false,
                isTieEnded = false,
              )
              Some(note(info, duration, currentPart))
            }
          case n: mei.shared.Note if !chordNoteIds(n.attributes.getOrElse("xml:id", "")) =>
            val id           = n.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString)
            val isTieStarted = ties.getByStartId(id).isDefined
            val isTieEnded   = ties.getByEndId(id).isDefined
            val info         = NoteInfo(getPitch(n), id, isTieStarted = isTieStarted, isTieEnded = isTieEnded)
            Some(note(info, getDuration(n.attributes), currentPart))
          case r: mei.shared.Rest =>
            val info = NoteInfo(
              Rest,
              r.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(note(info, getDuration(r.attributes), currentPart))
          case mr: mei.cmn.MRest =>
            val info = NoteInfo(
              Rest,
              mr.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(note(info, measureDuration, currentPart))
          case ms: mei.cmn.MSpace =>
            val info = NoteInfo(
              Rest,
              ms.attributes.getOrElse("xml:id", java.util.UUID.randomUUID.toString),
              isTieStarted = false,
              isTieEnded = false,
            )
            Some(note(info, measureDuration, currentPart))
          case other =>
            val dur = calculateDuration(other)
            if (dur > Duration.of(0)) {
              val subNotes = parseSafe(other, currentPart)
              if (subNotes.nonEmpty) Some(Melody(subNotes, ())) else None
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
        List(note(info, measureDuration, part))
      } else if (staff.children.exists(_.isInstanceOf[mei.shared.Layer])) {
        val layerMelodies = elems.collect { case s: Melody[_, _, _] => s.asInstanceOf[Score[NoteInfo, Unit]] }
        if (layerMelodies.nonEmpty) List(Chord(layerMelodies.toSet, ())) else elems
      } else {
        elems
      }
    }
    Melody(scores.toList, ())
  }

  private def elementToScore(e: Element, part: Part): Option[Score[Element, Unit]] = {
    e match {
      case l: mei.shared.Layer =>
        val childrenScores = l.children.flatMap(c => elementToScore(c, part))
        if (childrenScores.nonEmpty) Some(Melody(childrenScores, ())) else None
      case c: mei.shared.Chord =>
        val dur        = getDuration(c.attributes)
        val chordAttrs = c.attributes.filter { case (k, _) => k == "dur" || k == "dots" }
        val notes      = c.children.zipWithIndex.flatMap {
          case (n: mei.shared.Note, i) =>
            val subPart = if (i == 0) part else part.spawn((i + 1).toString)
            val newNote = n.copy(attributes = chordAttrs ++ n.attributes)
            Some(note(newNote: Element, dur, subPart))
          case _ => None
        }
        if (notes.nonEmpty) Some(Chord[Element, Unit, Score[Element, Unit]](notes.toSet, ()))
        else Some(note(c: Element, dur, part))
      case n: mei.shared.Note =>
        val dur = getDuration(n.attributes)
        if (dur > Duration.of(0)) Some(note(n: Element, dur, part)) else None
      case r: mei.shared.Rest =>
        val dur = getDuration(r.attributes)
        if (dur > Duration.of(0)) Some(note(r: Element, dur, part)) else None
      case mr: mei.cmn.MRest =>
        Some(note(mr: Element, measureDuration, part))
      case ms: mei.cmn.MSpace =>
        Some(note(ms: Element, measureDuration, part))
      case other =>
        val dur = calculateDuration(other)
        if (dur > Duration.of(0)) {
          Some(note(other: Element, dur, part))
        } else None
    }
  }

  def partwise: mei.shared.Mei = {
    val partToN: Map[Part, String] = partMap.map(_.swap)

    def transformScore(score: mei.shared.Score): mei.shared.Score = {
      val scoreDef = score.elements.collectFirst { case e: mei.shared.ScoreDef => e }.head
      val section  = score.elements.collectFirst { case e: mei.shared.Section => e }.head
      val measures = section.collect { case e: mei.cmn.Measure => e }.toList

      if (measures.isEmpty) return score

      // 1. Collect all used parts across all measures to build the new ScoreDef
      //    We do this by simulating the partwise extraction for each measure.
      val measureContents: Map[Int, Map[Part, List[Element]]] = measures.zipWithIndex.map { case (measure, mIdx) =>
        val staffs      = measure.collect { case s: mei.shared.Staff => s }
        val staffScores = staffs.flatMap { staff =>
          val n    = staff.attributes("n")
          val part = partMap.getOrElse(n, Part.of(n))

          val layers      = staff.children.collect { case l: mei.shared.Layer => l }
          val layerScores = layers.zipWithIndex.flatMap { case (layer, idx) =>
            val layerN    = layer.attributes.getOrElse("n", (idx + 1).toString)
            val layerPart = if (layerN == "1") part else part.spawn(layerN)
            elementToScore(layer, layerPart)
          }.toList

          if (layerScores.nonEmpty) {

            val maxDur         = layerScores.map(_.duration).max
            val adjustedLayers = layerScores.map { ls =>
              if (ls.duration < maxDur) {
                val pad = note(mei.cmn.MSpace()(): Element, maxDur - ls.duration, ls.part)
                Melody(List(ls, pad), ())
              } else ls
            }
            Some(Chord[Element, Unit, Score[Element, Unit]](adjustedLayers.toSet, ()))
          } else None
        }.toList

        val measureContent: Map[Part, List[Element]] = if (staffScores.nonEmpty) {
          val maxDur         = staffScores.map(_.duration).max
          val adjustedStaffs = staffScores.map { ss =>
            if (ss.duration < maxDur) {
              val pad = note(mei.cmn.MSpace()(): Element, maxDur - ss.duration, ss.part)
              Melody[Element, Unit, Score[Element, Unit]](List(ss, pad), ())
            } else ss
          }

          val measureScore = Chord[Element, Unit, Score[Element, Unit]](adjustedStaffs.toSet, ())

          val pw = measureScore.partwise

          pw.elems.map { melody =>
            val part     = melody.part
            val elements = melody.elems.flatMap { n =>
              n.value match {
                case Some(e) => List(e)
                case None    => List(mei.cmn.MSpace()()) // Padding become MSpace
              }
            }
            part -> elements
          }.toMap
        } else Map.empty

        mIdx -> measureContent
      }.toMap

      val allParts = measureContents.values.flatMap(_.keys).toSet.toList.sortWith { (m1, m2) =>
        val n1Str = partToN.find { case (p, _) => p.isSupersetOf(m1) || p == m1 }.map(_._2).getOrElse("999")
        val n2Str = partToN.find { case (p, _) => p.isSupersetOf(m2) || p == m2 }.map(_._2).getOrElse("999")
        val n1    = n1Str.toIntOption.getOrElse(Int.MaxValue)
        val n2    = n2Str.toIntOption.getOrElse(Int.MaxValue)
        if (n1 != n2) n1 < n2
        else m1 < m2
      }

      val newPartToN = allParts.zipWithIndex.map { case (p, i) => p -> (i + 1).toString }.toMap

      // --- Rebuild ScoreDef (Logic reused) ---
      val originalStaffDefs    = scoreDef.collect { case s: mei.shared.StaffDef => s }.toList
      val parentPartToStaffDef = originalStaffDefs.flatMap { s =>
        s.attributes.get("n").map { n =>
          val part = partMap.getOrElse(n, Part.of(n))
          part -> s
        }
      }.toMap

      val newPartToStaffDef = allParts.map { part =>
        val parentPart  = parentPartToStaffDef.keys.find(p => p.isSupersetOf(part) || p == part).getOrElse(Part.Root)
        val originalDef = parentPartToStaffDef.getOrElse(parentPart, mei.shared.StaffDef(n = "1", lines = "5")())

        val originalId = originalDef.attributes.get("xml:id").orElse(originalDef.attributes.get("id"))
        val newId      =
          if (part == parentPart) originalId.getOrElse(java.util.UUID.randomUUID.toString)
          else java.util.UUID.randomUUID.toString

        val newAttrs = originalDef.attributes.map {
          case ("n", _)      => ("n", newPartToN(part))
          case ("xml:id", _) => ("xml:id", newId)
          case ("id", _)     => ("id", newId)
          case other         => other
        } ++ (if (!originalDef.attributes.contains("xml:id") && !originalDef.attributes.contains("id"))
                SeqMap("xml:id" -> newId)
              else SeqMap.empty)

        val newChildren = originalDef.children.map {
          case l: mei.shared.Label =>
            val labelWithSuffix = if (part.hierarchy.size > parentPart.hierarchy.size) {
              val suffix = part.hierarchy.drop(parentPart.hierarchy.size).mkString(" ")
              l.transform {
                case t: Text => Text(t.value + " " + suffix)
                case other   => other
              }.asInstanceOf[mei.shared.Label]
            } else l
            if (part == parentPart) labelWithSuffix
            else labelWithSuffix.copy(attributes = labelWithSuffix.attributes.removed("xml:id").removed("id"))
          case c: mei.shared.Clef =>
            if (part == parentPart) c
            else c.copy(attributes = c.attributes.removed("xml:id").removed("id"))
          case k: mei.shared.KeySig =>
            if (part == parentPart) k
            else k.copy(attributes = k.attributes.removed("xml:id").removed("id"))
          case m: mei.cmn.MeterSig =>
            if (part == parentPart) m
            else m.copy(attributes = m.attributes.removed("xml:id").removed("id"))
          case other => other
        }
        part -> originalDef.withChildren(newChildren).asInstanceOf[mei.shared.StaffDef].copy(attributes = newAttrs)
      }.toMap

      def transformScoreDefElements(elements: List[Element]): List[Element] = {
        elements.flatMap {
          case s: mei.shared.StaffGrp =>
            List(s.withChildren(transformScoreDefElements(s.children)))
          case s: mei.shared.StaffDef =>
            val n    = s.attributes("n")
            val part = partMap.getOrElse(n, Part.of(n))
            allParts.filter(p => part.isSupersetOf(p) || p == part).map(newPartToStaffDef)
          case other => List(other)
        }
      }

      val newScoreDef = score.elements
        .collectFirst { case e: mei.shared.ScoreDef => e }
        .headOption
        .getOrElse(mei.shared.ScoreDef()())
        .copy(elements = transformScoreDefElements(scoreDef.elements))

      // --- Rebuild Measures using the extracted content ---
      val newMeasures = measures.zipWithIndex.map { case (originalMeasure, mIdx) =>
        val partsContent  = measureContents.getOrElse(mIdx, Map.empty)
        val otherElements = originalMeasure.children.filterNot(_.isInstanceOf[mei.shared.Staff])

        val originalStaffs =
          originalMeasure.collect { case s: mei.shared.Staff => s }.map(s => s.attributes.getOrElse("n", "") -> s).toMap

        val newStaffs = allParts.map { part =>
          val contentElements = partsContent.getOrElse(part, Nil)
          val n               = newPartToN(part)

          val originalN     = partToN.find { case (p, _) => p.isSupersetOf(part) || p == part }.map(_._2).getOrElse("1")
          val originalStaff = originalStaffs.get(originalN)

          val staffId =
            if (part.hierarchy.size == 1 && originalStaff.isDefined)
              originalStaff.get.attributes
                .get("xml:id")
                .orElse(originalStaff.get.attributes.get("id"))
                .getOrElse(java.util.UUID.randomUUID.toString)
            else java.util.UUID.randomUUID.toString

          val baseAttrs  = originalStaff.map(_.attributes).getOrElse(SeqMap.empty)
          val staffAttrs = baseAttrs.map {
            case ("n", _)      => ("n", n)
            case ("xml:id", _) => ("xml:id", staffId)
            case ("id", _)     => ("id", staffId)
            case other         => other
          } ++ (if (!baseAttrs.contains("n")) SeqMap("n" -> n) else SeqMap.empty) ++
            (if (!baseAttrs.contains("xml:id") && !baseAttrs.contains("id")) SeqMap("xml:id" -> staffId)
             else SeqMap.empty)

          val layerElements = if (contentElements.nonEmpty) contentElements else List(mei.cmn.MSpace()())

          val originalLayer = originalStaff.flatMap(_.collect {
            case l: mei.shared.Layer if l.attributes.getOrElse("n", "1") == "1" => l
          }.nextOption())
          val layerId =
            if (part.hierarchy.size == 1 && originalLayer.isDefined)
              originalLayer.get.attributes
                .get("xml:id")
                .orElse(originalLayer.get.attributes.get("id"))
                .getOrElse(java.util.UUID.randomUUID.toString)
            else java.util.UUID.randomUUID.toString

          val baseLayerAttrs = originalLayer.map(_.attributes).getOrElse(SeqMap.empty)
          val layerAttrs     = baseLayerAttrs.map {
            case ("n", _)      => ("n", "1")
            case ("xml:id", _) => ("xml:id", layerId)
            case ("id", _)     => ("id", layerId)
            case other         => other
          } ++ (if (!baseLayerAttrs.contains("n")) SeqMap("n" -> "1") else SeqMap.empty) ++
            (if (!baseLayerAttrs.contains("xml:id") && !baseLayerAttrs.contains("id")) SeqMap("xml:id" -> layerId)
             else SeqMap.empty)

          val layer = mei.shared.Layer(elements = layerElements, attributes = layerAttrs)
          mei.shared.Staff(elements = List(layer), attributes = staffAttrs)
        }
        originalMeasure.withChildren(newStaffs ++ otherElements)
      }.toList

      val newSection = section.withChildren(newMeasures)
      score.withChildren(List(newScoreDef, newSection)).asInstanceOf[mei.shared.Score]
    }

    meiStructure
      .transform {
        case s: mei.shared.Score => transformScore(s)
        case other               => other
      }
      .asInstanceOf[mei.shared.Mei]
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
}

package parser.musicxml

import scala.xml.{Node, Elem}
import scala.xml.factory.XMLLoader
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import parser.musicxml.{
  Score => MxScore,
  Part => MxPart,
  Measure => MxMeasure,
  Note => MxNote,
  Attributes => MxAttributes,
  Key => MxKey,
  Pitch => MxPitch,
  Time => MxTime,
  Backup,
  Forward,
  Direction,
  DirectionWord,
  TimeModification,
}
import model._
import model.Math.Rational

import scala.collection.immutable.ListMap

object MusicXMLParser {

  def parse(inputFile: String): SheetMusic = {
    val rawScore = RawMusicXMLParser.parseToRawScore(inputFile)
    SheetMusicBuilder.buildSheetMusic(rawScore)
  }

  def parse(url: java.net.URL): SheetMusic = {
    val rawScore = RawMusicXMLParser.parseToRawScore(url)
    SheetMusicBuilder.buildSheetMusic(rawScore)
  }

}

object CustomXML extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    f.newSAXParser()
  }
}

object RawMusicXMLParser {

  def parseToRawScore(inputFile: String): MxScore = {
    val root = CustomXML.loadFile(inputFile)
    parseRoot(root)
  }

  def parseToRawScore(url: java.net.URL): MxScore = {
    val root = CustomXML.load(url)
    parseRoot(root)
  }

  private def parseRoot(root: Elem): MxScore = {

    val workTitle = (root \ "work" \ "work-title").text.trim
    val title     = if (workTitle.nonEmpty) workTitle else (root \\ "work-title").text.trim
    if (title.isEmpty) throw new Exception("missing work title")

    val parts = (root \ "part").map { partElem =>
      val id       = (partElem \ "@id").text
      val measures = parseMeasures(partElem)
      MxPart(id, measures.toList)
    }
    MxScore(title, parts.toList)
  }

  private def parseMeasures(partElem: Node): Seq[MxMeasure] = {
    (partElem \ "measure").flatMap { measureElem =>
      val numberStr = (measureElem \ "@number").text
      if (numberStr.isEmpty) None
      else {
        val number   = numberStr.toInt
        val elements = measureElem.child.flatMap { case elem =>
          elem.label match {
            case "note"   => Some(parseNote(elem))
            case "backup" =>
              val duration = (elem \ "duration").text.toIntOption
              duration.map(Backup.apply)
            case "forward" =>
              val duration = (elem \ "duration").text.toIntOption
              duration.map(Forward.apply)
            case "attributes" => Some(parseAttributes(elem))
            case "direction"  =>
              val words = (elem \ "words").headOption.map(_.text.trim)
              words.map(w => Direction(List(DirectionWord(w))))
            case _ => None
          }
        }
        Some(MxMeasure(number, elements.toList))
      }
    }
  }

  private def parseNote(noteElem: Node): MxNote = {
    val isRest = (noteElem \ "rest").nonEmpty
    val pitch  = if (!isRest) {
      val pitchElem = noteElem \ "pitch"
      if (pitchElem.nonEmpty) {
        val step   = (pitchElem \ "step").text
        val alter  = (pitchElem \ "alter").text.toIntOption.getOrElse(0)
        val octave = (pitchElem \ "octave").text.toIntOption.getOrElse(0)
        Some(MxPitch(step, alter, octave))
      } else None
    } else None

    val duration = (noteElem \ "duration").text.toIntOption.getOrElse(0)
    val voice    = (noteElem \ "voice").text.toIntOption.getOrElse(1)
    val isChord  = (noteElem \ "chord").nonEmpty
    val isGrace  = (noteElem \ "grace").nonEmpty

    val tieTypes = (noteElem \ "tie").flatMap(t => Option(t \ "@type").map(_.text)).toList

    val timeModification = (noteElem \ "time-modification").headOption.flatMap { case tmElem =>
      val actualNotes = (tmElem \ "actual-notes").text.toIntOption
      val normalNotes = (tmElem \ "normal-notes").text.toIntOption
      if (actualNotes.isDefined && normalNotes.isDefined) {
        val normalType = (tmElem \ "normal-type").text
        val normalDots = (tmElem \ "normal-dot").length
        Some(
          TimeModification(
            actualNotes.get,
            normalNotes.get,
            if (normalType.isEmpty) None else Some(normalType),
            normalDots,
          ),
        )
      } else None
    }

    MxNote(pitch, duration, voice, isChord, isGrace, tieTypes, timeModification)
  }

  private def parseAttributes(attrElem: Node): MxAttributes = {
    val divisions = (attrElem \ "divisions").text.toIntOption

    val key = (attrElem \ "key").headOption.flatMap { case keyElem =>
      val fifths = (keyElem \ "fifths").text.toIntOption
      val mode   = (keyElem \ "mode").text
      fifths.map(f => MxKey(f, if (mode.isEmpty) "major" else mode))
    }

    val time = (attrElem \ "time").headOption.flatMap { case timeElem =>
      val beats    = (timeElem \ "beats").text.toIntOption
      val beatType = (timeElem \ "beat-type").text.toIntOption
      if (beats.isDefined && beatType.isDefined) {
        Some(MxTime(beats.get, beatType.get))
      } else None
    }

    MxAttributes(divisions, key, time)
  }
}

object SheetMusicBuilder {

  def buildSheetMusic(rawScore: MxScore): SheetMusic = {
    val initialKey  = findInitialKey(rawScore)
    val initialTime = findInitialTime(rawScore)

    val partsMap = rawScore.parts.flatMap { case part =>
      val partIdOpt = part.id match {
        case "P1" => Some(PartId.Soprano)
        case "P2" => Some(PartId.Alto)
        case "P3" => Some(PartId.Tenor)
        case "P4" => Some(PartId.Bass)
        case _    => None
      }

      partIdOpt.map { case partId =>
        val measures = buildMeasures(part.measures)
        partId -> measures
      }
    }.toMap

    SheetMusic(initialKey, initialTime, PartMapScore(partsMap))
  }

  private def findInitialKey(rawScore: MxScore): Key = {
    rawScore.parts.iterator
      .flatMap(_.measures)
      .flatMap(_.elements)
      .collectFirst {
        case attr: MxAttributes if attr.key.isDefined =>
          val k        = attr.key.get
          val mode     = if (k.mode == "minor") Key.Mode.Minor else Key.Mode.Major
          val tonicVal = if (mode == Key.Mode.Major) k.fifths else k.fifths + 3
          Key(Pitch.NoteName(tonicVal), mode)
      }
      .getOrElse(Key(Pitch.NoteName(0), Key.Mode.Major))
  }

  private def findInitialTime(rawScore: MxScore): TimeSignature = {
    rawScore.parts.iterator
      .flatMap(_.measures)
      .flatMap(_.elements)
      .collectFirst {
        case attr: MxAttributes if attr.time.isDefined =>
          val t = attr.time.get
          TimeSignature(t.beats, Duration(Rational(4, t.beatType)))
      }
      .getOrElse(TimeSignature(4, Duration(Rational(1))))
  }

  private def buildMeasures(mxMeasures: List[MxMeasure]): List[model.Measure] = {
    var currentDivisions: Int = 1

    mxMeasures.map { case m =>
      val (notes, newDivs) = buildMeasureContent(m, currentDivisions)
      currentDivisions = newDivs
      model.Measure(Melody(notes))
    }
  }

  private def buildMeasureContent(
      m: MxMeasure,
      currentDivisions: Int,
  ): (List[model.Note[Pitch | Rest, Option[ScoreAttrs]]], Int) = {
    var divisions = currentDivisions

    var cursor = Rational(0)
    var notes  = ListMap.empty[Rational, model.Note[Pitch | Rest, Option[ScoreAttrs]]]

    m.elements.foreach {
      case attr: MxAttributes =>
        if (attr.divisions.isDefined) divisions = attr.divisions.get

      case note: MxNote =>
        if (note.voice == 1) {
          val durationVal = if (note.timeModification.isDefined) {
            Rational(note.duration, divisions)
          } else {
            Rational(note.duration, divisions)
          }

          val duration = Duration(durationVal)

          val pitchOrRest: Pitch | Rest = note.pitch match {
            case Some(p) =>
              val step   = InternationalPitch.Step.valueOf(p.step)
              val alter  = InternationalPitch.Alter(p.alter)
              val octave = InternationalPitch.Octave(p.octave)
              InternationalPitch(step, alter, octave).toPitch
            case None => Rest
          }

          val attr = if (note.tieTypes.contains("start")) Some(ScoreAttrs(true)) else None

          val newNote = model.Note(pitchOrRest, duration, attr)

          if (note.isChord) {
            // Ignore chord notes
          } else {
            notes = notes + (cursor -> newNote)
            cursor = cursor + durationVal
          }
        }

      case Backup(dur) =>
        cursor = cursor - Rational(dur, divisions)

      case Forward(dur) =>
        cursor = cursor + Rational(dur, divisions)

      case _ => // Ignore others
    }

    // Sort by offset to prepare for filling gaps
    // notes is ListMap, already insertion ordered if we only appended, but Backup/Forward changes logic.
    // So sorting is safer.

    var filledNotes = List.empty[model.Note[Pitch | Rest, Option[ScoreAttrs]]]
    var currentPos  = Rational(0)

    notes.toList.sortBy(_._1).foreach { case (offset, note) =>
      if (offset > currentPos) {
        val gap = offset - currentPos
        if (gap > Rational(0)) {
          filledNotes = filledNotes :+ model.Note(Rest, Duration(gap), None)
        }
      }
      filledNotes = filledNotes :+ note
      currentPos = offset + note.duration.value
    }

    (filledNotes, divisions)
  }
}

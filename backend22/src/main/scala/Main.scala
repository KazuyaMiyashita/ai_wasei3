import model._
import parser.musicxml.MusicXMLParser

object Main {

  val resourcePath = "/data/268.musicxml"

  def main(args: Array[String]): Unit = {

    val resource = getClass.getResource(resourcePath)

    if (resource == null) {
      System.err.println(s"Error: Resource not found: $resourcePath")
      sys.exit(1)
    }

    println(s"Loading MusicXML from: $resource")

    // 1. Parse to SheetMusic
    val sheetMusic = MusicXMLParser.parse(resource)
    println("Successfully parsed SheetMusic.")
    println(s"Key: ${sheetMusic.key}")
    println(s"Time Signature: ${sheetMusic.timeSignature}")

    // 2. Convert to Score
    println("=== Converted Score Structure ===")
    val score = toScore(sheetMusic)
    printPrettyScore(score)(partIdOrdering)

    // 4. Grid Transformation (Homophonic View)
    println()
    println("=== Grid (Homophonic View) ===")

    val gridScore = toGridScore(sheetMusic)(partIdOrdering)
    printPrettyScore(gridScore)(partIdOrdering)

    // 5. Windowed Score (Duration.of(3, 2))
    println()
    println("=== Windowed Score (Window: 3/2) ===")
    val windowedScore = toWindowedScore(sheetMusic, Duration.of(3, 2))(partIdOrdering)
    printPrettyScore(windowedScore)(partIdOrdering)

  }

  val partIdOrdering: Ordering[PartId] = Ordering.by(_.ordinal)

  def printPrettyScore[Id, A, Attr](score: Score[Id, A, Attr], indentLevel: Int = 0)(ordering: Ordering[Id]): Unit = {
    val indent = "  " * indentLevel
    score match {
      case Score.NoteScore(note) =>
        println(s"${indent}Note: $note")

      case Score.MelodyScore(melody) =>
        val isLeafMelody = melody.elems.forall {
          case Score.NoteScore(_) => true
          case _                  => false
        }

        if (isLeafMelody) {
          val notesStr = melody.elems
            .collect { case Score.NoteScore(n) =>
              s"${n.value}(${n.duration})"
            }
            .mkString(", ")
          println(s"${indent}Melody: [$notesStr]")
        } else {
          println(s"${indent}Melody:")
          melody.elems.foreach { s =>
            printPrettyScore(s, indentLevel + 1)(ordering)
          }
        }

      case Score.ChordScore(chord) =>
        println(s"${indent}Chord:")
        // パートの順序を指定された Ordering に従ってソート
        chord.keyElems.toList.sortBy(_._1)(using ordering).foreach { case (key, subScore) =>
          println(s"${indent}  Part $key:")
          printPrettyScore(subScore, indentLevel + 2)(ordering)
        }
    }
  }

  def toScore(sheetMusic: SheetMusic): Score[PartId, Pitch | Rest, Option[ScoreAttrs]] = {
    import ScoreSyntax.*
    sheetMusic.toChord.asScore
  }

  def toGridScore(
      sheetMusic: SheetMusic,
  )(ordering: Ordering[PartId]): Score[PartId, Pitch | Rest, Option[ScoreAttrs]] = {

    import ScoreSyntax.*

    val flattenMeasureChord: Chord[PartId, Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]] =
      sheetMusic.toChord.map(_.flatten)

    val grid = Grid.fromPolyphonicMelodiesChord(flattenMeasureChord, ordering)(_.value)

    grid.toChordsMelody.asScore[PartId, Pitch | Rest, Option[ScoreAttrs]]

  }

  def toWindowedScore(sheetMusic: SheetMusic, windowSize: Duration)(
      ordering: Ordering[PartId],
  ): Score[PartId, Pitch | Rest, Option[ScoreAttrs]] = {

    import ScoreSyntax.*

    val flattenMeasureChord: Chord[PartId, Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]] =
      sheetMusic.toChord.map(_.flatten)

    val windowedMelodiesChord: Chord[PartId, Melody[Melody[Note[Pitch | Rest, Option[ScoreAttrs]]]]] =
      flattenMeasureChord.map { melody =>

        val splitPoints = Iterator.iterate(Offset.of(0))(_ + windowSize)

        val windows = Sliceable.sliceList(melody)(splitPoints).map(_.value)

        Melody(windows)

      }

    val grid = Grid.fromMelodiesChord(windowedMelodiesChord, ordering)

    grid.toChordsMelody.asScore[PartId, Pitch | Rest, Option[ScoreAttrs]]

  }

}

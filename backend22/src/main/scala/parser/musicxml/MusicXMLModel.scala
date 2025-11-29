package parser.musicxml

case class Pitch(
    step: String,
    alter: Int,
    octave: Int,
)

case class TimeModification(
    actualNotes: Int,
    normalNotes: Int,
    normalType: Option[String] = None,
    normalDotCount: Int = 0,
)

case class Note(
    pitch: Option[Pitch] = None, // Noneの場合は休符
    duration: Int = 0,
    voice: Int = 1,
    isChord: Boolean = false,
    isGrace: Boolean = false,
    tieTypes: List[String] = Nil, // 'start' or 'stop'
    timeModification: Option[TimeModification] = None,
) extends MeasureElement

case class Backup(duration: Int) extends MeasureElement

case class Forward(duration: Int) extends MeasureElement

case class Key(
    fifths: Int,
    mode: String,
)

case class Time(
    beats: Int,
    beatType: Int,
)

case class Clef(
    sign: String,
    line: Int,
)

case class Attributes(
    divisions: Option[Int] = None,
    key: Option[Key] = None,
    time: Option[Time] = None,
) extends MeasureElement

case class DirectionWord(value: String)

case class Direction(
    words: List[DirectionWord] = Nil,
) extends MeasureElement

sealed trait MeasureElement

case class Measure(
    number: Int,
    elements: List[MeasureElement] = Nil,
)

case class Part(
    id: String,
    measures: List[Measure] = Nil,
)

case class Score(
    workTitle: String,
    parts: List[Part] = Nil,
)

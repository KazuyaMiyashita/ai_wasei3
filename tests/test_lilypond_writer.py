from my_project.harmony import Chord
from my_project.lilypond_writer import write
from my_project.model import Key, Mode, NoteName, Pitch


def test_write() -> None:
    harmonies = [
        Chord(
            Pitch.parse("C3"),
            Pitch.parse("E4"),
            Pitch.parse("G4"),
            Pitch.parse("C5"),
        ),
        Chord(
            Pitch.parse("F3"),
            Pitch.parse("F4"),
            Pitch.parse("A4"),
            Pitch.parse("C5"),
        ),
        Chord(
            Pitch.parse("G3"),
            Pitch.parse("D4"),
            Pitch.parse("G4"),
            Pitch.parse("B4"),
        ),
        Chord(
            Pitch.parse("C3"),
            Pitch.parse("E4"),
            Pitch.parse("G4"),
            Pitch.parse("C5"),
        ),
    ]
    key = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
    str = write(harmonies, key)

    print(str)
    assert (
        str
        == """\\version "2.24.4"

keyTime = { \\key c \\major \\time 2/2 }

SopMusic   = { c''2 c''2 b'2 c''2 }
AltoMusic  = { g'2 a'2 g'2 g'2 }
TenorMusic = { e'2 f'2 d'2 e'2 }
BassMusic  = { c2 f2 g2 c2 }

\\score {
  \\new PianoStaff <<
    \\new Staff <<
      \\clef "treble"
      \\new Voice = "Sop"  { \\voiceOne \\keyTime \\SopMusic }
      \\new Voice = "Alto" { \\voiceTwo \\AltoMusic }
    >>
    \\new Staff <<
      \\clef "bass"
      \\new Voice = "Tenor" { \\voiceOne \\keyTime \\TenorMusic }
      \\new Voice = "Bass"  { \\voiceTwo \\BassMusic }
    >>
  >>
}
"""
    )

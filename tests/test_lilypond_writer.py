from fractions import Fraction

from my_project.lilypond_writer import score_to_lilypond
from my_project.model import (
    Duration,
    Key,
    Measure,
    Mode,
    Note,
    NoteName,
    Part,
    PartId,
    Pitch,
    Score,
    TimeSignature,
)


def test_write() -> None:
    key = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)

    sop_pitches = [
        Pitch.parse("C5"),
        Pitch.parse("C5"),
        Pitch.parse("B4"),
        Pitch.parse("C5"),
    ]
    alto_pitches = [
        Pitch.parse("G4"),
        Pitch.parse("A4"),
        Pitch.parse("G4"),
        Pitch.parse("G4"),
    ]
    tenor_pitches = [
        Pitch.parse("E4"),
        Pitch.parse("F4"),
        Pitch.parse("D4"),
        Pitch.parse("E4"),
    ]
    bass_pitches = [
        Pitch.parse("C3"),
        Pitch.parse("F3"),
        Pitch.parse("G3"),
        Pitch.parse("C3"),
    ]

    duration = Duration(Fraction(2))

    sop_notes = [Note(p, duration) for p in sop_pitches]
    alto_notes = [Note(p, duration) for p in alto_pitches]
    tenor_notes = [Note(p, duration) for p in tenor_pitches]
    bass_notes = [Note(p, duration) for p in bass_pitches]

    sop_measures = [Measure(notes) for notes in [sop_notes[0:2], sop_notes[2:4]]]
    alto_measures = [Measure(notes) for notes in [alto_notes[0:2], alto_notes[2:4]]]
    tenor_measures = [Measure(notes) for notes in [tenor_notes[0:2], tenor_notes[2:4]]]
    bass_measures = [Measure(notes) for notes in [bass_notes[0:2], bass_notes[2:4]]]

    score = Score(
        key=key,
        time_signature=TimeSignature(2, Fraction(2)),
        parts=[
            Part(part_id=PartId.SOPRANO, measures=sop_measures),
            Part(part_id=PartId.ALTO, measures=alto_measures),
            Part(part_id=PartId.TENOR, measures=tenor_measures),
            Part(part_id=PartId.BASS, measures=bass_measures),
        ],
    )

    print(score)

    str_result = score_to_lilypond(score)

    print(str_result)

    assert (
        str_result
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

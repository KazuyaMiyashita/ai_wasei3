from my_project.harmony import Chord, start_chord, triad_note_names
from my_project.model import Key, Mode, NoteName, PartId, Pitch
from my_project.util import part_range, scale_pitches


def test_scale_pitches() -> None:
    assert scale_pitches(
        key=Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR),
        range=part_range(PartId.BASS),
    ) == [
        Pitch.parse("F#2"),
        Pitch.parse("G2"),
        Pitch.parse("A2"),
        Pitch.parse("B2"),
        Pitch.parse("C#3"),
        Pitch.parse("D3"),
        Pitch.parse("E3"),
        Pitch.parse("F#3"),
        Pitch.parse("G3"),
        Pitch.parse("A3"),
        Pitch.parse("B3"),
        Pitch.parse("C#4"),
        Pitch.parse("D4"),
    ]


def test_triad_note_names() -> None:
    # ニ長調でバスがD (Iの和音)
    assert triad_note_names(
        bass=NoteName.parse("D"),
        key=Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR),
    ) == set(
        [
            NoteName.parse("D"),
            NoteName.parse("F#"),
            NoteName.parse("A"),
        ]
    )

    # ホ短調でバスがB (Vの和音)
    assert triad_note_names(
        bass=NoteName.parse("B"),
        key=Key(tonic=NoteName.parse("E"), mode=Mode.MINOR),
    ) == set(
        [
            NoteName.parse("B"),
            NoteName.parse("D#"),
            NoteName.parse("F#"),
        ]
    )


def test_start_chord() -> None:
    chord = start_chord(
        Pitch.parse("C4"),
        key=Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR),
    )
    assert chord == Chord(
        bass=Pitch.parse("C4"),
        tenor=Pitch.parse("G4"),
        alto=Pitch.parse("C5"),
        soprano=Pitch.parse("E5"),
    )

    chord = start_chord(
        Pitch.parse("C4"),
        key=Key(tonic=NoteName.parse("C"), mode=Mode.MINOR),
    )
    assert chord == Chord(
        bass=Pitch.parse("C4"),
        tenor=Pitch.parse("G4"),
        alto=Pitch.parse("C5"),
        soprano=Pitch.parse("Eb5"),
    )

    chord = start_chord(
        Pitch.parse("D3"),
        key=Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR),
    )
    assert chord == Chord(
        bass=Pitch.parse("D3"),
        tenor=Pitch.parse("F#4"),
        alto=Pitch.parse("A4"),
        soprano=Pitch.parse("D5"),
    )

    chord = start_chord(
        Pitch.parse("G2"),
        key=Key(tonic=NoteName.parse("G"), mode=Mode.MINOR),
    )
    assert chord == Chord(
        bass=Pitch.parse("G2"),
        tenor=Pitch.parse("G3"),
        alto=Pitch.parse("D4"),
        soprano=Pitch.parse("Bb4"),
    )

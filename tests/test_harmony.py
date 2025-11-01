from my_project.harmony import Harmony, available_harmonies, compare_pitch, scale_pitches, triad_note_names
from my_project.model import Key, Mode, NoteName, Pitch


def test_compare_pitch() -> None:
    assert compare_pitch(Pitch.parse("F#4"), Pitch.parse("G4")) == -1
    assert compare_pitch(Pitch.parse("F#4"), Pitch.parse("F#4")) == 0
    assert compare_pitch(Pitch.parse("F#4"), Pitch.parse("Gb4")) == 0
    assert compare_pitch(Pitch.parse("F#4"), Pitch.parse("F4")) == 1


def test_scale_pitches() -> None:
    assert scale_pitches(
        key=Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR),
        min=Pitch.parse("F2"),
        max=Pitch.parse("D4"),
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


def test_available_harmonies() -> None:
    assert available_harmonies(
        bass=Pitch.parse("C4"),
        key=Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR),
    ) == set(
        [
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G4"), Pitch.parse("C5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("C5"), Pitch.parse("G5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("C4"), Pitch.parse("G4"), Pitch.parse("E5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("E4"), Pitch.parse("G4")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G4"), Pitch.parse("G4")),
            Harmony(Pitch.parse("C4"), Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G4"), Pitch.parse("E5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("G4"), Pitch.parse("C5"), Pitch.parse("E5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("E4"), Pitch.parse("G5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G4"), Pitch.parse("G5")),
            Harmony(Pitch.parse("C4"), Pitch.parse("C4"), Pitch.parse("E4"), Pitch.parse("G4")),
            Harmony(Pitch.parse("C4"), Pitch.parse("G4"), Pitch.parse("G4"), Pitch.parse("E5")),
        ]
    )

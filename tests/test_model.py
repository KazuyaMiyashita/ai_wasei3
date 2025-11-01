from my_project.model import Degree, Key, Mode, NoteName, Pitch


def test_pitch() -> None:
    pitch = Pitch.parse("F#4")
    name = pitch.name()
    assert name == "F#4"


def test_degree() -> None:
    # ニ長調

    pitches = [
        Pitch.parse("D3"),
        Pitch.parse("E3"),
        Pitch.parse("F#3"),
        Pitch.parse("G3"),
        Pitch.parse("A3"),
        Pitch.parse("B3"),
        Pitch.parse("C#4"),
        Pitch.parse("D4"),
    ]
    key = Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR)

    degrees = [Degree.from_note_name_key(p.note_name, key) for p in pitches]

    assert degrees == [
        Degree.of(step=1, alter=0),
        Degree.of(step=2, alter=0),
        Degree.of(step=3, alter=0),
        Degree.of(step=4, alter=0),
        Degree.of(step=5, alter=0),
        Degree.of(step=6, alter=0),
        Degree.of(step=7, alter=0),
        Degree.of(step=1, alter=0),
    ]

    # ハ短調・和声的短音階

    pitches = [
        Pitch.parse("C3"),
        Pitch.parse("D3"),
        Pitch.parse("Eb3"),
        Pitch.parse("F3"),
        Pitch.parse("G3"),
        Pitch.parse("Ab3"),
        Pitch.parse("B3"),
        Pitch.parse("C3"),
    ]
    key = Key(tonic=NoteName.parse("C"), mode=Mode.MINOR)

    degrees = [Degree.from_note_name_key(p.note_name, key) for p in pitches]

    assert degrees == [
        Degree.of(step=1, alter=0),
        Degree.of(step=2, alter=0),
        Degree.of(step=3, alter=0),
        Degree.of(step=4, alter=0),
        Degree.of(step=5, alter=0),
        Degree.of(step=6, alter=0),
        Degree.of(step=7, alter=1),
        Degree.of(step=1, alter=0),
    ]

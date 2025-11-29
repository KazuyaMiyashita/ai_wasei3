from my_project.model import Key, Mode, NoteName, PartId, Pitch
from my_project.util import part_range, scale_pitches


def test_scale_pitches() -> None:
    ps = scale_pitches(
        Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR),
        part_range(PartId.TENOR),
        include_all_minor_scale=True,
    )
    for p in ps:
        print(p.name())
    assert ps == [
        Pitch.parse("C3"),
        Pitch.parse("D3"),
        Pitch.parse("E3"),
        Pitch.parse("F3"),
        Pitch.parse("G3"),
        Pitch.parse("A3"),
        Pitch.parse("B3"),
        Pitch.parse("C4"),
        Pitch.parse("D4"),
        Pitch.parse("E4"),
        Pitch.parse("F4"),
        Pitch.parse("G4"),
        Pitch.parse("A4"),
    ]

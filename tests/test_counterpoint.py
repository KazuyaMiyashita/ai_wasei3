from my_project.counterpoint import SearchingPassingNoteInMeasureState
from my_project.model import (
    IntervalStep,
    Key,
    Mode,
    NoteName,
    Pitch,
)

KEY = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)


def test_passing_note_conjunct_pitches() -> None:
    assert SearchingPassingNoteInMeasureState.conjunct_pitches(
        key=KEY, pitch=Pitch.parse("C4"), interval_step=IntervalStep.idx_1(3)
    ) == [
        Pitch.parse("D4"),
        Pitch.parse("E4"),
    ]

    assert SearchingPassingNoteInMeasureState.conjunct_pitches(
        key=KEY, pitch=Pitch.parse("C4"), interval_step=IntervalStep.idx_1(-3)
    ) == [
        Pitch.parse("B3"),
        Pitch.parse("A3"),
    ]

    assert SearchingPassingNoteInMeasureState.conjunct_pitches(
        key=KEY, pitch=Pitch.parse("C4"), interval_step=IntervalStep.idx_1(-4)
    ) == [
        Pitch.parse("B3"),
        Pitch.parse("A3"),
        Pitch.parse("G3"),
    ]

    assert (
        SearchingPassingNoteInMeasureState.conjunct_pitches(
            key=KEY, pitch=Pitch.parse("C4"), interval_step=IntervalStep.idx_1(1)
        )
        == []
    )

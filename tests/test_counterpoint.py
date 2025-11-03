from my_project.counterpoint import (
    State,
    on_searching_harmonic_tone,
    on_searching_neighbor_tone,
    on_start,
    state_previous_pitch,
)
from my_project.model import Duration, Measure, Note, Pitch


def test_state_previous_pitch() -> None:
    state = State(
        unprocessed_cfs=[Pitch.parse("C4")],
        previous_measures=[
            Measure(
                [
                    Note(pitch=Pitch.parse("C4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("D4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("E4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("F4"), duration=Duration.of(1)),
                ]
            )
        ],
        current_measure_notes=[
            Note(pitch=None, duration=Duration.of(1)),
            Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
        ],
    )
    assert state_previous_pitch(state) == Pitch.parse("G4")

    state = State(
        unprocessed_cfs=[Pitch.parse("C4")],
        previous_measures=[
            Measure(
                [
                    Note(pitch=Pitch.parse("C4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("D4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("E4"), duration=Duration.of(1)),
                    Note(pitch=Pitch.parse("F4"), duration=Duration.of(1)),
                ]
            )
        ],
        current_measure_notes=[],
    )
    assert state_previous_pitch(state) == Pitch.parse("F4")


def test_on_start() -> None:
    cf = Pitch.parse("C4")
    state = State.init_state([cf])
    next_states = list(on_start(state, randomized=False))

    for next_state in next_states:
        assert next_state.unprocessed_cfs == [Pitch.parse("C4")]
        assert next_state.previous_measures == []

    result = [next_state.current_measure_notes for next_state in next_states]

    assert result[0] == [
        Note(pitch=None, duration=Duration.of(1)),
        Note(pitch=Pitch.parse("C4"), duration=Duration.of(1)),
    ]
    assert result[1] == [
        Note(pitch=None, duration=Duration.of(1)),
        Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
    ]
    assert result[2] == [
        Note(pitch=None, duration=Duration.of(1)),
        Note(pitch=Pitch.parse("C5"), duration=Duration.of(1)),
    ]
    assert result[3] == [
        Note(pitch=None, duration=Duration.of(1)),
        Note(pitch=Pitch.parse("G5"), duration=Duration.of(1)),
    ]
    assert len(result) == 4


def test_on_searching_harmonic_tone() -> None:
    state = State(
        unprocessed_cfs=[Pitch.parse("C4")],
        previous_measures=[],
        current_measure_notes=[
            Note(pitch=None, duration=Duration.of(1)),
            Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
        ],
    )

    next_states = list(on_searching_harmonic_tone(state, randomized=False))

    for next_state in next_states:
        assert next_state.unprocessed_cfs == [Pitch.parse("C4")]
        assert next_state.previous_measures == []

    result = [next_state.current_measure_notes for next_state in next_states]
    for current_measure_notes in result:
        assert len(current_measure_notes) == 3
        # [2] の要素が今回追加されたもの
        assert current_measure_notes[2].pitch

    pitches = [notes[2].pitch for notes in result]

    # CFは C4, 旋律の直前の音は G4
    assert pitches == [
        Pitch.parse("C4"),
        Pitch.parse("E4"),
        Pitch.parse("A4"),
        Pitch.parse("C5"),
        Pitch.parse("E5"),
        Pitch.parse("G5"),
    ]


def test_on_searching_neighbor_tone_1() -> None:
    state = State(
        unprocessed_cfs=[Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")],
        previous_measures=[],
        current_measure_notes=[
            Note(pitch=None, duration=Duration.of(1)),
            Note(pitch=Pitch.parse("C5"), duration=Duration.of(1)),
        ],
    )

    next_states = list(on_searching_neighbor_tone(state, randomized=False))

    assert len(next_states) == 2

    # 1つ目のnext_state
    next_state = next_states[0]
    assert next_state.unprocessed_cfs == [Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")]
    assert len(next_state.previous_measures) == 0
    assert [notes.pitch for notes in next_state.current_measure_notes] == [
        None,
        Pitch.parse("C5"),
        Pitch.parse("B4"),
        Pitch.parse("C5"),
    ]

    # 2つ目のnext_state
    next_state = next_states[1]
    assert next_state.unprocessed_cfs == [Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")]
    assert len(next_state.previous_measures) == 0
    assert [notes.pitch for notes in next_state.current_measure_notes] == [
        None,
        Pitch.parse("C5"),
        Pitch.parse("D5"),
        Pitch.parse("C5"),
    ]


def test_on_searching_neighbor_tone_2() -> None:
    state = State(
        unprocessed_cfs=[Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")],
        previous_measures=[],
        current_measure_notes=[
            Note(pitch=None, duration=Duration.of(1)),
            Note(pitch=Pitch.parse("C5"), duration=Duration.of(1)),
            Note(pitch=Pitch.parse("E5"), duration=Duration.of(1)),
        ],
    )

    next_states = list(on_searching_neighbor_tone(state, randomized=False))

    assert len(next_states) == 2

    # 1つ目のnext_state
    next_state = next_states[0]
    assert next_state.unprocessed_cfs == [Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")]
    assert len(next_state.previous_measures) == 0
    print([notes.pitch.name() if notes.pitch else "None" for notes in next_state.current_measure_notes])
    assert [notes.pitch for notes in next_state.current_measure_notes] == [
        None,
        Pitch.parse("C5"),
        Pitch.parse("E5"),
        Pitch.parse("D5"),
        Pitch.parse("E5"),
    ]

    # 2つ目のnext_state
    next_state = next_states[1]
    assert next_state.unprocessed_cfs == [Pitch.parse("C4"), Pitch.parse("A3"), Pitch.parse("G3")]
    assert len(next_state.previous_measures) == 0
    assert [notes.pitch for notes in next_state.current_measure_notes] == [
        None,
        Pitch.parse("C5"),
        Pitch.parse("E5"),
        Pitch.parse("F5"),
        Pitch.parse("E5"),
    ]

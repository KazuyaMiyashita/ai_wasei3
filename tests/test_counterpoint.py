from my_project.counterpoint import AnnotatedMeasure, AnnotatedNote, EachCheckState, SearchState, State, ToneType
from my_project.model import Duration, Note, Pitch


def _print_annotated_note(annotated_notes: list[AnnotatedNote]) -> None:
    print([an.note.pitch.name() if an.note.pitch else "None" for an in annotated_notes])


def test_on_start() -> None:
    cfs = [
        Pitch.parse("C4"),
        Pitch.parse("A3"),
    ]
    state = State.start_state(cfs)
    assert isinstance(state, SearchState)

    next_states = list(state.on_first_measure_start_of_measure(randomized=False))

    expected_pitches = [
        Pitch.parse("C4"),
        Pitch.parse("G4"),
        Pitch.parse("C5"),
        Pitch.parse("G5"),
    ]

    assert len(next_states) == 4

    for i, next_state in enumerate(next_states):
        assert isinstance(next_state, EachCheckState)
        assert next_state.cf_cursor == 0
        assert next_state.completed_measures == []
        assert next_state.note_buffer == []

        expected_pitch = expected_pitches[i]

        # notes_to_add に休符・目的の音が含まれているかチェック
        expected_notes_to_add = [
            AnnotatedNote(
                note=Note(pitch=None, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=expected_pitch, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ]
        assert next_state.notes_to_add == expected_notes_to_add


def test_on_last_measure() -> None:
    state = SearchState(
        cantus_firmus=[
            Pitch.parse("D3"),
            Pitch.parse("C3"),
        ],
        cf_cursor=1,
        completed_measures=[
            AnnotatedMeasure(
                [
                    AnnotatedNote(note=Note(Pitch.parse("F4"), Duration.of(1)), tone_type=ToneType.HARMONIC_TONE),
                    AnnotatedNote(note=Note(Pitch.parse("G4"), Duration.of(1)), tone_type=ToneType.HARMONIC_TONE),
                    AnnotatedNote(note=Note(Pitch.parse("A4"), Duration.of(1)), tone_type=ToneType.HARMONIC_TONE),
                    AnnotatedNote(note=Note(Pitch.parse("B4"), Duration.of(1)), tone_type=ToneType.HARMONIC_TONE),
                ]
            )
        ],
        note_buffer=[],
    )

    next_states = list(state.on_last_measure(randomized=False))

    expected_pitches = [
        Pitch.parse("C4"),  # 長7度の進行。現在は認められる
        Pitch.parse("G4"),  # 導音が主音に到達しない。現在は認められる
        Pitch.parse("C5"),
        # G5 はCFのC3と2オクターブを超えるので除外される
    ]

    assert len(next_states) == 3

    for i, next_state in enumerate(next_states):
        assert isinstance(next_state, EachCheckState)

        assert next_state.cf_cursor == 1
        assert len(next_state.completed_measures) == 1
        assert next_state.note_buffer == []

        expected_pitch = expected_pitches[i]

        # notes_to_addに全音符で目的の音が含まれているかチェック
        expected_notes_to_add = [
            AnnotatedNote(
                note=Note(pitch=expected_pitch, duration=Duration.of(4)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ]
        assert next_state.notes_to_add == expected_notes_to_add


def test_on_searching_harmonic_tone() -> None:
    state = SearchState(
        cantus_firmus=[Pitch.parse("C4"), Pitch.parse("D4")],
        cf_cursor=0,
        completed_measures=[],
        note_buffer=[
            AnnotatedNote(
                note=Note(pitch=None, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
    )

    next_states = list(state.on_searching_harmonic_tone(randomized=False))

    expected_pitches = [
        Pitch.parse("C4"),
        Pitch.parse("E4"),
        Pitch.parse("A4"),
        Pitch.parse("C5"),
        Pitch.parse("E5"),
        Pitch.parse("G5"),
    ]

    assert len(next_states) == len(expected_pitches)

    for i, next_state in enumerate(next_states):
        assert isinstance(next_state, EachCheckState)
        _print_annotated_note(next_state.notes_to_add)

        assert next_state.cantus_firmus == [Pitch.parse("C4"), Pitch.parse("D4")]
        assert next_state.cf_cursor == 0
        assert next_state.completed_measures == []

        expected_pitch = expected_pitches[i]

        assert next_state.notes_to_add == [
            AnnotatedNote(
                note=Note(pitch=expected_pitch, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ]


def test_on_searching_passing_tone() -> None:
    state = SearchState(
        cantus_firmus=[Pitch.parse("C4"), Pitch.parse("A3")],
        cf_cursor=0,
        completed_measures=[],
        note_buffer=[
            AnnotatedNote(
                note=Note(pitch=None, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
    )

    next_states = list(state.on_searching_passing_tone(randomized=False))

    expected_add_pitches_list = [
        [Pitch.parse("F4"), Pitch.parse("E4")],
    ]

    assert len(next_states) == len(expected_add_pitches_list)

    for i, next_state in enumerate(next_states):
        assert isinstance(next_state, EachCheckState)
        _print_annotated_note(next_state.notes_to_add)

        expected_add_pitches = expected_add_pitches_list[i]

        expected_notes_to_add = [
            AnnotatedNote(
                note=Note(pitch=expected_add_pitches[0], duration=Duration.of(1)),
                tone_type=ToneType.PASSING_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=expected_add_pitches[1], duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ]
        assert next_state.notes_to_add == expected_notes_to_add


def test_on_searching_neighbor_tone() -> None:
    state = SearchState(
        cantus_firmus=[Pitch.parse("C4"), Pitch.parse("D4")],
        cf_cursor=0,
        completed_measures=[],
        note_buffer=[
            AnnotatedNote(
                note=Note(pitch=None, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
    )

    next_states = list(state.on_searching_neighbor_tone(randomized=False))

    expected_notes_to_add_list = [
        [
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("A4"), duration=Duration.of(1)),
                tone_type=ToneType.NEIGHBOR_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
        [
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("F4"), duration=Duration.of(1)),
                tone_type=ToneType.NEIGHBOR_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
    ]

    assert len(next_states) == len(expected_notes_to_add_list)

    for i, next_state in enumerate(next_states):
        assert isinstance(next_state, EachCheckState)
        _print_annotated_note(next_state.notes_to_add)
        assert next_state.notes_to_add == expected_notes_to_add_list[i]


def test_each_check_state() -> None:
    state = EachCheckState(
        cantus_firmus=[Pitch.parse("C3"), Pitch.parse("D3")],
        cf_cursor=0,
        completed_measures=[],
        note_buffer=[
            AnnotatedNote(
                note=Note(pitch=None, duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("C5"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
        notes_to_add=[
            AnnotatedNote(
                note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                tone_type=ToneType.HARMONIC_TONE,
            ),
        ],
    )

    next_states = list(state.next_states(randomized=False))
    assert len(next_states) == 1
    next_state = next_states[0]

    assert isinstance(next_state, SearchState)
    assert next_state.cf_cursor == 1
    _print_annotated_note(next_state.completed_measures[0].annotated_notes)
    assert next_state.completed_measures == [
        AnnotatedMeasure(
            [
                AnnotatedNote(
                    note=Note(pitch=None, duration=Duration.of(1)),
                    tone_type=ToneType.HARMONIC_TONE,
                ),
                AnnotatedNote(
                    note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                    tone_type=ToneType.HARMONIC_TONE,
                ),
                AnnotatedNote(
                    note=Note(pitch=Pitch.parse("C5"), duration=Duration.of(1)),
                    tone_type=ToneType.HARMONIC_TONE,
                ),
                AnnotatedNote(
                    note=Note(pitch=Pitch.parse("G4"), duration=Duration.of(1)),
                    tone_type=ToneType.HARMONIC_TONE,
                ),
            ]
        )
    ]
    assert next_state.note_buffer == []

import re

import pytest

from my_project.counterpoint.measure_search.measure_step_sequence import (
    AbstractMeasureStepSequence,
    MeasureStepSequence,
)
from my_project.counterpoint.measure_search.pitch_applyer import (
    _apply_pitch_diatonic,
    _degree_candidates,
    apply_pitch_candidates,
)
from my_project.counterpoint.model import ToneType
from my_project.model import Degree, DegreeStep, Duration, IntervalStep, Key, Melody, Mode, Note, Pitch


def test_apply_pitch_diatonic_simple() -> None:
    """
    apply_pitch_diatonicの簡単なテスト
    G Major, D4 開始で1オクターブ分
    """
    key = Key.parse("G Minor")
    start_pitch = Pitch.parse("D4")
    interval_steps = [IntervalStep(i) for i in range(0, 8)]
    expected = [
        Pitch.parse("D4"),
        Pitch.parse("Eb4"),
        Pitch.parse("F4"),
        Pitch.parse("G4"),
        Pitch.parse("A4"),
        Pitch.parse("Bb4"),
        Pitch.parse("C5"),
        Pitch.parse("D5"),
    ]
    assert _apply_pitch_diatonic(key, start_pitch, interval_steps) == expected


def test_apply_pitch_candidates_minor_leading_tone() -> None:
    """
    apply_pitch_candidatesの簡単なテスト
    E Minor, 和音V, B4 から開始。
    E4 から 和音の音で3度ずつ上行する
    """
    key = Key.parse("E Minor")
    chord_degrees = {
        Degree.idx_1(5, 0),
        Degree.idx_1(7, 1),  # vii の上方変位
        Degree.idx_1(2, 0),
    }
    start_pitch = Pitch.parse("B4")

    measure = Melody.of(
        Note(IntervalStep(0), Duration.of(1), ToneType.HARMONIC_TONE),
        Note(IntervalStep(2), Duration.of(1), ToneType.HARMONIC_TONE),
        Note(IntervalStep(4), Duration.of(1), ToneType.HARMONIC_TONE),
    )
    mss = MeasureStepSequence(measure, IntervalStep(0))

    expected = [
        AbstractMeasureStepSequence(
            Melody.of(
                Note(Pitch.parse("B4"), Duration.of(1), ToneType.HARMONIC_TONE),
                Note(Pitch.parse("D#5"), Duration.of(1), ToneType.HARMONIC_TONE),
                Note(Pitch.parse("F#5"), Duration.of(1), ToneType.HARMONIC_TONE),
            ),
            Pitch.parse("B4"),
        )
    ]
    result = apply_pitch_candidates(key, chord_degrees, start_pitch, mss)
    print(result)
    assert result == expected


def test_degree_candidates_minor_harmonic() -> None:
    """
    degree_candidatesの簡単なテスト
    A Minor, 和音V (E, G#, B) において、第7音が和声音として指定された場合
    """
    mode = Mode.MINOR

    # A MinorにおけるVの和音 (E Major)
    # E: 第5音(index 4), G#: 第7音(index 6)の半音上げ, B: 第2音(index 1)
    chord_degrees = {
        Degree.idx_1(5, 0),
        Degree.idx_1(7, 1),  # G#
        Degree.idx_1(2, 0),
    }

    # 第7音(G)が和声音(HARMONIC_TONE)として与えられる
    input_measure_degrees = Melody.of(Note(DegreeStep.idx_1(7), Duration.of(1), ToneType.HARMONIC_TONE))
    input_next_measure_degree_step = DegreeStep.idx_1(1)  # 現時点では未使用

    input_mss_degree_step = AbstractMeasureStepSequence(input_measure_degrees, input_next_measure_degree_step)

    # 和音に含まれるG# (DegreeAlter(1)) が選択されるはず
    expected_measure = Melody.of(Note(Degree.idx_1(7, 1), Duration.of(1), ToneType.HARMONIC_TONE))
    expected_next_degree_step = Degree.idx_1(1, 0)  # TODO: alter も考慮に入れる
    expected = [AbstractMeasureStepSequence(expected_measure, expected_next_degree_step)]

    assert (
        _degree_candidates(
            mode,
            chord_degrees,
            input_mss_degree_step,
        )
        == expected
    )


_DEGREE_CANDIDATES_TEST_CASES = [
    # # -- Major --
    ("Major, I, [5, 6p, 7p, 1 | 1]", ["5, 6, 7, 1 | 1"]),
    ("Major, I, [5, 6p, 7p, 1 | 3]", ["5, 6, 7, 1 | 3"]),
    # # -- Minor --
    # 和声音
    ("Minor, V, [5, 7, 2 | 1]", ["5, 7, 2 | 1"]),  # 和声音に 7 が含まれるため、この結果に絞られる
    ("Minor, +V, [5, 7, 2 | 1]", ["5, 7^, 2 | 1"]),  # 和声音に 7^ が含まれるため、この結果に絞られる
    # 経過音
    # 以下は ["5, 6, 7^, 1"], ["5, 6^, 7, 1"] は不適当
    ("Minor, I, [1, 7p, 6p, 5 | 1]", ["1, 7, 6, 5 | 1", "1, 7^, 6^, 5 | 1"]),
    # 以下は ["5, 6, 7^, 1"], ["5, 6^, 7, 1"] は不適当
    ("Minor, I, [5, 6p, 7p, 1 | 1]", ["5, 6^, 7^, 1 | 1", "5, 6, 7, 1 | 1"]),
    # 経過音、和声音によって規制されるもの
    ("Minor, +V, [5, 6p, 7p, 1 | 1]", ["5, 6^, 7^, 1 | 1"]),  # 和声音に 7^ が含まれるため、この結果に絞られる
    ("Minor, +V, [1, 7p, 6p, 5 | 1]", ["1, 7^, 6^, 5 | 1"]),  # 和声音に 7^ が含まれるため、この結果に絞られる
    ("Minor, V, [5, 6p, 7p, 1 | 1]", ["5, 6, 7, 1 | 1"]),  # 和声音に 7 が含まれるため、この結果に絞られる
    ("Minor, V, [1, 7p, 6p, 5 | 1]", ["1, 7, 6, 5 | 1"]),  # 和声音に 7 が含まれるため、この結果に絞られる
    ("Minor, II, [4, 5p, 6p, 7p | 1]", ["4, 5, 6, 7 | 1"]),  # 和声音に 6 が含まれるため、この結果に絞られる
    # 掛留音
    ("Minor, +V, [1r, 7 | 1]", ["1, 7^ | 1"]),
    ("Minor, V, [1r, 7 | 1]", ["1, 7 | 1"]),
    ("Minor, I, [7r, 1 | 1]", ["7^, 1 | 1"]),
    ("Minor, VI, [7r, 6 | 1]", ["7, 6 | 1"]),
    ("Minor, +V, [6r, 7 | 1]", []),
    ("Minor, I, [6r, 5 | 1]", ["6, 5 | 1"]),
    # 刺繍音
    ("Minor, I, [1, 7br, 1 | 1]", ["1, 7^, 1 | 1"]),
    ("Minor, +V, [7, 6br, 7 | 1]", ["7^, 6^, 7^ | 1"]),
    ("Minor, V, [7, 6br, 7 | 1]", ["7, 6, 7 | 1"]),
    ("Minor, +V, [7, 1br, 7 | 1]", ["7^, 1, 7^ | 1"]),
    ("Minor, V, [7, 1br, 7 | 1]", ["7, 1, 7 | 1"]),
    ("Minor, I, [5, 6br, 5 | 1]", ["5, 6, 5 | 1"]),
    ("Minor, IV, [6, 7br, 6 | 1]", ["6, 7, 6 | 1"]),
    ("Minor, +IV, [6, 7br, 6 | 1]", ["6^, 7^, 6^ | 1"]),
    # 次の小節の音
    ("Minor, I, [1, 2 | 3]", ["1, 2 | 3"]),
    ("Minor, I, [5, 6p | 7]", ["5, 6 | 7", "5, 6^ | 7^"]),  # "5, 6 | 7^", "5, 6^ | 7" は不適当。
    ("Minor, I, [1 | 6]", ["1 | 6"]),  # "1 | 6^" は 6^ を上行順次進行で利用していないため不適当
    ("Minor, I, [1, 5 | 6]", ["1, 5 | 6", "1, 5 | 6^"]),
    # 以下は "1, 7 | ^6", "1, 7^ | 6" は不適当。
    # "1, 7^ | 6^" は 6^ を上行順次進行で利用していないため不適当
    ("Minor, I, [1, 7p | 6]", ["1, 7 | 6"]),
]


@pytest.mark.parametrize("input_str, expected_strs", _DEGREE_CANDIDATES_TEST_CASES)
def test_degree_candidate(input_str: str, expected_strs: list[str]) -> None:
    _run_degree_test(input_str, expected_strs)


# --- Test Utilities ---


_TONE_MAP = {
    "": ToneType.HARMONIC_TONE,
    "H": ToneType.HARMONIC_TONE,
    "p": ToneType.PASSING_TONE,
    "br": ToneType.NEIGHBOR_TONE,
    "r": ToneType.SUSPENDED_TONE,
    "srh": ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE,
}

_STR_CHORD_MAP: dict[str, set[Degree]] = {
    "I": {
        Degree.idx_1(1, 0),
        Degree.idx_1(3, 0),
        Degree.idx_1(5, 0),
    },
    "II": {
        Degree.idx_1(2, 0),
        Degree.idx_1(4, 0),
        Degree.idx_1(6, 0),
    },
    "IV": {
        Degree.idx_1(4, 0),
        Degree.idx_1(6, 0),
        Degree.idx_1(1, 0),
    },
    "+IV": {
        Degree.idx_1(4, 0),
        Degree.idx_1(6, 1),
        Degree.idx_1(1, 0),
    },
    "V": {
        Degree.idx_1(5, 0),
        Degree.idx_1(7, 0),  # G in A Minor
        Degree.idx_1(2, 0),
    },
    "+V": {
        Degree.idx_1(5, 0),
        Degree.idx_1(7, 1),  # G# in A Minor
        Degree.idx_1(2, 0),
    },
    "VI": {
        Degree.idx_1(6, 0),
        Degree.idx_1(1, 0),
        Degree.idx_1(3, 0),
    },
}


def _parse_degree_sequence(seq_str: str) -> list[Degree]:
    # "5, 6^, 7^, 2" -> [Degree...]
    # replace comma with space to handle both "5, 6" and "5 6"
    parts = seq_str.replace(",", " ").split()
    res = []
    for p in parts:
        p = p.strip()
        if not p:
            continue
        alter = 0
        if p.endswith("^"):
            step_val = int(p[:-1])
            alter = 1
        elif p.endswith("v"):
            step_val = int(p[:-1])
            alter = -1
        else:
            step_val = int(p)
            alter = 0
        res.append(Degree.idx_1(step_val, alter))
    return res


def _parse_input_elem(s: str) -> tuple[DegreeStep, ToneType]:
    # "6p" -> (DegreeStep(5), PASSING_TONE)
    match = re.match(r"^\s*(\d+)([a-zA-Z]*)\s*$", s)
    if not match:
        raise ValueError(f"Invalid element format: {s}")
    step_val = int(match.group(1))
    type_suffix = match.group(2)
    tone_type = _TONE_MAP.get(type_suffix)
    if tone_type is None:
        raise ValueError(f"Unknown tone type suffix: {type_suffix}")
    return (DegreeStep.idx_1(step_val), tone_type)


def _degree_to_str(d: Degree) -> str:
    step_val = d.step.value + 1
    alter_char = ""
    if d.alter.value == 1:
        alter_char = "^"
    elif d.alter.value == -1:
        alter_char = "v"
    return f"{step_val}{alter_char}"


def _degree_sequence_to_str(seq: list[Degree]) -> str:
    return ", ".join([_degree_to_str(d) for d in seq])


def _run_degree_test(input_str: str, expected_strs: list[str]) -> None:
    # input_str: "A Minor, +V, [5, 6p, 7p, 2 | 1]"
    parts = [p.strip() for p in input_str.split(",", 2)]
    if len(parts) != 3:
        raise ValueError("Input must be 'Mode, Chord, [Steps | NextStep]'")

    mode_part, chord_part, steps_and_next_part = parts

    mode = Mode.parse(mode_part)

    # Parse Chord
    if chord_part not in _STR_CHORD_MAP:
        raise ValueError(f"Unknown chord: {chord_part}")
    chord_degrees = _STR_CHORD_MAP[chord_part]

    # Parse Steps and NextStep
    if not (steps_and_next_part.startswith("[") and steps_and_next_part.endswith("]")):
        raise ValueError("Steps part must be enclosed in []")
    content = steps_and_next_part[1:-1]

    # Split steps and next_step
    if " | " not in content:
        raise ValueError("Steps content must contain ' | ' to separate steps and next_step")
    steps_str, next_step_str = content.split(" | ")

    parsed_elems = [_parse_input_elem(s) for s in steps_str.split(",") if s.strip()]

    # Measure[DegreeStep, ToneType] を作成
    measure_notes: list[Note[DegreeStep, ToneType]] = []
    for elem in parsed_elems:
        measure_notes.append(Note(elem[0], Duration.of(1), elem[1]))
    input_measure_degrees = Melody.of(*measure_notes)

    # next_measure_degree_step は必ず DegreeStep(0) (1度) としておく。
    # このテストはDegreeAlterを見るためのもので、next_measure_degree_step自体が変化することはないため。
    input_next_measure_degree_step = DegreeStep.idx_1(int(next_step_str))

    # AbstractMeasureStepSequence[DegreeStep] を構築
    input_mss_degree_step = AbstractMeasureStepSequence(input_measure_degrees, input_next_measure_degree_step)

    # Parse Expected
    expected_measure_sequences: list[AbstractMeasureStepSequence[Degree]] = []
    for expected_str in expected_strs:
        if " | " not in expected_str:
            raise ValueError("Expected string must contain ' | ' to separate steps and next_step")
        exp_steps_str, exp_next_step_str = expected_str.split(" | ")
        expected_degrees = _parse_degree_sequence(exp_steps_str)

        # Measure[Degree, ToneType] を作成 (DurationとToneTypeは元のMeasureStepSequenceから再構築)
        expected_measure_notes: list[Note[Degree, ToneType]] = []
        for i, deg in enumerate(expected_degrees):
            # 元の MeasureStepSequence の Duration と ToneType を利用 (現状は Duration.of(1) 固定)
            # elems は (DegreeStep, ToneType) のタプルなので、そこから ToneType を取得
            original_tone_type = parsed_elems[i][1]
            expected_measure_notes.append(Note(deg, Duration.of(1), original_tone_type))

        next_step_val = int(exp_next_step_str.strip("^v"))
        next_alter = 0
        if exp_next_step_str.endswith("^"):
            next_alter = 1
        elif exp_next_step_str.endswith("v"):
            next_alter = -1
        expected_next_degree_step = Degree.idx_1(next_step_val, next_alter)

        expected_measure_sequences.append(
            AbstractMeasureStepSequence(Melody.of(*expected_measure_notes), expected_next_degree_step)
        )

    # Execute
    actual_measure_sequences = _degree_candidates(mode, chord_degrees, input_mss_degree_step)

    # Verify
    # Convert to sets of tuples for order-independent comparison of candidates
    actual_set: set[str] = set(
        f"[{_degree_sequence_to_str([n.value for n in m.measure.notes if n.value is not None])}"
        f" | {_degree_to_str(m.next_measure_step)}]"
        for m in actual_measure_sequences
    )
    expected_set: set[str] = set(
        f"[{_degree_sequence_to_str([n.value for n in m.measure.notes if n.value is not None])}"
        f" | {_degree_to_str(m.next_measure_step)}]"
        for m in expected_measure_sequences
    )

    assert actual_set == expected_set, f"Failed for: {input_str}.\nExpected: {expected_set}\nActual:   {actual_set}"

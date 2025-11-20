from my_project.counterpoint.measure_search.measure_step_sequence import MeasureStepSequence
from my_project.model import IntervalStep


def test_used_harmonic_steps() -> None:
    """
    MeasureStepSequence.used_harmonic_steps が正しい和声音のセットを返すことをテストする
    """
    test_cases = [
        ("0|2", frozenset({IntervalStep(0)})),
        ("0,3|3", frozenset({IntervalStep(0), IntervalStep(3)})),
        ("0|4", frozenset({IntervalStep(0)})),
        ("-1r,0,2|5", frozenset({IntervalStep(0), IntervalStep(2)})),
        ("0,2|2", frozenset({IntervalStep(0), IntervalStep(2)})),
        ("0,3,5,3|4", frozenset({IntervalStep(0), IntervalStep(3), IntervalStep(5)})),
        ("1r,0,2|1", frozenset({IntervalStep(0), IntervalStep(2)})),
        ("0,1p,2|3", frozenset({IntervalStep(0), IntervalStep(2)})),
        ("0,1br,0|-1", frozenset({IntervalStep(0)})),
        # inversion_normalized のテスト
        ("0,10|1", frozenset({IntervalStep(0), IntervalStep(3)})),  # 10 は 3 と同じ
    ]

    for seq_str, expected_steps in test_cases:
        seq = MeasureStepSequence.parse(seq_str)
        assert seq.used_harmonic_steps == expected_steps, f"Failed for sequence: {seq_str}"


def test_parse_and_name_roundtrip() -> None:
    """
    MeasureStepSequence.parse と name メソッドが対になっていることをテストする
    """
    sequences_str = [
        "0|2",
        "0,3|3",
        "-1r,0,2|5",
        "0,1p,2|3",
        "0,1br,0|-1",
    ]
    for s_str in sequences_str:
        seq = MeasureStepSequence.parse(s_str)
        assert seq.name() == s_str

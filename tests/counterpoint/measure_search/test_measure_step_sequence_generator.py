from my_project.counterpoint.measure_search.measure_step_sequence_generator import generate


def test_generate_simple_sequence() -> None:
    """
    generate()が基本的なシーケンスを生成できるかテストする
    """
    sequences = generate()
    assert len(sequences) > 0

    expected_sequences = [
        "0,1p|2",
        "0,2|3",
        "0,3|1",
        "0,3|3",
        "0,1p,2|3",
        "0,1p,2p,3|2",
        "0,1p,2p,3p|4",
        "0,1br,0|-1",
        "1r,0|2",
        "0,2|2",
        "0,-2|-2",
    ]

    no_expected_sequences = [
        "0,1|1",  # 和音交代を伴うため。 H = {0, 1} のパターンは出現しない
    ]

    for expected in expected_sequences:
        assert any(s.name() == expected for s in sequences), f"Expected sequence '{expected}' was not generated."

    for unexpected in no_expected_sequences:
        assert not any(s.name() == unexpected for s in sequences), f"Unexpected sequence '{unexpected}' was generated."


def test_no_consecutive_same_pitch_in_measure() -> None:
    """
    生成されたシーケンスの小節内で、同じ高さの音が連続しないことを確認する。
    """
    sequences = generate()
    for seq in sequences:
        notes = seq.measure.notes
        for i in range(len(notes) - 1):
            assert notes[i].value != notes[i + 1].value, f"Consecutive same pitch found in sequence: {seq.name()}"

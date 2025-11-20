import re

import pytest

from my_project.counterpoint.measure_search.measure_step_sequence import MeasureStepSequence
from my_project.counterpoint.measure_search.measure_step_sequence_indexer import (
    MeasureStepSequenceIndexer,
    Q,
    SearchField,
)
from my_project.counterpoint.model import MeasureRythmnPattern
from my_project.model import IntervalStep


@pytest.fixture
def sample_sequences() -> list[MeasureStepSequence]:
    # fmt: off
    sequences_str = [
        "0|2",          # notes=1, next=2, first=0, harmonics={0}, tie=False
        "0,3|3",        # notes=2, next=3, first=0, harmonics={0, 3}, tie=True
        "0|4",          # notes=1, next=4, first=0, harmonics={0}, tie=False
        "-1r,0,2|5",    # notes=3, next=5, first=-1, harmonics={0, 2}, tie=False
        "0,2|2",        # notes=2, next=2, first=0, harmonics={0, 2}, tie=True
        "0,3,5,3|4",    # notes=4, next=4, first=0, harmonics={0, 3, 5}, tie=False
        "1r,0,2|1",     # notes=3, next=1, first=1, harmonics={0, 2}, tie=False
    ]
    # fmt: on
    return [MeasureStepSequence.parse(s) for s in sequences_str]


def test_find_simple_equal(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    results = indexer.find(Q(SearchField.NUM_NOTES_IN_MEASURE).eq(1))
    assert len(results) == 2
    assert all(s.num_notes_in_measure() == 1 for s in results)

    results = indexer.find(Q(SearchField.NEXT_MEASURE_STEP).eq(IntervalStep(5)))
    assert len(results) == 1
    assert all(s.next_measure_step == IntervalStep(5) for s in results)

    results = indexer.find(Q(SearchField.FIRST_NOTE_INTERVAL_STEP).eq(IntervalStep(-1)))
    assert len(results) == 1
    assert all(s.first_note_interval_step_of_measure() == IntervalStep(-1) for s in results)


def test_find_is_in(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    results = indexer.find(
        Q(SearchField.RYTHMN_PATTERNS).is_in([MeasureRythmnPattern.R_22t, MeasureRythmnPattern.R_4444])
    )
    assert len(results) == 3
    assert all(s.name() in ["0,3|3", "0,2|2", "0,3,5,3|4"] for s in results)


def test_find_is_subset_of(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    results = indexer.find(
        Q(SearchField.USED_HARMONIC_STEPS).is_subset_of(frozenset({IntervalStep(0), IntervalStep(3)}))
    )
    assert len(results) == 3

    # 正規化されていない IntervalStep を含む場合
    results = indexer.find(
        Q(SearchField.USED_HARMONIC_STEPS).is_subset_of(
            frozenset({IntervalStep(0), IntervalStep(-2)})
        )  # -2 は 5 と同じ
    )
    assert len(results) == 2
    assert all(s.used_harmonic_steps.issubset(frozenset({IntervalStep(0), IntervalStep(5)})) for s in results)

    # IntervalStep(0) を含まない available_harmonic_steps はエラー
    with pytest.raises(ValueError, match=re.escape("available_harmonic_steps must always contain IntervalStep(0)")):
        indexer.find(Q(SearchField.USED_HARMONIC_STEPS).is_subset_of(frozenset({IntervalStep(3)})))


def test_find_combined_and(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    condition = (
        Q(SearchField.NUM_NOTES_IN_MEASURE)
        .eq(3)
        .and_(Q(SearchField.FIRST_NOTE_INTERVAL_STEP).eq(IntervalStep(-1)))
        .and_(Q(SearchField.USED_HARMONIC_STEPS).is_subset_of(frozenset({IntervalStep(0), IntervalStep(2)})))
    )
    results = indexer.find(condition)
    assert len(results) == 1
    assert results[0].name() == "-1r,0,2|5"


def test_find_combined_or(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    condition = Q(SearchField.NUM_NOTES_IN_MEASURE).eq(4).or_(Q(SearchField.NEXT_MEASURE_STEP).eq(IntervalStep(1)))
    results = indexer.find(condition)
    assert len(results) == 2
    assert all(s.name() in ["0,3,5,3|4", "1r,0,2|1"] for s in results)


def test_find_complex_query(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))

    # (num_notes == 2 AND is_tied == True) OR (num_notes == 1 AND next_step == 2)
    condition = (
        Q(SearchField.NUM_NOTES_IN_MEASURE).eq(2).and_(Q(SearchField.IS_TIED_TO_NEXT_MEASURE_REQUIRED).eq(True))
    ).or_(Q(SearchField.NUM_NOTES_IN_MEASURE).eq(1).and_(Q(SearchField.NEXT_MEASURE_STEP).eq(IntervalStep(2))))
    results = indexer.find(condition)
    assert len(results) == 3
    assert all(s.name() in ["0,3|3", "0,2|2", "0|2"] for s in results)


def test_find_no_condition(sample_sequences: list[MeasureStepSequence]) -> None:
    indexer = MeasureStepSequenceIndexer(sample_sequences, list(MeasureRythmnPattern))
    results = indexer.find()
    assert len(results) == len(sample_sequences)

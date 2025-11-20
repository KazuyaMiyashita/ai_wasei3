import pytest

from my_project.counterpoint.measure_search.measure_search import MeasureSearch, MeasureSearchResult
from my_project.counterpoint.measure_search.measure_step_sequence_generator import generate
from my_project.counterpoint.model import MeasureRythmnPattern
from my_project.model import Key, NoteName, Pitch


@pytest.fixture
def measure_search() -> MeasureSearch:
    sequences = generate()
    return MeasureSearch(sequences, list(MeasureRythmnPattern))


def parse(s: str) -> MeasureSearchResult:
    return MeasureSearchResult.parse(s)


def test_search_returns_results(measure_search: MeasureSearch) -> None:
    results = measure_search.search(
        start_pitch=Pitch.parse("C4"),
        start_harmonic_pitch=Pitch.parse("C4"),
        next_measure_start_harmonic_pitch=Pitch.parse("E4"),
        harmonic_note_names=(NoteName.parse("C"), NoteName.parse("E"), NoteName.parse("G")),
        key=Key.parse("C Major"),
        measure_rythmn_patterns=(MeasureRythmnPattern.R_1,),
        pitch_range=(Pitch.parse("G3"), Pitch.parse("D5")),
    )

    assert parse("[C4(d=4) | E4]") in results


def test_search_returns_results_2(measure_search: MeasureSearch) -> None:
    results = measure_search.search(
        start_pitch=Pitch.parse("G4"),
        start_harmonic_pitch=Pitch.parse("G4"),
        next_measure_start_harmonic_pitch=Pitch.parse("B4"),
        harmonic_note_names=(NoteName.parse("C"), NoteName.parse("E"), NoteName.parse("G")),
        key=Key.parse("C Major"),
        measure_rythmn_patterns=(
            MeasureRythmnPattern.R_22,
            MeasureRythmnPattern.R_22t,
            MeasureRythmnPattern.R_244,
            MeasureRythmnPattern.R_4444,
        ),
        pitch_range=(Pitch.parse("G3"), Pitch.parse("D5")),
    )

    assert parse("[G4(d=2) A4(d=2, p) | B4]") in results
    assert parse("[G4(d=1) A4(d=1, p) B4(d=1, p) C5(d=1) | B4]") in results
    assert parse("[G4(d=2) C5(d=2) | B4]") in results
    assert parse("[G4(d=2) C5(d=2, tied) | C5]") in results


def test_search_returns_results_3(measure_search: MeasureSearch) -> None:
    results = measure_search.search(
        start_pitch=Pitch.parse("C5"),
        start_harmonic_pitch=Pitch.parse("B4"),
        next_measure_start_harmonic_pitch=Pitch.parse("C5"),
        harmonic_note_names=(NoteName.parse("G"), NoteName.parse("B"), NoteName.parse("D")),
        key=Key.parse("C Major"),
        measure_rythmn_patterns=(MeasureRythmnPattern.R_t22,),
        pitch_range=(Pitch.parse("G3"), Pitch.parse("D5")),
    )

    assert parse("[C5(d=2, r) B4(d=2) | C5]") in results

from my_project.counterpoint.model import AnnotatedMeasure
from my_project.model import IntervalStep


def validate(completed_measures: list[AnnotatedMeasure]) -> bool:
    return _validate_part_total_range(completed_measures) and True  # TODO


def _validate_part_total_range(completed_measures: list[AnnotatedMeasure]) -> bool:
    """
    各声部の音域は同一課題中において11度を越えてはならない。越えた場合 False

    順次進行が長く続く場合には例外として12度が認められるが、ここでは禁止としている。
    """
    all_pitches = [note.value for measure in completed_measures for note in measure.notes if note.value is not None]
    p_min = min(all_pitches, key=lambda p: p.num())
    p_max = max(all_pitches, key=lambda p: p.num())

    return (p_max - p_min).step() <= IntervalStep.idx_1(11)

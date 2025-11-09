from my_project.counterpoint.global_context import GlobalContext
from my_project.model import IntervalStep


def validate(global_ctx: GlobalContext) -> bool:
    return validate_part_total_range(global_ctx) and True  # TODO


def validate_part_total_range(global_ctx: GlobalContext) -> bool:
    """
    各声部の音域は同一課題中において11度を越えてはならない。越えた場合 False

    順次進行が長く続く場合には例外として12度が認められるが、ここでは禁止としている。
    """
    all_pitches = [
        an.note.pitch for m in global_ctx.completed_measures for an in m.annotated_notes if an.note.pitch is not None
    ]
    p_min = min(all_pitches, key=lambda p: p.num())
    p_max = max(all_pitches, key=lambda p: p.num())

    return (p_max - p_min).step() <= IntervalStep.idx_1(11)

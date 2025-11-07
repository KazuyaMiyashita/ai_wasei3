from dataclasses import replace

from my_project.counterpoint.context import LocalMeasureContext
from my_project.counterpoint.model import ToneType
from my_project.counterpoint.search_common import (
    available_harmonic_pitches_with_chord,
    is_valid_melodic_interval,
)
from my_project.counterpoint.util import make_annotated_note
from my_project.model import IntervalStep, Pitch


def next_ctxs(local_ctx: LocalMeasureContext) -> list[LocalMeasureContext]:
    """
    和声音を1音追加する LocalMeasureContext のリストを返す
    """
    next_pitch_and_chord_list: list[tuple[Pitch, bool | None]] = []

    if local_ctx.next_measure_mark is not None:
        cf_mark_step = (local_ctx.current_cf - local_ctx.next_measure_mark).normalize().step()
        if cf_mark_step == IntervalStep.idx_1(5):
            is_root_chord = True
        elif cf_mark_step == IntervalStep.idx_1(6):
            is_root_chord = False
        elif cf_mark_step in [IntervalStep.idx_1(1), IntervalStep.idx_1(3)]:
            is_root_chord = None
        else:
            raise ValueError(
                "invalid next_measure_mark. "
                f"current_cf: {local_ctx.current_cf}, next_measure_mark: {local_ctx.next_measure_mark}"
            )
        next_pitch_and_chord_list = [(local_ctx.next_measure_mark, is_root_chord)]
    else:
        # 和音上利用できる音の中で、前の音との旋律的音程が許されるもの
        previous_pitch = local_ctx.previous_latest_added_pitch()
        all_candidates: list[tuple[Pitch, bool | None]] = available_harmonic_pitches_with_chord(local_ctx)
        for next_pitch, next_is_root_chord in all_candidates:
            if is_valid_melodic_interval(local_ctx, next_pitch - previous_pitch):
                next_pitch_and_chord_list.append((next_pitch, next_is_root_chord))

    next_ctxs: list[LocalMeasureContext] = []
    for next_pitch, next_is_root_chord in next_pitch_and_chord_list:
        duration = local_ctx.rythmn_type.note_duration()
        new_local_ctx = replace(
            local_ctx,
            note_buffer=[
                *local_ctx.note_buffer,
                make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, duration),
            ],
            next_measure_mark=None,
            is_root_chord=next_is_root_chord,
        )
        next_ctxs.append(new_local_ctx)

    return next_ctxs

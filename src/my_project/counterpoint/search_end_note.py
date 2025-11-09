from dataclasses import replace

from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.model import ToneType
from my_project.counterpoint.search_common import end_available_pitches, is_valid_melodic_interval
from my_project.counterpoint.util import make_annotated_note
from my_project.model import Duration, Pitch


def next_ctxs(local_ctx: LocalMeasureContext) -> list[LocalMeasureContext]:
    """
    最終小節の和声音を選択する LocalMeasureContext のリストを返す
    """
    next_pitches: list[Pitch]
    if local_ctx.next_measure_mark is not None:
        next_pitches = [local_ctx.next_measure_mark]
    else:
        cf = local_ctx.current_cf
        next_pitches = end_available_pitches(local_ctx, cf)
        # 前の音との音程の確認
        previous_pitch = local_ctx.previous_latest_added_pitch()
        next_pitches = [p for p in next_pitches if is_valid_melodic_interval(local_ctx, p - previous_pitch)]

    next_ctxs: list[LocalMeasureContext] = []
    for next_pitch in next_pitches:
        new_local_ctx = replace(
            local_ctx,
            note_buffer=[
                # NOTE: RythmnType によらずこの音価は一定で全音符
                make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, Duration.of(4))
            ],
            next_measure_mark=None,
            is_root_chord=True,
        )
        next_ctxs.append(new_local_ctx)

    return next_ctxs

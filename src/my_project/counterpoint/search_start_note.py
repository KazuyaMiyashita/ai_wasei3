from dataclasses import replace

from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.model import AnnotatedNote, RythmnType, ToneType
from my_project.counterpoint.search_common import start_available_pitches
from my_project.counterpoint.util import make_annotated_note
from my_project.model import Pitch


def next_ctxs(local_ctx: LocalMeasureContext) -> list[LocalMeasureContext]:
    """
    課題冒頭の音を選択する LocalMeasureContext のリストを返す
    """
    cf = local_ctx.current_cf
    possible_pitches: list[Pitch] = start_available_pitches(local_ctx, cf)

    next_ctxs: list[LocalMeasureContext] = []
    for pitch in possible_pitches:
        duration = local_ctx.rythmn_type.note_duration()
        note_buffer: list[AnnotatedNote]
        match local_ctx.rythmn_type:
            case RythmnType.WHOLE_NOTE:
                # 全音符の場合は冒頭の休符はなく、音符だけ入れる
                note_buffer = [make_annotated_note(pitch, ToneType.HARMONIC_TONE, duration)]

            case _:
                # その他の場合は休符と音符を入れる
                note_buffer = [
                    make_annotated_note(None, ToneType.HARMONIC_TONE, duration),
                    make_annotated_note(pitch, ToneType.HARMONIC_TONE, duration),
                ]
        new_local_ctx = replace(
            local_ctx,
            note_buffer=note_buffer,
            next_measure_mark=None,
            is_root_chord=True,
        )
        next_ctxs.append(new_local_ctx)

    return next_ctxs

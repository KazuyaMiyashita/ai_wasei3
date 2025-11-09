from dataclasses import replace

from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.model import (
    KEY,
    RythmnType,
    ToneType,
)
from my_project.counterpoint.search_common import AVAILABLE_PITCHES_SET, available_pitches, end_available_pitches
from my_project.counterpoint.util import make_annotated_note
from my_project.model import (
    IntervalStep,
    Offset,
    Pitch,
)
from my_project.util import add_interval_step_in_key


def next_ctxs(local_ctx: LocalMeasureContext) -> list[LocalMeasureContext]:
    """
    刺繍音を利用する。
    note_bufferに1つの NEIGHBOR_TONE と1つの HARMONIC_TONE を追加する。
    必要に応じて next_measure_mark に値が設定される。
    """
    # 最終小節や、小節の1拍目(課題の冒頭を含む)では利用できない。
    if local_ctx.is_last_measure or local_ctx.current_offset() == Offset.of(0):
        return []
    # マークがある場合は非和声音を利用できない
    if local_ctx.next_measure_mark is not None:
        return []
    # 最終小節ではないので次の小節のCFは必ず取得できる
    assert local_ctx.next_measure_cf is not None

    next_ctxs: list[LocalMeasureContext] = []

    # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
    if _is_target_note_in_current_measure(local_ctx):
        # 小節を跨がない場合は、直前に追加した音が和声音であるため、音域内であれば利用可能
        for neighbor_note_pitch in _available_neighbor_note_pitches(local_ctx):
            previous_pitch = local_ctx.previous_latest_added_pitch()
            duration = local_ctx.rythmn_type.note_duration()
            new_local_ctx = replace(
                local_ctx,
                note_buffer=[
                    *local_ctx.note_buffer,
                    make_annotated_note(neighbor_note_pitch, ToneType.NEIGHBOR_TONE, duration),
                    make_annotated_note(previous_pitch, ToneType.HARMONIC_TONE, duration),
                ],
                next_measure_mark=None,
            )
            next_ctxs.append(new_local_ctx)
    else:
        # 小節を跨ぐ場合、最終小節かどうかに応じて利用できる音高を求め、その中に直前の音が含まれるかを確認する
        if local_ctx.is_next_last_measure:
            a_pitches = set(end_available_pitches(local_ctx, local_ctx.next_measure_cf))
        else:
            a_pitches = set(available_pitches(local_ctx, local_ctx.next_measure_cf))

        previous_pitch = local_ctx.previous_latest_added_pitch()
        if previous_pitch in a_pitches:
            for neighbor_note_pitch in _available_neighbor_note_pitches(local_ctx):
                new_local_ctx = replace(
                    local_ctx,
                    note_buffer=[
                        *local_ctx.note_buffer,
                        make_annotated_note(
                            neighbor_note_pitch,
                            ToneType.NEIGHBOR_TONE,
                            local_ctx.rythmn_type.note_duration(),
                        ),
                    ],
                    next_measure_mark=previous_pitch,
                )
                next_ctxs.append(new_local_ctx)

    return next_ctxs


def _available_neighbor_note_pitches(local_ctx: LocalMeasureContext) -> list[Pitch]:
    """
    直前の音をもとに、音域内で利用できる刺繍音の一覧を返す。
    2度上・2度下
    """
    previous_latest_added_pitch = local_ctx.previous_latest_added_pitch()
    neighbor_steps = [IntervalStep.idx_1(2), IntervalStep.idx_1(-2)]
    result: list[Pitch] = []
    for step in neighbor_steps:
        neighbor_pitch = add_interval_step_in_key(KEY, previous_latest_added_pitch, step)
        if neighbor_pitch in AVAILABLE_PITCHES_SET:  # 声域内か
            result.append(neighbor_pitch)
    return result


def _is_target_note_in_current_measure(local_ctx: LocalMeasureContext) -> bool:
    """
    現在のオフセットに応じて、刺繍音を利用した時の最後の音が現在の小節に含まれるか(小節を跨いでいないか)どうかを返す
    1拍目からは刺繍音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる。
    その他リズムパターンに含まれないオフセットを渡すと例外となる。
    """
    current_offset = local_ctx.current_offset()
    match local_ctx.rythmn_type:
        case RythmnType.QUATER_NOTE:
            if current_offset in [Offset.idx_1(2), Offset.idx_1(3)]:
                # 2拍目の探索中は現在の小節の3拍目に到達する
                # 3拍目の探索中は現在の小節の4拍目に到達する
                return True
            elif current_offset == Offset.idx_1(4):
                # 4拍目の探索中は次の小節の1拍目に到達する
                return False
            else:
                raise RuntimeError(f"invalid current_offset: {current_offset}")
        case RythmnType.HALF_NOTE:
            if current_offset == Offset.idx_1(3):
                return False
            else:
                raise RuntimeError(f"invalid current_offset: {current_offset}")
        case RythmnType.WHOLE_NOTE:
            # 全音符では刺繍音は利用できない。 def next_ctxs で弾かれてこのメソッドは呼ばれない
            raise RuntimeError(f"invalid current_offset: {current_offset}")

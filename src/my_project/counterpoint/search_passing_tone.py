from dataclasses import replace

from my_project.counterpoint.context import LocalMeasureContext
from my_project.counterpoint.model import (
    KEY,
    RythmnType,
    ToneType,
)
from my_project.counterpoint.search_common import (
    available_harmonic_pitches_with_chord,
    available_pitches,
    end_available_pitches,
)
from my_project.counterpoint.util import make_annotated_note
from my_project.model import (
    IntervalStep,
    Key,
    Offset,
    Pitch,
)
from my_project.util import add_interval_step_in_key


def next_ctxs(local_ctx: LocalMeasureContext) -> list[LocalMeasureContext]:
    """
    経過音を利用する。
    note_bufferに1つ以上の PASSING_TONE と1つの HARMONIC_TONE を追加する。
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

    # 直前の音から目標音までの音程(上向きのみ), 到達音が現在の小節に含まれるかどうか(小節を跨がないか)の一覧を求める
    patterns: list[tuple[IntervalStep, bool]] = progression_pattern(
        current_offset=local_ctx.current_offset(), rythmn_type=local_ctx.rythmn_type
    )

    next_ctxs: list[LocalMeasureContext] = []
    for step, is_target_note_in_current_number in patterns:
        # 到達する音高を求める
        target_pitch = add_interval_step_in_key(KEY, local_ctx.previous_latest_added_pitch(), step)

        # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
        if is_target_note_in_current_number:
            # 小節を跨がない場合

            # 小節を跨がない場合、到達した音は課題の冒頭の音または最終小節以外の音である。
            # それらの利用できる音を求める
            available_pitches_and_next_chord = available_harmonic_pitches_with_chord(local_ctx)

            for available_pitch, is_next_root_chord in available_pitches_and_next_chord:
                if target_pitch != available_pitch:
                    continue

                duration = local_ctx.rythmn_type.note_duration()
                pitches = conjunct_pitches(KEY, local_ctx.previous_latest_added_pitch(), step)
                init_notes = [make_annotated_note(p, ToneType.PASSING_TONE, duration) for p in pitches[:-1]]
                last_note = make_annotated_note(pitches[-1], ToneType.HARMONIC_TONE, duration)
                new_local_ctx = replace(
                    local_ctx,
                    note_buffer=[*local_ctx.note_buffer, *init_notes, last_note],
                    next_measure_mark=None,
                    is_root_chord=is_next_root_chord,
                )
                next_ctxs.append(new_local_ctx)
            pass
        else:
            # 小節を跨ぐ場合

            # この小節の和音の音かどうかは気にしなくて良い。
            # 次の小節が最終小節かどうかに応じて利用できる音高が異なる。
            assert local_ctx.next_measure_cf is not None
            if local_ctx.is_next_last_measure:
                a_pitches = end_available_pitches(local_ctx, local_ctx.next_measure_cf)
            else:
                a_pitches = available_pitches(local_ctx, local_ctx.next_measure_cf)

            for available_pitch in a_pitches:
                if target_pitch != available_pitch:
                    continue

                pitches = conjunct_pitches(KEY, local_ctx.previous_latest_added_pitch(), step)
                notes_to_add_buffer = [
                    make_annotated_note(p, ToneType.PASSING_TONE, local_ctx.rythmn_type.note_duration())
                    for p in pitches[:-1]
                ]
                next_measure_mark = pitches[-1]

                new_local_ctx = replace(
                    local_ctx,
                    note_buffer=[*local_ctx.note_buffer, *notes_to_add_buffer],
                    next_measure_mark=next_measure_mark,
                )
                next_ctxs.append(new_local_ctx)

    return next_ctxs


def conjunct_pitches(key: Key, pitch: Pitch, interval_step: IntervalStep) -> list[Pitch]:
    """
    順次進行の音高列を返す。
    指定した key で、指定された pitch に対し、そこから interval_step 分離れた音まで順次進行した時の音高の列を返す。
    指定した pitch は結果に含まれない。

    例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(3) -> [D4, E4]
    例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(-4) -> [B3, A3, G3]
    例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(1) -> []
    """

    steps: list[IntervalStep]
    if interval_step == IntervalStep(0):
        steps = []
    elif interval_step > IntervalStep(0):
        steps = [IntervalStep(v) for v in range(1, interval_step.value + 1)]
    else:
        steps = [IntervalStep(v) for v in range(-1, interval_step.value - 1, -1)]

    return [add_interval_step_in_key(key, pitch, step) for step in steps]


def progression_pattern(current_offset: Offset, rythmn_type: RythmnType) -> list[tuple[IntervalStep, bool]]:
    """
    現在のオフセットに応じて、
    直前の音から目標音までのIntervalStepと、到達した音が現在の小節に含まれるか(小節を跨いでいないか)どうかの一覧を返す
    1拍目からは経過音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる
    その他リズムパターンに含まれないオフセットを渡すと例外となる。
    """
    patterns: list[tuple[IntervalStep, bool]] = []

    match rythmn_type:
        case RythmnType.QUATER_NOTE:
            if current_offset == Offset.idx_1(2):
                # 2拍目の探索中は、3拍目・4拍目・次の小節の1拍目に向けて経過音が利用できる。
                patterns = [
                    (IntervalStep.idx_1(3), True),
                    (IntervalStep.idx_1(4), True),
                    (IntervalStep.idx_1(5), False),
                ]
            elif current_offset == Offset.idx_1(3):
                # 3拍目の探索中は、4拍目・次の小節の1拍目に向けて経過音が利用できる。
                patterns = [
                    (IntervalStep.idx_1(3), True),
                    (IntervalStep.idx_1(4), False),
                ]
            elif current_offset == Offset.idx_1(4):
                # 4拍目の探索中は、次の小節の1拍目に向けて経過音が利用できる。
                patterns = [
                    (IntervalStep.idx_1(3), False),
                ]
            else:
                raise RuntimeError(f"invalid current_offset: {current_offset}")
        case RythmnType.HALF_NOTE:
            if current_offset == Offset.idx_1(3):
                # 3拍目の探索中は、次の小節の1拍目に向けて経過音が利用できる。
                patterns = [
                    (IntervalStep.idx_1(3), False),
                ]
            else:
                raise RuntimeError(f"invalid current_offset: {current_offset}")

    # パターンに下向きの音程を追加
    patterns = [*patterns, *[(p[0] * -1, p[1]) for p in patterns]]
    return patterns

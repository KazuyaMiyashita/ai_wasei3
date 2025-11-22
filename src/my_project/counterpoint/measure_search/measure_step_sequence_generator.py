import logging
from dataclasses import dataclass
from enum import Enum, auto

from tqdm import tqdm

from my_project.counterpoint.measure_search.measure_step_sequence import AnnotatedIntervalStep, MeasureStepSequence
from my_project.counterpoint.model import ToneType
from my_project.model import Duration, IntervalStep, Melody, Note

logger = logging.getLogger(__name__)


class HarmonicSteps(Enum):
    """
    小節の最初の音(掛留音の場合は解決音)をIntervalStep.idx_1(1)とした時、
    和声音はその音に対して何度上のものを利用するかということを表す
    """

    S_1_3_5 = auto()
    S_1_3_6 = auto()
    S_1_4_6 = auto()

    def interval_steps(self) -> set[IntervalStep]:
        """
        協和音として利用できる音程。
        ここに含まれる音のほかにも、それらのオクターブ違いのものも利用できる。
        そのため、利用できるかどうかは確認したい音をユニゾン~7度までの範囲に正規化(inversion_normalized)した上で調べる必要がある。
        """
        step_values: list[int] = []
        match self:
            case HarmonicSteps.S_1_3_5:
                step_values = [1, 3, 5]
            case HarmonicSteps.S_1_3_6:
                step_values = [1, 3, 6]
            case HarmonicSteps.S_1_4_6:
                step_values = [1, 4, 6]
        return set([IntervalStep.idx_1(i) for i in step_values])


@dataclass(frozen=True)
class _ExplorationState:
    melody: list[AnnotatedIntervalStep]
    is_suspension_unresolved: bool
    used_harmonic_steps: set[IntervalStep]


def generate() -> list[MeasureStepSequence]:
    logger.info("MeasureStepSequenceの生成を開始します")
    """
    以下の規則を満たす MeasureStepSequence を生成する。

    和声音に関する規則:
    - 和声音として、 IntervalStep(0) が必ず利用される。
    - その他の和声音は IntervalStep の組み合わせ [0, 2, 4], [0, 2, 5], [0, 3, 5] のいずれかから部分的に選ばれる。
    - 和声音は異なる高さの和声音に跳躍して進行することができる。

    非和声音に関する規則:
    - 掛留音は小節内のいずれかの位置で IntervalStep(0) に解決する。
    - 経過音は和声音とその2つ以上後の和声音の間を順次進行で埋める形で利用される。後者の音は次の小節の音でも良い。
    - 刺繍音は和声音とその2つ後の同じ高さの音を順次進行で埋める形で利用される。後者の音は次の小節の音でも良い。

    旋律に関する規則:
    - 分散和音(例: [0, 2, 4])の音形は利用しない。ただし反転分散和音(例: [0, 4, 2])は長さ3までのものは利用する。
    - 同一音への3度の回帰(例: [0, 1, 0, -1 | 0])は利用しない。
    - 跳躍を伴う隣接2音の反復(例: [0, 2, 0, 2])は利用しない
    - 小節をまたぐ時、同一方向への跳躍(例 [0, 1, 2, 3 | 6]) は利用しない。
    """

    # 探索中に生成された、長さが1〜4音の全ての旋律パターンを格納する
    intermediate_melodies: list[list[AnnotatedIntervalStep]] = []

    # 1〜4音の各長さのメロディを生成する
    for max_len in tqdm(range(1, 5), desc="Generating measure step sequences"):
        # 探索の起点となる初期状態のリスト
        initial_states: list[_ExplorationState] = [
            _ExplorationState(
                melody=[_create_step(IntervalStep(0), ToneType.HARMONIC_TONE)],
                is_suspension_unresolved=False,
                used_harmonic_steps={IntervalStep.idx_1(1)},
            ),
            _ExplorationState(
                melody=[_create_step(IntervalStep(1), ToneType.SUSPENDED_TONE)],
                is_suspension_unresolved=True,
                used_harmonic_steps=set(),
            ),
            _ExplorationState(
                melody=[_create_step(IntervalStep(-1), ToneType.SUSPENDED_TONE)],
                is_suspension_unresolved=True,
                used_harmonic_steps=set(),
            ),
        ]

        # 各初期状態から探索を開始

        for state in initial_states:
            intermediate_melodies.extend([s.melody for s in _explore_melodies_recursive(state, max_len)])

    # 各旋律候補から次の小節の音を求め、最終的な結果を生成
    all_patterns: list[MeasureStepSequence] = []
    for melody in intermediate_melodies:
        next_options = _generate_next_measure_step(melody)
        for next_measure_step in next_options:
            all_patterns.append(
                MeasureStepSequence(
                    measure=Melody.of(*melody),
                    next_measure_step=next_measure_step,
                )
            )

    # 共通フィルタを適用
    filtered_patterns = filter(_is_valid_pattern, all_patterns)

    # 重複を除外して返す
    unique_results = sorted(list(set(filtered_patterns)), key=lambda r: r.name())
    logger.info(f"MeasureStepSequenceの生成を完了しました。{len(unique_results)}個のユニークなパターンを生成しました。")
    return unique_results


def _explore_melodies_recursive(
    state: _ExplorationState,
    max_melody_length: int,
) -> list[_ExplorationState]:
    # 目的: 現在の状態から、操作によって伸長可能な次の状態を再帰的に探索し、結果をリストで返す

    assert len(state.melody) <= max_melody_length, f"Melody length exceeded max length: {state.melody}"

    # 終了条件: 旋律が指定の長さに達したら、その状態を結果として返す
    if len(state.melody) == max_melody_length:
        # ただし、掛留音が未解決のまま終了するのは不適切
        if state.is_suspension_unresolved:
            return []
        return [state]

    if state.is_suspension_unresolved:
        # 掛留音が未解決の場合、解決を試みる
        return [
            next_state
            for next_state_option in _op_resolve_suspension(state, max_melody_length)
            for next_state in _explore_melodies_recursive(next_state_option, max_melody_length)
        ]
    else:
        # 通常の操作 (掛留音が解決済み、または元々ない場合)
        results: list[_ExplorationState] = []
        for next_state_option in _op_add_harmonic_tone(state, max_melody_length):
            results.extend(_explore_melodies_recursive(next_state_option, max_melody_length))
        for next_state_option in _op_add_passing_tones(state, max_melody_length):
            results.extend(_explore_melodies_recursive(next_state_option, max_melody_length))
        for next_state_option in _op_add_neighbor_tone(state, max_melody_length):
            results.extend(_explore_melodies_recursive(next_state_option, max_melody_length))
        return results


# --- 操作関数 ---


def _op_resolve_suspension(
    state: _ExplorationState,
    max_melody_length: int,
) -> list[_ExplorationState]:
    # 掛留音を解決するパターンを生成する。
    if not state.melody:
        raise ValueError("Melody cannot be empty when resolving suspension.")

    results: list[_ExplorationState] = []
    all_harmonic_patterns = [h.interval_steps() for h in HarmonicSteps]
    start_note = state.melody[0]
    resolve_step = IntervalStep(0)

    # 1. 基本的な解決 (1r, 0H)
    if len(state.melody) <= max_melody_length - 1:
        new_used_steps_simple = state.used_harmonic_steps | {resolve_step}
        if any(new_used_steps_simple.issubset(p) for p in all_harmonic_patterns):
            new_melody = [*state.melody, _create_step(resolve_step, ToneType.HARMONIC_TONE)]
            results.append(_ExplorationState(new_melody, False, new_used_steps_simple))

    # 2. 解決の間に和声音に進行するパターン (1r, 4srh, 0H)
    if len(state.melody) <= max_melody_length - 2:
        for degree in [2, 3, 4, 5]:
            intermediate_step = IntervalStep(degree)
            # TODO: 利用した和声音に記録するが、srhとしてアノテーションするのが統一感が無い感じがする。
            new_used_steps_jump = state.used_harmonic_steps | {resolve_step, intermediate_step.inversion_normalized()}

            if any(new_used_steps_jump.issubset(p) for p in all_harmonic_patterns):
                new_melody = [
                    *state.melody,
                    _create_step(intermediate_step, ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE),
                    _create_step(resolve_step, ToneType.HARMONIC_TONE),
                ]
                results.append(_ExplorationState(new_melody, False, new_used_steps_jump))

    # 3. 特殊なパターン (1r, 0srh, -1br, 0H)
    # TODO: -1r, -2?, -1?, 0H を含めるか？ ([A Minor, I, G#(d=1), F#(d=1/2), G#(d=1/2), A(d=2)])
    if len(state.melody) <= max_melody_length - 3 and start_note.value == IntervalStep(1):
        new_used_steps_special = state.used_harmonic_steps | {resolve_step}
        if any(new_used_steps_special.issubset(p) for p in all_harmonic_patterns):
            new_melody = [
                *state.melody,
                _create_step(IntervalStep(0), ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE),
                # 音楽的にも、Degreeの判断の都合上も刺繍音とするのがちょうど良い。
                _create_step(IntervalStep(-1), ToneType.NEIGHBOR_TONE),
                _create_step(resolve_step, ToneType.HARMONIC_TONE),
            ]
            results.append(_ExplorationState(new_melody, False, new_used_steps_special))

    return results


def _op_add_harmonic_tone(
    state: _ExplorationState,
    max_melody_length: int,
) -> list[_ExplorationState]:
    # 現在の旋律に、有効な和声音を1つ追加した新しい旋律のリストを返す
    if not state.melody or len(state.melody) >= max_melody_length:
        raise ValueError("Melody cannot be empty or full when adding harmonic tone.")
    last_note = state.melody[-1]
    results: list[_ExplorationState] = []

    leap_degrees = [2, 3, 4, 5, 7, -2, -3, -4, -5, -7]
    candidate_steps: set[IntervalStep] = set()
    for degree in leap_degrees:
        candidate_steps.add(last_note.value + IntervalStep(degree))

    for step in sorted(list(candidate_steps)):
        if step == last_note.value:
            continue

        new_used_steps = state.used_harmonic_steps | {step.inversion_normalized()}
        if not any(new_used_steps.issubset(p) for p in [h.interval_steps() for h in HarmonicSteps]):
            continue

        new_melody = [*state.melody, _create_step(step, ToneType.HARMONIC_TONE)]
        results.append(_ExplorationState(new_melody, False, new_used_steps))
    return results


def _op_add_passing_tones(
    state: _ExplorationState,
    max_melody_length: int,
) -> list[_ExplorationState]:
    # 現在の旋律に、経過音(群)とそれを解決する和声音を追加した新しい旋律のリストを返す
    if not state.melody:
        return []
    last_note = state.melody[-1]
    results: list[_ExplorationState] = []

    for direction in [1, -1]:
        # 1音から3音までの経過音を試す
        for num_passing_tones in range(1, 4):
            # --- 経過音(群)とそれを解決する和声音を追加するパターン ---
            if len(state.melody) + num_passing_tones < max_melody_length:
                passing_notes: list[AnnotatedIntervalStep] = []
                for i in range(1, num_passing_tones + 1):
                    step = last_note.value + IntervalStep(i * direction)
                    passing_notes.append(_create_step(step, ToneType.PASSING_TONE))

                target_step = last_note.value + IntervalStep((num_passing_tones + 1) * direction)
                new_used_steps = state.used_harmonic_steps | {target_step.inversion_normalized()}

                if any(new_used_steps.issubset(p) for p in [h.interval_steps() for h in HarmonicSteps]):
                    new_melody = [
                        *state.melody,
                        *passing_notes,
                        _create_step(target_step, ToneType.HARMONIC_TONE),
                    ]
                    results.append(_ExplorationState(new_melody, False, new_used_steps))

            # --- 経過音(群)が小節の最後まで続くパターン ---
            if len(state.melody) + num_passing_tones == max_melody_length:
                passing_notes = []
                for i in range(1, num_passing_tones + 1):
                    step = last_note.value + IntervalStep(i * direction)
                    passing_notes.append(_create_step(step, ToneType.PASSING_TONE))

                new_melody = [*state.melody, *passing_notes]
                results.append(_ExplorationState(new_melody, False, state.used_harmonic_steps))

    return results


def _op_add_neighbor_tone(
    state: _ExplorationState,
    max_melody_length: int,
) -> list[_ExplorationState]:
    # 現在の旋律に、刺繍音とそれを解決する和声音を追加した新しい旋律のリストを返す
    if not state.melody or len(state.melody) > max_melody_length - 2:
        return []
    last_note = state.melody[-1]
    results: list[_ExplorationState] = []

    for direction in [1, -1]:
        br_step = last_note.value + IntervalStep(direction)
        h_step = last_note.value

        new_melody = [
            *state.melody,
            _create_step(br_step, ToneType.NEIGHBOR_TONE),
            _create_step(h_step, ToneType.HARMONIC_TONE),
        ]
        results.append(_ExplorationState(new_melody, False, state.used_harmonic_steps))
    return results


# -- 小節が埋まってから実行する系


def _generate_next_measure_step(
    melody: list[AnnotatedIntervalStep],
) -> list[IntervalStep]:
    """
    現在の旋律の音から、次の小節の音とタイの有無の可能な組み合わせを生成する。
    """
    last_step = melody[-1]
    mext_steps: list[IntervalStep] = []

    match last_step.attribute:
        case ToneType.HARMONIC_TONE:
            # 和声音で終わる場合の処理

            # タイありパターン
            mext_steps.append(last_step.value)

            # タイ無しパターン
            if len(melody) == 1:
                # 1音のみの場合は上下2, 3, 4, 5, 6, 8度への跳躍が可能
                leap_degrees = [2, 3, 4, 5, 6, 8, -2, -3, -4, -5, -6, -8]
                for degree in leap_degrees:
                    next_step = last_step.value + IntervalStep.idx_1(degree)
                    mext_steps.append(next_step)
            else:  # len(melody) >= 2
                # NOTE: 簡単に記述するために多少簡易な規則としている
                second_to_last_step = melody[-2]
                diff = last_step.value - second_to_last_step.value
                direction = 1 if diff.to_idx_1() > 0 else -1

                # 同方向に2度進行
                mext_steps.append(last_step.value + IntervalStep.idx_1(2 * direction))

                # 逆方向に2,3,4,5,6,8度進行
                opposite_direction_degrees = [2, 3, 4, 5, 6, 8]
                for degree in opposite_direction_degrees:
                    next_step = last_step.value + IntervalStep.idx_1(degree * -direction)
                    mext_steps.append(next_step)

        case ToneType.PASSING_TONE:
            # 経過音で終わる場合は、順次進行で解決する
            if len(melody) < 2:
                raise ValueError("Passing tone must be preceded by another note")
            second_to_last_step = melody[-2]
            diff = last_step.value - second_to_last_step.value
            next_step = last_step.value + diff
            mext_steps.append(next_step)

        case ToneType.NEIGHBOR_TONE:
            # 刺繍音で終わる場合は、元の音に戻る
            if len(melody) < 2:
                raise ValueError("Neighbor tone must be preceded by another note")
            second_to_last_step = melody[-2]
            diff = last_step.value - second_to_last_step.value
            next_step = last_step.value + (diff * -1)
            mext_steps.append(next_step)

        case _:
            # 掛留音で終わる場合は例外。探索中に解決されるはず
            raise ValueError(f"tone_type: {last_step.attribute} found in the end of melody: {melody}")

    return mext_steps


def _is_valid_pattern(p: MeasureStepSequence) -> bool:
    # 音域チェックなど、全てのパターンに共通するフィルタを適用する
    all_steps = [s.value for s in p.measure.notes] + [p.next_measure_step]
    min_idx = min(all_steps)
    max_idx = max(all_steps)
    if max_idx - min_idx <= IntervalStep.idx_1(11):
        return True
    return False


# -- ユーティリティ


def _create_step(interval_step: IntervalStep, tone_type: ToneType) -> AnnotatedIntervalStep:
    return Note(interval_step, Duration.of(1), tone_type)


# -- デバッグ用の全出力


def main() -> None:
    results = generate()
    for result in results:
        print(result.name())


if __name__ == "__main__":
    main()

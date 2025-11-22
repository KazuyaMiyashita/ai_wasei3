import itertools
from collections import defaultdict
from typing import cast

from my_project.counterpoint.measure_search.measure_step_sequence import (
    AbstractMeasureStepSequence,
    MeasureStepSequence,
)
from my_project.counterpoint.model import ToneType
from my_project.model import (
    Degree,
    DegreeAlter,
    DegreeStep,
    Interval,
    IntervalStep,
    Key,
    Melody,
    Mode,
    Note,
    Pitch,
)


def apply_pitch_candidates(
    key: Key,
    chord_degrees: set[Degree],
    start_pitch: Pitch,
    measure_step_sequence: MeasureStepSequence,
) -> list[AbstractMeasureStepSequence[Pitch]]:
    """
    調・和音・開始音と、与えられた音列(IntervalStepとToneTypeを持つ)に応じて、短音階の変位音を考慮して音高列の候補を得る。

    音列によって、結果は0~2個となる。
    長調や、短調でvi,viiを含まない場合は結果は1つとなる。
    vi,viiを含む短調の場合は結果が2個になる場合や、不適当と判断されて結果が0個になることがある。
    """
    annotated_interval_steps = [
        (n.value, n.attribute) for n in measure_step_sequence.measure.notes if n.value is not None
    ]

    # start_pitch を元に Pitch の一覧を作成
    all_interval_steps = [ais[0] for ais in annotated_interval_steps] + [measure_step_sequence.next_measure_step]
    all_diatonic_pitches = _apply_pitch_diatonic(key, start_pitch, all_interval_steps)
    diatonic_pitches = all_diatonic_pitches[:-1]
    next_diatonic_pitch = all_diatonic_pitches[-1]

    # DegreeStep への変換

    degree_steps: list[DegreeStep] = [Degree.from_note_name_key(p.note_name, key).step for p in diatonic_pitches]
    next_measure_degree_step = Degree.from_note_name_key(next_diatonic_pitch.note_name, key).step

    SIXTH = DegreeStep.idx_1(6)
    SEVENTH = DegreeStep.idx_1(7)

    # 長調や、短調で小節内および次の音にvi,viiを含まない場合は結果は明らかなので先に返す
    if (key.mode == Mode.MAJOR) or (not (set([*degree_steps, next_measure_degree_step]) & {SIXTH, SEVENTH})):
        new_measure_notes: list[Note[Pitch, ToneType]] = []
        pitch_idx = 0
        for note in measure_step_sequence.measure.notes:
            if note.value is not None:
                new_measure_notes.append(Note(diatonic_pitches[pitch_idx], note.duration, note.attribute))
                pitch_idx += 1
            else:
                new_measure_notes.append(Note(cast(Pitch | None, None), note.duration, note.attribute))

        return [AbstractMeasureStepSequence(Melody.of(*new_measure_notes), next_diatonic_pitch)]

    degree_iter = iter(degree_steps)
    measure_degrees = measure_step_sequence.measure.map_elements(lambda n: n.map_value(lambda _: next(degree_iter)))
    measure_degree_step_sequence = AbstractMeasureStepSequence(measure_degrees, next_measure_degree_step)

    # 2. Degreeの候補を取得
    possible_degree_sequences = _degree_candidates(
        key.mode,
        chord_degrees,
        measure_degree_step_sequence,
    )

    # 3. Degree列をPitch列に変換
    result_measure_pitch_sequences: list[AbstractMeasureStepSequence[Pitch]] = []

    for m_degree_sequence in possible_degree_sequences:
        pitches: list[Pitch] = []
        pitch_idx = 0
        for d_note in m_degree_sequence.measure.notes:
            deg = d_note.value

            # diatonic_pitches は小節内の音に対応
            base_pitch = diatonic_pitches[pitch_idx]
            # DegreeのAlter（#やb）分だけ、Pitchをずらす
            adjusted_pitch = base_pitch + Interval.A1 * deg.alter.value
            pitches.append(adjusted_pitch)
            pitch_idx += 1

        # 次の小節の開始音のPitchを決定
        next_degree_for_pitch = m_degree_sequence.next_measure_step  # Degree型

        # next_diatonic_pitch は次の小節のIntervalStepに対応するdiatonic_pitch
        # next_degree_for_pitch の DegreeAlter と next_diatonic_pitch を組み合わせて Pitch を作る
        next_pitch_candidate = next_diatonic_pitch + Interval.A1 * next_degree_for_pitch.alter.value

        # Melody[Note[Pitch | None, ToneType]] を作成
        pitches_iter = iter(pitches)
        new_melody = measure_step_sequence.measure.map_elements(lambda n: n.map_value(lambda _: next(pitches_iter)))
        result_measure_pitch_sequences.append(AbstractMeasureStepSequence(new_melody, next_pitch_candidate))

    return result_measure_pitch_sequences


def _degree_candidates(
    mode: Mode,
    chord_degrees: set[Degree],
    measure_degree_step_sequence: AbstractMeasureStepSequence[DegreeStep],
) -> list[AbstractMeasureStepSequence[Degree]]:
    """
    調・和音に応じて、一小節および次の音から小節の音列のDegreeを判断する。

    音列によって、結果は0~2個となる。
    長調や、短調でvi,viiを含まない場合は結果は1つとなる。
    vi,viiを含む短調の場合は結果が2個になる場合や、不適当と判断されて結果が0個になることがある。
    """
    measure_degrees: Melody[Note[DegreeStep, ToneType]] = measure_degree_step_sequence.measure
    next_measure_degree_step: DegreeStep = measure_degree_step_sequence.next_measure_step
    # elems を復元
    elems = [(n.value, n.attribute) for n in measure_degrees.notes if n.value is not None]
    degree_steps: tuple[DegreeStep, ...]
    degree_steps, _ = zip(*elems)

    # 長調や、短調で小節内および次の音にvi,viiを含まない場合は結果は明らかなので先に返す
    SIXTH = DegreeStep.idx_1(6)
    SEVENTH = DegreeStep.idx_1(7)

    if (mode == Mode.MAJOR) or (not (set([*degree_steps, next_measure_degree_step]) & {SIXTH, SEVENTH})):
        return [
            AbstractMeasureStepSequence(
                measure_degree_step_sequence.measure.map_elements(
                    lambda n: n.map_value(lambda v: Degree(v, DegreeAlter(0)))
                ),
                Degree(measure_degree_step_sequence.next_measure_step, DegreeAlter(0)),
            )
        ]
    # 共通の候補を事前に計算
    base_candidates: dict[int, list[Degree]] = defaultdict(list)

    for i, elem in enumerate(elems):
        degree_step, tone_type = elem
        if degree_step not in {SIXTH, SEVENTH}:
            base_candidates[i] = [Degree(degree_step, DegreeAlter(0))]
            continue

        # 以下、 6 または 7 が含まれている場合

        if tone_type in [ToneType.HARMONIC_TONE, ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE]:
            # 和声音と、掛留音が解決する際に経由する和声音。
            #
            # これらの場合、和音(Chord[Degree])の中にDegreeStepが一致するものを選ぶ。
            degrees = [d for d in chord_degrees if d.step == degree_step]
            # 和音の中に同一DegreeStepのDegreeが重複しないことや、和音外の音を和声音として選んでいないことを
            # 仮定すると要素は一つになる。ここでは要素数の検証は行なっていない。
            base_candidates[i] = degrees

        elif tone_type == ToneType.PASSING_TONE:
            # 経過音は、和声音の中で vi, vii がどのDegreeAlterで用いられているかで候補が絞られる。
            #
            # やりたいこととしては次のようなことである:
            # - 和音に 本位の vi または本位の vii が含まれる場合: [F, G] を採用
            # - 上方変位の vi または 上方変位の vii が含まれる場合: [F#, G#] を採用
            # - 和音に vi, vii のいずれも含まれない場合は
            #   - [F, G] か [F#, G#] を採用
            #   - [F, G#] や [F#, G] は不採用
            #
            # しかしこれを一音ずつの処理と相性が悪いため、
            # このループ処理では [F, G#] や [F#, G] といった結果を含めている
            if {Degree.idx_1(6, 0), Degree.idx_1(7, 0)} & chord_degrees:
                base_candidates[i] = [Degree(degree_step, DegreeAlter(0))]
            elif {Degree.idx_1(6, 1), Degree.idx_1(7, 1)} & chord_degrees:
                base_candidates[i] = [Degree(degree_step, DegreeAlter(1))]
            else:
                base_candidates[i] = [Degree(degree_step, DegreeAlter(0)), Degree(degree_step, DegreeAlter(1))]

        elif tone_type == ToneType.SUSPENDED_TONE:
            # 掛留音はそれが解決した音が何かによって定まる
            resolve_degree_step: DegreeStep = next(elem[0] for elem in elems[i:] if elem[1] == ToneType.HARMONIC_TONE)
            if (degree_step == SEVENTH) and (resolve_degree_step == DegreeStep.idx_1(1)):
                base_candidates[i] = [Degree(degree_step, DegreeAlter(1))]
            elif (degree_step == SEVENTH) and (resolve_degree_step == SIXTH):
                base_candidates[i] = [Degree(degree_step, DegreeAlter(0))]
            elif (degree_step == SEVENTH) and (resolve_degree_step == SIXTH):
                # 7r->6 (短調で自然短音階の7度が6度に解決)
                base_candidates[i] = [Degree(degree_step, DegreeAlter(0))]
            elif (degree_step == SIXTH) and (resolve_degree_step == DegreeStep.idx_1(5)):
                base_candidates[i] = [Degree(degree_step, DegreeAlter(0))]
            else:
                base_candidates[i] = []

        elif tone_type == ToneType.NEIGHBOR_TONE:
            # 刺繍音は、その前後の音の和声音の音度とChordに応じて定まる。
            # 音列として小節の冒頭からの音が与えられていると仮定し、刺繍音が小節の冒頭にない性質を利用して
            # 一つ前の音を取得して比較する。
            harmonic_ds: DegreeStep = elems[i - 1][0]
            br_ds: DegreeStep = degree_step
            if (harmonic_ds == DegreeStep.idx_1(1)) and (br_ds == SEVENTH):
                # [1, 7br, 1] -> [A G# A]
                base_candidates[i].append(Degree(degree_step, DegreeAlter(1)))
            elif (harmonic_ds == SIXTH) and (br_ds == SEVENTH):
                # "[6, 7br, 6]"
                if Degree.idx_1(6, 0) in chord_degrees:
                    # [F G F]
                    base_candidates[i].append(Degree(degree_step, DegreeAlter(0)))
                elif Degree.idx_1(6, 1) in chord_degrees:
                    # [F# G# F#]
                    base_candidates[i].append(Degree(degree_step, DegreeAlter(1)))
            elif (harmonic_ds == SEVENTH) and (br_ds == SIXTH):
                # [7, 6br, 7]
                if Degree.idx_1(7, 0) in chord_degrees:
                    # [G F G] (まれ)
                    base_candidates[i].append(Degree(degree_step, DegreeAlter(0)))
                if Degree.idx_1(7, 1) in chord_degrees:
                    # [G# F# G#]
                    #
                    # 掛留音が解決する際にsrhを伴う特殊なパターンで登場するbr [_A(r) G#(srh), F#(br), G#(H)] は、
                    # この音度上で行われるため、ここで処理する。
                    base_candidates[i].append(Degree(degree_step, DegreeAlter(1)))
            elif (harmonic_ds == DegreeStep.idx_1(5)) and (br_ds == SIXTH):
                # [5, 6br, 5]
                # [E F E] のみ追加。 [E F# E] は禁止。
                base_candidates[i].append(Degree(degree_step, DegreeAlter(0)))

        else:
            raise RuntimeError("unreachable")  # 全部網羅したはずだが間違えた時にすぐに気づけるように

    measure_sequences: list[AbstractMeasureStepSequence[Degree]] = []

    candidate_lists = [base_candidates[k] for k in base_candidates]
    next_step = measure_degree_step_sequence.next_measure_step

    def get_next_degree_candidates(current_degrees: tuple[Degree, ...]) -> list[Degree]:
        """
        次の小節の開始音の候補を作成する。
        基本的に0(Natural)だが、Degree 6, 7の場合は旋律短音階の考慮が必要。
        """
        candidates: list[Degree] = []

        # 1. 基本候補 (Natural は常に候補)
        candidates.append(Degree(next_step, DegreeAlter(0)))

        if next_step == SEVENTH:
            # 第7音は Raised (導音) も候補
            candidates.append(Degree(next_step, DegreeAlter(1)))

        elif next_step == SIXTH:
            # ^6 (Raised 6) は上行する場合のみ可能なため、直前の音が5度(Dominant)である場合にのみ追加
            if current_degrees and current_degrees[-1].step == DegreeStep.idx_1(5):
                candidates.append(Degree(next_step, DegreeAlter(1)))

        return candidates

    def is_valid_alteration_combination(full_sequence: list[Degree]) -> bool:
        """
        旋律全体の第6音と第7音の変位の整合性をチェックする。
        旋律短音階の上行形(^6, ^7)と下行形(6, 7)が不適切に混ざっていないかを確認する。
        """
        alters_6 = {d.alter.value for d in full_sequence if d.step == SIXTH}
        alters_7 = {d.alter.value for d in full_sequence if d.step == SEVENTH}

        # Nat 6 (0) と Raised 7 (1) の組み合わせは禁止 ([6, ^7], [^7, 6])
        if 0 in alters_6 and 1 in alters_7:
            return False
        # Raised 6 (1) と Nat 7 (0) の組み合わせも禁止 ([^6, 7])
        if 1 in alters_6 and 0 in alters_7:
            return False

        return True

    for degrees_for_measure in itertools.product(*candidate_lists):
        # 次の音の候補を取得
        possible_next_degrees = get_next_degree_candidates(degrees_for_measure)

        for next_degree in possible_next_degrees:
            full_seq = [*list(degrees_for_measure), next_degree]

            # 整合性チェック
            if not is_valid_alteration_combination(full_seq):
                continue

            # 結果の構築
            degree_iter = iter(degrees_for_measure)
            new_melody = measure_degree_step_sequence.measure.map_elements(
                lambda n: n.map_value(lambda _: next(degree_iter))
            )
            measure_sequences.append(
                AbstractMeasureStepSequence(
                    new_melody,
                    next_degree,
                )
            )
    return measure_sequences


def _apply_pitch_diatonic(
    key: Key,
    start_pitch: Pitch,
    interval_steps: list[IntervalStep],
) -> list[Pitch]:
    # 中央ハ音を基準としたIntervalStepを考える
    mc_start_step = start_pitch.as_interval().step()
    mc_interval_steps = [mc_start_step + step for step in interval_steps]
    return [key.diatonic_scale_pitch(step) for step in mc_interval_steps]

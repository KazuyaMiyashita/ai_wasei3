from my_project.counterpoint.measure_search.measure_step_sequence import (
    AbstractMeasureStepSequence,
)
from my_project.model import Degree, Interval, IntervalStep, Key, Mode, Pitch
from my_project.util import sliding


def filter_pitch_sequences(
    candidates: list[AbstractMeasureStepSequence[Pitch]],
    next_measure_start_harmonic_pitch: Pitch,
    pitch_range: tuple[Pitch, Pitch],
    key: Key,
) -> list[AbstractMeasureStepSequence[Pitch]]:
    """
    生成されたピッチ列の候補から、不適切なものを除外する。

    (ここにどこまでルールを詰め込むのかは悩みどころである)
    """

    results: list[AbstractMeasureStepSequence[Pitch]] = []
    for candidate in candidates:
        # next_measure_start_harmonic_pitch との関係のチェック
        #
        # candidate.next_measure_step (Pitch) と next_measure_start_harmonic_pitch の関係は以下の3つに絞られる
        # 1. 一致する -> OK
        # 2. 増一度の関係 -> pitch_applyer が生成した最後の音の vi, vii の alter が合わない。 NG
        # 3. 2度の関係 -> measure_search の search 時に 2度違いの音を含めたもの。 OK
        next_measure_diff = candidate.next_measure_step - next_measure_start_harmonic_pitch
        if next_measure_diff == Interval.P1 or next_measure_diff.step().abs() == IntervalStep.idx_1(2):
            pass
        elif next_measure_diff in {Interval.A1, Interval.d1}:
            continue
        else:
            raise RuntimeError(
                "unexpected interval between candidate.next_measure_step and next_measure_start_harmonic_pitch"
                f" : {next_measure_diff.name()}"
            )

        pitches = [*[note.value for note in candidate.measure.notes], candidate.next_measure_step]
        degrees = [Degree.from_note_name_key(pitch.note_name, key) for pitch in pitches]

        # 音域の確認
        is_valid = True
        for pitch in pitches:
            if not (pitch_range[0].num() <= pitch.num() <= pitch_range[1].num()):
                is_valid = False
                break
        if not is_valid:
            continue

        # 隣接する音の音程による除外
        for (p1, d1), (p2, _d2) in sliding(list(zip(pitches, degrees)), 2):
            # step で生成された音のうち、 減4度, 増4度, 減5度, 増5度, 増6度 は不適切なため除外する。
            # 7度音程などは step で生成されないため処理しなくて良い。
            # 増2度は pitch_applyer で除外されているはずである。
            interval = (p1 - p2).abs()
            if interval in {Interval.d4, Interval.A4, Interval.d5, Interval.A5, Interval.M6}:
                is_valid = False
                break
            if interval == Interval.A2:
                raise RuntimeError("unexpected interval A2")

            # 導音がオクターブ跳躍するものを除く(ルールが強すぎるかも？)
            if interval == Interval.P8:
                # p1, p2 がオクターブなので d1 == d2 となる。
                if (key.mode == Mode.MAJOR and d1 == Degree.idx_1(7, 0)) or (
                    key.mode == Mode.MINOR and d1 == Degree.idx_1(7, 1)
                ):
                    is_valid = False
                    break

        if not is_valid:
            continue

        results.append(candidate)

    return results

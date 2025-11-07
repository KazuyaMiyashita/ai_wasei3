# harmony.py と counterpoint.py の両方で利用されるものの置き場所

import itertools
import random
from collections.abc import Iterable, Iterator, Sequence
from typing import TypeVar

from my_project.model import Degree, DegreeStep, Interval, IntervalStep, Key, Mode, NoteName, Octave, PartId, Pitch

T = TypeVar("T")


def part_range(part_id: PartId) -> tuple[Pitch, Pitch]:
    match part_id:
        case PartId.SOPRANO:
            return (Pitch.parse("C4"), Pitch.parse("A5"))
        case PartId.ALTO:
            return (Pitch.parse("F3"), Pitch.parse("D5"))
        case PartId.TENOR:
            return (Pitch.parse("C3"), Pitch.parse("A4"))
        case PartId.BASS:
            return (Pitch.parse("F2"), Pitch.parse("D4"))


def is_in_part_range(pitch: Pitch, part_id: PartId) -> bool:
    min, max = part_range(part_id)
    return min.num() <= pitch.num() <= max.num()


def sorted_pitches(list: list[Pitch]) -> list[Pitch]:
    """
    異名同音は無視して音高のリストを昇順に並べる
    """
    return sorted(list, key=lambda p: p.num())


def scale_pitches(
    key: Key,
    range: tuple[Pitch, Pitch],
    include_all_minor_scale: bool = False,
) -> list[Pitch]:
    """
    ある調における音階の range[0] から range[1] までに含まれる Pitch を返す。
    短調の場合は和声的短音階を利用する。両端は含まれる。
    返り値のリストの音高は昇順となる。
    """

    min = range[0]
    max = range[1]

    # 与えられた調の主音の音名からなるさまざまな音高のうち、 min 以下の最大の音高を求める
    key_p = Pitch(
        octave=Octave(((min.note_name.value - key.tonic.value) * 7 + min.octave.value * 12) // 12),
        note_name=key.tonic,
    )

    # 求めた音名から始まる音階を1オクターブ分作成する
    match key.mode:
        case Mode.MAJOR:
            scale = [
                Pitch.parse("C4"),
                Pitch.parse("D4"),
                Pitch.parse("E4"),
                Pitch.parse("F4"),
                Pitch.parse("G4"),
                Pitch.parse("A4"),
                Pitch.parse("B4"),
            ]
        case Mode.MINOR:
            if include_all_minor_scale:
                scale = [
                    Pitch.parse("C4"),
                    Pitch.parse("D4"),
                    Pitch.parse("Eb4"),
                    Pitch.parse("F4"),
                    Pitch.parse("G4"),
                    Pitch.parse("Ab4"),
                    Pitch.parse("A4"),
                    Pitch.parse("Bb4"),
                    Pitch.parse("B4"),
                ]
            else:
                scale = [
                    Pitch.parse("C4"),
                    Pitch.parse("D4"),
                    Pitch.parse("Eb4"),
                    Pitch.parse("F4"),
                    Pitch.parse("G4"),
                    Pitch.parse("Ab4"),
                    Pitch.parse("B4"),
                ]
    scale = [
        Pitch(
            octave=Octave(key_p.octave.value + p.octave.value),
            note_name=NoteName(key_p.note_name.value + p.note_name.value),
        )
        for p in scale
    ]

    # 音階をオクターブ移動しながら、 min <= p <= max の範囲の音階を作成する
    result: list[Pitch] = []
    for o_value in itertools.count():
        octave = Octave(o_value)
        shift_scale = [Pitch(octave + p.octave, note_name=p.note_name) for p in scale]
        for p in shift_scale:
            if p.num() < min.num():
                continue
            if max.num() < p.num():
                break
            result.append(p)
        else:
            continue
        break

    return result


def add_interval_step_in_key(key: Key, pitch: Pitch, interval_step: IntervalStep) -> Pitch:
    """
    指定されたピッチに対し、キーの文脈で音程分だけ上方に移動したピッチを返します。

    この関数はダイアトニックな音程(キー固有の音階上の音程)を計算します。

    例: ハ長調 (C Major) で D4 に "3度上" (IntervalStep(2)) を適用すると、
        キーのダイアトニックな3度上である F4 を返します。

    開始音が変化音(D#4など)の場合、その変化(Alter)は保持されます。
    例: ハ長調で D#4 (Degree(Step(1), Alter(1))) に "3度上" を適用すると、
        F#4 (Degree(Step(3), Alter(1))) を返します。

    Args:
        key (Key): 基準となる調。
        pitch (Pitch): 開始ピッチ。
        interval_step (IntervalStep): 上方に移動する音程のステップ数(ユニゾン=0, 2度=1, 3度=2)。

    Returns:
        Pitch: 新しいピッチ。
    """

    # 1. 開始ピッチの音名と、キーにおける音度(Degree)を取得
    start_note_name = pitch.note_name
    start_degree = Degree.from_note_name_key(start_note_name, key)

    # 2. 目標の音度(Degree)を計算
    #    音度距離(Step)は interval_step 分だけ上方に移動 (mod 7)
    #    変化度(Alter)は開始音のものをそのまま引き継ぐ
    target_step_value = (start_degree.step.value + interval_step.value) % 7
    target_step = DegreeStep(target_step_value)
    target_alter = start_degree.alter  # 変化を保持

    target_degree = Degree(target_step, target_alter)

    # 3. 目標の音度(Degree)から、目標の音名(NoteName)を逆算
    target_note_name = target_degree.note_name(key)

    # 4. 開始音名と目標音名の差から、オクターブと五度の移動量(Interval)を計算

    # 五度の移動回数 (note_name の value の差)
    fifth_diff = target_note_name.value - start_note_name.value

    # 音程のステップ(s)は、五度の移動回数(f)とオクターブの移動回数(o)から
    # s = 4*f + 7*o という関係にある。
    # s は interval_step.value と等しいため、o について解く。
    # 7*o = s - 4*f
    # o = (s - 4*f) / 7

    s = interval_step.value
    f = fifth_diff

    numerator = s - 4 * f
    if numerator % 7 != 0:
        # このモデルの前提が正しければ、ここは常に7で割り切れるはず
        raise RuntimeError(
            f"Internal logic error: (s - 4f) not divisible by 7. "
            f"s={s}, f={f}, start={start_note_name.name()}, "
            f"target={target_note_name.name()}"
        )

    octave_diff = numerator // 7

    # 5. 計算した Interval を元の Pitch に足して、新しい Pitch を得る
    #    Pitch.__add__ は Interval を受け取るように定義されている
    interval_to_add = Interval(octave=octave_diff, fifth=fifth_diff)

    return pitch + interval_to_add


def shuffled_interleave(iterables: Sequence[Iterable[T]], randomized: bool = True) -> Iterator[T]:
    """
    複数のイテラブル(イテレータ)を受け取り、
    それらが尽きるまでランダムに要素を取り出して返す
    新しいイテレータを生成します。
    """

    if randomized:
        # 1. すべての入力(リストやrangeなども可)をイテレータに変換し、
        #    「まだ尽きていない」イテレータのリストを作成
        active_iterators = [iter(it) for it in iterables]

        # 2. 尽きていないイテレータが1つでも残っている間、ループ
        while active_iterators:
            # 2-a. リストからランダムにイテレータを1つ選ぶ (これが「シャッフル」)
            try:
                chosen_iter = random.choice(active_iterators)
            except IndexError:
                # active_iteratorsが空になったらループを抜ける
                # (whileの条件判定の直後にremoveされた場合に備える)
                break

            # 2-b, 2-c. 選んだイテレータから要素を1つ取り出して yield
            try:
                item = next(chosen_iter)
                yield item

            # 2-d. もしイテレータが尽きていたら (StopIteration)
            except StopIteration:
                # 「尽きていない」リストから除外する
                active_iterators.remove(chosen_iter)
    else:
        for it in iterables:
            yield from it


# 音列から隣り合わせの3つの音を作成
def sliding(input_list: list[T], window_size: int) -> list[list[T]]:
    n = len(input_list)
    return [input_list[i : i + window_size] for i in range(n - window_size + 1)]

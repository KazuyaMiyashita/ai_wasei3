import itertools
import random
from dataclasses import dataclass

from my_project.model import Alter, Degree, Key, Mode, NoteName, Octave, Pitch, Step


@dataclass(frozen=True)
class Harmony:
    bass: Pitch
    tenor: Pitch
    alto: Pitch
    soprano: Pitch

    def name(self) -> str:
        return f"[{self.bass.name()}, {self.tenor.name()}, {self.alto.name()}, {self.soprano.name()}]"

    def to_list(self) -> list[Pitch]:
        return [self.bass, self.tenor, self.alto, self.soprano]


def solve(bass_sequence: list[Pitch], key: Key) -> list[Harmony]:
    result: list[Harmony] = []
    for bass_pitch in bass_sequence:
        harmonies = available_harmonies(bass_pitch, key)
        # TODO
        harmony = random.choice(list(harmonies))
        result.append(harmony)

    return result


def compare_pitch(a: Pitch, b: Pitch) -> int:
    """
    二つの音高の高低を比較し、結果を整数(-1, 0, 1)で返す。
    異名同音は無視する。
    a < b  => -1;
    a == b => 0;
    a > b  => 1
    """
    a_value = a.note_name.value * 7 + a.octave.value * 12
    b_value = b.note_name.value * 7 + b.octave.value * 12
    if a_value < b_value:
        return -1
    elif a_value == b_value:
        return 0
    else:
        return 1


def scale_pitches(key: Key, min: Pitch, max: Pitch) -> list[Pitch]:
    """
    ある調における音階の min から max までに含まれる Pitch を返す。
    短調の場合は和声的短音階を利用する。両端は含まれる。
    返り値のリストの音高は昇順となる。
    """

    # 与えられた調の主音の音名からなるさまざまな音高のうち、 min 以下の最大の音高を求める
    key_p = Pitch(
        octave=Octave((12 * (min.note_name.value - key.tonic.value) + 7 * min.note_name.value) // 7),
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
        shift_scale = [Pitch(Octave(octave.value + p.octave.value), note_name=p.note_name) for p in scale]
        for p in shift_scale:
            if compare_pitch(p, min) == -1:
                continue
            if compare_pitch(p, max) == 1:
                break
            result.append(p)
        else:
            continue
        break

    return result


def triad_note_names(bass: NoteName, key: Key) -> set[NoteName]:
    """
    与えられたバスの音名と調によって三和音の基本形の構成音を返す
    """

    bass_degree = Degree.from_pitch_key(bass, key)

    # 基本形の和音ではバスに変位音はないため、指定されたら空のセットを返す
    if bass_degree.alter.value != 0:
        return set()

    # 第三音は短調のVの和音の場合上方変位する
    if bass_degree.step == Step.of(5) and key.mode == Mode.MINOR:
        third_degree = Degree.of(7, 1)
    else:
        third_degree = Degree(step=Step((bass_degree.step.value + 2) % 7), alter=Alter(0))

    fifth_degree = Degree(step=Step((bass_degree.step.value + 4) % 7), alter=Alter(0))

    names = [Degree.note_name(degree, key) for degree in [bass_degree, third_degree, fifth_degree]]

    return set(names)


def available_harmonies(bass: Pitch, key: Key) -> set[Harmony]:
    chord_note_names = triad_note_names(bass.note_name, key)

    tenor_pitches = [
        pitch
        for pitch in scale_pitches(key=key, min=Pitch.parse("C3"), max=Pitch.parse("A4"))
        if pitch.note_name in chord_note_names
    ]
    alto_pitches = [
        pitch
        for pitch in scale_pitches(key=key, min=Pitch.parse("F3"), max=Pitch.parse("D5"))
        if pitch.note_name in chord_note_names
    ]
    soprano_pitches = [
        pitch
        for pitch in scale_pitches(key=key, min=Pitch.parse("C4"), max=Pitch.parse("A5"))
        if pitch.note_name in chord_note_names
    ]

    # bass <= tenor <= alto <= soprano となるものを探す
    result: list[Harmony] = []
    for p_tenor in tenor_pitches:
        if not (compare_pitch(bass, p_tenor) <= 0):
            continue
        for p_alto in alto_pitches:
            if not (compare_pitch(p_tenor, p_alto) <= 0):
                continue
            for p_soprano in soprano_pitches:
                if compare_pitch(p_alto, p_soprano) <= 0:
                    result.append(Harmony(bass, p_tenor, p_alto, p_soprano))

    # 構成音が全て含まれているものに絞る
    result2 = [harmony for harmony in result if set([p.note_name for p in harmony.to_list()]) == chord_note_names]

    # TODO: 第三音の重複はどうする?
    # TODO: 隣接声部の音程が離れすぎているものを除外する

    return set(result2)

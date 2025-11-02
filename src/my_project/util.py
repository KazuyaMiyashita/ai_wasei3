# harmony.py と counterpoint.py の両方で利用されるものの置き場所

import functools
import itertools

from my_project.model import Key, Mode, NoteName, Octave, PartId, Pitch


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


def sorted_pitches(list: list[Pitch]) -> list[Pitch]:
    """
    異名同音は無視して音高のリストを昇順に並べる
    """
    return sorted(list, key=functools.cmp_to_key(compare_pitch))


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
            if compare_pitch(p, min) == -1:
                continue
            if compare_pitch(p, max) == 1:
                break
            result.append(p)
        else:
            continue
        break

    return result

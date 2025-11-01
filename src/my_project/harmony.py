import itertools
from dataclasses import dataclass

from my_project.model import Alter, Degree, Key, Mode, NoteName, Octave, Pitch, Step


@dataclass(frozen=True)
class Chord:
    bass: Pitch
    tenor: Pitch
    alto: Pitch
    soprano: Pitch

    def name(self) -> str:
        return f"[{self.bass.name()}, {self.tenor.name()}, {self.alto.name()}, {self.soprano.name()}]"

    def to_list(self) -> list[Pitch]:
        return [self.bass, self.tenor, self.alto, self.soprano]


def solve(bass_sequence: list[Pitch], key: Key) -> list[Chord]:
    prev_chord: Chord | None = None
    result: list[Chord] = []
    for bass in bass_sequence:
        if not prev_chord:
            chord = start_chord(bass, key)
        else:
            chord = next_chord(bass, key, prev_chord)
        prev_chord = chord
        result.append(chord)

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


def triad_note_names(bass: NoteName, key: Key) -> set[NoteName]:
    """
    与えられたバスの音名と調によって三和音の基本形の構成音を返す
    """

    bass_degree = Degree.from_note_name_key(bass, key)

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


def available_harmonies(bass: Pitch, key: Key) -> set[Chord]:
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
    result: list[Chord] = []
    for p_tenor in tenor_pitches:
        if not (compare_pitch(bass, p_tenor) <= 0):
            continue
        for p_alto in alto_pitches:
            if not (compare_pitch(p_tenor, p_alto) <= 0):
                continue
            for p_soprano in soprano_pitches:
                if compare_pitch(p_alto, p_soprano) <= 0:
                    result.append(Chord(bass, p_tenor, p_alto, p_soprano))

    # 構成音が全て含まれているものに絞る
    result2 = [harmony for harmony in result if set([p.note_name for p in harmony.to_list()]) == chord_note_names]

    # TODO: 第三音の重複はどうする?
    # TODO: 隣接声部の音程が離れすぎているものを除外する

    return set(result2)


def start_chord(bass: Pitch, key: Key) -> Chord:
    """
    課題の最初のバスの音と調から、最初の和音の配置として適切なものを返す
    """

    minor_factor = Pitch.parse("Cb4") if key.mode == Mode.MINOR else Pitch.parse("C4")

    if compare_pitch(bass, Pitch.parse("F3")) >= 1:
        # ドソドミ
        return Chord(
            bass=bass,
            tenor=bass + Pitch.parse("G4"),
            alto=bass + Pitch.parse("C5"),
            soprano=bass + Pitch.parse("E5") + minor_factor,
        )
    elif compare_pitch(bass, Pitch.parse("A2")) >= 1:
        # ド - ミソド
        return Chord(
            bass=bass,
            tenor=bass + Pitch.parse("E5"),
            alto=bass + Pitch.parse("G5"),
            soprano=bass + Pitch.parse("C6") + minor_factor,
        )
    else:
        # ドドソミ
        return Chord(
            bass=bass,
            tenor=bass + Pitch.parse("C5"),
            alto=bass + Pitch.parse("G5"),
            soprano=bass + Pitch.parse("E6") + minor_factor,
        )


def next_chord(next_bass: Pitch, key: Key, current_chord: Chord) -> Chord:
    """
    バス、調、現在の和音から、次の和音として適切な配置の和音を返す
    """

    # 音度距離同士の対応を作成する。
    # そのために、それぞれの和音の根音を求める。

    current_chord_notes: set[NoteName] = triad_note_names(current_chord.bass.note_name, key)
    current_chord_steps: set[Step] = set([Degree.from_note_name_key(n, key).step for n in current_chord_notes])

    current_chord_root: Step | None = None
    for i in range(0, 7):
        root = Step(i)
        steps: set[Step] = set([root, root + Step(2), root + Step(4)])
        if steps == current_chord_steps:
            current_chord_root = root
            break
    if not current_chord_root:
        raise Exception(f"根音が見つかりません。 current_chord_steps: {current_chord_steps}")

    # 現在の和音の音度距離と次の和音の音度距離のマッピングを作成 (例: {v -> v, vii -> i, ii -> iii})
    # 根音が何度移動するかによって定める
    def create_mapping(current_chord_root: Step, next_chord_root: Step) -> dict[Step, Step]:
        root = current_chord_root
        third = current_chord_root + Step(2)
        fifth = current_chord_root + Step(4)
        match next_chord_root - current_chord_root:
            case Step(0):
                # ドミソ->ドミソ や、 レファラ->レファラ
                return {
                    root: root,
                    third: third,
                    fifth: fifth,
                }
            case Step(1):
                # ドミソ->レファラなど
                return {
                    root: root - Step(2),
                    third: third - Step(1),
                    fifth: fifth - Step(1),
                }
            case Step(2):
                # ドミソ -> シミソ など
                return {
                    root: root - Step(1),
                    third: third,
                    fifth: fifth,
                }
            case Step(3):
                # ドミソ -> ドファラ などの4度上行
                # II -> V の場合のみ下行させる
                if current_chord_root == Step(1):
                    return {
                        root: root - Step(2),
                        third: third - Step(2),
                        fifth: fifth - Step(1),
                    }
                else:
                    return {
                        root: root,
                        third: third + Step(1),
                        fifth: fifth + Step(1),
                    }
            case Step(4):
                # ドミソ -> シレソ など
                return {
                    root: root - Step(1),
                    third: third - Step(1),
                    fifth: fifth,
                }
            case Step(5):
                # ドミソ -> ドミラ など
                return {
                    root: root,
                    third: third,
                    fifth: fifth + Step(1),
                }
            case Step(6):
                # ドミソ -> レファシ など
                return {
                    root: root + Step(1),
                    third: third + Step(1),
                    fifth: fifth + Step(2),
                }
            case _:
                raise Exception()

    next_chord_root: Step = Degree.from_note_name_key(next_bass.note_name, key).step
    mapping: dict[Step, Step] = create_mapping(current_chord_root, next_chord_root)

    def is_neighborhood_pitch(a: Pitch, b: Pitch) -> bool:
        """二つのPitchが三度以下であるか"""
        diff: Pitch = a - b
        return compare_pitch(Pitch.parse("Ab3"), diff) <= 0 and compare_pitch(diff, Pitch.parse("E4")) <= 0

    def find_next_pitch(current_voice_pitch: Pitch, voice_range: list[Pitch]) -> Pitch:
        """
        音域内の音であり、現在の音とmappingによって定まった音度であり、現在の音と3度以下の音を探す
        """
        next_step = mapping[Degree.from_note_name_key(current_voice_pitch.note_name, key).step]
        next_pitches = [
            pitch
            for pitch in voice_range
            if Degree.from_note_name_key(pitch.note_name, key).step == next_step
            and is_neighborhood_pitch(pitch, current_voice_pitch)
        ]
        if len(next_pitches) > 2 or len(next_pitches) == 0:
            raise Exception(
                f"can not find. current_voice_pitch: {current_voice_pitch.name()}, next_step: {next_step}, mapping: {mapping}, next_pitches: {next_pitches}. "
            )
        return next_pitches[0]

    next_tenor = find_next_pitch(
        current_chord.tenor, scale_pitches(key=key, min=Pitch.parse("C3"), max=Pitch.parse("A4"))
    )
    next_alto = find_next_pitch(
        current_chord.alto, scale_pitches(key=key, min=Pitch.parse("F3"), max=Pitch.parse("D5"))
    )
    next_soprano = find_next_pitch(
        current_chord.soprano, scale_pitches(key=key, min=Pitch.parse("C4"), max=Pitch.parse("A5"))
    )

    return Chord(next_bass, next_tenor, next_alto, next_soprano)

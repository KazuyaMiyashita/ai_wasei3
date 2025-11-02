from dataclasses import dataclass
from fractions import Fraction

from my_project.model import (
    Degree,
    DegreeAlter,
    DegreeStep,
    Duration,
    Interval,
    IntervalStep,
    Key,
    Measure,
    Mode,
    Note,
    NoteName,
    Part,
    PartId,
    Pitch,
    Score,
    TimeSignature,
)
from my_project.util import compare_pitch, part_range, scale_pitches


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


def solve(bass_sequence: list[Pitch], key: Key) -> Score:
    prev_chord: Chord | None = None
    result: list[Chord] = []
    for bass in bass_sequence:
        if not prev_chord:
            chord = start_chord(bass, key)
        else:
            chord = next_chord(bass, key, prev_chord)
        prev_chord = chord
        result.append(chord)

    return _chords_to_score(result, key)


def _chords_to_score(chords: list[Chord], key: Key) -> Score:
    duration = Duration(Fraction(2))

    sop_notes = [Note(c.soprano, duration) for c in chords]
    alto_notes = [Note(c.alto, duration) for c in chords]
    tenor_notes = [Note(c.tenor, duration) for c in chords]
    bass_notes = [Note(c.bass, duration) for c in chords]

    score = Score(
        key=key,
        time_signature=TimeSignature(2, Fraction(2)),
        parts=[
            Part(part_id=PartId.SOPRANO, measures=[Measure(notes=sop_notes)]),
            Part(part_id=PartId.ALTO, measures=[Measure(notes=alto_notes)]),
            Part(part_id=PartId.TENOR, measures=[Measure(notes=tenor_notes)]),
            Part(part_id=PartId.BASS, measures=[Measure(notes=bass_notes)]),
        ],
    )
    return score


def triad_note_names(bass: NoteName, key: Key) -> set[NoteName]:
    """
    与えられたバスの音名と調によって三和音の基本形の構成音を返す
    """

    bass_degree = Degree.from_note_name_key(bass, key)

    # 基本形の和音ではバスに変位音はないため、指定されたら空のセットを返す
    if bass_degree.alter.value != 0:
        return set()

    # 第三音は短調のVの和音の場合上方変位する
    if bass_degree.step == DegreeStep.idx_1(5) and key.mode == Mode.MINOR:
        third_degree = Degree.idx_1(7, 1)
    else:
        third_degree = Degree(step=DegreeStep((bass_degree.step.value + 2) % 7), alter=DegreeAlter(0))

    fifth_degree = Degree(step=DegreeStep((bass_degree.step.value + 4) % 7), alter=DegreeAlter(0))

    names = [Degree.note_name(degree, key) for degree in [bass_degree, third_degree, fifth_degree]]

    return set(names)


def start_chord(bass: Pitch, key: Key) -> Chord:
    """
    課題の最初のバスの音と調から、最初の和音の配置として適切なものを返す
    """

    # 増1度下は減1度上として表現する
    minor_factor = Interval.parse("d1") if key.mode == Mode.MINOR else Interval.parse("P1")

    if compare_pitch(bass, Pitch.parse("F3")) >= 1:
        # ドソドミ
        return Chord(
            bass=bass,
            tenor=bass + Interval.parse("P5"),
            alto=bass + Interval.parse("P8"),
            soprano=bass + Interval.parse("M10") + minor_factor,
        )
    elif compare_pitch(bass, Pitch.parse("A2")) >= 1:
        # ド - ミソド
        return Chord(
            bass=bass,
            tenor=bass + Interval.parse("M10"),
            alto=bass + Interval.parse("P12"),
            soprano=bass + Interval.parse("P15") + minor_factor,
        )
    else:
        # ドドソミ
        return Chord(
            bass=bass,
            tenor=bass + Interval.parse("P8"),
            alto=bass + Interval.parse("P12"),
            soprano=bass + Interval.parse("M17") + minor_factor,
        )


def next_chord(next_bass: Pitch, key: Key, current_chord: Chord) -> Chord:
    """
    バス、調、現在の和音から、次の和音として適切な配置の和音を返す
    """

    # 音度距離同士の対応を作成する。
    # そのために、それぞれの和音の根音を求める。

    current_chord_notes: set[NoteName] = triad_note_names(current_chord.bass.note_name, key)
    current_chord_steps: set[DegreeStep] = set([Degree.from_note_name_key(n, key).step for n in current_chord_notes])

    current_chord_root: DegreeStep | None = None
    for i in range(0, 7):
        root = DegreeStep(i)
        steps: set[DegreeStep] = set([root, root + DegreeStep(2), root + DegreeStep(4)])
        if steps == current_chord_steps:
            current_chord_root = root
            break
    if not current_chord_root:
        raise Exception(f"根音が見つかりません。 current_chord_steps: {current_chord_steps}")

    # 現在の和音の音度距離と次の和音の音度距離のマッピングを作成 (例: {v -> v, vii -> i, ii -> iii})
    # 根音が何度移動するかによって定める
    def create_mapping(current_chord_root: DegreeStep, next_chord_root: DegreeStep) -> dict[DegreeStep, DegreeStep]:
        root = current_chord_root
        third = current_chord_root + DegreeStep(2)
        fifth = current_chord_root + DegreeStep(4)
        match next_chord_root - current_chord_root:
            case DegreeStep(0):
                # ドミソ->ドミソ や、 レファラ->レファラ
                return {
                    root: root,
                    third: third,
                    fifth: fifth,
                }
            case DegreeStep(1):
                # ドミソ->レファラなど
                return {
                    root: root - DegreeStep(2),
                    third: third - DegreeStep(1),
                    fifth: fifth - DegreeStep(1),
                }
            case DegreeStep(2):
                # ドミソ -> シミソ など
                return {
                    root: root - DegreeStep(1),
                    third: third,
                    fifth: fifth,
                }
            case DegreeStep(3):
                # ドミソ -> ドファラ などの4度上行
                # II -> V の場合のみ下行させる
                if current_chord_root == DegreeStep(1):
                    return {
                        root: root - DegreeStep(2),
                        third: third - DegreeStep(2),
                        fifth: fifth - DegreeStep(1),
                    }
                else:
                    return {
                        root: root,
                        third: third + DegreeStep(1),
                        fifth: fifth + DegreeStep(1),
                    }
            case DegreeStep(4):
                # ドミソ -> シレソ など
                return {
                    root: root - DegreeStep(1),
                    third: third - DegreeStep(1),
                    fifth: fifth,
                }
            case DegreeStep(5):
                # ドミソ -> ドミラ など
                return {
                    root: root,
                    third: third,
                    fifth: fifth + DegreeStep(1),
                }
            case DegreeStep(6):
                # ドミソ -> レファシ など
                return {
                    root: root + DegreeStep(1),
                    third: third + DegreeStep(1),
                    fifth: fifth + DegreeStep(2),
                }
            case _:
                raise Exception()

    next_chord_root: DegreeStep = Degree.from_note_name_key(next_bass.note_name, key).step
    mapping: dict[DegreeStep, DegreeStep] = create_mapping(current_chord_root, next_chord_root)

    def is_neighborhood_pitch(a: Pitch, b: Pitch) -> bool:
        """二つのPitchが三度以下であるか"""
        return (a - b).step().abs() <= IntervalStep(3)

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
                f"can not find. current_voice_pitch: {current_voice_pitch.name()}, "
                f"next_step: {next_step}, mapping: {mapping}, next_pitches: {next_pitches}. "
            )
        return next_pitches[0]

    next_tenor = find_next_pitch(current_chord.tenor, scale_pitches(key=key, range=part_range(PartId.TENOR)))
    next_alto = find_next_pitch(current_chord.alto, scale_pitches(key=key, range=part_range(PartId.ALTO)))
    next_soprano = find_next_pitch(current_chord.soprano, scale_pitches(key=key, range=part_range(PartId.SOPRANO)))

    return Chord(next_bass, next_tenor, next_alto, next_soprano)

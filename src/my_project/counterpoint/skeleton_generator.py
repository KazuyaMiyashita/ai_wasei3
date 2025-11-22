import random
from dataclasses import dataclass

from my_project.model import Duration, Key, Melody, Note, NoteName, PartId, Pitch


@dataclass(frozen=True)
class Skeleton:
    """
    課題全体を仮に全音符で実施した際の、協和音の音高と和音の設定を表す。
    """

    measures: list[Melody[Note[Pitch, list[NoteName]]]]
    # TODO: attribute は Chord[Degree] なのかで迷っている


@dataclass
class SkeletonGenerator:
    cantus_firmus: list[Pitch]
    key: Key
    part_id: PartId
    rand: random.Random
    _measure_length: int

    # TODO Iterator でさまざまなスケルトンを返すようにする
    def generate_skeleton(self) -> Skeleton:
        """
        CFを元に実施する声部のそれぞれの小節の最初に利用する和声音を定める。
        この和声音を骨格として、間に旋律を埋めることで課題が実施される。
        そのため、ここでは旋律の同音の連続や7度などの不協和音程を選択することができる。

        和音と旋律の規則。和音はCFと協和する
        - 冒頭小節
            - 和音は I の基本形
            - 対位旋律は i, v 度音から始める。(移勢の類(?)の場合は iii 度音が可能)
            - CFと1度になることが可能
        - 途中の小節
            - 長調の場合、 I, II, III, IV, V, VI の和音の基本形および TODO
        - 最終小節の1小節前
            - V の基本形、第一転回形
            - VII の第一転回形
            - II の基本形、第一転回形(!)。ただし対位旋律は経過音として導音を経過する場合に限られる。
        - 最終小節
            - 和音は I の基本形
            - 対位旋律は i 度音 (2声の場合)
            - CFと1度になることが可能
        """

        # TODO: ひとまずダミーのものを一つ返すようにしている
        if self.key == Key.parse("C Major") and self.cantus_firmus == [
            Pitch.parse("C4"),
            Pitch.parse("A3"),
            Pitch.parse("G3"),
            Pitch.parse("C3"),
        ]:
            skeleton = Skeleton(
                measures=[
                    _create_measure(
                        Pitch.parse("G4"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                    _create_measure(
                        Pitch.parse("A4"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("B4"),
                        [NoteName.parse(n) for n in ["G", "B", "D"]],
                    ),
                    _create_measure(
                        Pitch.parse("C5"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                ]
            )
            return skeleton
        elif self.key == Key.parse("C Major") and self.cantus_firmus == [
            Pitch.parse("C4"),
            Pitch.parse("A3"),
            Pitch.parse("G3"),
            Pitch.parse("E3"),
            Pitch.parse("F3"),
            Pitch.parse("A3"),
            Pitch.parse("G3"),
            Pitch.parse("E3"),
            Pitch.parse("D3"),
            Pitch.parse("C3"),
        ]:
            skeleton = Skeleton(
                measures=[
                    _create_measure(
                        Pitch.parse("G4"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                    _create_measure(
                        Pitch.parse("A4"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("B4"),
                        [NoteName.parse(n) for n in ["G", "B", "D"]],
                    ),
                    _create_measure(
                        Pitch.parse("C5"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                    _create_measure(
                        Pitch.parse("D5"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                    _create_measure(
                        Pitch.parse("F5"),
                        [NoteName.parse(n) for n in ["F", "A", "C"]],
                    ),
                    _create_measure(
                        Pitch.parse("G5"),
                        [NoteName.parse(n) for n in ["G", "B", "D"]],
                    ),
                    _create_measure(
                        Pitch.parse("C5"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                    _create_measure(
                        Pitch.parse("B4"),
                        [NoteName.parse(n) for n in ["D", "F", "B"]],
                    ),
                    _create_measure(
                        Pitch.parse("C5"),
                        [NoteName.parse(n) for n in ["C", "E", "G"]],
                    ),
                ]
            )
            return skeleton
        elif self.key == Key.parse("A Minor") and self.cantus_firmus == [
            Pitch.parse("A2"),
            Pitch.parse("B2"),
            Pitch.parse("C3"),
            Pitch.parse("E3"),
            Pitch.parse("F3"),
            Pitch.parse("E3"),
            Pitch.parse("C3"),
            Pitch.parse("A2"),
            Pitch.parse("B2"),
            Pitch.parse("A2"),
        ]:
            skeleton = Skeleton(
                measures=[
                    _create_measure(
                        Pitch.parse("E3"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("G#3"),
                        [NoteName.parse(n) for n in ["B", "D", "G#"]],
                    ),
                    _create_measure(
                        Pitch.parse("A3"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("G#3"),
                        [NoteName.parse(n) for n in ["E", "G#", "B"]],
                    ),
                    _create_measure(
                        Pitch.parse("D4"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                    _create_measure(
                        Pitch.parse("E4"),
                        [NoteName.parse(n) for n in ["E", "G#", "B"]],
                    ),
                    _create_measure(
                        Pitch.parse("A4"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("C4"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],  # or F A C
                    ),
                    _create_measure(
                        Pitch.parse("G#3"),
                        [NoteName.parse(n) for n in ["B", "D", "G#"]],
                    ),
                    _create_measure(
                        Pitch.parse("A3"),
                        [NoteName.parse(n) for n in ["A", "C", "E"]],
                    ),
                ]
            )
            return skeleton
        elif self.key == Key.parse("D Minor") and self.cantus_firmus == [
            Pitch.parse("D3"),
            Pitch.parse("E3"),
            Pitch.parse("F3"),
            Pitch.parse("A3"),
            Pitch.parse("Bb3"),
            Pitch.parse("A3"),
            Pitch.parse("F3"),
            Pitch.parse("D3"),
            Pitch.parse("E3"),
            Pitch.parse("D3"),
        ]:
            skeleton = Skeleton(
                measures=[
                    _create_measure(
                        Pitch.parse("A3"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                    _create_measure(
                        Pitch.parse("C#4"),
                        [NoteName.parse(n) for n in ["E", "G", "C#"]],
                    ),
                    _create_measure(
                        Pitch.parse("D4"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                    _create_measure(
                        Pitch.parse("C#4"),
                        [NoteName.parse(n) for n in ["A", "C#", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("G4"),
                        [NoteName.parse(n) for n in ["G", "Bb", "D"]],
                    ),
                    _create_measure(
                        Pitch.parse("A4"),
                        [NoteName.parse(n) for n in ["A", "C#", "E"]],
                    ),
                    _create_measure(
                        Pitch.parse("D5"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                    _create_measure(
                        Pitch.parse("F4"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],  # or D F Bb
                    ),
                    _create_measure(
                        Pitch.parse("C#4"),
                        [NoteName.parse(n) for n in ["E", "G", "C#"]],
                    ),
                    _create_measure(
                        Pitch.parse("D4"),
                        [NoteName.parse(n) for n in ["D", "F", "A"]],
                    ),
                ]
            )
            return skeleton
        raise NotImplementedError


def _create_measure(pitch: Pitch, chord: list[NoteName]) -> Melody[Note[Pitch, list[NoteName]]]:
    return Melody.of(Note(pitch, Duration.of(4), chord))

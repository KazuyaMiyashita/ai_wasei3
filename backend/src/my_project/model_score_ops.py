from __future__ import annotations

from collections.abc import Callable, Iterator, Mapping
from dataclasses import dataclass
from typing import TYPE_CHECKING, Any, TypeVar, cast

from my_project.model import (
    Chord,
    Duration,
    Measure,
    Melody,
    Note,
    Offset,
)

if TYPE_CHECKING:
    from my_project.model import Score, VerticalMoment, VerticalScoreView

T_Value = TypeVar("T_Value")
T_Attr = TypeVar("T_Attr")
T_Id = TypeVar("T_Id")


@dataclass(frozen=True)
class Slice[T_Value]:
    """
    分割可能な要素を表すコンテナ。
    分析の際の編集や転置において、音符が分割された際の状態（結合可能性）を保持する。

    """

    value: T_Value
    connects_left: bool = False
    connects_right: bool = False


@dataclass(frozen=True)
class Identified[T_Id, T_Value]:
    """
    ID付けされた要素。
    声部（PartId）などを区別するために利用する。
    """

    id: T_Id
    value: T_Value

    def map_value[U_Value](self, func: Callable[[T_Value], U_Value]) -> Identified[T_Id, U_Value]:
        return Identified(self.id, func(self.value))


class VerticalMomentImpl[T_Id, T_Value, T_Attr]:
    """
    ある一瞬の垂直断面。Durationを持つ。
    """

    _inner_note: Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr]

    def __init__(self, inner_note: Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr]):
        self._inner_note = inner_note

    @property
    def duration(self) -> Duration:
        return self._inner_note.duration

    @property
    def chord(self) -> Chord[T_Value]:
        """分析用に純粋な「値の和音」を返す。"""
        values: list[T_Value] = []
        for note in self._inner_note.value.elements:
            identified = note.value
            slice_val = identified.value
            values.append(slice_val.value)
        return Chord.of(*values)

    def get(self, part_id: T_Id) -> T_Value | None:
        """特定のパートの現在の値を取得"""
        for note in self._inner_note.value.elements:
            if note.value.id == part_id:
                return note.value.value.value
        return None

    def is_tied_from_prev(self, part_id: T_Id) -> bool:
        for note in self._inner_note.value.elements:
            if note.value.id == part_id:
                return note.value.value.connects_left
        return False

    def is_tied_to_next(self, part_id: T_Id) -> bool:
        for note in self._inner_note.value.elements:
            if note.value.id == part_id:
                return note.value.value.connects_right
        return False


class VerticalScoreViewImpl[T_Id, T_Value, T_Attr]:
    """Scoreの垂直方向のビューの実装"""

    _moments: tuple[VerticalMomentImpl[T_Id, T_Value, T_Attr], ...]
    _raw_vertical_notes: tuple[Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr], ...]
    _score_cls: type[Score[T_Id, T_Value, T_Attr]]

    def __init__(
        self,
        raw_vertical_notes: tuple[Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr], ...],
        score_cls: type[Score[T_Id, T_Value, T_Attr]],
    ):
        self._raw_vertical_notes = raw_vertical_notes
        self._moments = tuple(VerticalMomentImpl(n) for n in raw_vertical_notes)
        self._score_cls = score_cls

    def __iter__(self) -> Iterator[VerticalMoment[T_Id, T_Value]]:
        return iter(self._moments)

    def __getitem__(self, index: int) -> VerticalMoment[T_Id, T_Value]:
        return self._moments[index]

    def __len__(self) -> int:
        return len(self._moments)

    def to_flat_score(self) -> Score[T_Id, T_Value, T_Attr]:
        """
        垂直ビューから、1小節だけの（あるいは全小節が結合された）Scoreを再構築する。
        戻り値は 1小節のリストを持つ Score になる。
        """
        reconstructed_parts_set = transpose_vertical_to_score(self._raw_vertical_notes)

        parts_measure_list: dict[T_Id, list[Measure[Note[T_Value, T_Attr]]]] = {}

        for part_note in reconstructed_parts_set:
            part_id = part_note.value.id
            sliced_melody = part_note.value.value

            def unwrap_slice(n: Note[Slice[T_Value], T_Attr]) -> Note[T_Value, T_Attr]:
                return Note(n.value.value, n.duration, n.attribute)

            unwrapped_melody = sliced_melody.map(unwrap_slice)

            # 再構築されたメロディ全体を1つの Measure として扱う
            parts_measure_list[part_id] = [Measure(unwrapped_melody)]

        return self._score_cls(parts_measure_list)


def score_measure[T_Id, T_Value, T_Attr](
    score: Score[T_Id, T_Value, T_Attr], index: int
) -> Score[T_Id, T_Value, T_Attr]:
    """
    指定したインデックスの小節だけを切り出した Score を返す。
    """
    sliced_parts = {}
    for pid, measures in score.parts.items():
        if 0 <= index < len(measures):
            sliced_parts[pid] = [measures[index]]
    return score.__class__(sliced_parts)


def create_vertical_score_view[T_Id, T_Value, T_Attr](
    parts: Mapping[T_Id, list[Measure[Note[T_Value, T_Attr]]]],
    score_cls: type[Score[T_Id, T_Value, T_Attr]],
) -> VerticalScoreView[T_Id, T_Value, T_Attr]:
    """
    Scoreの垂直ビューを作成するファクトリ関数
    """
    elements: list[Note[Identified[T_Id, Melody[Note[Slice[T_Value], T_Attr]]], T_Attr]] = []
    for part_id, measure_list in parts.items():
        # 全小節のメロディを結合
        all_notes: list[Note[T_Value, T_Attr]] = []
        for m in measure_list:
            all_notes.extend(m.notes)

        full_melody = Melody.of(*all_notes)

        # Wrap values in Slice
        def slice_note(n: Note[T_Value, T_Attr]) -> Note[Slice[T_Value], T_Attr]:
            return n.map_value(lambda v: Slice(v))

        sliced_melody = full_melody.map(slice_note)
        identified_value = Identified(part_id, sliced_melody)

        # Outer note wrapping the part
        attr: T_Attr = cast(Any, None)
        note = Note(identified_value, full_melody.total_duration, attr)
        elements.append(note)

    raw_vertical_notes = transpose_score_to_vertical(frozenset(elements))
    # We return the implementation, which satisfies the Protocol
    return VerticalScoreViewImpl(raw_vertical_notes, score_cls)


def transpose_score_to_vertical(
    elements: frozenset[Note[Identified[T_Id, Melody[Note[Slice[T_Value], T_Attr]]], T_Attr]],
) -> tuple[Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr], ...]:
    """
    Score.T の実装詳細。
    Score(Chord) の elements を受け取り、Score_T(Melody) のコンストラクタ引数となる tuple(notes) を返す。
    """
    # 1. 全てのパートの音符の変わり目（Offset）を収集する
    global_offsets = {Offset.of(0)}
    for part_note in elements:
        # part_note.value: Identified[T_Id, Melody[...]]
        melody = part_note.value.value
        current = Offset.of(0)
        for note in melody.notes:
            current = current.add_duration(note.duration)
            global_offsets.add(current)

    sorted_offsets = sorted(list(global_offsets))

    # 2. 各区間ごとに垂直スライス（Chord）を生成する
    vertical_slices: list[Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr]] = []

    for i in range(len(sorted_offsets) - 1):
        start = sorted_offsets[i]
        end = sorted_offsets[i + 1]
        interval_duration = Duration(end.value - start.value)

        chord_elements: list[Note[Identified[T_Id, Slice[T_Value]], T_Attr]] = []

        # 3. 各パートについて、この区間の音（Slice）を抽出
        for part_note in elements:
            part_id = part_note.value.id
            melody = part_note.value.value

            # この区間開始時点で鳴っている音符を取得
            note_start_offset, original_note = melody.at(start)
            original_slice = original_note.value

            # 結合属性の計算
            # 左側: 「区間の開始が音符の途中」または「元のSliceが左と繋がっている」場合
            is_middle_of_note_start = start.value > note_start_offset.value
            connects_left = is_middle_of_note_start or original_slice.connects_left

            # 右側: 「区間の終了が音符の途中」または「元のSliceが右と繋がっている」場合
            note_end_offset = note_start_offset.add_duration(original_note.duration)
            is_middle_of_note_end = end.value < note_end_offset.value
            connects_right = is_middle_of_note_end or original_slice.connects_right

            # 新しいSliceを生成
            new_slice = Slice(original_slice.value, connects_left, connects_right)

            # スライスされたNoteを生成（属性は元の音符から継承）
            chord_elements.append(
                Note(
                    value=Identified(part_id, new_slice),
                    duration=interval_duration,
                    attribute=original_note.attribute,
                )
            )

        # 4. 垂直方向の和音（Chord）を作成
        # New Chord takes generic elements (Notes)
        interval_chord = Chord(frozenset(chord_elements))

        # 5. 旋律の要素となるNoteを作成
        representative_attr = chord_elements[0].attribute

        vertical_slices.append(Note(value=interval_chord, duration=interval_duration, attribute=representative_attr))

    return tuple(vertical_slices)


def transpose_vertical_to_score(
    vertical_notes: tuple[Note[Chord[Note[Identified[T_Id, Slice[T_Value]], T_Attr]], T_Attr], ...],
) -> frozenset[Note[Identified[T_Id, Melody[Note[Slice[T_Value], T_Attr]]], T_Attr]]:
    """
    Score_T.T の実装詳細。
    Score_T(Melody) の notes を受け取り、Score(Chord) のコンストラクタ引数となる frozenset(elements) を返す。
    """
    # 1. IDごとにスライス（音符）を収集する辞書を作成
    part_map: dict[T_Id, list[Note[Slice[T_Value], T_Attr]]] = {}

    for vertical_slice_note in vertical_notes:
        chord = vertical_slice_note.value
        for part_note in chord.elements:
            p_id = part_note.value.id
            slice_val = part_note.value.value

            note_in_melody = Note(value=slice_val, duration=part_note.duration, attribute=part_note.attribute)

            if p_id not in part_map:
                part_map[p_id] = []
            part_map[p_id].append(note_in_melody)

    # 2. 水平方向にスライスを走査し、結合可能なものをマージする
    reconstructed_parts: list[Note[Identified[T_Id, Melody[Note[Slice[T_Value], T_Attr]]], T_Attr]] = []

    for p_id, slices in part_map.items():
        if not slices:
            continue

        merged_notes: list[Note[Slice[T_Value], T_Attr]] = []
        curr = slices[0]

        for next_n in slices[1:]:
            prev_s = curr.value
            next_s = next_n.value

            # 結合判定
            if prev_s.connects_right and next_s.connects_left and prev_s.value == next_s.value:
                # マージ処理
                new_dur = curr.duration + next_n.duration
                new_slice = Slice(
                    value=prev_s.value,
                    connects_left=prev_s.connects_left,
                    connects_right=next_s.connects_right,
                )
                curr = Note(new_slice, new_dur, curr.attribute)
            else:
                merged_notes.append(curr)
                curr = next_n

        merged_notes.append(curr)

        # 3. Melodyオブジェクトを作成し、PartとしてのNoteにラップする
        melody = Melody.of(*merged_notes)
        part_attr = merged_notes[0].attribute

        reconstructed_part_note = Note(
            value=Identified(p_id, melody), duration=melody.total_duration, attribute=part_attr
        )
        reconstructed_parts.append(reconstructed_part_note)

    return frozenset(reconstructed_parts)

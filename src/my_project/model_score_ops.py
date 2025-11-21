from typing import TypeVar

from my_project.model import (
    Chord,
    Duration,
    Identified,
    Melody,
    Note,
    Offset,
    Slice,
)

T_Value = TypeVar("T_Value")
T_Attr = TypeVar("T_Attr")
T_Id = TypeVar("T_Id")


def transpose_score_to_vertical(
    elements: frozenset[Note[Identified[T_Id, Melody[Slice[T_Value], T_Attr]], T_Attr]],
) -> tuple[Note[Chord[Identified[T_Id, Slice[T_Value]], T_Attr], T_Attr], ...]:
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
    vertical_slices: list[Note[Chord[Identified[T_Id, Slice[T_Value]], T_Attr], T_Attr]] = []

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
        interval_chord = Chord(frozenset(chord_elements))

        # 5. 旋律の要素となるNoteを作成
        representative_attr = chord_elements[0].attribute

        vertical_slices.append(Note(value=interval_chord, duration=interval_duration, attribute=representative_attr))

    return tuple(vertical_slices)


def transpose_vertical_to_score(
    vertical_notes: tuple[Note[Chord[Identified[T_Id, Slice[T_Value]], T_Attr], T_Attr], ...],
) -> frozenset[Note[Identified[T_Id, Melody[Slice[T_Value], T_Attr]], T_Attr]]:
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
    reconstructed_parts: list[Note[Identified[T_Id, Melody[Slice[T_Value], T_Attr]], T_Attr]] = []

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

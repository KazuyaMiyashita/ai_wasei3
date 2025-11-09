from dataclasses import dataclass

from my_project.counterpoint.model import (
    MEASURE_TOTAL_DURATION,
    AnnotatedMeasure,
    AnnotatedNote,
    RythmnType,
)
from my_project.model import (
    Duration,
    Offset,
    Pitch,
)


@dataclass(frozen=True)
class LocalMeasureContext:
    previous_measure: AnnotatedMeasure | None
    previous_cf: Pitch | None

    current_cf: Pitch
    next_measure_cf: Pitch | None
    # TODO: GlobalContext に類を持たせ、 rythmn_type はManageGlobalContextStateあたりで決める責務がある?
    rythmn_type: RythmnType

    is_first_measure: bool
    is_last_measure: bool
    is_next_last_measure: bool  # 次の小節は最終小節か。経過音の探索で利用する

    # 現在構築中の音符バッファ。最大で一小節に相当する音価の音が入る。最大の要素数は rythmn_type に依存する。
    note_buffer: list[AnnotatedNote]
    # 和音の設定。基本形は True, 第一転回形の場合は False, 未設定の場合は None
    is_root_chord: bool | None

    # 小節の探索の結果、次の小節の冒頭のピッチを決める必要がある場合、ピッチのみマーキングする
    next_measure_mark: Pitch | None

    def __post_init__(self) -> None:
        assert self.is_first_measure == (self.previous_cf is None)
        assert (self.previous_cf is None) == (self.previous_measure is None)
        assert self.is_last_measure == (self.next_measure_cf is None)
        if self.is_next_last_measure:
            assert self.next_measure_cf is not None
        assert self.total_note_buffer_duration() <= MEASURE_TOTAL_DURATION

    def is_buffer_fulfilled(self) -> bool:
        return self.total_note_buffer_duration() == MEASURE_TOTAL_DURATION

    # ---

    def total_note_buffer_duration(self) -> Duration:
        """
        バッファにある音価の合計。4未満の場合は探索中、4であれば探索完了を表す。
        """
        return sum([an.note.duration for an in self.note_buffer], Duration.of(0))

    def current_offset(self) -> Offset:
        """
        現在の探索中の小節の位置。探索中の場合値を 0 から 3 のいずれかから返し(rythmn_typeに依存する)、
        探索完了の場合に呼び出すと例外を出す。
        (total_note_buffer_durationよりも厳しい)
        """
        offset = Offset(self.total_note_buffer_duration().value)
        if offset in [Offset.of(0), Offset.of(1), Offset.of(2), Offset.of(3)]:
            return offset
        else:
            raise ValueError(f"invalid call of current_offset. offset: {offset}")

    def previous_latest_added_pitch(self) -> Pitch:
        """
        バッファと完了済みの小節を参照し、最後に追加された音を返す。
        課題の冒頭や、冒頭の休符の直後に利用すると例外。
        """
        if len(self.note_buffer) > 0:
            if self.note_buffer[-1].note.pitch:
                return self.note_buffer[-1].note.pitch
        # 冒頭以外で休符を利用することはないという前提
        elif self.previous_measure is not None and self.previous_measure.annotated_notes[-1].note.pitch:
            return self.previous_measure.annotated_notes[-1].note.pitch
        raise ValueError("previous_latest_added_pitch not found.")

    # --

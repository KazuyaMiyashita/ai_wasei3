from dataclasses import dataclass, replace

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


# ------ GlobalContext --------


@dataclass(frozen=True)
class GlobalContext:
    cantus_firmus: list[Pitch]
    rythmn_type: RythmnType
    completed_measures: list[AnnotatedMeasure]
    next_measure_mark: Pitch | None

    def __post_init__(self) -> None:
        # CFは空ではない。(通常は2つ以上。最小で1つだがそれは最終小節として扱われ全音符が実施されるだけになる。)
        assert self.cantus_firmus
        # 完了した小節の長さはCFの長さと同じかそれよりも少ない
        assert len(self.completed_measures) <= len(self.cantus_firmus)

    @classmethod
    def new_global_context(cls, cantus_firmus: list[Pitch], rythmn_type: RythmnType) -> "GlobalContext":
        return cls(
            cantus_firmus=cantus_firmus,
            rythmn_type=rythmn_type,
            completed_measures=[],
            next_measure_mark=None,
        )

    def local_ctx_appended(self, local_ctx: LocalMeasureContext) -> "GlobalContext":
        """
        この小節に実施済みの local_ctx を追加した新しい GlobalContext を返す

        local_ctx のバッファが埋まっていない状態で呼び出すと例外
        """
        assert local_ctx.is_buffer_fulfilled()

        return replace(
            self,
            completed_measures=[
                *self.completed_measures,
                AnnotatedMeasure(local_ctx.note_buffer),
            ],
            next_measure_mark=local_ctx.next_measure_mark,
        )

    def new_local_measure_countext(self) -> LocalMeasureContext:
        return LocalMeasureContext(
            previous_measure=self._previous_measure(),
            previous_cf=self._previous_cf(),
            current_cf=self._current_cf(),
            next_measure_cf=self._next_measure_cf(),
            rythmn_type=self.rythmn_type,
            is_first_measure=self._is_first_measure(),
            is_last_measure=self._is_last_measure(),
            is_next_last_measure=self._is_next_last_measure(),
            note_buffer=[],
            is_root_chord=None,
            next_measure_mark=self.next_measure_mark,
        )

    def is_measures_fulfilled(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus)

    # 以下は new_local_measure_countext のためのヘルパー。
    # LocalMeasureContext に情報が渡されるので、 GlobalContext を参照しないこと。

    def _is_first_measure(self) -> bool:
        return len(self.completed_measures) == 0

    def _is_last_measure(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus) - 1

    def _is_next_last_measure(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus) - 2

    def _previous_measure(self) -> AnnotatedMeasure | None:
        if self._is_first_measure():
            return None
        else:
            return self.completed_measures[len(self.completed_measures) - 1]

    def _previous_cf(self) -> Pitch | None:
        if self._is_first_measure():
            return None
        else:
            return self.cantus_firmus[len(self.completed_measures) - 1]

    def _current_cf(self) -> Pitch:
        return self.cantus_firmus[len(self.completed_measures)]

    def _next_measure_cf(self) -> Pitch | None:
        if self._is_last_measure():
            return None
        else:
            return self.cantus_firmus[len(self.completed_measures) + 1]

import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass

import my_project.counterpoint.search_end_note as search_end_note
import my_project.counterpoint.search_harmonic_note as search_harmonic_note
import my_project.counterpoint.search_neighbor_tone as search_neighbor_tone
import my_project.counterpoint.search_passing_tone as search_passing_tone
import my_project.counterpoint.search_start_note as search_start_note
import my_project.counterpoint.validator as validator
from my_project.counterpoint.context import LocalMeasureContext
from my_project.counterpoint.model import (
    MEASURE_TOTAL_DURATION,
)
from my_project.model import (
    Duration,
    Offset,
)
from my_project.util import shuffled_interleave

# ---


class LocalMeasureState(ABC):
    local_ctx: LocalMeasureContext

    @abstractmethod
    def next_states(self) -> Iterator["LocalMeasureState"]:
        pass

    def _find_terminal_states(self, randomized: bool = True) -> Iterator["MeasureEndState | MeasurePrunedState"]:
        if isinstance(self, MeasureEndState):
            yield self
            return
        elif isinstance(self, MeasurePrunedState):
            # 小節単位のバリデーションに失敗した時は、直前から再試行した方が早いように感じる。
            # (冒頭あたりで連続のバリデーションに失敗した場合は無駄ではある)
            # yield self  # 最初から再試行。一度返した後に呼び出し側の final_states で破棄する。
            yield from []  # 直前から再試行させる場合。
            return
        else:
            child_states = self.next_states()
            child_iterators = (child._find_terminal_states(randomized) for child in child_states)
            # 結果にバラエティを持たせるためにランダムに並び替える
            yield from shuffled_interleave(child_iterators, randomized)

    def final_states(self, randomized: bool = True) -> Iterator["MeasureEndState"]:
        return (s for s in self._find_terminal_states(randomized) if isinstance(s, MeasureEndState))


@dataclass(frozen=True)
class ChooseSearchState(LocalMeasureState):
    """
    小節の探索方法を選ぶか、バリデーションの状態に移動する
    """

    local_ctx: LocalMeasureContext

    def next_states(self) -> Iterator[LocalMeasureState]:
        if self.local_ctx.is_buffer_fulfilled():
            yield ValidatingInMeasureState(self.local_ctx)
        elif self.local_ctx.is_last_measure:
            yield SearchingEndNoteState(self.local_ctx)
        elif self.local_ctx.is_first_measure and self.local_ctx.current_offset() == Offset.of(0):
            yield SearchingStartNoteState(self.local_ctx)
        else:
            next_states: list[LocalMeasureState] = [
                SearchingHarmonicNoteInMeasureState(self.local_ctx),
                SearchingPassingNoteInMeasureState(self.local_ctx),
                SearchingNeighborNoteInMeasureState(self.local_ctx),
            ]
            yield from next_states


class SearchNoteState(LocalMeasureState):
    """
    音を追加する系ステート
    """

    local_ctx: LocalMeasureContext

    @abstractmethod
    def next_ctxs(self) -> list[LocalMeasureContext]:
        pass

    def next_states(self) -> Iterator[LocalMeasureState]:
        new_local_ctxs = self.next_ctxs()
        # 結果にバラエティを持たせるためにランダムに並び替える
        random.shuffle(new_local_ctxs)
        next_states = [ChooseSearchState(new_local_ctx) for new_local_ctx in new_local_ctxs]
        yield from next_states


@dataclass(frozen=True)
class SearchingStartNoteState(SearchNoteState):
    """
    課題冒頭の音を選択する
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)
        assert self.local_ctx.next_measure_mark is None

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_start_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingEndNoteState(SearchNoteState):
    """
    課題の最後の小節の和声音を選択する
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_end_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingHarmonicNoteInMeasureState(SearchNoteState):
    """
    小節内の探索中。和声音を1音追加する
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_harmonic_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingPassingNoteInMeasureState(SearchNoteState):
    """
    小節内の探索中。経過音を追加する。note_bufferに2つ以上の音が追加され、next_measure_markが付くこともある。
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_passing_tone.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingNeighborNoteInMeasureState(SearchNoteState):
    """
    小節内の探索中。刺繍音を追加する。note_bufferに2つの音が追加され、next_measure_markが付くこともある。
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_neighbor_tone.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class ValidatingInMeasureState(LocalMeasureState):
    """
    小節内のバリデーション中。

    note_buffer の音に対して連続・並達の禁則が含まれるものを除外する。
    (next_measure_mark に関しては次の小節が埋まった時のバリデーションで確認される)
    """

    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.is_buffer_fulfilled()

    def next_states(self) -> Iterator[LocalMeasureState]:
        if not validator.validate(self.local_ctx):
            yield MeasurePrunedState(self.local_ctx)
            return
        else:
            yield MeasureEndState(self.local_ctx)


@dataclass(frozen=True)
class MeasurePrunedState(LocalMeasureState):
    """
    小節のバリデーションに失敗した。
    この結果は破棄されるのだが、破棄された件数やその時の情報が欲しくなると思うので、このステートを経由して呼び出し側で破棄させる。
    """

    local_ctx: LocalMeasureContext

    def next_states(self) -> Iterator[LocalMeasureState]:
        raise RuntimeError("not called")


@dataclass(frozen=True)
class MeasureEndState(LocalMeasureState):
    """
    小節のバリデーションが終わり、生成が完了した状態。
    """

    local_ctx: LocalMeasureContext

    def next_states(self) -> Iterator[LocalMeasureState]:
        raise RuntimeError("not called")

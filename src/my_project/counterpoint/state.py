from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass

import my_project.counterpoint.all_measure_validator as all_measure_validator
import my_project.counterpoint.search_end_note as search_end_note
import my_project.counterpoint.search_harmonic_note as search_harmonic_note
import my_project.counterpoint.search_neighbor_tone as search_neighbor_tone
import my_project.counterpoint.search_passing_tone as search_passing_tone
import my_project.counterpoint.search_start_note as search_start_note
import my_project.counterpoint.validator as validator
from my_project.counterpoint.context import GlobalContext, LocalMeasureContext
from my_project.counterpoint.model import (
    CF_PART_ID,
    KEY,
    MEASURE_TOTAL_DURATION,
    REALIZE_PART_ID,
    TIME_SIGNATURE,
    RythmnType,
)
from my_project.model import (
    Duration,
    Measure,
    Note,
    Offset,
    Part,
    Pitch,
    Score,
)
from my_project.util import shuffled_interleave


def generate(cantus_firmus: list[Pitch], rythmn_type: "RythmnType") -> Iterator[Score]:
    return map(lambda state: state.to_score(), State.start_state(cantus_firmus, rythmn_type).final_states())


# ---


@dataclass(frozen=True)
class State(ABC):
    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext | None  # ValidatingAllMeasureState, EndState, PrunedState で None

    @classmethod
    def start_state(cls, cantus_firmus: list[Pitch], rythmn_type: RythmnType) -> "State":
        g_ctx = GlobalContext.new_global_context(cantus_firmus, rythmn_type)
        return ChooseSearchState(g_ctx, g_ctx.new_local_measure_countext())

    @abstractmethod
    def next_states(self) -> Iterator["State"]:
        pass

    def _find_terminal_states(self, randomized: bool = True) -> Iterator["EndState | PrunedState"]:
        if isinstance(self, EndState) or isinstance(self, PrunedState):
            # EndState は当然採用。PrunedState の場合、課題の最初から再試行させるために一度返した後に捨てる
            yield self
            return
        elif isinstance(self, MeasurePrunedState):
            # MeasurePrunedState は破棄し、直前の音を再試行させる。
            yield from []
            return
        else:
            # その他はそれぞれの方法で探索。結果にバラエティを持たせるために次のステートをランダムに並び替える
            child_states = self.next_states()
            child_iterators = [child._find_terminal_states(randomized) for child in child_states]
            yield from shuffled_interleave(child_iterators, randomized)

    def final_states(self, randomized: bool = True) -> Iterator["EndState"]:
        return (s for s in self._find_terminal_states(randomized) if isinstance(s, EndState))


@dataclass(frozen=True)
class ChooseSearchState(State):
    """
    小節の探索方法を選ぶか、バリデーションの状態に移動する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def next_states(self) -> Iterator["State"]:
        if self.local_ctx.is_buffer_fulfilled():
            yield ValidatingInMeasureState(self.global_ctx, self.local_ctx)
        elif self.local_ctx.is_last_measure:
            yield SearchingEndNoteState(self.global_ctx, self.local_ctx)
        elif self.local_ctx.is_first_measure and self.local_ctx.current_offset() == Offset.of(0):
            yield SearchingStartNoteState(self.global_ctx, self.local_ctx)
        else:
            next_states: list[State] = [
                SearchingHarmonicNoteInMeasureState(self.global_ctx, self.local_ctx),
                SearchingPassingNoteInMeasureState(self.global_ctx, self.local_ctx),
                SearchingNeighborNoteInMeasureState(self.global_ctx, self.local_ctx),
            ]
            yield from next_states


@dataclass(frozen=True)
class SearchNote(State):
    """
    音を追加する系ステート
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    @abstractmethod
    def next_ctxs(self) -> list[LocalMeasureContext]:
        pass

    def next_states(self) -> Iterator["State"]:
        new_local_ctxs = self.next_ctxs()
        next_states = [ChooseSearchState(self.global_ctx, new_local_ctx) for new_local_ctx in new_local_ctxs]
        yield from next_states


@dataclass(frozen=True)
class SearchingStartNoteState(SearchNote):
    """
    課題冒頭の音を選択する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)
        assert self.local_ctx.next_measure_mark is None

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_start_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingEndNoteState(SearchNote):
    """
    課題の最後の小節の和声音を選択する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_end_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingHarmonicNoteInMeasureState(SearchNote):
    """
    小節内の探索中。和声音を1音追加する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_harmonic_note.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingPassingNoteInMeasureState(SearchNote):
    """
    小節内の探索中。経過音を追加する。note_bufferに2つ以上の音が追加され、next_measure_markが付くこともある。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_passing_tone.next_ctxs(self.local_ctx)


@dataclass(frozen=True)
class SearchingNeighborNoteInMeasureState(SearchNote):
    """
    小節内の探索中。刺繍音を追加する。note_bufferに2つの音が追加され、next_measure_markが付くこともある。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_ctxs(self) -> list[LocalMeasureContext]:
        return search_neighbor_tone.next_ctxs(self.local_ctx)


# ------------ ValidatingInMeasureState --------------


@dataclass(frozen=True)
class ValidatingInMeasureState(State):
    """
    小節内のバリデーション中。

    note_buffer の音に対して連続・並達の禁則が含まれるものを除外する。
    (next_measure_mark に関しては次の小節が埋まった時のバリデーションで確認される)
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.is_buffer_fulfilled()

    def next_states(self) -> Iterator["State"]:
        if not validator.validate(self.local_ctx):
            yield MeasurePrunedState(self.global_ctx, self.local_ctx)
            return
        else:
            new_global_ctx = self.global_ctx.local_ctx_appended(self.local_ctx)

            if new_global_ctx.is_measures_fulfilled():
                yield ValidatingAllMeasureState(new_global_ctx, None)
            else:
                yield ChooseSearchState(new_global_ctx, new_global_ctx.new_local_measure_countext())


@dataclass(frozen=True)
class MeasurePrunedState(State):
    """
    旋律のバリデーションに失敗した。
    この結果は破棄されるのだが、破棄された件数やその時の情報が欲しくなると思うので、このステートを経由して呼び出し側で破棄させる。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def next_states(self) -> Iterator["State"]:
        raise RuntimeError("not called")


# ------------ ValidatingAllMeasureState --------------


@dataclass(frozen=True)
class ValidatingAllMeasureState(State):
    """
    全体のバリデーション中
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self) -> Iterator[State]:
        if not all_measure_validator.validate(self.global_ctx):
            yield PrunedState(self.global_ctx, None)

        else:
            yield EndState(self.global_ctx, None)


# ------------ EndState --------------


@dataclass(frozen=True)
class EndState(State):
    """
    課題全体のバリデーションが終わり、生成が完了した状態。
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self) -> Iterator["State"]:
        raise RuntimeError("not called")

    def to_score(self) -> Score:
        cf_notes = [Note(pitch, Duration.of(4)) for pitch in self.global_ctx.cantus_firmus]
        cf_measures = [Measure([note]) for note in cf_notes]

        realized_measures = [am.to_measure() for am in self.global_ctx.completed_measures]

        return Score(
            key=KEY,
            time_signature=TIME_SIGNATURE,
            parts=[
                Part(part_id=CF_PART_ID, measures=cf_measures),
                Part(part_id=REALIZE_PART_ID, measures=realized_measures),
            ],
        )


# ------------ PrunedState --------------


@dataclass(frozen=True)
class PrunedState(State):
    """
    課題全体のバリデーションに失敗した。
    途中からではなく最初からやり直すために、擬似的な完了状態にして呼び出し側でフィルタさせる。
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self) -> Iterator["State"]:
        raise RuntimeError("not called")

import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass

import my_project.counterpoint.all_measure_validator as all_measure_validator
from my_project.counterpoint.global_context import GlobalContext
from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.local_measure_state import ChooseSearchState, MeasureEndState
from my_project.counterpoint.model import (
    CF_PART_ID,
    KEY,
    REALIZE_PART_ID,
    TIME_SIGNATURE,
    RythmnType,
)
from my_project.model import (
    Duration,
    Measure,
    Note,
    Part,
    Pitch,
    Score,
)
from my_project.util import shuffled_interleave


def generate(cantus_firmus: list[Pitch], rythmn_type: RythmnType) -> Iterator[Score]:
    random.seed()
    return map(lambda state: state.to_score(), GlobalState.start_state(cantus_firmus, rythmn_type).final_states())


class GlobalState(ABC):
    """
    課題全体を解くステート
    """

    global_ctx: GlobalContext

    @classmethod
    def start_state(cls, cantus_firmus: list[Pitch], rythmn_type: RythmnType) -> "GlobalState":
        return GenerateMeasureState(GlobalContext.new_global_context(cantus_firmus, rythmn_type))

    @abstractmethod
    def next_states(self) -> Iterator["GlobalState"]:
        pass

    def _find_terminal_states(self, randomized: bool = True) -> Iterator["EndState | PrunedState"]:
        if isinstance(self, EndState):
            yield self
            return
        elif isinstance(self, PrunedState):
            # 課題全体のバリデーションに失敗した時は最初からやり直しした方が良い。
            # 例えば冒頭あたりで音域のバリデーションに失敗した時は、末尾の小節を変更したところで意味がない。
            yield self  # 最初から再試行。一度返した後に呼び出し側の final_states で破棄する。
            # yield from [] # 直前から再試行させる場合。
            return
        else:
            # その他はそれぞれの方法で探索。
            child_states = self.next_states()
            child_iterators = (child._find_terminal_states(randomized) for child in child_states)
            # 結果にバラエティを持たせるためにランダムに並び替える
            yield from shuffled_interleave(child_iterators, randomized)

    def final_states(self, randomized: bool = True) -> Iterator["EndState"]:
        return (s for s in self._find_terminal_states(randomized) if isinstance(s, EndState))


@dataclass(frozen=True)
class GenerateMeasureState(GlobalState):
    """
    小節を生成するステート。LocalMeasureStateとの橋渡しの役目を担う。
    """

    global_ctx: GlobalContext

    def next_states(self) -> Iterator[GlobalState]:
        local_ctx: LocalMeasureContext = self.global_ctx.new_local_measure_countext()

        start_local_state = ChooseSearchState(local_ctx)
        local_final_states: Iterator[MeasureEndState] = start_local_state.final_states()

        def create_next_global_state(measure_end_state: MeasureEndState) -> GlobalState:
            new_global_ctx = self.global_ctx.local_ctx_appended(measure_end_state.local_ctx)
            if new_global_ctx.is_measures_fulfilled():
                return ValidatingAllMeasureState(new_global_ctx)
            else:
                return GenerateMeasureState(new_global_ctx)

        yield from map(create_next_global_state, local_final_states)


@dataclass(frozen=True)
class ValidatingAllMeasureState(GlobalState):
    """
    課題全体のバリデーション中
    """

    global_ctx: GlobalContext

    def next_states(self) -> Iterator[GlobalState]:
        if not all_measure_validator.validate(self.global_ctx):
            yield PrunedState(self.global_ctx)
        else:
            yield EndState(self.global_ctx)


# ------------ EndState --------------


@dataclass(frozen=True)
class EndState(GlobalState):
    """
    課題全体のバリデーションが終わり、生成が完了した状態。
    """

    global_ctx: GlobalContext

    def next_states(self) -> Iterator["GlobalState"]:
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
class PrunedState(GlobalState):
    """
    課題全体のバリデーションに失敗した。
    途中からではなく最初からやり直すために、擬似的な完了状態にして呼び出し側でフィルタさせる。
    """

    global_ctx: GlobalContext

    def next_states(self) -> Iterator["GlobalState"]:
        raise RuntimeError("not called")

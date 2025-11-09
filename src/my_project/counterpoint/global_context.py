from dataclasses import dataclass, replace

from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.model import (
    AnnotatedMeasure,
    RythmnType,
)
from my_project.model import (
    Pitch,
)

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

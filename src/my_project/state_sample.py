import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass


@dataclass(frozen=True)
class State(ABC):
    list: list[int]

    @classmethod
    def start_state(cls) -> "State":
        return AppendingState([])

    @abstractmethod
    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        pass

    def final_states(self) -> Iterator["State"]:
        if isinstance(self, EndState):
            yield self
            return
        for next_state in self.next_states():
            yield from next_state.final_states()


@dataclass(frozen=True)
class AppendingState(State):
    list: list[int]

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        next_states = [EachCheckState([*self.list, i]) for i in range(0, 10)]
        # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class EachCheckState(State):
    list: list[int]

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        """
        隣接する2つの数値の差が3以上であればNGとする
        """

        is_valid = False
        if len(self.list) <= 1:
            is_valid = True
        else:
            diff = self.list[-1] - self.list[-2]
            if abs(diff) < 3:
                is_valid = True

        if is_valid:
            if len(self.list) < 10:
                yield AppendingState(self.list)
            else:
                yield FinalCheckState(self.list)
        else:
            # バリデーションNG。探索停止
            yield from []


@dataclass(frozen=True)
class FinalCheckState(State):
    list: list[int]

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if sum(self.list) <= 30:
            yield EndState(self.list)
            return
        else:
            yield from []
            return


@dataclass(frozen=True)
class EndState(State):
    list: list[int]

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        raise RuntimeError("not called")


def main() -> None:
    state = State.start_state()

    for i in range(0, 10):
        solved = next(state.final_states())
        print(f"i={i}, list: {solved.list}, sum: {sum(solved.list)}")


if __name__ == "__main__":
    main()

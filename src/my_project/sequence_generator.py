# 適当な条件で旋律を探索するプログラム。対位法のプログラムのための練習。
# ・1小節には四分音符が4個
# ・最初の音はド
# ・旋律は前の音から順次進行するか、5度上下のいずれか
# ・音域内の、#bがない音だけを使う
# ・1小節に同じ音が2つ以上含まれてはいけない
# ・4小節作ったらおわり
# ・ドで終わる
import random
from collections.abc import Iterator
from dataclasses import dataclass
from fractions import Fraction

from my_project.lilypond_writer import score_to_lilypond
from my_project.model import (
    Duration,
    IntervalAlter,
    IntervalStep,
    Key,
    Measure,
    Mode,
    Note,
    NoteName,
    Part,
    PartId,
    Pitch,
    Score,
    TimeSignature,
)
from my_project.util import add_interval_step_in_key, is_in_part_range


@dataclass(frozen=True)
class State:
    previous_measures: list[Measure]
    current_measure_notes: list[Note]


def next(state: State) -> Iterator[State]:
    if not state.previous_measures and not state.current_measure_notes:
        # 最初の状態
        next_state = State(
            previous_measures=[],
            current_measure_notes=[make_note(Pitch.parse("C4"))],
        )
        yield from next(next_state)

    elif len(state.previous_measures) == 4:
        # 最後の状態
        if last_check(state):
            yield state
        else:
            yield from []
    elif len(state.current_measure_notes) == 4:
        # 小節が埋まった。1小節に同じ音が2つ以上含まれていたらアウト
        if has_no_duplicate_pitches(state.current_measure_notes):
            next_state = State(
                previous_measures=[*state.previous_measures, notes_to_measure(state.current_measure_notes)],
                current_measure_notes=[],
            )
            yield from next(next_state)
        else:
            yield from []
    else:
        # 現在の小節が埋まっていない状態
        last_note_pitch = get_last_note_pitch(state)
        for next_pitch in iter_next_pitch(last_note_pitch):
            note: Note = make_note(next_pitch)
            next_state = State(
                previous_measures=state.previous_measures,
                current_measure_notes=[*state.current_measure_notes, note],
            )
            yield from next(next_state)


def make_note(pitch: Pitch) -> Note:
    return Note(pitch, Duration(Fraction(1)))


def notes_to_measure(notes: list[Note]) -> Measure:
    assert len(notes) == 4
    return Measure(notes)


def has_no_duplicate_pitches(notes: list[Note]) -> bool:
    pitches = [note.pitch for note in notes]
    return len(pitches) == len(set(pitches))


def get_last_note_pitch(state: State) -> Pitch:
    if len(state.current_measure_notes) > 0:
        maybe_pitch = state.current_measure_notes[-1].pitch
    else:
        maybe_pitch = state.previous_measures[-1].notes[-1].pitch
    if not maybe_pitch:
        raise RuntimeError()
    return maybe_pitch


def iter_next_pitch(current_pitch: Pitch) -> Iterator[Pitch]:
    # 旋律は前の音から順次進行するか、5度上下のいずれか
    possible_intervals = [
        IntervalStep.idx_1(2),
        IntervalStep.idx_1(-2),
        IntervalStep.idx_1(5),
        IntervalStep.idx_1(-5),
    ]

    # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
    random.shuffle(possible_intervals)

    for interval_step in possible_intervals:
        next_pitch = add_interval_step_in_key(
            key=Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR),
            pitch=current_pitch,
            interval_step=interval_step,
        )
        if not is_in_part_range(next_pitch, PartId.SOPRANO):
            yield from []
        elif not (IntervalAlter(-2) <= (next_pitch - current_pitch).alter() <= IntervalAlter(1)):
            # 完全・長・短・減音程のみを許可する
            yield from []
        else:
            yield next_pitch


def last_check(state: State) -> bool:
    return get_last_note_pitch(state).note_name == NoteName.parse("C")


def to_score(state: State) -> Score:
    assert len(state.previous_measures) == 4
    assert not state.current_measure_notes
    return Score(
        key=Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR),
        time_signature=TimeSignature(4, Fraction(1)),
        parts=[Part(part_id=PartId.SOPRANO, measures=state.previous_measures)],
    )


def generate() -> Iterator[Score]:
    state = State(previous_measures=[], current_measure_notes=[])
    return map(to_score, next(state))


if __name__ == "__main__":
    print("hello")
    for i, score in enumerate(generate()):
        print(f"試行: {i}")  # i は 0 から始まります
        print(score_to_lilypond(score))
        print()

        # i が 99 に達したら (これが 100 回目の試行)、ループを抜ける
        if i >= 99:
            print("--- 100個見つかったので停止します ---")
            break

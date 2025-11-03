import itertools
import random
from collections.abc import Iterator
from dataclasses import dataclass, replace
from enum import Enum
from fractions import Fraction

from my_project.model import (
    Duration,
    Interval,
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
from my_project.util import add_interval_step_in_key, compare_pitch, part_range, scale_pitches, shuffled_interleave

KEY = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
TIME_SIGNATURE = TimeSignature(4, Fraction(1))
NOTES_IN_MEASURE = 4
CF_PART_ID = PartId.BASS
RELIZE_PART_ID = PartId.SOPRANO


def generate(cantus_firmus: list[Pitch]) -> Iterator[Score]:
    return map(lambda state: to_score(cantus_firmus, state), State.init_state(cantus_firmus).iter_next())


## ------


class ToneType(Enum):
    HARMONIC_TONE = 1
    PASSING_TONE = 2
    NEIGHBOR_TONE = 3


@dataclass(frozen=True)
class AnnotatedNote:
    note: Note
    tone_type: ToneType


class StateType(Enum):
    START = 1  # 最初の状態
    SEARCHING = 2  # 探索中
    CURRENT_MEASURE_FULFILLED = 3  # 現在の小節が埋まった。小節単位のバリデーションを実行する
    ALL_MEASURE_FULFILLED = 4  # 全ての小節が埋まった。全ての小節のバリデーションを実行する


@dataclass(frozen=True)
class State:
    unprocessed_cfs: list[Pitch]  # 処理中はリストに含め、処理が終わったらリストから除外する
    previous_measures: list[Measure]
    current_measure_notes: list[Note]  # on_searching の後、一時的に要素が4つ以上になる

    def state_type(self) -> StateType:
        if not self.previous_measures and not self.current_measure_notes:
            return StateType.START
        elif sum([note.duration for note in self.current_measure_notes], Duration.of(0)) >= Duration.of(
            NOTES_IN_MEASURE
        ):
            return StateType.CURRENT_MEASURE_FULFILLED
        elif not self.unprocessed_cfs:
            return StateType.ALL_MEASURE_FULFILLED
        else:
            return StateType.SEARCHING

    def iter_next(self) -> Iterator["State"]:
        match self.state_type():
            case StateType.START:
                for next_state in on_start(self):
                    yield from next_state.iter_next()
            case StateType.SEARCHING:
                for next_state in on_searching(self):
                    yield from next_state.iter_next()
            case StateType.CURRENT_MEASURE_FULFILLED:
                for next_state in on_current_measure_fulfilled(self):
                    yield from next_state.iter_next()
            case StateType.ALL_MEASURE_FULFILLED:
                yield from on_all_measure_fulfilled(self)
                return

    @classmethod
    def init_state(cls, cfs: list[Pitch]) -> "State":
        return State(
            unprocessed_cfs=cfs,
            previous_measures=[],
            current_measure_notes=[],
        )


## ---


# 最初の状態
def on_start(state: State, randomized: bool = True) -> Iterator[State]:
    # - 冒頭小節は四部休符から始める

    cf = state_current_target_cf(state)
    possible_pitches: list[Pitch] = start_or_end_available_pitches(cf)

    next_states: list[State] = []
    for pitch in possible_pitches:
        next_state = replace(
            state,
            current_measure_notes=[
                Note(pitch=None, duration=Duration.of(1)),
                Note(pitch=pitch, duration=Duration.of(1)),
            ],
        )
        next_states.append(next_state)

    # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
    if randomized:
        random.shuffle(next_states)
    yield from next_states


### ---

# 探索中


def on_searching(state: State, randomized: bool = True) -> Iterator[State]:
    # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
    it: Iterator[State] = shuffled_interleave(
        [
            on_searching_harmonic_tone(state, randomized),
            on_searching_passing_tone(state, randomized),
            on_searching_neighbor_tone(state, randomized),
        ],
        randomized,
    )
    yield from it


def on_searching_harmonic_tone(state: State, randomized: bool = True) -> Iterator[State]:
    available_interval_steps = set([IntervalStep.idx_1(i) for i in [-8, -6, -5, -4, -3, -2, 2, 3, 4, 5, 6, 8]])

    # 協和音として利用できる音のなかから、2~6度または8度の進行を探す
    cf = state_current_target_cf(state)
    is_target_last = state_is_target_last(state)
    previous_pitch = state_previous_pitch(state)

    available_pitches = []
    if is_target_last:
        available_pitches = start_or_end_available_pitches(cf)
    else:
        available_pitches = available_harmonic_pitches(cf)

    possible_pitches: list[Pitch] = []
    for pitch in available_pitches:
        step = (pitch - previous_pitch).step()
        if step in available_interval_steps:
            possible_pitches.append(pitch)

    next_states: list[State] = []
    for pitch in possible_pitches:
        if is_target_last:
            next_state = state_append_last_pitch(state, pitch)
        else:
            next_state = state_append_pitches(state, [pitch])
        next_states.append(next_state)

    # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
    if randomized:
        random.shuffle(next_states)
    yield from next_states


def on_searching_passing_tone(state: State, randomized: bool = True) -> Iterator[State]:
    yield from []  # TODO


def on_searching_neighbor_tone(state: State, randomized: bool = True) -> Iterator[State]:
    # 刺繍音を利用する
    # 最終小節や、小節の最後の音では利用できない。
    if state_is_target_last(state) or (
        len(state.unprocessed_cfs) == 2 or len(state.current_measure_notes) == NOTES_IN_MEASURE - 1
    ):
        yield from []
    else:
        # 2つ次の音でCFと協和する音の一覧を求める。
        # 2つ次の音が最終小節かどうかで協和の条件が変わり、
        # 現在処理している位置によって対象とするCFが今の小節か次の小節か変わる。
        if len(state.unprocessed_cfs) == 2 and len(state.current_measure_notes) == NOTES_IN_MEASURE - 2:
            last_cf = state_next_target_cf(state)
            next_2_possible_pitches = start_or_end_available_pitches(last_cf)
        else:
            if len(state.current_measure_notes) <= NOTES_IN_MEASURE - 3:  # 0 or 1
                cf = state_current_target_cf(state)
            else:
                cf = state_next_target_cf(state)
            next_2_possible_pitches = available_harmonic_pitches(cf)

        previous_pitch = state_previous_pitch(state)
        if previous_pitch in next_2_possible_pitches:
            # 現在の音から2度下・2度上
            next_pitches = [
                add_interval_step_in_key(KEY, previous_pitch, interval_step)
                for interval_step in [IntervalStep.idx_1(-2), IntervalStep.idx_1(2)]
            ]
            next_pitches = filter_available_pitches(next_pitches)

            next_states: list[State] = []
            for next_pitch in next_pitches:
                pitches_to_add = [next_pitch, previous_pitch]
                next_state = state_append_pitches(state, pitches_to_add)
                next_states.append(next_state)

            # 試行結果がバラエティを富むように、順序をランダムにシャッフルして探索する
            if randomized:
                random.shuffle(next_states)
            yield from next_states

        else:
            yield from []


### ---


# 現在の小節が埋まった。ステートを更新した上で、ステートの小節単位のバリデーションを実行する
def on_current_measure_fulfilled(state: State) -> Iterator[State]:
    next_state = State(
        # unprocessed_cfs の先頭を削除
        unprocessed_cfs=state.unprocessed_cfs[1:],
        # current_measure_notes に 4つ以上の音が含まれるので、先頭4つを切り出して previous_measures の末尾に追加
        previous_measures=[*state.previous_measures, Measure(state.current_measure_notes[:NOTES_IN_MEASURE])],
        # 残りは current_measure_notes は入れたままにする。
        current_measure_notes=state.current_measure_notes[NOTES_IN_MEASURE:],
    )

    validated = True  # TODO

    if validated:
        yield next_state
    else:
        yield from []


# 全ての小節が埋まった。全ての小節のバリデーションを実行する
def on_all_measure_fulfilled(state: State) -> Iterator[State]:
    validated = True  # TODO

    if validated:
        yield state
    else:
        yield from []


## ---

AVAILABLE_PITCHES_LIST: list[Pitch] = scale_pitches(KEY, part_range(RELIZE_PART_ID))
AVAILABLE_PITCHES_SET: set[Pitch] = set(AVAILABLE_PITCHES_LIST)


def filter_available_pitches(pitches: list[Pitch]) -> list[Pitch]:
    return [pitch for pitch in pitches if pitch in AVAILABLE_PITCHES_SET]


def make_note(pitch: Pitch) -> Note:
    return Note(pitch, Duration(Fraction(1)))


def state_append_pitches(state: State, pitches: list[Pitch]) -> State:
    """
    ステートにいくつかの音を追加。これにより一時的に current_measure_notes に4つ以上の音が含まれるが、
    それらは on_current_measure_fulfilled でバリデーションとともに処理される
    """
    return replace(
        state,
        current_measure_notes=[*state.current_measure_notes, *[make_note(p) for p in pitches]],
    )


def state_append_last_pitch(state: State, pitch: Pitch) -> State:
    """
    最後の全音符を追加する
    """
    note: Note = Note(pitch, Duration.of(4))
    return replace(
        state,
        current_measure_notes=[*state.current_measure_notes, note],
    )


def state_is_target_last(state: State) -> bool:
    """
    このステートがあと一つ音符を追加するだけという状態かどうか
    """
    return len(state.unprocessed_cfs) == 1


def state_current_target_cf(state: State) -> Pitch:
    return state.unprocessed_cfs[0]


def state_next_target_cf(state: State) -> Pitch:
    return state.unprocessed_cfs[1]


def state_previous_pitch(state: State) -> Pitch:
    iter1 = (note.pitch for note in reversed(state.current_measure_notes) if note.pitch)
    iter2 = (
        note.pitch for measure in reversed(state.previous_measures) for note in reversed(measure.notes) if note.pitch
    )
    return next(itertools.chain(iter1, iter2))


def start_or_end_available_pitches(cf: Pitch) -> list[Pitch]:
    """
    冒頭または最終小節で利用可能な音を返す。
    CFと完全1度・完全5度・その複音程で、2オクターブの範囲、声域内。
    """
    intervals = [
        Interval.parse("P1"),
        Interval.parse("P5"),
        Interval.parse("P8"),
        Interval.parse("P12"),
        Interval.parse("P15"),
    ]
    return [pitch for pitch in [cf + interval for interval in intervals] if pitch in AVAILABLE_PITCHES_SET]


def available_harmonic_pitches(cf: Pitch) -> list[Pitch]:
    """
    冒頭または最終小節以外で、協和音として利用できる音を返す
    CFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
    """
    return [
        pitch
        for pitch in AVAILABLE_PITCHES_LIST  # 声域内の調の音
        if compare_pitch(cf, pitch) <= 0  # CFより上方
        and Interval.of(cf, pitch).step() <= IntervalStep.idx_1(15)  # 2オクターブ未満
        and (
            Interval.of(cf, pitch).normalize().step()
            in [
                IntervalStep.idx_1(1),
                IntervalStep.idx_1(3),
                IntervalStep.idx_1(5),
                IntervalStep.idx_1(6),
            ]
        )
    ]


## ---


def to_score(cantus_firmus: list[Pitch], state: State) -> Score:
    cf_notes = [Note(pitch, Duration(Fraction(4))) for pitch in cantus_firmus]
    cf_measures = [Measure([note]) for note in cf_notes]

    return Score(
        key=KEY,
        time_signature=TIME_SIGNATURE,
        parts=[
            Part(part_id=CF_PART_ID, measures=cf_measures),
            Part(part_id=RELIZE_PART_ID, measures=state.previous_measures),
        ],
    )

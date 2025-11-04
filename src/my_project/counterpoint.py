import itertools
import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass
from enum import Enum
from fractions import Fraction

from my_project.model import (
    Duration,
    Interval,
    IntervalStep,
    Key,
    Measure,
    MeasureNumber,
    Mode,
    Note,
    NoteName,
    Offset,
    Part,
    PartId,
    Pitch,
    Score,
    TimeSignature,
)
from my_project.util import add_interval_step_in_key, part_range, scale_pitches, shuffled_interleave

KEY = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
TIME_SIGNATURE = TimeSignature(4, Fraction(1))
NOTES_IN_MEASURE = 4
CF_PART_ID = PartId.BASS
RELIZE_PART_ID = PartId.SOPRANO


def generate(cantus_firmus: list[Pitch]) -> Iterator[Score]:
    return map(lambda state: to_score(cantus_firmus, state), State.start_state(cantus_firmus).final_states())


## ------


class ToneType(Enum):
    """
    探索した音に対し、その音が和声音か非和声音かを記録しておく必要がある。そのための音の種別
    """

    # 和声音。冒頭の休符も便宜上和声音として扱う。
    HARMONIC_TONE = 1
    # 経過音
    PASSING_TONE = 2
    # 刺繍音
    NEIGHBOR_TONE = 3


@dataclass(frozen=True)
class AnnotatedNote:
    note: Note
    tone_type: ToneType


@dataclass(frozen=True)
class AnnotatedMeasure:
    """ToneType で注釈付けされた音符のリストを持つ小節"""

    annotated_notes: list[AnnotatedNote]

    def to_measure(self) -> Measure:
        """Score 生成のために model.Measure に変換する"""
        return Measure([an.note for an in self.annotated_notes])


# 状態の前後に関わる命名は "current" または "next" の2つから利用し、 "previous" は利用しない。
# 必ず「現在」の状態から「次」の状態を選ぶという形にする。


class State(ABC):
    # 与えられた定旋律(CF)全体。ステートの途中で変わることはない。
    cantus_firmus: list[Pitch]
    # 現在処理中のCFのインデックス (0-indexed)
    # EachCheckState -> SearchState | FinalCheckState のタイミングで必要に応じてインクリメントされる
    cf_cursor: int
    # 完了した小節。
    completed_measures: list[AnnotatedMeasure]
    # 現在構築中の音符バッファ。最大4つの音が入る。
    # EachCheckState -> SearchState | FinalCheckState のタイミングで必要に応じて空になる
    note_buffer: list[AnnotatedNote]

    def __post_init__(self) -> None:
        # CFは空ではない
        assert self.cantus_firmus
        # カーソルはCFの長さを超えることはない。(FinalCheckState, EndStateでは一致する)
        assert 0 <= self.cf_cursor <= len(self.cantus_firmus)
        # バッファが4拍(NOTES_IN_MEASURE)を超えることはない
        assert len(self.note_buffer) <= NOTES_IN_MEASURE
        # 完了した小節の数と、現在のカーソル位置は常に一致する
        assert len(self.completed_measures) == self.cf_cursor

    @classmethod
    def start_state(cls, cfs: list[Pitch]) -> "State":
        return SearchState(cantus_firmus=cfs, cf_cursor=0, completed_measures=[], note_buffer=[])

    @abstractmethod
    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        pass

    def final_states(self) -> Iterator["State"]:
        if isinstance(self, EndState):
            yield self
            return
        for next_state in self.next_states():
            yield from next_state.final_states()

    ## ---

    def current_measure_number(self) -> MeasureNumber:
        return MeasureNumber(self.cf_cursor + 1)

    def current_note_buffer_duration(self) -> Duration:
        return sum([an.note.duration for an in self.note_buffer], Duration.of(0))

    def current_offset(self) -> Offset:
        return Offset(self.current_note_buffer_duration().value)

    def has_cf_at(self, measure_number: MeasureNumber) -> bool:
        return 1 <= measure_number.value <= len(self.cantus_firmus)

    def get_cf_at(self, measure_number: MeasureNumber) -> Pitch:
        if not self.has_cf_at(measure_number):
            raise IndexError(f"No CF pitch exists at measure {measure_number.value}.")
        return self.cantus_firmus[measure_number.value - 1]

    def is_first_measure_start_of_measure(self) -> bool:
        return self.cf_cursor == 0 and not self.note_buffer

    def is_first_measure(self) -> bool:
        return self.cf_cursor == 0

    def is_last_measure(self) -> bool:
        return self.cf_cursor == len(self.cantus_firmus) - 1

    def is_next_last_measure(self) -> bool:
        return self.cf_cursor == len(self.cantus_firmus) - 2

    def is_start_of_measure(self) -> bool:
        return not self.note_buffer

    def next_cf(self) -> Pitch:
        return self.cantus_firmus[self.cf_cursor]

    def next_next_cf(self) -> Pitch:
        """次の小節のCF音を返す (存在する場合)"""
        if self.cf_cursor + 1 >= len(self.cantus_firmus):
            raise IndexError("No next CF pitch exists.")
        return self.cantus_firmus[self.cf_cursor + 1]

    def current_latest_pitch(self) -> Pitch:
        """
        最後に追加した音。ステートの初期状態で利用すると例外となるが、そのタイミングで利用されることはないためPitch型で返す。
        """
        iter1 = (an.note.pitch for an in reversed(self.note_buffer) if an.note.pitch)
        iter2 = (
            an.note.pitch
            for measure in reversed(self.completed_measures)
            for an in reversed(measure.annotated_notes)
            if an.note.pitch
        )
        return next(itertools.chain(iter1, iter2))


@dataclass(frozen=True)
class SearchState(State):
    """
    冒頭・最後の和声音、途中の和声音・経過音・刺繍音を追加する。

    EachCheckState に進行し、notes_to_add に追加したい音を指定する。
    cf_cursor, completed_measures, note_buffer は変更しない。
    """

    cantus_firmus: list[Pitch]
    cf_cursor: int
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if self.is_last_measure():
            # エッジケースだが、CFが1つで指定された場合は最終小節を出力するため、先に判定する
            yield from self.on_last_measure(randomized)
        elif self.is_first_measure_start_of_measure():
            yield from self.on_first_measure_start_of_measure(randomized)
        else:
            yield from shuffled_interleave(
                [
                    self.on_searching_harmonic_tone(randomized),
                    self.on_searching_passing_tone(randomized),
                    self.on_searching_neighbor_tone(randomized),
                ],
                randomized,
            )

    def on_first_measure_start_of_measure(self, randomized: bool = True) -> Iterator[State]:
        cf = self.next_cf()
        possible_pitches: list[Pitch] = start_or_end_available_pitches(cf)

        next_states: list[State] = []
        for pitch in possible_pitches:
            next_state = EachCheckState(
                cantus_firmus=self.cantus_firmus,
                cf_cursor=0,
                note_buffer=[],
                completed_measures=[],
                notes_to_add=[
                    make_annotated_note(None, ToneType.HARMONIC_TONE),
                    make_annotated_note(pitch, ToneType.HARMONIC_TONE),
                ],
            )
            next_states.append(next_state)

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def on_last_measure(self, randomized: bool = True) -> Iterator[State]:
        next_pitches: list[Pitch] = start_or_end_available_pitches(self.next_cf())
        next_states: list[State] = []
        for next_pitch in next_pitches:
            next_states.append(
                EachCheckState(
                    cantus_firmus=self.cantus_firmus,
                    cf_cursor=self.cf_cursor,
                    completed_measures=self.completed_measures,
                    note_buffer=[],
                    notes_to_add=[make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, Duration.of(4))],
                )
            )
        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def on_searching_harmonic_tone(self, randomized: bool = True) -> Iterator[State]:
        available_interval_steps = set([IntervalStep.idx_1(i) for i in [-8, -6, -5, -4, -3, -2, 2, 3, 4, 5, 6, 8]])

        # 協和音として利用できる音のなかから、2~6度または8度の進行を探す
        cf = self.next_cf()
        previous_pitch = self.current_latest_pitch()
        available_pitches = available_harmonic_pitches(cf)

        next_pitches: list[Pitch] = []
        for pitch in available_pitches:
            step = (pitch - previous_pitch).step()
            if step in available_interval_steps:
                next_pitches.append(pitch)

        next_states: list[State] = []
        for next_pitch in next_pitches:
            next_states.append(
                EachCheckState(
                    cantus_firmus=self.cantus_firmus,
                    cf_cursor=self.cf_cursor,
                    completed_measures=self.completed_measures,
                    note_buffer=self.note_buffer,
                    notes_to_add=[make_annotated_note(next_pitch, ToneType.HARMONIC_TONE)],
                )
            )

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def on_searching_passing_tone(self, randomized: bool = True) -> Iterator[State]:
        # 経過音を利用する。notes_to_addに1つ以上の PASSING_TONE と1つの HARMONIC_TONE を追加する。
        # 最終小節や、小節の1拍目では利用できない。
        # また課題の冒頭でも利用できないが、その場合そもそもこのメソッドが呼ばれない。
        if self.is_last_measure() or self.is_start_of_measure():
            yield from []
            return

        # パターンの定義: (目標音までの音程(1-indexedの度数), 対象となるCFのMeasureNumber)
        patterns: list[tuple[int, MeasureNumber]] = []
        current_measure_num = self.current_measure_number()
        next_measure_num = current_measure_num + MeasureNumber(1)

        current_offset = self.current_offset()
        if current_offset == Offset.idx_1(2):  # 2拍目
            patterns = [(3, current_measure_num), (4, current_measure_num), (5, next_measure_num)]
        elif current_offset == Offset.idx_1(3):
            patterns = [(3, current_measure_num), (4, next_measure_num)]
        elif current_offset == Offset.idx_1(4):
            patterns = [(3, next_measure_num)]
        else:
            raise RuntimeError(f"invalid duration: {self.current_note_buffer_duration()}")

        # パターンから上向き・下向き両方の探索候補 (tups) を生成
        tups: list[tuple[IntervalStep, Pitch, MeasureNumber]] = []
        for step_idx_1, measure_num in patterns:
            target_cf = self.get_cf_at(measure_num)
            tups.append((IntervalStep.idx_1(step_idx_1), target_cf, measure_num))
            tups.append((IntervalStep.idx_1(-step_idx_1), target_cf, measure_num))

        current_latest_pitch = self.current_latest_pitch()
        next_states: list[State] = []
        for step, target_cf, target_measure_num in tups:
            # 経過音の音程リストを動的に計算
            sign = 1 if step.value > 0 else -1
            abs_step_idx_1 = abs(step.value) + 1
            passing_values_idx_1 = range(2, abs_step_idx_1)
            passing_steps = [IntervalStep.idx_1(v * sign) for v in passing_values_idx_1]

            # 到達した HARMONIC_TONE の音高を求め、CFと協和音程かを調べる
            target_pitch = add_interval_step_in_key(KEY, current_latest_pitch, step)

            is_target_cf_last = target_measure_num.value == len(self.cantus_firmus)
            if is_target_cf_last:
                available_pitchees = set(start_or_end_available_pitches(target_cf))
            else:
                available_pitchees = set(available_harmonic_pitches(target_cf))

            if target_pitch in available_pitchees:
                passing_pitches = [add_interval_step_in_key(KEY, current_latest_pitch, ps) for ps in passing_steps]
                next_states.append(
                    EachCheckState(
                        cantus_firmus=self.cantus_firmus,
                        cf_cursor=self.cf_cursor,
                        completed_measures=self.completed_measures,
                        note_buffer=self.note_buffer,
                        notes_to_add=[
                            *[make_annotated_note(p, ToneType.PASSING_TONE) for p in passing_pitches],
                            make_annotated_note(target_pitch, ToneType.HARMONIC_TONE),
                        ],
                    )
                )

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def on_searching_neighbor_tone(self, randomized: bool = True) -> Iterator[State]:
        # 刺繍音を利用する。刺繍音・和声音の2つの音を追加する。
        # 最終小節や、小節の1拍目では利用できない。
        # また課題の冒頭でも利用できないが、その場合そもそもこのメソッドが呼ばれない。
        if self.is_last_measure() or self.is_start_of_measure():
            yield from []
            return

        current_latest_pitch = self.current_latest_pitch()
        current_offset = self.current_offset()

        # 解決音の対象となるCFのMeasureNumberを決定
        target_measure_num: MeasureNumber
        if current_offset == Offset.idx_1(4):  # 4拍目から開始する場合、解決音は次の小節の1拍目
            target_measure_num = self.current_measure_number() + MeasureNumber(1)
        else:  # それ以外の場合、解決音は現在の小節内
            target_measure_num = self.current_measure_number()

        # 対象CFが存在しない場合は処理をスキップ (is_last_measure()でカバーされるはずだが念のため)
        if not self.has_cf_at(target_measure_num):
            yield from []
            return

        target_cf = self.get_cf_at(target_measure_num)

        # 解決音 (current_latest_pitch) が対象CFと協和するかを調べる
        is_target_cf_last = target_measure_num.value == len(self.cantus_firmus)
        if is_target_cf_last:
            available_pitches = set(start_or_end_available_pitches(target_cf))
        else:
            available_pitches = set(available_harmonic_pitches(target_cf))

        if current_latest_pitch not in available_pitches:
            yield from []
            return

        # 協和する場合、上部・下部刺繍音を試す
        next_states: list[State] = []
        neighbor_steps = [IntervalStep.idx_1(2), IntervalStep.idx_1(-2)]
        for step in neighbor_steps:
            neighbor_pitch = add_interval_step_in_key(KEY, current_latest_pitch, step)
            if neighbor_pitch in AVAILABLE_PITCHES_SET:  # 声域内か
                next_states.append(
                    EachCheckState(
                        cantus_firmus=self.cantus_firmus,
                        cf_cursor=self.cf_cursor,
                        completed_measures=self.completed_measures,
                        note_buffer=self.note_buffer,
                        notes_to_add=[
                            make_annotated_note(neighbor_pitch, ToneType.NEIGHBOR_TONE),
                            make_annotated_note(current_latest_pitch, ToneType.HARMONIC_TONE),
                        ],
                    )
                )

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class EachCheckState(State):
    cantus_firmus: list[Pitch]
    cf_cursor: int
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]

    # SearchState の1回のステップで追加された音はここに溜まっている
    notes_to_add: list[AnnotatedNote]

    def __post_init__(self) -> None:
        # 必ず notes_to_add に要素がある状態でこのステートになる
        assert len(self.notes_to_add) > 0

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if not self.validate():
            yield from []
            return
        else:
            notes = self.notes()
            if self.notes_fulfilled():
                notes_for_measure = notes[:NOTES_IN_MEASURE]
                remaining_buffer = notes[NOTES_IN_MEASURE:]
                if self.is_last_measure():
                    yield FinalCheckState(
                        cantus_firmus=self.cantus_firmus,
                        cf_cursor=self.cf_cursor + 1,
                        completed_measures=[*self.completed_measures, AnnotatedMeasure(notes_for_measure)],
                        note_buffer=remaining_buffer,
                    )
                else:
                    yield SearchState(
                        cantus_firmus=self.cantus_firmus,
                        cf_cursor=self.cf_cursor + 1,
                        completed_measures=[*self.completed_measures, AnnotatedMeasure(notes_for_measure)],
                        note_buffer=remaining_buffer,
                    )
            else:
                yield SearchState(
                    cantus_firmus=self.cantus_firmus,
                    cf_cursor=self.cf_cursor,
                    completed_measures=self.completed_measures,
                    note_buffer=notes,
                )

    def note_buffer_and_notes_to_add_fulfilled(self) -> bool:
        notes_to_add_duration = sum([an.note.duration for an in self.notes_to_add], Duration.of(0))
        return self.current_note_buffer_duration() + notes_to_add_duration >= Duration.of(NOTES_IN_MEASURE)

    def notes(self) -> list[AnnotatedNote]:
        return [*self.note_buffer, *self.notes_to_add]

    def notes_fulfilled(self) -> bool:
        notes_duration = sum([an.note.duration for an in self.notes()], Duration.of(0))
        return notes_duration >= Duration.of(NOTES_IN_MEASURE)

    # ---

    def validate(self) -> bool:
        """
        notes_to_add に追加された音の中に、連続などの禁則がなければTrue, あればFalseを返す。

        以下の直接・間接の連続は禁じられる

        - 直接: 2声部が同時に移動した際に生じた連続
        - 間接: 同時に打音されてはいないが、打音された音より全音符1個分前との音
            以下の場合は問題ない
                - 反行の場合
                - 刺繍音・経過音の場合

        2声・4分音符で実施する場合、直接の連続は1拍目に生じうる。
        """

        current_measure_number = self.current_measure_number()
        if current_measure_number == MeasureNumber(1):
            # 1小節目ではCFとの連続の起きようがない
            return True
        prev_cf = self.get_cf_at(current_measure_number - MeasureNumber(1))
        current_cf = self.get_cf_at(current_measure_number)

        current_latest_pitch = self.current_latest_pitch()
        first_add_pitch = self.notes_to_add[0].note.pitch
        if not first_add_pitch:
            raise RuntimeError  # 冒頭以外では休符を利用しないので起こり得ない

        current_offset = self.current_offset()
        if current_offset == Offset.idx_1(1):
            # 1拍目の直接の連続のみをまずはトライ
            return EachCheckState.is_allowed_parallel_intervals(
                (prev_cf, current_cf), (current_latest_pitch, first_add_pitch)
            )

        else:
            return True

    @classmethod
    def is_allowed_parallel_intervals(cls, sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
        """
        2つの旋律の横の進行 sequence_1 と sequence_2 の間に連続5度・8度の禁則が存在しないかどうかを返す
        """
        first_interval_normalized = Interval.of(sequence_1[0], sequence_2[0]).normalize()
        second_interval_normalized = Interval.of(sequence_1[1], sequence_2[1]).normalize()

        # 連続8度(1度)
        if first_interval_normalized == second_interval_normalized == Interval.parse("P1"):
            return False

        # 連続5度(完全-完全)
        if first_interval_normalized == second_interval_normalized == Interval.parse("P5"):
            return False

        # 連続5度(完全-減) 3声からは許されるが、現在は2声のみ扱うので禁則扱い
        if first_interval_normalized == Interval.parse("P5") and second_interval_normalized == Interval.parse("d5"):
            return False

        return True


@dataclass(frozen=True)
class FinalCheckState(State):
    cantus_firmus: list[Pitch]
    cf_cursor: int
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]

    def __post_init__(self) -> None:
        assert len(self.note_buffer) == 0

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if not self.validate():
            yield from []
            return
        else:
            yield EndState(
                cantus_firmus=self.cantus_firmus,
                cf_cursor=self.cf_cursor,
                completed_measures=self.completed_measures,
                note_buffer=self.note_buffer,
            )

    # ---

    def validate(self) -> bool:
        return True  # TODO


@dataclass(frozen=True)
class EndState(State):
    cantus_firmus: list[Pitch]
    cf_cursor: int
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        raise RuntimeError("not called")


## ---

AVAILABLE_PITCHES_LIST: list[Pitch] = scale_pitches(KEY, part_range(RELIZE_PART_ID))
AVAILABLE_PITCHES_SET: set[Pitch] = set(AVAILABLE_PITCHES_LIST)


def filter_available_pitches(pitches: list[Pitch]) -> list[Pitch]:
    return [pitch for pitch in pitches if pitch in AVAILABLE_PITCHES_SET]


def make_annotated_note(
    pitch: Pitch | None, tone_type: ToneType, duration: Duration = Duration(Fraction(1))
) -> AnnotatedNote:
    """Pitch, ToneType, Duration から AnnotatedNote を作成するヘルパー"""
    return AnnotatedNote(Note(pitch, duration), tone_type)


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
        if cf.num() <= pitch.num()
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


## ------


def to_score(cantus_firmus: list[Pitch], state: State) -> Score:
    cf_notes = [Note(pitch, Duration(Fraction(4))) for pitch in cantus_firmus]
    cf_measures = [Measure([note]) for note in cf_notes]

    realized_measures = [am.to_measure() for am in state.completed_measures]

    return Score(
        key=KEY,
        time_signature=TIME_SIGNATURE,
        parts=[
            Part(part_id=CF_PART_ID, measures=cf_measures),
            Part(part_id=RELIZE_PART_ID, measures=realized_measures),
        ],
    )

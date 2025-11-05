import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass
from enum import Enum
from fractions import Fraction
from typing import ClassVar

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
MEASURE_TOTAL_DURATION = Duration.of(NOTES_IN_MEASURE)
CF_PART_ID = PartId.BASS
REALIZE_PART_ID = PartId.SOPRANO


def generate(cantus_firmus: list[Pitch]) -> Iterator[Score]:
    return map(lambda state: state.to_score(), State.start_state(cantus_firmus).final_states())


# ---


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

    def offset_notes(self) -> dict[Offset, AnnotatedNote]:
        """
        この小節のannotated_notesのオフセットとAnnotatedNoteの組みに変換してdictで返す
        """
        result: dict[Offset, AnnotatedNote] = {}
        current_offset = Offset.of(0)

        for annotated_note in self.annotated_notes:
            result[current_offset] = annotated_note
            current_offset = current_offset.add_duration(annotated_note.note.duration)

        return result

    def pitch_at(self, offset: Offset) -> Pitch | None:
        """
        この小節のOffsetの時刻に鳴っているPitchを返す。
        そのOffsetの時に休符であればNone, 小節の範囲外のOffsetを指定した場合は例外
        """
        current_offset = Offset.of(0)

        for annotated_note in self.annotated_notes:
            note_duration = annotated_note.note.duration
            note_end_offset = current_offset.add_duration(note_duration)
            if current_offset <= offset < note_end_offset:
                return annotated_note.note.pitch  # (休符の場合は None が返る)

            current_offset = note_end_offset

        raise ValueError(
            f"Offset {offset.value} is out of bounds for this measure. Total duration is {current_offset.value}."
        )


# ---

# ステートの継承関係と遷移先は次のようになっている。
#
# - State -> SearchingInMeasureState
#
#   - SearchingInMeasureState -> ValidatingInMeasureState
#     - ChooseSearchState -> ValidatingInMeasureState
#                            | SearchingStartNoteState
#                            | SearchingEndNoteState
#                            | SearchingHarmonicNoteInMeasureState
#                            | SearchingPassingNoteInMeasureState
#                            | SearchingNeighborNoteInMeasureState
#     - SearchingStartNoteState -> ChooseSearchState
#     - SearchingEndNoteState -> ChooseSearchState
#     - SearchingHarmonicNoteInMeasureState -> ChooseSearchState
#     - SearchingPassingNoteInMeasureState -> ChooseSearchState
#     - SearchingNeighborNoteInMeasureState -> ChooseSearchState
#
#   - ValidatingInMeasureState -> SearchingInMeasureState
#                                 | ValidatingAllMeasureState
#
#   - ValidatingAllMeasureState -> ValidatingAllMeasureState
#
#   - EndState


class State(ABC):
    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]

    def __post_init__(self) -> None:
        # CFは空ではない。(通常は2つ以上。最小で1つだがそれは最終小節として扱われ全音符が実施されるだけになる。)
        assert self.cantus_firmus
        # 完了した小節の長さはCFの長さと同じかそれよりも少ない
        assert len(self.completed_measures) <= len(self.cantus_firmus)

    # ---

    @classmethod
    def start_state(cls, cfs: list[Pitch]) -> "State":
        return SearchingInMeasureState.start_searching_measure_state(
            cantus_firmus=cfs,
            completed_measures=[],
            next_measure_mark=None,
        )

    @abstractmethod
    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        pass

    def final_states(self, randomized: bool = True) -> Iterator["EndState"]:
        if isinstance(self, EndState):
            yield self
            return

        child_states = self.next_states(randomized)
        child_iterators = [child.final_states(randomized) for child in child_states]
        yield from shuffled_interleave(child_iterators, randomized)

    # ---

    def previous_measure_number(self) -> MeasureNumber | None:
        """
        現在の探索中・バリデーション中の一つ前の小節番号。
        現在が最初の小節にいる場合は None を返す

        最終確認状態では最終小節番号と一致する。
        """
        current_measure_number = MeasureNumber(len(self.completed_measures) + 1)
        if current_measure_number == MeasureNumber(1):
            return None
        else:
            return current_measure_number - MeasureNumber(1)

    def current_measure_number(self) -> MeasureNumber:
        """
        現在の探索中・バリデーション中の小節番号

        最終確認状態では呼び出すと例外となる。
        それは値を返すと最終小節番号の一つ次の値となり、実施する範囲外の小節番号を示すことになるためであり、
        その対処のために None を返すのは探索中・バリデーション中では不便だから。
        """
        current_measure_number = MeasureNumber(len(self.completed_measures) + 1)
        if current_measure_number > self.last_measure_number():
            raise ValueError(f"measure number out of range: {current_measure_number}")
        else:
            return current_measure_number

    def next_measure_number(self) -> MeasureNumber | None:
        """
        現在の探索中・バリデーション中の一つ後の小節番号。
        現在が最後の小節にいる場合は None を返す
        """
        current_measure_number = self.current_measure_number()
        if current_measure_number >= self.last_measure_number():
            return None
        else:
            return current_measure_number + MeasureNumber(1)

    def last_measure_number(self) -> MeasureNumber:
        """
        最終小節の小節番号
        """
        return MeasureNumber(len(self.cantus_firmus))

    def get_current_cf(self) -> Pitch:
        """
        現在の探索中・バリデーション中の小節番号

        最終確認状態では呼び出すと例外となる。
        """
        return self.get_cf_at(self.current_measure_number())

    def get_cf_at(self, measure_number: MeasureNumber) -> Pitch:
        """
        指定された小節番号のCFを取得する。
        指定された小節番号が範囲外の場合は例外となる。
        """
        if MeasureNumber(1) <= measure_number <= self.last_measure_number():
            return self.cantus_firmus[measure_number.value - 1]
        else:
            raise ValueError(f"invalid measure_number: {measure_number}")

    def get_cm_at(self, measure_number: MeasureNumber) -> AnnotatedMeasure:
        """
        指定された小節番号の実施した小節(completed_measure)を取得する。
        指定された小節番号が範囲外の場合は例外となる。
        """
        if MeasureNumber(1) <= measure_number <= self.current_measure_number():
            return self.completed_measures[measure_number.value - 1]
        else:
            raise ValueError(f"invalid measure_number: {measure_number}")


# ------------ SearchingInMeasureState --------------


class SearchingInMeasureState(State):
    """
    小節内の探索中。
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]

    # 現在構築中の音符バッファ。最大4つの音が入る。
    note_buffer: list[AnnotatedNote]
    # 小節の探索の結果、次の小節の冒頭のピッチを決める必要がある場合、ピッチのみマーキングする
    next_measure_mark: Pitch | None
    # 和音の設定。基本形は True, 第一転回形の場合は False, 未設定の場合は None
    is_root_chord: bool | None

    @classmethod
    def start_searching_measure_state(
        cls, cantus_firmus: list[Pitch], completed_measures: list[AnnotatedMeasure], next_measure_mark: Pitch | None
    ) -> "SearchingInMeasureState":
        """
        SearchingInMeasureStateの子クラス以外から SearchingInMeasureState のステートを作成する場合はここを経由すること
        """
        return ChooseSearchState(
            cantus_firmus=cantus_firmus,
            completed_measures=completed_measures,
            note_buffer=[],
            next_measure_mark=next_measure_mark,
            is_root_chord=None,
        )

    @abstractmethod
    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        pass

    # ---

    def total_note_buffer_duration(self) -> Duration:
        """
        バッファにある音価の合計。4未満の場合は探索中、4であれば探索完了を表す。
        """
        return sum([an.note.duration for an in self.note_buffer], Duration.of(0))

    def current_offset(self) -> Offset:
        """
        現在の探索中の小節の位置。探索中の場合値を 0, 1, 2, 3 のいずれかから返し、探索完了の場合に呼び出すと例外を出す。
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
        elif len(self.completed_measures) > 0:
            if self.completed_measures[-1].annotated_notes[-1].note.pitch:
                return self.completed_measures[-1].annotated_notes[-1].note.pitch
        raise ValueError("previous_latest_added_pitch not found.")

    AVAILABLE_PITCHES_LIST: ClassVar[list[Pitch]] = scale_pitches(KEY, part_range(REALIZE_PART_ID))
    AVAILABLE_PITCHES_SET: ClassVar[set[Pitch]] = set(AVAILABLE_PITCHES_LIST)

    @classmethod
    def filter_available_pitches(cls, pitches: list[Pitch]) -> list[Pitch]:
        return [pitch for pitch in pitches if pitch in SearchingInMeasureState.AVAILABLE_PITCHES_SET]

    @classmethod
    def make_annotated_note(
        cls, pitch: Pitch | None, tone_type: ToneType, duration: Duration = Duration.of(1)
    ) -> AnnotatedNote:
        """Pitch, ToneType, Duration から AnnotatedNote を作成するヘルパー"""
        return AnnotatedNote(Note(pitch, duration), tone_type)

    def available_harmonic_pitches_with_chord(self) -> list[tuple[Pitch, bool | None]]:
        """
        課題の冒頭の音または最終小節以外で、協和音として利用できる音と、利用したことにより確定した和音を返す。
        確定した和音は is_first_inversion_chord と同様に bool | None で返す。
        和音が未設定の場合はCFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
        """

        cf = self.get_current_cf()

        step_and_next_chord_dict: dict[IntervalStep, bool | None] = {}
        if self.is_root_chord is None:
            step_and_next_chord_dict = {
                IntervalStep.idx_1(1): None,
                IntervalStep.idx_1(3): None,
                IntervalStep.idx_1(5): True,
                IntervalStep.idx_1(6): False,
            }
        elif self.is_root_chord:
            step_and_next_chord_dict = {
                IntervalStep.idx_1(1): True,
                IntervalStep.idx_1(3): True,
                IntervalStep.idx_1(5): True,
            }
        else:
            step_and_next_chord_dict = {
                IntervalStep.idx_1(1): False,
                IntervalStep.idx_1(3): False,
                IntervalStep.idx_1(6): False,
            }

        all_available_pitches = [
            pitch
            for pitch in SearchingInMeasureState.AVAILABLE_PITCHES_LIST  # 声域内の調の音
            if cf.num() <= pitch.num() and Interval.of(cf, pitch).step() <= IntervalStep.idx_1(15)  # 2オクターブ未満
        ]

        result: list[tuple[Pitch, bool | None]] = []
        for pitch in all_available_pitches:
            step = Interval.of(cf, pitch).normalize().step()
            if step in step_and_next_chord_dict.keys():
                result.append((pitch, step_and_next_chord_dict[step]))
        return result

    @classmethod
    def start_available_pitches(cls, cf: Pitch) -> list[Pitch]:
        """
        課題の冒頭で利用可能な音を返す。
        2声の場合、I度音またはV度音。
        すなわち、CFと完全1度・完全5度・その複音程。2オクターブの範囲、声域内の条件も加える。
        """
        intervals = [
            Interval.parse("P1"),
            Interval.parse("P5"),
            Interval.parse("P8"),
            Interval.parse("P12"),
            Interval.parse("P15"),
        ]
        return [
            pitch
            for pitch in [cf + interval for interval in intervals]
            if pitch in SearchingInMeasureState.AVAILABLE_PITCHES_SET
        ]

    @classmethod
    def end_available_pitches(cls, cf: Pitch) -> list[Pitch]:
        """
        課題の最終小節で利用可能な音を返す。
        2声の場合、I度音のみ。
        すなわち、CFと完全1度・その複音程。2オクターブの範囲、声域内の条件も加える。
        """
        intervals = [
            Interval.parse("P1"),
            Interval.parse("P8"),
            Interval.parse("P15"),
        ]
        return [
            pitch
            for pitch in [cf + interval for interval in intervals]
            if pitch in SearchingInMeasureState.AVAILABLE_PITCHES_SET
        ]

    @classmethod
    def available_pitches(cls, cf: Pitch) -> list[Pitch]:
        """
        冒頭または最終小節以外で、協和音として利用できる音を返す
        CFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
        """
        return [
            pitch
            for pitch in SearchingInMeasureState.AVAILABLE_PITCHES_LIST  # 声域内の調の音
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

    VALID_MELODIC_INTERVAL_LIST: ClassVar[list[Interval]] = [
        Interval.parse("m2"),
        Interval.parse("M2"),
        Interval.parse("m3"),
        Interval.parse("M3"),
        Interval.parse("P4"),
        Interval.parse("P5"),
        Interval.parse("m6"),
        # 長6度はだめ
        # 7度はだめ
        Interval.parse("P8"),
    ]
    VALID_MELODIC_INTERVAL_SET: ClassVar[set[Interval]] = set(VALID_MELODIC_INTERVAL_LIST)

    @classmethod
    def is_valid_melodic_interval(cls, interval: Interval) -> bool:
        """
        ある音程が旋律的音程として認められるかどうかを返す。
        同音の連続を行わないようにするため、ユニゾンはFalseとしている。
        """
        return interval.abs() in SearchingInMeasureState.VALID_MELODIC_INTERVAL_SET


# ---


@dataclass(frozen=True)
class ChooseSearchState(SearchingInMeasureState):
    """
    小節の探索方法を選ぶか、バリデーションの状態に移動する
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        if self.is_buffer_fulfilled():
            yield ValidatingInMeasureState(
                self.cantus_firmus,
                self.completed_measures,
                self.note_buffer,
                self.next_measure_mark,
            )
        elif self.current_measure_number() == self.last_measure_number():
            yield SearchingEndNoteState(
                self.cantus_firmus,
                self.completed_measures,
                self.note_buffer,
                self.next_measure_mark,
                is_root_chord=True,
            )
        elif self.current_measure_number() == MeasureNumber(1) and self.current_offset() == Offset.of(0):
            yield SearchingStartNoteState(
                self.cantus_firmus,
                self.completed_measures,
                self.note_buffer,
                self.next_measure_mark,
                is_root_chord=True,
            )
        else:
            next_states: list[State] = [
                SearchingHarmonicNoteInMeasureState(
                    self.cantus_firmus,
                    self.completed_measures,
                    self.note_buffer,
                    self.next_measure_mark,
                    self.is_root_chord,
                ),
                SearchingPassingNoteInMeasureState(
                    self.cantus_firmus,
                    self.completed_measures,
                    self.note_buffer,
                    self.next_measure_mark,
                    self.is_root_chord,
                ),
                SearchingNeighborNoteInMeasureState(
                    self.cantus_firmus,
                    self.completed_measures,
                    self.note_buffer,
                    self.next_measure_mark,
                    self.is_root_chord,
                ),
            ]
            if randomized:
                random.shuffle(next_states)
            yield from next_states

    def is_buffer_fulfilled(self) -> bool:
        total_note_buffer_duration = self.total_note_buffer_duration()
        if total_note_buffer_duration == MEASURE_TOTAL_DURATION:
            return True
        elif total_note_buffer_duration < MEASURE_TOTAL_DURATION:
            return False
        else:
            raise ValueError(f"total_note_buffer_duration: {total_note_buffer_duration}")


@dataclass(frozen=True)
class SearchingStartNoteState(SearchingInMeasureState):
    """
    課題冒頭の音を選択する
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() == Duration.of(0)
        assert self.next_measure_mark is None
        assert self.is_root_chord

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        cf = self.get_current_cf()
        possible_pitches: list[Pitch] = SearchingInMeasureState.start_available_pitches(cf)

        next_states: list[State] = []
        for pitch in possible_pitches:
            next_states.append(
                ChooseSearchState(
                    cantus_firmus=self.cantus_firmus,
                    completed_measures=self.completed_measures,
                    note_buffer=[
                        SearchingInMeasureState.make_annotated_note(None, ToneType.HARMONIC_TONE),
                        SearchingInMeasureState.make_annotated_note(pitch, ToneType.HARMONIC_TONE),
                    ],
                    next_measure_mark=None,
                    is_root_chord=True,
                )
            )

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingEndNoteState(SearchingInMeasureState):
    """
    課題の最後の小節の和声音を選択する
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() == Duration.of(0)
        assert self.is_root_chord

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        if self.next_measure_mark is not None:
            next_pitches: list[Pitch] = [self.next_measure_mark]
        else:
            cf = self.get_current_cf()
            next_pitches: list[Pitch] = SearchingInMeasureState.end_available_pitches(cf)
            # 前の音との音程の確認
            previous_pitch = self.previous_latest_added_pitch()
            next_pitches = [
                p for p in next_pitches if SearchingInMeasureState.is_valid_melodic_interval(p - previous_pitch)
            ]
        next_states: list[State] = []
        for next_pitch in next_pitches:
            next_states.append(
                ChooseSearchState(
                    cantus_firmus=self.cantus_firmus,
                    completed_measures=self.completed_measures,
                    note_buffer=[
                        SearchingInMeasureState.make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, Duration.of(4))
                    ],
                    next_measure_mark=None,
                    is_root_chord=True,
                )
            )

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingHarmonicNoteInMeasureState(SearchingInMeasureState):
    """
    小節内の探索中。和声音を1音追加する
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        next_pitch_and_chord_list: list[tuple[Pitch, bool | None]] = []

        if self.next_measure_mark is not None:
            cf_mark_step = (self.get_current_cf() - self.next_measure_mark).normalize().step()
            if cf_mark_step == IntervalStep.idx_1(5):
                is_root_chord = True
            elif cf_mark_step == IntervalStep.idx_1(6):
                is_root_chord = False
            elif cf_mark_step in [IntervalStep.idx_1(1), IntervalStep.idx_1(3)]:
                is_root_chord = None
            else:
                raise ValueError(
                    "invalid next_measure_mark. "
                    f"current_cf: {self.get_current_cf()}, next_measure_mark: {self.next_measure_mark}"
                )
            next_pitch_and_chord_list = [(self.next_measure_mark, is_root_chord)]
        else:
            # 和音上利用できる音の中で、前の音との旋律的音程が許されるもの
            previous_pitch = self.previous_latest_added_pitch()
            all_candidates: list[tuple[Pitch, bool | None]] = self.available_harmonic_pitches_with_chord()
            for next_pitch, next_is_root_chord in all_candidates:
                if SearchingInMeasureState.is_valid_melodic_interval(next_pitch - previous_pitch):
                    next_pitch_and_chord_list.append((next_pitch, next_is_root_chord))

        next_states: list[State] = []
        for next_pitch, next_is_root_chord in next_pitch_and_chord_list:
            next_states.append(
                ChooseSearchState(
                    cantus_firmus=self.cantus_firmus,
                    completed_measures=self.completed_measures,
                    note_buffer=[
                        *self.note_buffer,
                        SearchingInMeasureState.make_annotated_note(next_pitch, ToneType.HARMONIC_TONE),
                    ],
                    next_measure_mark=None,  # 和声音の探索では次のマークは行わない。
                    is_root_chord=next_is_root_chord,
                )
            )

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingPassingNoteInMeasureState(SearchingInMeasureState):
    """
    小節内の探索中。経過音を追加する。note_bufferに2つ以上の音が追加され、next_measure_markが付くこともある。
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        """
        経過音を利用する。
        note_bufferに1つ以上の PASSING_TONE と1つの HARMONIC_TONE を追加する。
        必要に応じて next_measure_mark に値が設定される。
        """
        # 最終小節や、小節の1拍目(課題の冒頭を含む)では利用できない。
        if self.current_measure_number() == self.last_measure_number() or self.current_offset() == Offset.of(0):
            yield from []
            return
        # マークがある場合は非和声音を利用できない
        if self.next_measure_mark is not None:
            yield from []
            return

        next_measure_num = self.next_measure_number()
        # 最終小節ではないため、次の小節番号は必ず取得できる。
        if not next_measure_num:
            raise RuntimeError

        # 直前の音から目標音までの音程(上向きのみ), 到達音が現在の小節に含まれるかどうか(小節を跨がないか)の一覧を求める
        patterns: list[tuple[IntervalStep, bool]] = SearchingPassingNoteInMeasureState.progression_pattern(
            current_offset=self.current_offset()
        )

        next_states: list[State] = []
        for step, is_target_note_in_current_number in patterns:
            # 到達する音高を求める
            target_pitch = add_interval_step_in_key(KEY, self.previous_latest_added_pitch(), step)

            # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
            if is_target_note_in_current_number:
                # 小節を跨がない場合

                # 小節を跨がない場合、到達した音は課題の冒頭の音または最終小節以外の音である。
                # それらの利用できる音を求める
                available_pitches_and_next_chord = self.available_harmonic_pitches_with_chord()

                for available_pitch, is_next_root_chord in available_pitches_and_next_chord:
                    if target_pitch != available_pitch:
                        continue

                    pitches = SearchingPassingNoteInMeasureState.conjunct_pitches(
                        KEY, self.previous_latest_added_pitch(), step
                    )
                    init_notes = [
                        SearchingInMeasureState.make_annotated_note(p, ToneType.PASSING_TONE) for p in pitches[:-1]
                    ]
                    last_note = SearchingInMeasureState.make_annotated_note(pitches[-1], ToneType.HARMONIC_TONE)

                    next_states.append(
                        ChooseSearchState(
                            cantus_firmus=self.cantus_firmus,
                            completed_measures=self.completed_measures,
                            note_buffer=[*self.note_buffer, *init_notes, last_note],
                            next_measure_mark=None,
                            is_root_chord=is_next_root_chord,
                        )
                    )
                pass
            else:
                # 小節を跨ぐ場合

                # この小節の和音の音かどうかは気にしなくて良い。
                # 次の小節が最終小節かどうかに応じて利用できる音高が異なる。
                if next_measure_num == self.last_measure_number():
                    available_pitches = SearchingInMeasureState.end_available_pitches(self.get_cf_at(next_measure_num))
                else:
                    available_pitches = SearchingInMeasureState.available_pitches(self.get_cf_at(next_measure_num))

                for available_pitch in available_pitches:
                    if target_pitch != available_pitch:
                        continue

                    pitches = SearchingPassingNoteInMeasureState.conjunct_pitches(
                        KEY, self.previous_latest_added_pitch(), step
                    )
                    notes_to_add_buffer = [
                        SearchingInMeasureState.make_annotated_note(p, ToneType.PASSING_TONE) for p in pitches[:-1]
                    ]
                    next_measure_mark = pitches[-1]

                    next_states.append(
                        ChooseSearchState(
                            cantus_firmus=self.cantus_firmus,
                            completed_measures=self.completed_measures,
                            note_buffer=[*self.note_buffer, *notes_to_add_buffer],
                            next_measure_mark=next_measure_mark,
                            is_root_chord=self.is_root_chord,
                        )
                    )

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    @classmethod
    def conjunct_pitches(cls, key: Key, pitch: Pitch, interval_step: IntervalStep) -> list[Pitch]:
        """
        順次進行の音高列を返す。
        指定した key で、指定された pitch に対し、そこから interval_step 分離れた音まで順次進行した時の音高の列を返す。
        指定した pitch は結果に含まれない。

        例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(3) -> [D4, E4]
        例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(-4) -> [B3, A3, G3]
        例: key=C major, pitch = C4, interval_step = IntervalStep_idx_1(1) -> []
        """

        steps: list[IntervalStep]
        if interval_step == IntervalStep(0):
            steps = []
        elif interval_step > IntervalStep(0):
            steps = [IntervalStep(v) for v in range(1, interval_step.value + 1)]
        else:
            steps = [IntervalStep(v) for v in range(-1, interval_step.value - 1, -1)]

        return [add_interval_step_in_key(key, pitch, step) for step in steps]

    @classmethod
    def progression_pattern(cls, current_offset: Offset) -> list[tuple[IntervalStep, bool]]:
        """
        現在のオフセットに応じて、
        直前の音から目標音までのIntervalStepと、到達した音が現在の小節に含まれるか(小節を跨いでいないか)どうかの一覧を返す
        1拍目からは経過音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる
        """
        patterns: list[tuple[IntervalStep, bool]] = []
        if current_offset == Offset.idx_1(2):
            # 2拍目の探索中は、3拍目・4拍目・次の小節の1拍目に向けて経過音が利用できる。
            patterns = [
                (IntervalStep.idx_1(3), True),
                (IntervalStep.idx_1(4), True),
                (IntervalStep.idx_1(5), False),
            ]
        elif current_offset == Offset.idx_1(3):
            # 3拍目の探索中は、4拍目・次の小節の1拍目に向けて経過音が利用できる。
            patterns = [
                (IntervalStep.idx_1(3), True),
                (IntervalStep.idx_1(4), False),
            ]
        elif current_offset == Offset.idx_1(4):
            # 4拍目の探索中は、次の小節の1拍目に向けて経過音が利用できる。
            patterns = [
                (IntervalStep.idx_1(3), False),
            ]
        else:
            raise RuntimeError(f"invalid current_offset: {current_offset}")
        # パターンに下向きの音程を追加
        patterns = [*patterns, *[(p[0] * -1, p[1]) for p in patterns]]
        return patterns


@dataclass(frozen=True)
class SearchingNeighborNoteInMeasureState(SearchingInMeasureState):
    """
    小節内の探索中。刺繍音を追加する。note_bufferに2つの音が追加され、next_measure_markが付くこともある。
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None
    is_root_chord: bool | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        """
        刺繍音を利用する。
        note_bufferに1つの NEIGHBOR_TONE と1つの HARMONIC_TONE を追加する。
        必要に応じて next_measure_mark に値が設定される。
        """
        # 最終小節や、小節の1拍目(課題の冒頭を含む)では利用できない。
        if self.current_measure_number() == self.last_measure_number() or self.current_offset() == Offset.of(0):
            yield from []
            return
        # マークがある場合は非和声音を利用できない
        if self.next_measure_mark is not None:
            yield from []
            return

        next_measure_num = self.next_measure_number()
        # 最終小節ではないため、次の小節番号は必ず取得できる。
        if not next_measure_num:
            raise RuntimeError

        next_states: list[State] = []

        # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
        if self.is_target_note_in_current_measure():
            # 小節を跨がない場合は、直前に追加した音が和声音であるため、音域内であれば利用可能
            for neighbor_note_pitch in self.available_neighbor_note_pitches():
                previous_pitch = self.previous_latest_added_pitch()
                next_states.append(
                    ChooseSearchState(
                        cantus_firmus=self.cantus_firmus,
                        completed_measures=self.completed_measures,
                        note_buffer=[
                            *self.note_buffer,
                            SearchingInMeasureState.make_annotated_note(neighbor_note_pitch, ToneType.NEIGHBOR_TONE),
                            SearchingInMeasureState.make_annotated_note(previous_pitch, ToneType.HARMONIC_TONE),
                        ],
                        next_measure_mark=None,
                        is_root_chord=self.is_root_chord,
                    )
                )
        else:
            # 小節を跨ぐ場合、最終小節かどうかに応じて利用できる音高を求め、その中に直前の音が含まれるかを確認する
            if next_measure_num == self.last_measure_number():
                available_pitches = set(SearchingInMeasureState.end_available_pitches(self.get_cf_at(next_measure_num)))
            else:
                available_pitches = set(SearchingInMeasureState.available_pitches(self.get_cf_at(next_measure_num)))

            previous_pitch = self.previous_latest_added_pitch()
            if previous_pitch in available_pitches:
                for neighbor_note_pitch in self.available_neighbor_note_pitches():
                    next_states.append(
                        ChooseSearchState(
                            cantus_firmus=self.cantus_firmus,
                            completed_measures=self.completed_measures,
                            note_buffer=[
                                *self.note_buffer,
                                SearchingInMeasureState.make_annotated_note(
                                    neighbor_note_pitch, ToneType.NEIGHBOR_TONE
                                ),
                            ],
                            next_measure_mark=previous_pitch,
                            is_root_chord=self.is_root_chord,
                        )
                    )

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def available_neighbor_note_pitches(self) -> list[Pitch]:
        """
        直前の音をもとに、音域内で利用できる刺繍音の一覧を返す。
        2度上・2度下
        """
        previous_latest_added_pitch = self.previous_latest_added_pitch()
        neighbor_steps = [IntervalStep.idx_1(2), IntervalStep.idx_1(-2)]
        result: list[Pitch] = []
        for step in neighbor_steps:
            neighbor_pitch = add_interval_step_in_key(KEY, previous_latest_added_pitch, step)
            if neighbor_pitch in SearchingInMeasureState.AVAILABLE_PITCHES_SET:  # 声域内か
                result.append(neighbor_pitch)
        return result

    def is_target_note_in_current_measure(self) -> bool:
        """
        現在のオフセットに応じて、刺繍音を利用した時の最後の音が現在の小節に含まれるか(小節を跨いでいないか)どうかを返す
        1拍目からは刺繍音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる
        """
        current_offset = self.current_offset()
        if current_offset in [Offset.idx_1(2), Offset.idx_1(3)]:
            # 2拍目の探索中は現在の小節の3拍目に到達する
            # 3拍目の探索中は現在の小節の4拍目に到達する
            return True
        elif current_offset == Offset.idx_1(4):
            # 4拍目の探索中は次の小節の1拍目に到達する
            return False
        else:
            raise RuntimeError(f"invalid current_offset: {current_offset}")


# ------------ ValidatingInMeasureState --------------


@dataclass(frozen=True)
class ValidatingInMeasureState(State):
    """
    小節内のバリデーション中。

    note_buffer の音に対して連続・並達の禁則が含まれるものを除外する。
    (next_measure_mark に関しては次の小節が埋まった時のバリデーションで確認される)
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]
    note_buffer: list[AnnotatedNote]
    next_measure_mark: Pitch | None

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() == MEASURE_TOTAL_DURATION

    # SearchingInMeasureState と定義が重複している。
    def total_note_buffer_duration(self) -> Duration:
        """
        バッファにある音価の合計。4未満の場合は探索中、4であれば探索完了を表す。
        """
        return sum([an.note.duration for an in self.note_buffer], Duration.of(0))

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        if not self.validate():
            yield from []
            return
        else:
            if self.current_measure_number() == self.last_measure_number():
                yield ValidatingAllMeasureState(
                    cantus_firmus=self.cantus_firmus,
                    completed_measures=[*self.completed_measures, AnnotatedMeasure(self.note_buffer)],
                )
            else:
                yield SearchingInMeasureState.start_searching_measure_state(
                    cantus_firmus=self.cantus_firmus,
                    completed_measures=[*self.completed_measures, AnnotatedMeasure(self.note_buffer)],
                    next_measure_mark=self.next_measure_mark,
                )

    def validate(self) -> bool:
        # 冒頭小節には直前の小節が存在しないため、連続は起こり得ない。
        previous_measure_number = self.previous_measure_number()
        if previous_measure_number is None:
            return True

        previous_cf = self.get_cf_at(previous_measure_number)
        current_cf = self.get_current_cf()

        previous_measure = self.get_cm_at(previous_measure_number)
        current_measure = AnnotatedMeasure(self.note_buffer)

        # 2つの声部が同時に動いている場合の確認。CFが全音符なので小節を跨いだタイミングのみ。
        previous_measure_last_pitch = previous_measure.annotated_notes[-1].note.pitch
        current_measure_first_pitch = current_measure.annotated_notes[0].note.pitch
        if previous_measure_last_pitch is not None and current_measure_first_pitch is not None:
            # 連続
            if ValidatingInMeasureState.is_parallel_violation(
                sequence_1=(previous_cf, current_cf),
                sequence_2=(previous_measure_last_pitch, current_measure_first_pitch),
            ):
                return False

            # 並達
            if ValidatingInMeasureState.is_hidden_interval_violation(
                sequence_1=(previous_cf, current_cf),
                sequence_2=(previous_measure_last_pitch, current_measure_first_pitch),
            ):
                return False

        # 間接の連続の確認
        # 便宜上前の小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
        #
        # 以下のいずれも満たしているものが間接の連続として禁じられる
        # - Duration.of(4) 未満の隔たりがある
        # - 2声が並行している
        # - 和声音である
        # - 直接の連続の規則として連続である
        cf_measure = AnnotatedMeasure(
            [
                AnnotatedNote(Note(previous_cf, Duration.of(4)), ToneType.HARMONIC_TONE),
                AnnotatedNote(Note(current_cf, Duration.of(4)), ToneType.HARMONIC_TONE),
            ]
        )
        realize_measure = AnnotatedMeasure([*previous_measure.annotated_notes, *current_measure.annotated_notes])
        for realize_current_offset, realize_current_a_note in realize_measure.offset_notes().items():
            if realize_current_offset < Offset.of(4):
                continue
            for realize_previous_offset, realize_previous_a_note in realize_measure.offset_notes().items():
                # Duration.of(4) 未満の隔たりがある
                if not (Offset.of(0) < realize_current_offset - realize_previous_offset < Offset.of(4)):
                    continue

                cf_current_pitch = cf_measure.pitch_at(realize_current_offset)
                cf_previous_pitch = cf_measure.pitch_at(realize_previous_offset)
                assert cf_current_pitch is not None
                assert cf_previous_pitch is not None

                realize_current_pitch = realize_current_a_note.note.pitch
                realize_previous_pitch = realize_previous_a_note.note.pitch

                # (休符の場合は連続ではない)
                if realize_current_pitch is None:
                    continue
                if realize_previous_pitch is None:
                    continue

                # 2声が並行している
                if not ValidatingInMeasureState.is_parallel_motion(
                    sequence_1=(cf_previous_pitch, cf_current_pitch),
                    sequence_2=(realize_previous_pitch, realize_current_pitch),
                ):
                    continue

                # 和声音である
                if not realize_current_a_note.tone_type == ToneType.HARMONIC_TONE:
                    continue
                if not realize_previous_a_note.tone_type == ToneType.HARMONIC_TONE:
                    continue

                # 直接の連続の規則として連続である
                if not ValidatingInMeasureState.is_parallel_violation(
                    sequence_1=(cf_previous_pitch, cf_current_pitch),
                    sequence_2=(realize_previous_pitch, realize_current_pitch),
                ):
                    continue

                return False

        return True

    @classmethod
    def is_parallel_motion(cls, sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
        """
        2つの旋律の進行が並行しているかどうかを返す
        """
        s1_start, s1_end = sequence_1
        s2_start, s2_end = sequence_2

        # どちらかが動いていない場合は並行ではない
        if s1_start == s1_end or s2_start == s2_end:
            return False

        dir1_up = s1_end.num() > s1_start.num()
        dir2_up = s2_end.num() > s2_start.num()
        return dir1_up == dir2_up

    @classmethod
    def is_contrary_motion(cls, sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
        """
        2つの旋律の進行が反行しているかどうかを返す
        """
        s1_start, s1_end = sequence_1
        s2_start, s2_end = sequence_2

        # どちらかが動いていない場合は反行していない
        if s1_start == s1_end or s2_start == s2_end:
            return False

        dir1_up = s1_end.num() > s1_start.num()
        dir2_up = s2_end.num() > s2_start.num()
        return dir1_up != dir2_up

    @classmethod
    def is_parallel_violation(cls, sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
        """
        連続5度・8度の禁則が含まれているかどうか。
        並行・反行のいずれも禁則とする。(斜行と同時保留はOK)
        """
        if not (
            ValidatingInMeasureState.is_parallel_motion(sequence_1, sequence_2)
            or ValidatingInMeasureState.is_contrary_motion(sequence_1, sequence_2)
        ):
            return False

        first_interval_normalized = Interval.of(sequence_1[0], sequence_2[0]).normalize()
        second_interval_normalized = Interval.of(sequence_1[1], sequence_2[1]).normalize()

        # 連続8度(1度)
        if first_interval_normalized == second_interval_normalized == Interval.parse("P1"):
            return True

        # 連続5度(完全-完全)
        if first_interval_normalized == second_interval_normalized == Interval.parse("P5"):
            return True

        # 連続5度(減-完全)
        if first_interval_normalized == Interval.parse("d5") and second_interval_normalized == Interval.parse("P5"):
            return True

        # 連続5度(完全-減) 3声からは許されるが、現在は2声のみ扱うので禁則扱い
        if first_interval_normalized == Interval.parse("P5") and second_interval_normalized == Interval.parse("d5"):
            return True

        return False

    @classmethod
    def is_hidden_interval_violation(cls, sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
        """
        並達5度・8度の禁則が含まれているかどうか
        """
        if not ValidatingInMeasureState.is_parallel_motion(sequence_1, sequence_2):
            return False

        second_interval_normalized = Interval.of(sequence_1[1], sequence_2[1]).normalize()
        if second_interval_normalized in [Interval.parse("P1"), Interval.parse("P5")]:
            return True
        else:
            return False


# ------------ ValidatingAllMeasureState --------------


@dataclass(frozen=True)
class ValidatingAllMeasureState(State):
    """
    全体のバリデーション中
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]

    def __post_init__(self) -> None:
        assert self.previous_measure_number() == self.last_measure_number()

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if not self.validate():
            yield from []
            return
        else:
            yield EndState(
                cantus_firmus=self.cantus_firmus,
                completed_measures=self.completed_measures,
            )

    def validate(self) -> bool:
        return True  # TODO


# ------------ EndState --------------


@dataclass(frozen=True)
class EndState(State):
    """
    バリデーションが終わり、生成が完了した状態。
    """

    cantus_firmus: list[Pitch]
    completed_measures: list[AnnotatedMeasure]

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        raise RuntimeError("not called")

    def to_score(self) -> Score:
        cf_notes = [Note(pitch, Duration.of(4)) for pitch in self.cantus_firmus]
        cf_measures = [Measure([note]) for note in cf_notes]

        realized_measures = [am.to_measure() for am in self.completed_measures]

        return Score(
            key=KEY,
            time_signature=TIME_SIGNATURE,
            parts=[
                Part(part_id=CF_PART_ID, measures=cf_measures),
                Part(part_id=REALIZE_PART_ID, measures=realized_measures),
            ],
        )

import random
from abc import ABC, abstractmethod
from collections.abc import Iterator
from dataclasses import dataclass, replace
from enum import Enum
from fractions import Fraction
from typing import ClassVar, TypeVar

from my_project.model import (
    Duration,
    Interval,
    IntervalStep,
    Key,
    Measure,
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
from my_project.util import add_interval_step_in_key, part_range, scale_pitches, shuffled_interleave, sliding

T = TypeVar("T")

KEY = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
TIME_SIGNATURE = TimeSignature(4, Fraction(1))
NOTES_IN_MEASURE = 4
MEASURE_TOTAL_DURATION = Duration.of(NOTES_IN_MEASURE)
CF_PART_ID = PartId.BASS
REALIZE_PART_ID = PartId.SOPRANO


def generate(cantus_firmus: list[Pitch], rythmn_type: "RythmnType") -> Iterator[Score]:
    return map(lambda state: state.to_score(), State.start_state(cantus_firmus, rythmn_type).final_states())


# ---


class RythmnType(Enum):
    """
    課題の実施で利用されるリズム
    """

    # 四部音符
    QUATER_NOTE = 1
    # 二部音符
    HALF_NOTE = 2

    def note_duration(self) -> Duration:
        match self:
            case RythmnType.QUATER_NOTE:
                return Duration.of(1)
            case RythmnType.HALF_NOTE:
                return Duration.of(2)


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

    def offset_note_at(self, offset: Offset) -> tuple[Offset, AnnotatedNote] | None:
        """
        この小節の Offset の時刻に鳴っている音の、開始した Offset と AnnotatedNote を返す。
        その Offset の時に休符であれば None, 小節の範囲外の Offset を指定した場合は例外
        """
        current_offset = Offset.of(0)

        for annotated_note in self.annotated_notes:
            note_duration = annotated_note.note.duration
            note_end_offset = current_offset.add_duration(note_duration)
            if current_offset <= offset < note_end_offset:
                if annotated_note.note.pitch is None:
                    return None
                else:
                    return (current_offset, annotated_note)

            current_offset = note_end_offset

        raise ValueError(
            f"Offset {offset.value} is out of bounds for this measure. Total duration is {current_offset.value}."
        )

    def pitch_at(self, offset: Offset) -> Pitch | None:
        """
        この小節のOffsetの時刻に鳴っているPitchを返す。
        そのOffsetの時に休符であればNone, 小節の範囲外のOffsetを指定した場合は例外

        offset_note_at
        """
        offset_note = self.offset_note_at(offset)
        if offset_note is None:
            return None
        return offset_note[1].note.pitch


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

    def is_first_measure(self) -> bool:
        return len(self.completed_measures) == 0

    def is_measures_fulfilled(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus)

    def is_last_measure(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus) - 1

    def is_next_last_measure(self) -> bool:
        return len(self.completed_measures) == len(self.cantus_firmus) - 2

    def previous_measure(self) -> AnnotatedMeasure | None:
        if self.is_first_measure():
            return None
        else:
            return self.completed_measures[len(self.completed_measures) - 1]

    def previous_cf(self) -> Pitch | None:
        if self.is_first_measure():
            return None
        else:
            return self.cantus_firmus[len(self.completed_measures) - 1]

    def current_cf(self) -> Pitch:
        return self.cantus_firmus[len(self.completed_measures)]

    def next_measure_cf(self) -> Pitch | None:
        if self.is_last_measure():
            return None
        else:
            return self.cantus_firmus[len(self.completed_measures) + 1]


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
        if not self.is_first_measure:
            assert self.previous_measure is not None
            assert self.previous_cf is not None
        if not self.is_last_measure:
            assert self.next_measure_cf is not None
        if self.is_next_last_measure:
            assert self.next_measure_cf is not None, (
                "次の小節が最終小節ならば、次の小節のCFが取得できる"
                f"{self.is_next_last_measure=}, {self.next_measure_cf=}",
            )
        assert ((self.previous_measure is None) and (self.previous_cf is None)) or (
            (self.previous_measure is not None) and (self.previous_cf is not None)
        ), (f"前の小節が無い時は前のCFも無く、逆もしかり{self.previous_measure=}, {self.previous_cf=}",)

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

    def is_buffer_fulfilled(self) -> bool:
        total_note_buffer_duration = self.total_note_buffer_duration()
        if total_note_buffer_duration == MEASURE_TOTAL_DURATION:
            return True
        elif total_note_buffer_duration < MEASURE_TOTAL_DURATION:
            return False
        else:
            raise ValueError(f"total_note_buffer_duration: {total_note_buffer_duration}")

    # --

    def available_harmonic_pitches_with_chord(self) -> list[tuple[Pitch, bool | None]]:
        """
        課題の冒頭の音または最終小節以外で、協和音として利用できる音と、利用したことにより確定した和音を返す。
        確定した和音は is_first_inversion_chord と同様に bool | None で返す。
        和音が未設定の場合はCFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
        """

        cf = self.current_cf

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
            for pitch in LocalMeasureContext.AVAILABLE_PITCHES_LIST  # 声域内の調の音
            if cf.num() <= pitch.num() and Interval.of(cf, pitch).step() <= IntervalStep.idx_1(15)  # 2オクターブ未満
        ]

        result: list[tuple[Pitch, bool | None]] = []
        for pitch in all_available_pitches:
            step = Interval.of(cf, pitch).normalize().step()
            if step in step_and_next_chord_dict.keys():
                result.append((pitch, step_and_next_chord_dict[step]))
        return result

    AVAILABLE_PITCHES_LIST: ClassVar[list[Pitch]] = scale_pitches(KEY, part_range(REALIZE_PART_ID))
    AVAILABLE_PITCHES_SET: ClassVar[set[Pitch]] = set(AVAILABLE_PITCHES_LIST)

    @classmethod
    def filter_available_pitches(cls, pitches: list[Pitch]) -> list[Pitch]:
        return [pitch for pitch in pitches if pitch in LocalMeasureContext.AVAILABLE_PITCHES_SET]

    @classmethod
    def make_annotated_note(cls, pitch: Pitch | None, tone_type: ToneType, duration: Duration) -> AnnotatedNote:
        """Pitch, ToneType, Duration から AnnotatedNote を作成するヘルパー"""
        return AnnotatedNote(Note(pitch, duration), tone_type)

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
            if pitch in LocalMeasureContext.AVAILABLE_PITCHES_SET
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
            if pitch in LocalMeasureContext.AVAILABLE_PITCHES_SET
        ]

    @classmethod
    def available_pitches(cls, cf: Pitch) -> list[Pitch]:
        """
        冒頭または最終小節以外で、協和音として利用できる音を返す
        CFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
        """
        return [
            pitch
            for pitch in LocalMeasureContext.AVAILABLE_PITCHES_LIST  # 声域内の調の音
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
        return interval.abs() in LocalMeasureContext.VALID_MELODIC_INTERVAL_SET


@dataclass(frozen=True)
class State(ABC):
    """
    全ての状態の基底クラス。
    現在の「グローバルコンテキスト」と「ローカル(作業中)コンテキスト」を保持する。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext | None  # 最終検証中などはNoneになる

    @classmethod
    def start_state(cls, cantus_firmus: list[Pitch], rythmn_type: RythmnType) -> "State":
        # 最初のGlobalContextを準備
        g_ctx = GlobalContext(
            cantus_firmus=cantus_firmus,
            rythmn_type=rythmn_type,
            completed_measures=[],
            next_measure_mark=None,
        )

        # 最初の小節(Measure 1)のためのLocalMeasureContextを準備
        l_ctx = LocalMeasureContext(
            previous_measure=None,
            previous_cf=None,
            current_cf=g_ctx.current_cf(),
            next_measure_cf=g_ctx.next_measure_cf(),
            rythmn_type=g_ctx.rythmn_type,
            is_first_measure=True,
            is_last_measure=g_ctx.is_last_measure(),
            is_next_last_measure=g_ctx.is_next_last_measure(),
            note_buffer=[],
            is_root_chord=None,
            next_measure_mark=None,
        )

        # 探索開始の起点となる ChooseSearchState を返す
        return ChooseSearchState(g_ctx, l_ctx)

    @abstractmethod
    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        pass

    def final_states(self, randomized: bool = True) -> Iterator["EndState"]:
        def _find_terminal_states(state: State) -> Iterator["EndState | PrunedState"]:
            """
            再帰的に探索し、EndState(成功) または PrunedState(グローバルな失敗) を見つける。
            MeasurePrunedStateは内部で破棄(バックトラック)する。
            """
            # グローバルな成功(EndState) または グローバルな失敗(PrunedState)
            # これらは最上位まで伝播させ、小節全体を再試行させる。
            if isinstance(state, EndState) or isinstance(state, PrunedState):
                yield state
                return
            # ローカルな失敗(MeasurePrunedState)
            #  この分岐はここで破棄し、直前の音を再試行させる。
            if isinstance(state, MeasurePrunedState):
                yield from []
                return

            # 試行結果がランダムになるように
            child_states = state.next_states(randomized)
            child_iterators = [_find_terminal_states(child) for child in child_states]
            iterator_wrapper = shuffled_interleave(child_iterators, randomized)

            for result_state in iterator_wrapper:
                if isinstance(result_state, PrunedState):
                    yield result_state
                    return

                else:
                    assert isinstance(result_state, EndState)
                    yield result_state

        while True:
            search_iterator = _find_terminal_states(self)
            found_global_failure = False
            for terminal_state in search_iterator:
                if isinstance(terminal_state, EndState):
                    yield terminal_state
                else:
                    assert isinstance(terminal_state, PrunedState)
                    found_global_failure = True
                    break

            if found_global_failure:
                continue
            else:
                break


@dataclass(frozen=True)
class ChooseSearchState(State):
    """
    小節の探索方法を選ぶか、バリデーションの状態に移動する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext  # 必ずLocalContextを持つ

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        # local_ctx を使って分岐を決定する
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
            if randomized:
                random.shuffle(next_states)
            yield from next_states


@dataclass(frozen=True)
class SearchingStartNoteState(State):
    """
    課題冒頭の音を選択する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)
        assert self.local_ctx.next_measure_mark is None

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        cf = self.local_ctx.current_cf
        possible_pitches: list[Pitch] = LocalMeasureContext.start_available_pitches(cf)

        next_states: list[State] = []
        for pitch in possible_pitches:
            duration = self.local_ctx.rythmn_type.note_duration()
            new_local_ctx = replace(
                self.local_ctx,
                note_buffer=[
                    LocalMeasureContext.make_annotated_note(None, ToneType.HARMONIC_TONE, duration),
                    LocalMeasureContext.make_annotated_note(pitch, ToneType.HARMONIC_TONE, duration),
                ],
                next_measure_mark=None,
                is_root_chord=True,
            )
            next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingEndNoteState(State):
    """
    課題の最後の小節の和声音を選択する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() == Duration.of(0)

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        next_pitches: list[Pitch]
        if self.local_ctx.next_measure_mark is not None:
            next_pitches = [self.local_ctx.next_measure_mark]
        else:
            cf = self.local_ctx.current_cf
            next_pitches = LocalMeasureContext.end_available_pitches(cf)
            # 前の音との音程の確認
            previous_pitch = self.local_ctx.previous_latest_added_pitch()
            next_pitches = [
                p for p in next_pitches if LocalMeasureContext.is_valid_melodic_interval(p - previous_pitch)
            ]
        next_states: list[State] = []
        for next_pitch in next_pitches:
            new_local_ctx = replace(
                self.local_ctx,
                note_buffer=[
                    # NOTE: RythmnType によらずこの音価は一定で全音符
                    LocalMeasureContext.make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, Duration.of(4))
                ],
                next_measure_mark=None,
                is_root_chord=True,
            )
            next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingHarmonicNoteInMeasureState(State):
    """
    小節内の探索中。和声音を1音追加する
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        next_pitch_and_chord_list: list[tuple[Pitch, bool | None]] = []

        if self.local_ctx.next_measure_mark is not None:
            cf_mark_step = (self.local_ctx.current_cf - self.local_ctx.next_measure_mark).normalize().step()
            if cf_mark_step == IntervalStep.idx_1(5):
                is_root_chord = True
            elif cf_mark_step == IntervalStep.idx_1(6):
                is_root_chord = False
            elif cf_mark_step in [IntervalStep.idx_1(1), IntervalStep.idx_1(3)]:
                is_root_chord = None
            else:
                raise ValueError(
                    "invalid next_measure_mark. "
                    f"current_cf: {self.local_ctx.current_cf}, next_measure_mark: {self.local_ctx.next_measure_mark}"
                )
            next_pitch_and_chord_list = [(self.local_ctx.next_measure_mark, is_root_chord)]
        else:
            # 和音上利用できる音の中で、前の音との旋律的音程が許されるもの
            previous_pitch = self.local_ctx.previous_latest_added_pitch()
            all_candidates: list[tuple[Pitch, bool | None]] = self.local_ctx.available_harmonic_pitches_with_chord()
            for next_pitch, next_is_root_chord in all_candidates:
                if LocalMeasureContext.is_valid_melodic_interval(next_pitch - previous_pitch):
                    next_pitch_and_chord_list.append((next_pitch, next_is_root_chord))

        next_states: list[State] = []
        for next_pitch, next_is_root_chord in next_pitch_and_chord_list:
            duration = self.local_ctx.rythmn_type.note_duration()
            new_local_ctx = replace(
                self.local_ctx,
                note_buffer=[
                    *self.local_ctx.note_buffer,
                    LocalMeasureContext.make_annotated_note(next_pitch, ToneType.HARMONIC_TONE, duration),
                ],
                next_measure_mark=None,
                is_root_chord=next_is_root_chord,
            )
            next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))

        if randomized:
            random.shuffle(next_states)
        yield from next_states


@dataclass(frozen=True)
class SearchingPassingNoteInMeasureState(State):
    """
    小節内の探索中。経過音を追加する。note_bufferに2つ以上の音が追加され、next_measure_markが付くこともある。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        """
        経過音を利用する。
        note_bufferに1つ以上の PASSING_TONE と1つの HARMONIC_TONE を追加する。
        必要に応じて next_measure_mark に値が設定される。
        """
        # 最終小節や、小節の1拍目(課題の冒頭を含む)では利用できない。
        if self.local_ctx.is_last_measure or self.local_ctx.current_offset() == Offset.of(0):
            yield from []
            return
        # マークがある場合は非和声音を利用できない
        if self.local_ctx.next_measure_mark is not None:
            yield from []
            return
        # 最終小節ではないので次の小節のCFは必ず取得できる
        assert self.local_ctx.next_measure_cf is not None

        # 直前の音から目標音までの音程(上向きのみ), 到達音が現在の小節に含まれるかどうか(小節を跨がないか)の一覧を求める
        patterns: list[tuple[IntervalStep, bool]] = SearchingPassingNoteInMeasureState.progression_pattern(
            current_offset=self.local_ctx.current_offset(), rythmn_type=self.local_ctx.rythmn_type
        )

        next_states: list[State] = []
        for step, is_target_note_in_current_number in patterns:
            # 到達する音高を求める
            target_pitch = add_interval_step_in_key(KEY, self.local_ctx.previous_latest_added_pitch(), step)

            # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
            if is_target_note_in_current_number:
                # 小節を跨がない場合

                # 小節を跨がない場合、到達した音は課題の冒頭の音または最終小節以外の音である。
                # それらの利用できる音を求める
                available_pitches_and_next_chord = self.local_ctx.available_harmonic_pitches_with_chord()

                for available_pitch, is_next_root_chord in available_pitches_and_next_chord:
                    if target_pitch != available_pitch:
                        continue

                    duration = self.local_ctx.rythmn_type.note_duration()
                    pitches = SearchingPassingNoteInMeasureState.conjunct_pitches(
                        KEY, self.local_ctx.previous_latest_added_pitch(), step
                    )
                    init_notes = [
                        LocalMeasureContext.make_annotated_note(p, ToneType.PASSING_TONE, duration)
                        for p in pitches[:-1]
                    ]
                    last_note = LocalMeasureContext.make_annotated_note(pitches[-1], ToneType.HARMONIC_TONE, duration)
                    new_local_ctx = replace(
                        self.local_ctx,
                        note_buffer=[*self.local_ctx.note_buffer, *init_notes, last_note],
                        next_measure_mark=None,
                        is_root_chord=is_next_root_chord,
                    )
                    next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))
                pass
            else:
                # 小節を跨ぐ場合

                # この小節の和音の音かどうかは気にしなくて良い。
                # 次の小節が最終小節かどうかに応じて利用できる音高が異なる。
                assert self.local_ctx.next_measure_cf is not None
                if self.local_ctx.is_next_last_measure:
                    available_pitches = LocalMeasureContext.end_available_pitches(self.local_ctx.next_measure_cf)
                else:
                    available_pitches = LocalMeasureContext.available_pitches(self.local_ctx.next_measure_cf)

                for available_pitch in available_pitches:
                    if target_pitch != available_pitch:
                        continue

                    pitches = SearchingPassingNoteInMeasureState.conjunct_pitches(
                        KEY, self.local_ctx.previous_latest_added_pitch(), step
                    )
                    notes_to_add_buffer = [
                        LocalMeasureContext.make_annotated_note(
                            p, ToneType.PASSING_TONE, self.local_ctx.rythmn_type.note_duration()
                        )
                        for p in pitches[:-1]
                    ]
                    next_measure_mark = pitches[-1]

                    new_local_ctx = replace(
                        self.local_ctx,
                        note_buffer=[*self.local_ctx.note_buffer, *notes_to_add_buffer],
                        next_measure_mark=next_measure_mark,
                    )
                    next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))

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
    def progression_pattern(cls, current_offset: Offset, rythmn_type: RythmnType) -> list[tuple[IntervalStep, bool]]:
        """
        現在のオフセットに応じて、
        直前の音から目標音までのIntervalStepと、到達した音が現在の小節に含まれるか(小節を跨いでいないか)どうかの一覧を返す
        1拍目からは経過音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる
        その他リズムパターンに含まれないオフセットを渡すと例外となる。
        """
        patterns: list[tuple[IntervalStep, bool]] = []

        match rythmn_type:
            case RythmnType.QUATER_NOTE:
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
            case RythmnType.HALF_NOTE:
                if current_offset == Offset.idx_1(3):
                    # 3拍目の探索中は、次の小節の1拍目に向けて経過音が利用できる。
                    patterns = [
                        (IntervalStep.idx_1(3), False),
                    ]
                else:
                    raise RuntimeError(f"invalid current_offset: {current_offset}")

        # パターンに下向きの音程を追加
        patterns = [*patterns, *[(p[0] * -1, p[1]) for p in patterns]]
        return patterns


@dataclass(frozen=True)
class SearchingNeighborNoteInMeasureState(State):
    """
    小節内の探索中。刺繍音を追加する。note_bufferに2つの音が追加され、next_measure_markが付くこともある。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.local_ctx.total_note_buffer_duration() < MEASURE_TOTAL_DURATION

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        """
        刺繍音を利用する。
        note_bufferに1つの NEIGHBOR_TONE と1つの HARMONIC_TONE を追加する。
        必要に応じて next_measure_mark に値が設定される。
        """
        # 最終小節や、小節の1拍目(課題の冒頭を含む)では利用できない。
        if self.local_ctx.is_last_measure or self.local_ctx.current_offset() == Offset.of(0):
            yield from []
            return
        # マークがある場合は非和声音を利用できない
        if self.local_ctx.next_measure_mark is not None:
            yield from []
            return
        # 最終小節ではないので次の小節のCFは必ず取得できる
        assert self.local_ctx.next_measure_cf is not None

        next_states: list[State] = []

        # 小節を跨ぐ場合と跨がない場合で大きく分岐して考える
        if self.is_target_note_in_current_measure():
            # 小節を跨がない場合は、直前に追加した音が和声音であるため、音域内であれば利用可能
            for neighbor_note_pitch in self.available_neighbor_note_pitches():
                previous_pitch = self.local_ctx.previous_latest_added_pitch()
                duration = self.local_ctx.rythmn_type.note_duration()
                new_local_ctx = replace(
                    self.local_ctx,
                    note_buffer=[
                        *self.local_ctx.note_buffer,
                        LocalMeasureContext.make_annotated_note(neighbor_note_pitch, ToneType.NEIGHBOR_TONE, duration),
                        LocalMeasureContext.make_annotated_note(previous_pitch, ToneType.HARMONIC_TONE, duration),
                    ],
                    next_measure_mark=None,
                )
                next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))
        else:
            # 小節を跨ぐ場合、最終小節かどうかに応じて利用できる音高を求め、その中に直前の音が含まれるかを確認する
            if self.local_ctx.is_next_last_measure:
                available_pitches = set(LocalMeasureContext.end_available_pitches(self.local_ctx.next_measure_cf))
            else:
                available_pitches = set(LocalMeasureContext.available_pitches(self.local_ctx.next_measure_cf))

            previous_pitch = self.local_ctx.previous_latest_added_pitch()
            if previous_pitch in available_pitches:
                for neighbor_note_pitch in self.available_neighbor_note_pitches():
                    new_local_ctx = replace(
                        self.local_ctx,
                        note_buffer=[
                            *self.local_ctx.note_buffer,
                            LocalMeasureContext.make_annotated_note(
                                neighbor_note_pitch,
                                ToneType.NEIGHBOR_TONE,
                                self.local_ctx.rythmn_type.note_duration(),
                            ),
                        ],
                        next_measure_mark=previous_pitch,
                    )
                    next_states.append(ChooseSearchState(self.global_ctx, new_local_ctx))

        if randomized:
            random.shuffle(next_states)
        yield from next_states

    def available_neighbor_note_pitches(self) -> list[Pitch]:
        """
        直前の音をもとに、音域内で利用できる刺繍音の一覧を返す。
        2度上・2度下
        """
        previous_latest_added_pitch = self.local_ctx.previous_latest_added_pitch()
        neighbor_steps = [IntervalStep.idx_1(2), IntervalStep.idx_1(-2)]
        result: list[Pitch] = []
        for step in neighbor_steps:
            neighbor_pitch = add_interval_step_in_key(KEY, previous_latest_added_pitch, step)
            if neighbor_pitch in LocalMeasureContext.AVAILABLE_PITCHES_SET:  # 声域内か
                result.append(neighbor_pitch)
        return result

    def is_target_note_in_current_measure(self) -> bool:
        """
        現在のオフセットに応じて、刺繍音を利用した時の最後の音が現在の小節に含まれるか(小節を跨いでいないか)どうかを返す
        1拍目からは刺繍音は利用できないため、 current_offset に Offset.of(0) を渡すと例外となる。
        その他リズムパターンに含まれないオフセットを渡すと例外となる。
        """
        current_offset = self.local_ctx.current_offset()
        match self.local_ctx.rythmn_type:
            case RythmnType.QUATER_NOTE:
                if current_offset in [Offset.idx_1(2), Offset.idx_1(3)]:
                    # 2拍目の探索中は現在の小節の3拍目に到達する
                    # 3拍目の探索中は現在の小節の4拍目に到達する
                    return True
                elif current_offset == Offset.idx_1(4):
                    # 4拍目の探索中は次の小節の1拍目に到達する
                    return False
                else:
                    raise RuntimeError(f"invalid current_offset: {current_offset}")
            case RythmnType.HALF_NOTE:
                if current_offset == Offset.idx_1(3):
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

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def __post_init__(self) -> None:
        assert self.total_note_buffer_duration() == MEASURE_TOTAL_DURATION

    # LocalMeasureContext と定義が重複している。
    def total_note_buffer_duration(self) -> Duration:
        """
        バッファにある音価の合計。4未満の場合は探索中、4であれば探索完了を表す。
        """
        return sum([an.note.duration for an in self.local_ctx.note_buffer], Duration.of(0))

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        # validate() は self.global_ctx と self.local_ctx を
        # 使って(古いコードと同様に)検証する
        if not self.validate():
            # 1. ★ 検証失敗
            #    「小節内の失敗」状態を yield する。
            #    final_states はこれを検知し、この分岐を破棄する (バックトラック)
            yield MeasurePrunedState(self.global_ctx, self.local_ctx)
            return
        else:
            # 2. ★ 検証成功！
            #    「ローカル」の作業結果(note_buffer)を
            #    「グローバル」の完了リストに追加する。
            new_global_ctx = replace(
                self.global_ctx,
                completed_measures=[
                    *self.global_ctx.completed_measures,
                    AnnotatedMeasure(self.local_ctx.note_buffer),
                ],
                next_measure_mark=self.local_ctx.next_measure_mark,
            )

            # 3. 曲全体が完成したか？
            if new_global_ctx.is_measures_fulfilled():
                # 曲が完成 -> 全体バリデーションへ
                yield ValidatingAllMeasureState(new_global_ctx, None)
            else:
                # 4. 次の小節の探索へ
                #    「更新されたグローバル」を使い、
                #    「次の小節用のローカル」を新規作成する

                next_l_ctx = LocalMeasureContext(
                    previous_measure=AnnotatedMeasure(self.local_ctx.note_buffer),
                    previous_cf=self.local_ctx.current_cf,
                    current_cf=new_global_ctx.current_cf(),
                    next_measure_cf=new_global_ctx.next_measure_cf(),
                    rythmn_type=new_global_ctx.rythmn_type,
                    is_first_measure=False,
                    is_last_measure=new_global_ctx.is_last_measure(),
                    is_next_last_measure=new_global_ctx.is_next_last_measure(),
                    note_buffer=[],
                    is_root_chord=None,
                    next_measure_mark=new_global_ctx.next_measure_mark,
                )

                yield ChooseSearchState(new_global_ctx, next_l_ctx)

    def validate(self) -> bool:
        return self.validate_interval() and self.validate_melody()

    def validate_interval(self) -> bool:
        """
        連続・並達に関するバリデーション。禁則があれば False を返す
        """

        # 冒頭小節には直前の小節が存在しないため、連続は起こり得ない。
        if self.local_ctx.is_first_measure:
            return True
        assert self.local_ctx.previous_measure is not None
        assert self.local_ctx.previous_cf is not None

        previous_cf = self.local_ctx.previous_cf
        current_cf = self.local_ctx.current_cf

        previous_measure = self.local_ctx.previous_measure
        current_measure = AnnotatedMeasure(self.local_ctx.note_buffer)

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
        # 間接の連続は、全音符1個に相当する長さが隔てられていれば許される。またもっと近くにあっても
        # 同時に打音されいるのではなく、かつ、反行している場合かいずれかの音が非和声音である場合は許される。
        #
        # すなわち、以下を満たした場合、禁則となる。
        # ある声部の Offset の差が Duration.of(4) 以下の異なる2音のうち、
        # ある他の声部の、それらの音に同時になっている2音を選び、
        # それら2声部の音が直接の連続の規則として禁則であり、
        # かつ、not (後続の5度・8度をなす音が同時に打音されていない and (反行している または いずれかの音が非和声音))

        # 簡単のため、小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
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
                # Offset の差が Duration.of(4) 以下の異なる2音を選ぶ。
                if not (Offset.of(0) < realize_current_offset - realize_previous_offset <= Offset.of(4)):
                    continue
                realize_current_pitch = realize_current_a_note.note.pitch
                realize_previous_pitch = realize_previous_a_note.note.pitch
                # (休符の場合は連続ではない)
                if realize_current_pitch is None:
                    continue
                if realize_previous_pitch is None:
                    continue

                # ある他の声部の、それらの音に同時になっている2音を選ぶ。
                # (現在は定旋律に対して確認しているので必ず音高が取得できる)
                cf_current_offset_note = cf_measure.offset_note_at(realize_current_offset)
                cf_previous_offset_note = cf_measure.offset_note_at(realize_previous_offset)
                assert cf_current_offset_note is not None
                assert cf_previous_offset_note is not None
                cf_current_offset, cf_current_annotated_note = cf_current_offset_note
                _cf_previous_offset, cf_previous_annotated_note = cf_previous_offset_note
                assert cf_current_annotated_note.note.pitch is not None
                assert cf_previous_annotated_note.note.pitch is not None
                cf_current_pitch = cf_current_annotated_note.note.pitch
                cf_previous_pitch = cf_previous_annotated_note.note.pitch

                # 直接の連続の規則として連続である
                is_parallel_violation = ValidatingInMeasureState.is_parallel_violation(
                    sequence_1=(cf_previous_pitch, cf_current_pitch),
                    sequence_2=(realize_previous_pitch, realize_current_pitch),
                )

                # 後続の5度・8度をなす音が同時に打音されている
                has_following_notes_same_offset = cf_current_offset == realize_current_offset

                # 2声が反行している
                is_parallel_motion = ValidatingInMeasureState.is_contrary_motion(
                    sequence_1=(cf_previous_pitch, cf_current_pitch),
                    sequence_2=(realize_previous_pitch, realize_current_pitch),
                )

                # いずれかの音が非和声音
                # (現在は定旋律に対して確認しているので実施声部のみを確認する)
                non_harmonic_tone_exists = (
                    realize_current_a_note.tone_type != ToneType.HARMONIC_TONE
                    or realize_previous_a_note.tone_type != ToneType.HARMONIC_TONE
                )

                if is_parallel_violation and not (
                    not has_following_notes_same_offset and (is_parallel_motion or non_harmonic_tone_exists)
                ):
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

    # --

    def validate_melody(self) -> bool:
        """
        旋律に関するバリデーション

        DONE:
        - 分散和音をしない
        - 3音符で形成される7度・9度は順次進行を含める

        優先して実装したい:
        - 完全8度の跳躍はできるだけその前後に反対方向の進行を伴う
        - 旋律の対称系や繰り返し(特に同一音への3度続く回帰)

        後回し?:
        - できるだけ非順次進行を避ける(どの程度?)
        - 小節線をはさんだ非順次進行を避ける(どの程度?)
        - 3,4個の音符で形成される増4度は同方向の順次進行で先行または後続させる
        """
        return (
            self.validate_melody_arpeggiio()
            and self.validate_melody_arpeggiio_extra()
            and self.validate_melody_interval_7_9()
            and True
        )  # TODO

    def validate_melody_arpeggiio(self) -> bool:
        """
        分散和音のバリデーション。旋律が分散和音の形になっているときFalseを返す

        TODO: 反転の分散和音はOKとしている
        """
        # 前の小節がもしあれば最後の2音を取得し、現在の小節と繋げた音列を作成
        pitches: list[Pitch] = [an.note.pitch for an in self.extended_note_buffer(2) if an.note.pitch is not None]

        arpeggiio_steps_list = [
            [IntervalStep.idx_1(3), IntervalStep.idx_1(5)],  # ドミソ
            [IntervalStep.idx_1(-3), IntervalStep.idx_1(-5)],
            [IntervalStep.idx_1(3), IntervalStep.idx_1(6)],  # ミソド
            [IntervalStep.idx_1(-3), IntervalStep.idx_1(-6)],
            [IntervalStep.idx_1(4), IntervalStep.idx_1(6)],  # ソドミ
            [IntervalStep.idx_1(-4), IntervalStep.idx_1(-6)],  # ソドミ
        ]

        for ps in sliding(pitches, window_size=3):
            base, p1, p2 = ps
            intervals = [p1 - base, p2 - base]
            # NOTE: interval を normalize すると [C4 A3 A4] が C4に対して3度・6度と判定されてしまう。
            # NOTE: 複音程は旋律の規則としてそもそも選ばれないので無視してよい。例えば [C4 *E4 *G5] の10度は選ばれない。
            # NOTE: 以下の steps を sort すると、反転の分散和音が判定に含まれる(その場合 arpeggiio_steps_list は上方だけでよい)
            steps = [i.step() for i in intervals]
            if steps in arpeggiio_steps_list:
                return False

        return True

    def validate_melody_arpeggiio_extra(self) -> bool:
        """
        特殊な形態のいくつかの分散和音を禁止する。

        - (A-1): [C4 G4 C5], [G4 C5 G5], [G4, C4, C5] といった第3音を伴わない分散和音(反転なし)

        ---
        以下も考えられるが、現在は認めている

        - (A-2): [G4, C4, C5] といった第3音を伴わない分散和音(反転あり)
          - (しかしこれは [G4 A4 *G4 *C4 | *C5 B4 A4 G4] といった認めたくなるケースがある
        - (B)] [C4 C5 C4] といったオクターブの移動
          - (しかしこれは困難な場合には例外として許される)
          - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
        - (C): [C4 G4 C4 C4] や [C5 G4 C5 G4] といった4度・5度の反復
          - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
        """
        pitches: list[Pitch] = [an.note.pitch for an in self.extended_note_buffer(2) if an.note.pitch is not None]

        arpeggiio_steps_list = [
            [IntervalStep.idx_1(5), IntervalStep.idx_1(8)],  # [C4 G4 C5]
            [IntervalStep.idx_1(-5), IntervalStep.idx_1(-8)],
            [IntervalStep.idx_1(4), IntervalStep.idx_1(8)],  # [G4 C5 G5]
            [IntervalStep.idx_1(-4), IntervalStep.idx_1(-8)],
        ]

        for ps in sliding(pitches, window_size=3):
            base, p1, p2 = ps
            intervals = [p1 - base, p2 - base]
            steps = [i.step() for i in intervals]
            if steps in arpeggiio_steps_list:
                return False

        return True

    def validate_melody_interval_7_9(self) -> bool:
        """
        3音符で形成される7度・9度は順次進行を含める必要がある。そうなっていなければFalseを返す
        (9度より大きい音程になることは別の規則で禁止されそうだが、この規則で扱う)
        """

        # 前の小節がもしあれば最後の2音を取得し、現在の小節と繋げた音列を作成
        pitches: list[Pitch] = [an.note.pitch for an in self.extended_note_buffer(2) if an.note.pitch is not None]
        for ps in sliding(pitches, window_size=3):
            p1, p2, p3 = ps
            step_1_3 = (p1 - p3).abs().step()
            if step_1_3 == IntervalStep.idx_1(7) or step_1_3 > IntervalStep.idx_1(9):
                step_1_2 = (p1 - p2).abs().step()
                step_2_3 = (p2 - p3).abs().step()
                if step_1_2 == IntervalStep.idx_1(2) or step_2_3 == IntervalStep.idx_1(2):
                    continue
                else:
                    return False

        return True

    def extended_note_buffer(self, num: int) -> list[AnnotatedNote]:
        """
        前の小節の末尾から num 音取得し、 note_buffer と繋げたリストを返す
        """
        annotated_notes: list[AnnotatedNote] = []
        if self.local_ctx.previous_measure is not None:
            annotated_notes.extend([an for an in self.local_ctx.previous_measure.annotated_notes[-2:]])
        annotated_notes.extend([an for an in self.local_ctx.note_buffer])
        return annotated_notes


@dataclass(frozen=True)
class MeasureEndState(State):
    """
    旋律の生成・バリデーションが完了した状態。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        raise RuntimeError("not called")


@dataclass(frozen=True)
class MeasurePrunedState(State):
    """
    旋律のバリデーションに失敗した。
    途中からではなく最初からやり直すために、擬似的な完了状態にしてフィルタさせる。<- これやらないほうがいいわ。
    一つ前に追加した音からやり直した方が高速。
    """

    global_ctx: GlobalContext
    local_ctx: LocalMeasureContext

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        raise RuntimeError("not called")


# ------------ ValidatingAllMeasureState --------------


@dataclass(frozen=True)
class ValidatingAllMeasureState(State):
    """
    全体のバリデーション中
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self, randomized: bool = True) -> Iterator[State]:
        if not self.validate():
            yield PrunedState(self.global_ctx, None)

        else:
            yield EndState(self.global_ctx, None)

    def validate(self) -> bool:
        return self.validate_part_total_range() and True  # TODO

    def validate_part_total_range(self) -> bool:
        """
        各声部の音域は同一課題中において11度を越えてはならない。越えた場合 False

        順次進行が長く続く場合には例外として12度が認められるが、ここでは禁止としている。
        """
        all_pitches = [
            an.note.pitch
            for m in self.global_ctx.completed_measures
            for an in m.annotated_notes
            if an.note.pitch is not None
        ]
        p_min = min(all_pitches, key=lambda p: p.num())
        p_max = max(all_pitches, key=lambda p: p.num())

        return (p_max - p_min).step() <= IntervalStep.idx_1(11)


# ------------ EndState --------------


@dataclass(frozen=True)
class EndState(State):
    """
    課題全体のバリデーションが終わり、生成が完了した状態。
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
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
    課題全体のバリデーションに失敗した。途中からではなく最初からやり直すために、擬似的な完了状態にしてフィルタさせる。
    """

    global_ctx: GlobalContext
    local_ctx: None

    def next_states(self, randomized: bool = True) -> Iterator["State"]:
        raise RuntimeError("not called")

from collections.abc import Callable, Sequence
from dataclasses import dataclass
from enum import Enum
from fractions import Fraction
from functools import cached_property
from typing import ClassVar, Protocol, TypeVar

import my_project.model_ops as ops

_REPR_HUMAN_READABLE = True

## ----- 音名に対する定義


@dataclass(frozen=True, order=True)
class NoteName:
    """
    五度圏上の位置に基づく音名を表現するクラス。
    value は C=0 とし、G=1, D=2... F=-1, Bb=-2... のように五度圏順に増減する整数。
    """

    value: int

    def __post_init__(self) -> None:
        if not -15 <= self.value <= 19:
            raise ValueError("NoteName must be between -15 and 19.")

    def __repr__(self) -> str:
        if _REPR_HUMAN_READABLE:
            return f'NoteName.parse("{self.name()}")'
        return f"NoteName(value={self.value!r})"

    def __add__(self, other: "NoteName") -> "NoteName":
        return NoteName(self.value + other.value)

    def __sub__(self, other: "NoteName") -> "NoteName":
        return NoteName(self.value - other.value)

    def name(self) -> str:
        """一般的な音名表記を返す（例: "C", "F#", "Bb"）。"""
        return ops.format_note_name(self.value)

    @classmethod
    def parse(cls, name: str) -> "NoteName":
        """
        一般的な音名表記から生成する。
        Format: "[A-G][#|b]*" (例: "C", "F#", "Bb")
        """
        return cls(ops.parse_note_name_to_value(name))

    def international_pitch_notation(self) -> tuple[str, int]:
        """
        一般的な音名表記の構成要素を返す。
        Returns:
            (step, alter): stepは "C", "D" などの文字。alterは #の数（フラットは負数）。
        """
        return ops.note_name_to_string_parts(self.value)

    @classmethod
    def from_internal_pitch_notation(cls, step: str, alter: int) -> "NoteName":
        # 逆変換もopsに持たせてもいいが、単純な定数参照ならここでもOK
        base_fifth = ops._STEP_TO_BASE_FIFTH[step]
        return NoteName(base_fifth + alter * 7)


@dataclass(frozen=True, order=True)
class Octave:
    """
    音高計算のための内部的なオクターブ値。
    一般的な "C4" の "4" とは異なり、五度圏の位置に応じてオフセットされる値。
    """

    value: int

    def __add__(self, other: "Octave") -> "Octave":
        return Octave(self.value + other.value)

    def __sub__(self, other: "Octave") -> "Octave":
        return Octave(self.value - other.value)


@dataclass(frozen=True)
class Pitch:
    """
    特定のオクターブと音名を持つ絶対的な音高。
    """

    octave: Octave
    note_name: NoteName

    def __repr__(self) -> str:
        if _REPR_HUMAN_READABLE:
            return f'Pitch.parse("{self.name()}")'
        return f"Pitch(octave={self.octave!r}, note_name={self.note_name!r})"

    def __add__(self, other: "Interval") -> "Pitch":
        return Pitch(self.octave + Octave(other.octave), self.note_name + NoteName(other.fifth))

    def __sub__(self, other: "Pitch") -> "Interval":
        return Interval(self.octave.value - other.octave.value, self.note_name.value - other.note_name.value)

    def name(self) -> str:
        """国際式音名表記を返す（例: "C4", "F#5"）。"""
        return ops.format_pitch(self.octave.value, self.note_name.value)

    @classmethod
    def parse(cls, name: str) -> "Pitch":
        """
        国際式音名表記から生成する。
        Format: "[NoteName][OctaveNumber]" (例: "C4", "A#3", "Bb5")
        """
        o_val, n_val = ops.parse_pitch_to_values(name)
        return cls(Octave(o_val), NoteName(n_val))

    def international_pitch_notation(self) -> tuple[str, int, int]:
        """
        国際式音名表記 (例: C#4) の構成要素を返す。
        Returns:
            (step, alter, octave_number): "C#4" なら ("C", 1, 4)
        """
        step, alter = self.note_name.international_pitch_notation()
        not_oct = ops.pitch_to_notation_octave(self.octave.value, self.note_name.value)
        return (step, alter, not_oct)

    @classmethod
    def from_internal_pitch_notation(cls, step: str, alter: int, octave: int) -> "Pitch":
        """
        国際式音名表記の要素からPitchを生成する。
        Args:
            step: "C", "D" などの音名文字
            alter: シャープの数（フラットは負数）
            octave: "C4" の "4" にあたるオクターブ番号
        """
        note_name = NoteName.from_internal_pitch_notation(step, alter)
        base_octave = ops._STEP_TO_BASE_OCTAVE[step]
        pitch_octave_value = base_octave + alter * -4 + octave - 4
        return cls(Octave(pitch_octave_value), note_name)

    def num(self) -> "PitchNumber":
        """半音単位の連続的な数値に変換する（MIDIノート番号に近い概念）。"""
        return PitchNumber(self.note_name.value * 7 + self.octave.value * 12)

    def as_interval(self) -> "Interval":
        """このPitchを、C4からのIntervalとして扱う。"""
        return Interval(self.octave.value, self.note_name.value)


## ----- 調性に対する定義


class Mode(Enum):
    MAJOR = "Major"
    MINOR = "Minor"

    @classmethod
    def parse(cls, name: str) -> "Mode":
        """
        文字列からModeを生成する。
        Format: "Major" | "Minor"
        """
        for mode in cls:
            if mode.value == name:
                return mode
        raise ValueError(f"Invalid mode name: {name}")

    def offset(self) -> int:
        """メジャーモードを基準(0)としたときの、五度圏上のオフセット値（マイナーなら-3）。"""
        return 0 if self == Mode.MAJOR else -3


@dataclass(frozen=True)
class Key:
    """
    主音(Tonic)と旋法(Mode)によって定義される調。
    """

    tonic: NoteName
    mode: Mode

    def __repr__(self) -> str:
        if _REPR_HUMAN_READABLE:
            return f'Key.parse("{self.name()}")'
        return f"Key(tonic={self.tonic!r}, mode={self.mode!r})"

    def name(self) -> str:
        """調を表す文字列を返す（例: "C Major", "F# Minor"）。"""
        return f"{self.tonic.name()} {self.mode.value}"

    @classmethod
    def parse(cls, name: str) -> "Key":
        """
        文字列からKeyを生成する。
        Format: "[Tonic] [Mode]" (例: "C Major", "Eb Minor")
        """
        parts = name.split(" ")
        if len(parts) != 2:
            raise ValueError(f"Invalid key format: {name}")
        return cls(tonic=NoteName.parse(parts[0]), mode=Mode.parse(parts[1]))

    def signature_num(self) -> int:
        """調号の数を返す（シャープは正の数、フラットは負の数）。"""
        return self.tonic.value + self.mode.offset()

    @classmethod
    def interval_step_to_c_major_pitch(cls, interval_step: "IntervalStep") -> Pitch:
        """Cメジャーにおける指定された度数(step)に対応するPitchを返す（基準はC4）。"""
        # C Major is Key(C, Major) -> tonic=0, mode=0.
        o, n = ops.calculate_scale_pitch(0, 0, interval_step.value)
        return Pitch(Octave(o), NoteName(n))

    def diatonic_scale_pitch(self, interval_step: "IntervalStep") -> Pitch:
        """
        このKeyのダイアトニックスケールにおける、指定された度数のPitchを返す。
        ダイアトニックスケールとは、長調の場合長音階、短調の場合は自然短音階。
        """
        o, n = ops.calculate_scale_pitch(self.tonic.value, self.mode.offset(), interval_step.value)
        return Pitch(Octave(o), NoteName(n))


## ----- 調性と音高から導けるもの


@dataclass(frozen=True, order=True)
class DegreeStep:
    """音度のステップ部分。0-indexedで表す。（第1音=0, 第2音=1...）。"""

    value: int

    def __post_init__(self) -> None:
        if not 0 <= self.value <= 6:
            raise ValueError("Step must be 0-6")

    def __add__(self, other: "DegreeStep") -> "DegreeStep":
        return DegreeStep((self.value + other.value) % 7)

    def __sub__(self, other: "DegreeStep") -> "DegreeStep":
        return DegreeStep((self.value - other.value) % 7)

    @classmethod
    def idx_1(cls, step: int) -> "DegreeStep":
        """1始まりの整数（第1音=1）から生成する。"""
        return cls(step - 1)


@dataclass(frozen=True, order=True)
class DegreeAlter:
    """音度の変化記号部分（変化なし=0）。"""

    value: int

    def __post_init__(self) -> None:
        if not -1 <= self.value <= 2:
            raise ValueError("Alter must be -1 to 2")


@dataclass(frozen=True, order=True)
class Degree:
    """
    音度。調内における相対的な位置を表す。
    例: ハ長調における F# は、第4音(Step=3)の半音上げ(Alter=1)。
    """

    step: DegreeStep
    alter: DegreeAlter

    @classmethod
    def from_note_name_key(cls, note_name: NoteName, key: Key) -> "Degree":
        """指定されたKeyにおけるNoteNameの音度を計算する。"""
        s, a = ops.calculate_degree_components(note_name.value, key.tonic.value, key.mode.offset())
        return Degree(DegreeStep(s), DegreeAlter(a))

    def note_name(self, key: Key) -> "NoteName":
        """この音度が、指定されたKeyにおいてどのNoteNameになるかを返す。"""
        val = ops.calculate_degree_note_name(self.step.value, self.alter.value, key.tonic.value, key.mode.offset())
        return NoteName(val)

    @classmethod
    def idx_1(cls, step: int, alter: int) -> "Degree":
        return cls(DegreeStep.idx_1(step), DegreeAlter(alter))


## ----- 音程に関する定義


@dataclass(frozen=True)
class Interval:
    """
    2音間の隔たり（音程）。
    内部的にはオクターブ差と五度圏上の距離(fifth)で表現される。
    """

    octave: int
    fifth: int

    def __repr__(self) -> str:
        if _REPR_HUMAN_READABLE:
            return f'Interval.parse("{self.name()}")'
        return f"Interval(octave={self.octave!r}, fifth={self.fifth!r})"

    def __add__(self, other: "Interval") -> "Interval":
        return Interval(self.octave + other.octave, self.fifth + other.fifth)

    def __sub__(self, other: "Interval") -> "Interval":
        return Interval(self.octave - other.octave, self.fifth - other.fifth)

    def __mul__(self, other: int) -> "Interval":
        return Interval(self.octave * other, self.fifth * other)

    @classmethod
    def of(cls, base: Pitch, target: Pitch) -> "Interval":
        """base から target への音程を計算する。"""
        return cls(
            octave=target.octave.value - base.octave.value,
            fifth=target.note_name.value - base.note_name.value,
        )

    def step(self) -> "IntervalStep":
        """音程の度数部分（1度=0, 2度=1...）を返す。"""
        return IntervalStep(4 * self.fifth + 7 * self.octave)

    def alter(self) -> "IntervalAlter":
        """音程の半音変化部分（完全/長=0, 短=-1...）を返す。"""
        return IntervalAlter(ops.calculate_interval_alter(self.fifth, self.step().value))

    @classmethod
    def from_step_alter(cls, step: "IntervalStep", alter: "IntervalAlter") -> "Interval":
        """度数と変化記号からIntervalを生成する。"""
        o, f = ops.interval_from_step_alter(step.value, alter.value)
        return cls(octave=o, fifth=f)

    def name(self) -> str:
        """音程の省略記法を返す（例: "P1", "m3", "A4"）。"""
        return ops.format_interval(self.octave, self.fifth)

    @classmethod
    def parse(cls, name: str) -> "Interval":
        """
        音程の省略記法から生成する。
        Format: "[sgn][Quality][Number]" (例: "P1", "-m3", "A4", "dd5")
        Quality: P=Perfect, M=Major, m=minor, A=Augmented, d=Diminished
        """
        s, a = ops.parse_interval_to_components(name)
        return cls.from_step_alter(IntervalStep(s), IntervalAlter(a))

    def normalize(self) -> "Interval":
        """
        音程を単音程（±1オクターブ以内）に正規化する。
        方向（正負）は維持される。
        """
        step = self.step()
        alter = self.alter()
        if step.value > 0:
            step = IntervalStep(step.value % 7)
        elif step.value < 0:
            step = IntervalStep((-1 * step.value) % 7)
        return Interval.from_step_alter(step, alter)

    def abs(self) -> "Interval":
        """絶対値（常に上行する音程）を返す。"""
        return Interval.from_step_alter(self.step().abs(), self.alter())

    def num(self) -> "IntervalNumber":
        """半音単位の数値に変換する。"""
        return IntervalNumber(self.fifth * 7 + self.octave * 12)

    P1: ClassVar["Interval"]
    d1: ClassVar["Interval"]
    A1: ClassVar["Interval"]
    A2: ClassVar["Interval"]
    d4: ClassVar["Interval"]
    A4: ClassVar["Interval"]
    d5: ClassVar["Interval"]
    P5: ClassVar["Interval"]
    A5: ClassVar["Interval"]
    M6: ClassVar["Interval"]
    A6: ClassVar["Interval"]
    P8: ClassVar["Interval"]


@dataclass(frozen=True, order=True)
class IntervalStep:
    """音程の度数を表す。 0-indexedで表現する。（0=1度, 1=2度...）。符号により上行・下行を区別する。"""

    value: int

    def __add__(self, other: "IntervalStep") -> "IntervalStep":
        return IntervalStep(self.value + other.value)

    def __sub__(self, other: "IntervalStep") -> "IntervalStep":
        return IntervalStep(self.value - other.value)

    def __mul__(self, value: int) -> "IntervalStep":
        return IntervalStep(self.value * value)

    def to_inverval(self, alter: "IntervalAlter") -> Interval:
        return Interval.from_step_alter(self, alter)

    @classmethod
    def idx_1(cls, value: int) -> "IntervalStep":
        """一般的な1始まりの度数（"3"度など）から生成する。"""
        if value == 0:
            raise ValueError("idx_1 cannot be 0")
        return cls(value - 1 if value >= 1 else value + 1)

    def to_idx_1(self) -> int:
        """一般的な1始まりの度数（整数）に変換する。"""
        return self.value + 1 if self.value >= 0 else self.value - 1

    def abs(self) -> "IntervalStep":
        return IntervalStep(abs(self.value))

    def inversion_normalized(self) -> "IntervalStep":
        """転回して7未満（1オクターブ以内）の正の度数にする。"""
        return IntervalStep(self.value % 7)

    @classmethod
    def octave(cls) -> "IntervalStep":
        return cls(7)


@dataclass(frozen=True, order=True)
class IntervalAlter:
    """音程の種別（完全/長/短/増/減）を表す値。"""

    value: int

    def to_inverval(self, step: "IntervalStep") -> Interval:
        return Interval.from_step_alter(step, self)

    def abs(self) -> "IntervalAlter":
        return IntervalAlter(abs(self.value))

    PERFECT: ClassVar["IntervalAlter"]
    MAJOR: ClassVar["IntervalAlter"]
    MINOR: ClassVar["IntervalAlter"]
    AUGMENTED: ClassVar["IntervalAlter"]
    DIMINISHED: ClassVar["IntervalAlter"]


# --- Interval Class Variables Initialization ---
IntervalAlter.PERFECT = IntervalAlter(0)
IntervalAlter.MAJOR = IntervalAlter(1)
IntervalAlter.MINOR = IntervalAlter(-1)
IntervalAlter.AUGMENTED = IntervalAlter(2)
IntervalAlter.DIMINISHED = IntervalAlter(-2)

Interval.P1 = Interval.parse("P1")
Interval.d1 = Interval.parse("d1")
Interval.A1 = Interval.parse("A1")
Interval.A2 = Interval.parse("A2")
Interval.d4 = Interval.parse("d4")
Interval.A4 = Interval.parse("A4")
Interval.d5 = Interval.parse("d5")
Interval.P5 = Interval.parse("P5")
Interval.A5 = Interval.parse("A5")
Interval.M6 = Interval.parse("M6")
Interval.A6 = Interval.parse("A6")
Interval.P8 = Interval.parse("P8")


## ----- 半音単位・音価・音符・楽譜定義 (Logicが少ないのでそのまま維持推奨)


@dataclass(frozen=True, order=True)
class PitchNumber:
    """半音単位の音高数値。C4を0とする"""

    value: int

    def __add__(self, other: "IntervalNumber") -> "PitchNumber":
        return PitchNumber(self.value + other.value)

    def __sub__(self, other: "PitchNumber") -> "IntervalNumber":
        return IntervalNumber(self.value - other.value)


@dataclass(frozen=True, order=True)
class IntervalNumber:
    """半音単位の音程差。"""

    value: int

    def __add__(self, other: "IntervalNumber") -> "IntervalNumber":
        return IntervalNumber(self.value + other.value)

    def __sub__(self, other: "IntervalNumber") -> "IntervalNumber":
        return IntervalNumber(self.value - other.value)


@dataclass(frozen=True, order=True)
class Duration:
    """音価（長さ）。四部音符を1とする分数で管理。"""

    value: Fraction

    PHANTOM: ClassVar["Duration"]

    def __add__(self, other: "Duration") -> "Duration":
        return Duration(self.value + other.value)

    def __sub__(self, other: "Duration") -> "Duration":
        return Duration(self.value - other.value)

    def __mul__(self, value: int | Fraction) -> "Duration":
        return Duration(self.value * value)

    @classmethod
    def of(cls, numerator: int, denominator: int | None = None) -> "Duration":
        return cls(Fraction(numerator, denominator) if denominator else Fraction(numerator))

    @property
    def is_phantom(self) -> bool:
        """音価を持たない（便宜的な0の長さ）かどうか。"""
        return self.value == 0


Duration.PHANTOM = Duration(Fraction(0))


@dataclass(frozen=True, order=True)
class Offset:
    """小節や楽曲先頭などの基準点からの相対的な位置。四分音符を1とする。"""

    value: Fraction

    def __add__(self, other: "Offset") -> "Offset":
        return Offset(self.value + other.value)

    def __sub__(self, other: "Offset") -> "Offset":
        return Offset(self.value - other.value)

    def add_duration(self, duration: Duration) -> "Offset":
        return Offset(self.value + duration.value)

    @classmethod
    def of(cls, numerator: int, denominator: int | None = None) -> "Offset":
        return cls(Fraction(numerator, denominator) if denominator else Fraction(numerator))

    @classmethod
    def idx_1(cls, numerator: int, denominator: int | None = None) -> "Offset":
        """1拍目を1とする数え方からOffset(0始まり)を生成する。"""
        return cls(Fraction(numerator - 1, denominator) if denominator else Fraction(numerator - 1))


T_Value = TypeVar("T_Value", covariant=True)
T_Attr = TypeVar("T_Attr", covariant=True)
T_Id = TypeVar("T_Id", covariant=True)


@dataclass(frozen=True)
class Note[T_Value, T_Attr]:
    """
    音符。音高などの任意の主要素(value)、長さ(duration)、その他の付加情報(attribute)を持つ。
    """

    value: T_Value
    duration: Duration
    attribute: T_Attr

    def map_value[U_Value](self, func: Callable[[T_Value], U_Value]) -> "Note[U_Value, T_Attr]":
        return Note(func(self.value), self.duration, self.attribute)

    def map_duration(self, func: Callable[[Duration], Duration]) -> "Note[T_Value, T_Attr]":
        return Note(self.value, func(self.duration), self.attribute)

    def map_attribute[U_Attr](self, func: Callable[[T_Attr], U_Attr]) -> "Note[T_Value, U_Attr]":
        return Note(self.value, self.duration, func(self.attribute))


@dataclass(frozen=True)
class Melody[T_Value, T_Attr]:
    """
    旋律。時間の順序を持つ音符の列。
    """

    notes: tuple[Note[T_Value, T_Attr], ...]

    @classmethod
    def of(cls, *notes: Note[T_Value, T_Attr]) -> "Melody[T_Value, T_Attr]":
        return cls(notes)

    def map[U_Value](self, func: Callable[[T_Value], U_Value]) -> "Melody[U_Value, T_Attr]":
        return Melody.of(*[Note(func(note.value), note.duration, note.attribute) for note in self.notes])

    @cached_property
    def total_duration(self) -> Duration:
        return sum([n.duration for n in self.notes], Duration.PHANTOM)

    def offset_notes(self) -> dict[Offset, Note[T_Value, T_Attr]]:
        """開始Offsetをキーとする辞書を返す。PhantomDurationはスキップされないが時間は進まない。"""
        res, curr = {}, Offset.of(0)
        for n in self.notes:
            res[curr] = n
            curr = curr.add_duration(n.duration)
        return res

    def at(self, offset: Offset) -> tuple[Offset, Note[T_Value, T_Attr]]:
        """指定されたOffsetの時点にある音符と、その開始Offsetを返す。"""
        curr = Offset.of(0)
        for n in self.notes:
            if n.duration.is_phantom:
                continue  # Phantomな音符は無視される
            end = curr.add_duration(n.duration)
            if curr <= offset < end:
                return (curr, n)
            curr = end
        raise ValueError(f"offset {offset} not found")


@dataclass(frozen=True)
class Chord[T_Value, T_Attr]:
    """
    和音。同時に鳴る要素の集合。
    空でなく、含まれる全要素のDurationは等しい。
    """

    elements: frozenset[Note[T_Value, T_Attr]]

    @classmethod
    def of(cls, *elements: Note[T_Value, T_Attr]) -> "Chord[T_Value, T_Attr]":
        return cls(frozenset(elements))

    def map[U_Value](self, func: Callable[[T_Value], U_Value]) -> "Chord[U_Value, T_Attr]":
        return Chord.of(*[Note(func(note.value), note.duration, note.attribute) for note in self.elements])

    def __post_init__(self) -> None:
        if not self.elements:
            raise ValueError("Chord cannot be empty.")

        unique_durations = {note.duration for note in self.elements}
        if len(unique_durations) != 1:
            raise ValueError(f"All notes in a chord must have the same duration. Found: {unique_durations}")

    @property
    def duration(self) -> "Duration":
        """この和音の長さを返す"""
        return next(iter(self.elements)).duration


@dataclass(frozen=True)
class ChordWithBass[T_Value, T_Attr]:
    """
    バス音を指定した和音。バスは和音の構成音に含まれていないといけない。
    """

    chord: Chord[T_Value, T_Attr]
    bass: T_Value

    def __post_init__(self) -> None:
        assert self.bass in self.chord.elements


@dataclass(frozen=True)
class Slice[T_Value]:
    """
    分割可能な要素を表すコンテナ。
    分析の際の編集や転置において、音符が分割された際の状態（結合可能性）を保持する。
    """

    value: T_Value
    connects_left: bool = False
    connects_right: bool = False


@dataclass(frozen=True)
class Identified[T_Id, T_Value]:
    """
    ID付けされた要素。
    声部（PartId）などを区別するために利用する。
    """

    id: T_Id
    value: T_Value


@dataclass(frozen=True)
class Score[T_Id, T_Value, T_Attr]:
    """
    楽譜。

    抽象的に、旋律が和音のように重なっているということを表している。
    また、それを和音の連なりとしても見れるように、 Identified と Slice を用いて転置と復元が可能なように定義している。
    """

    chord: Chord[Identified[T_Id, Melody[Slice[T_Value], T_Attr]], T_Attr]

    def T(self) -> "Score_T[T_Id, T_Value, T_Attr]":
        """
        スコアを転置（変形）し、時間軸でスライスされた「和音の旋律」として返す。
        これにより、垂直方向（和声的）な操作が可能になる。
        """
        from my_project import model_score_ops

        vertical_notes_tuple = model_score_ops.transpose_score_to_vertical(self.chord.elements)
        return Score_T(Melody.of(*vertical_notes_tuple))


@dataclass(frozen=True)
class Score_T[T_Id, T_Value, T_Attr]:
    """
    楽譜を転置したもの。
    Score（旋律の重なり）を、時間軸でスライスされた「和音の連なり」として表現する形式。
    """

    melody: Melody[Chord[Identified[T_Id, Slice[T_Value]], T_Attr], T_Attr]

    def T(self) -> "Score[T_Id, T_Value, T_Attr]":
        """
        転置を元に戻し、垂直スライスの連なりを「旋律の重なり」であるScore形式に復元する。
        隣り合うスライスが結合可能（タイで繋がっている）であれば、一つの音符にマージする。
        """
        from my_project import model_score_ops

        score_elements_set = model_score_ops.transpose_vertical_to_score(self.melody.notes)
        return Score(Chord.of(*score_elements_set))


# ---


@dataclass(frozen=True)
class TimeSignature:
    """拍子記号。"""

    beats: int
    beat_type: Duration

    def duration(self) -> Fraction:
        """1小節の長さを返す。"""
        return self.beats * self.beat_type.value

    def name(self) -> str:
        """'4/4' のような文字列表現を返す。"""
        denom = 4 / self.beat_type.value
        return f"{self.beats}/{int(denom) if denom.denominator == 1 else denom}"


class PartId(Enum):
    SOPRANO = 1
    ALTO = 2
    TENOR = 3
    BASS = 4


# deprecated. Melody へ
@dataclass(frozen=True)
class Measure[T_Value, T_Attr]:
    """小節。音符のリストを持つ。"""

    notes: tuple[Note[T_Value, T_Attr], ...]

    @classmethod
    def of(cls, *notes: Note[T_Value, T_Attr]) -> "Measure[T_Value, T_Attr]":
        return cls(notes)

    def map_notes[U_Value, U_Attr](
        self, func: Callable[[Note[T_Value, T_Attr]], Note[U_Value, U_Attr]]
    ) -> "Measure[U_Value, U_Attr]":
        return Measure.of(*[func(note) for note in self.notes])

    @cached_property
    def total_duration(self) -> Duration:
        return sum([n.duration for n in self.notes], Duration.of(0))

    def offset_notes(self) -> dict[Offset, Note[T_Value, T_Attr]]:
        """小節先頭を0とした各音符のOffsetをキーとする辞書を返す。"""
        res, curr = {}, Offset.of(0)
        for n in self.notes:
            res[curr] = n
            curr = curr.add_duration(n.duration)
        return res

    def at(self, offset: Offset) -> tuple[Offset, Note[T_Value, T_Attr]]:
        """指定されたOffsetの時点にある音符と、その開始Offsetを返す。"""
        curr = Offset.of(0)
        for n in self.notes:
            end = curr.add_duration(n.duration)
            if curr <= offset < end:
                return (curr, n)
            curr = end
        raise ValueError(f"offset {offset} not found")


# deprecated. 使わなくなりそう。
@dataclass(frozen=True, order=True)
class MeasureNumber:
    value: int

    def __add__(self, other: "MeasureNumber") -> "MeasureNumber":
        return MeasureNumber(self.value + other.value)

    def __sub__(self, other: "MeasureNumber") -> "MeasureNumber":
        return MeasureNumber(self.value - other.value)


# deprecated FullScore の body を変更したら不要になる。
@dataclass(frozen=True)
class Part[T_Value, T_Attr]:
    """パート。特定の識別子(S/A/T/B)と小節のシーケンスを持つ。"""

    part_id: PartId
    measures: Sequence[Measure[T_Value, T_Attr]]


class HasScoreAttrs(Protocol):
    @property
    def is_tied_start(self) -> bool: ...


@dataclass(frozen=True)
class ScoreAttrs:
    is_tied_start: bool


@dataclass
class FullScore[A: HasScoreAttrs]:
    """
    楽譜全体を表すクラス。
    """

    key: Key
    time_signature: TimeSignature

    # # deprecated 既存コードを移行するまでは一旦これ
    parts: Sequence[Part[Pitch | None, A]]

    # Chord を利用した新しい定義はこれ。
    # body: Chord[Identified[PartId, Melody[Slice[T_Value], T_Attr]]]

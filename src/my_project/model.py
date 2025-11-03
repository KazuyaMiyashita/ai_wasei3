import re
from dataclasses import dataclass
from enum import Enum
from fractions import Fraction
from typing import ClassVar

## ----- 音名に対する定義


@dataclass(frozen=True, order=True)
class NoteName:
    value: int

    def __post_init__(self) -> None:
        if not -15 <= self.value <= 19:
            raise ValueError("NoteName must be between -15 and 19.")

    def __add__(self, other: "NoteName") -> "NoteName":
        return NoteName(self.value + other.value)

    def __sub__(self, other: "NoteName") -> "NoteName":
        return NoteName(self.value - other.value)

    def name(self) -> str:
        """
        この音名の英語表記の名称を返す。C, F#, Bb など。
        """
        step, alter = self.internal_pitch_notation()
        return f"{step}{'#' * alter if alter > 0 else 'b' * -alter}"

    @classmethod
    def parse(cls, name: str) -> "NoteName":
        """
        "C#", "Bb", "F##" といった音名表記をパースしてNoteNameオブジェクトを作成する。
        """
        pattern = r"^([A-G])([#b]*)$"
        match = re.fullmatch(pattern, name)
        if not match:
            raise ValueError(f"Invalid note name format: {name}")

        step_str, accidental_str = match.groups()
        alter = accidental_str.count("#") - accidental_str.count("b")

        return cls.from_internal_pitch_notation(step_str, alter)

    def internal_pitch_notation(self) -> tuple[str, int]:
        """
        C# などの国際式音名や、MusicXMLのpitch要素のための幹音と変化記号の2つの組を返す
        例: 6(F#) -> ("F", 1), -2(Bb) -> ("B", -1)
        """
        # F, C, G, D, A, E, B の順で、7で割った余りが一致するものを探す
        # これにより、変化記号が最も少なくなるスペリングを選ぶ
        for base_fifth in [-1, 0, 1, 2, 3, 4, 5]:
            if (self.value - base_fifth) % 7 == 0:
                alter = (self.value - base_fifth) // 7
                step = self._BASE_FIFTH_TO_STEP[base_fifth]
                return step, alter
        raise RuntimeError("unreachable")

    @classmethod
    def from_internal_pitch_notation(cls, step: str, alter: int) -> "NoteName":
        base_fifth = cls._STEP_TO_BASE_FIFTH[step]
        return NoteName(base_fifth + alter * 7)

    # --- private maps for name/parse ---

    _STEP_TO_BASE_FIFTH: ClassVar[dict[str, int]] = {"C": 0, "D": 2, "E": 4, "F": -1, "G": 1, "A": 3, "B": 5}
    _BASE_FIFTH_TO_STEP: ClassVar[dict[int, str]] = {v: k for k, v in _STEP_TO_BASE_FIFTH.items()}


@dataclass(frozen=True, order=True)
class Octave:
    """
    音高のオクターブ成分を整数で表す。
    """

    value: int

    def __add__(self, other: "Octave") -> "Octave":
        return Octave(self.value + other.value)

    def __sub__(self, other: "Octave") -> "Octave":
        return Octave(self.value - other.value)


@dataclass(frozen=True)
class Pitch:
    """
    音高は、C4の音に対し上方のオクターブ移動と完全五度移動がそれぞれ何回行われたかによって表現される。
    """

    octave: Octave
    note_name: NoteName

    def __add__(self, other: "Interval") -> "Pitch":
        return Pitch(self.octave + Octave(other.octave), self.note_name + NoteName(other.fifth))

    def __sub__(self, other: "Pitch") -> "Interval":
        return Interval(self.octave.value - other.octave.value, self.note_name.value - other.note_name.value)

    def name(self) -> str:
        """
        この音高の英語表記の名称を返す。C4, F#3, Bb5 など。
        """
        step, alter = self.note_name.internal_pitch_notation()
        base_octave = self._STEP_TO_BASE_OCTAVE[step]

        # self.octave.value = base_octave + alter * -4 + notation_octave - 4
        # を notation_octave について解く
        notation_octave = self.octave.value - base_octave + 4 * alter + 4
        return f"{self.note_name.name()}{notation_octave}"

    @classmethod
    def parse(cls, name: str) -> "Pitch":
        """
        F##4といった表記をパースしてPitchオブジェクトを作成する

        Pitch.parse(str(pitch)) == pitch が成り立つ。
        """
        pattern = r"^([A-G][#b]*)(\d+)$"
        match = re.fullmatch(pattern, name)
        if not match:
            raise ValueError(f"Invalid pitch name format: {name}")

        note_name_str, octave_str = match.groups()

        note_name = NoteName.parse(note_name_str)
        octave = int(octave_str)

        step, alter = note_name.internal_pitch_notation()

        base_octave = cls._STEP_TO_BASE_OCTAVE[step]

        pitch_octave_value = base_octave + alter * -4 + octave - 4

        return cls(Octave(pitch_octave_value), note_name)

    def internal_pitch_notation(self) -> tuple[str, int, int]:
        """
        C#4 などの国際式音名や、MusicXMLのpitch要素のための幹音と変化記号・オクターブの3つ組を返す
        """
        step, alter = self.note_name.internal_pitch_notation()
        base_octave = self._STEP_TO_BASE_OCTAVE[step]
        # self.octave.value = base_octave + alter * -4 + notation_octave - 4
        # を notation_octave について解く
        notation_octave = self.octave.value - base_octave + 4 * alter + 4
        return (step, alter, notation_octave)

    @classmethod
    def from_internal_pitch_notation(cls, step: str, alter: int, octave: int) -> "Pitch":
        note_name = NoteName.from_internal_pitch_notation(step, alter)
        base_octave = cls._STEP_TO_BASE_OCTAVE[step]
        pitch_octave_value = base_octave + alter * -4 + octave - 4
        return cls(Octave(pitch_octave_value), note_name)

    # --- private map for name/parse ---
    _STEP_TO_BASE_OCTAVE: ClassVar[dict[str, int]] = {"C": 0, "D": -1, "E": -2, "F": 1, "G": 0, "A": -1, "B": -2}


## ----- 調性に対する定義


class Mode(Enum):
    """
    旋法。長調と短調の区別を行う。
    """

    MAJOR = "Major"
    MINOR = "Minor"

    def offset(self) -> int:
        """
        旋法を表す値。これにより、平行調(例: ハ長調とイ短調)が同じ調号を持つことを簡潔に表現できる。
        """
        match self:
            case Mode.MAJOR:
                return 0
            case Mode.MINOR:
                return -3


@dataclass(frozen=True)
class Key:
    """
    調。主音の音名と旋法の組み。
    """

    tonic: NoteName
    mode: Mode


## ----- 調性と音高から導けるもの


@dataclass(frozen=True, order=True)
class DegreeStep:
    """
    音度距離。調の音階上の位置を示す。
    0 ~ 6 で扱う
    """

    value: int

    def __post_init__(self) -> None:
        if not 0 <= self.value <= 6:
            raise ValueError("Step must be between 0 and 6.")

    def __add__(self, other: "DegreeStep") -> "DegreeStep":
        return DegreeStep((self.value + other.value) % 7)

    def __sub__(self, other: "DegreeStep") -> "DegreeStep":
        return DegreeStep((self.value - other.value) % 7)

    @classmethod
    def idx_1(cls, step: int) -> "DegreeStep":
        """
        step (ただし 1 ~ 7) と alter から Degree を作成
        """
        s = step - 1
        return cls(s)


@dataclass(frozen=True, order=True)
class DegreeAlter:
    """
    変化度。音階の固有の音度に対し増一度の変化が何回行われているか。
    """

    value: int

    def __post_init__(self) -> None:
        if not -1 <= self.value <= 2:
            raise ValueError("Step must be between -1 and 2.")


@dataclass(frozen=True, order=True)
class Degree:
    """
    音度。音度距離と変化度の組み。
    """

    step: DegreeStep
    alter: DegreeAlter

    @classmethod
    def from_note_name_key(cls, note_name: NoteName, key: Key) -> "Degree":
        # 調の主音から見た音高の音程(定位相対音名)を求める
        r = note_name.value - key.tonic.value

        # 1. 変化度 a の計算
        #
        # 定位相対音名 Rm: { -1 + m <= r_0 <= 5 + m } に対し、
        # r_0 = r - 7a を代入して a について解く。
        m = key.mode.offset()
        alter = DegreeAlter(round((r - m - 2) / 7))

        # 2. 基準となる定位相対音名 r_0 の特定
        # 上記で求めた a を使って r から逆算する
        r_0 = r - (7 * alter.value)

        # 3. 音度距離 d の計算
        step = DegreeStep((4 * r_0) % 7)
        return Degree(step, alter)

    def note_name(self, key: Key) -> "NoteName":
        """
        この音度に調を与えて音名を得る
        """

        m = key.mode.offset()

        # 1. r_0 を求める
        # 4r_0 ≡ d (mod 7) を解く。 4*2=8≡1 より r_0 ≡ 2d (mod 7)
        r_0_candidate = (2 * self.step.value) % 7

        # 2. r_0 を定位相対音名の範囲に収める
        # -1+m <= r_0 <= 5+m
        # 候補は r_0_candidate, r_0_candidate - 7, r_0_candidate + 7 のいずれか
        if -1 + m <= r_0_candidate <= 5 + m:
            r_0 = r_0_candidate
        elif -1 + m <= r_0_candidate - 7 <= 5 + m:
            r_0 = r_0_candidate - 7
        elif -1 + m <= r_0_candidate + 7 <= 5 + m:
            r_0 = r_0_candidate + 7
        else:
            # このケースは発生しないはず
            raise ValueError(f"Cannot find r_0 for step {self.step} in mode {key.mode}")

        # 3. 相対音名 r を計算する
        r = r_0 + 7 * self.alter.value

        # 調の主音の音名と相対音名を足す
        return NoteName(key.tonic.value + r)

    @classmethod
    def idx_1(cls, step: int, alter: int) -> "Degree":
        """
        step (ただし 1 ~ 7) と alter から Degree を作成
        """
        return cls(DegreeStep.idx_1(step), DegreeAlter(alter))


## ----- 音程に関する定義


@dataclass(frozen=True)
class Interval:
    """
    音程。2つのPitchの差。上方・下方やオクターブ、長短の区別がされる。

    基準となる音に対し、上方のオクターブ移動と完全五度移動がそれぞれ何回行われたかによって表現される。
    """

    octave: int
    fifth: int

    @classmethod
    def of(cls, base: Pitch, target: Pitch) -> "Interval":
        return cls(
            octave=target.octave.value - base.octave.value,
            fifth=target.note_name.value - base.note_name.value,
        )

    def step(self) -> "IntervalStep":
        return IntervalStep(4 * self.fifth + 7 * self.octave)

    def alter(self) -> "IntervalAlter":
        abs_fifth = abs(self.fifth)
        step_sgn = -1 if self.step().value < 0 else 1
        fifth_sgn = -1 if self.fifth < 0 else 1
        sgn = step_sgn * fifth_sgn
        if abs_fifth <= 1:  # 完全は3つ
            return IntervalAlter(0)
        elif abs_fifth <= 5:  # 長短はそれぞれ4つ
            return IntervalAlter(sgn * 1)
        else:  # それ以降の増, 減, 重増, 重減, ... はそれぞれ7つ
            return IntervalAlter(sgn * (2 + ((abs_fifth - 6) // 7)))

    @classmethod
    def from_step_alter(cls, step: "IntervalStep", alter: "IntervalAlter") -> "Interval":
        """
        IntervalStepとIntervalAlterからIntervalを逆算して生成する
        """
        s = step.value
        a = alter.value

        # 1. f_class (f % 7) は s から決まる (f ≡ 2s (mod 7))
        f_class = (2 * s) % 7
        f = 0  # fifth の値

        # 共通で使う step の符号 (0の場合は上方)
        step_sgn = -1 if s < 0 else 1

        # f % 7 が f_class となる候補
        # (f_class は [0, 6])
        # f_base (シャープ系: 6..12)
        f_base_sharp = (f_class - 6) % 7 + 6
        # f_base (フラット系: -12..-6)
        f_base_flat = (f_class - 2) % 7 - 12

        if a == 0:  # Perfect (P)
            # P (a=0) は f = -1, 0, 1
            f_map = {0: 0, 1: 1, 6: -1}
            if f_class not in f_map:
                raise ValueError(f"IntervalStep {s} cannot be Perfect (alter=0)")
            f = f_map[f_class]

        elif a == 1:  # Major (M)
            # M (a=1) は s と f が同符号
            if f_class not in [2, 3, 4, 5]:
                raise ValueError(f"IntervalStep {s} cannot be Major (alter=1)")

            if step_sgn == 1:  # 上方 (f > 0)
                f = f_class  # (f = 2, 3, 4, 5)
            else:  # 下方 (f < 0)
                f = f_class - 7  # (f = -5, -4, -3, -2)

        elif a == -1:  # Minor (m)
            # m (a=-1) は s と f が異符号
            if f_class not in [2, 3, 4, 5]:
                raise ValueError(f"IntervalStep {s} cannot be Minor (alter=-1)")

            if step_sgn == 1:  # 上方 (f < 0)
                f = f_class - 7  # (f = -5, -4, -3, -2)
            else:  # 下方 (f > 0)
                f = f_class  # (f = 2, 3, 4, 5)

        elif a >= 2:  # Augmented (A)
            # A (a >= 2) は M (a=1) と同じ符号関係 (s, f が同符号)
            k = a - 2
            if step_sgn == 1:  # 上方 (f > 0)
                f = f_base_sharp + 7 * k
            else:  # 下方 (f < 0)
                f = f_base_flat - 7 * k

        elif a <= -2:  # Diminished (d)
            # d (a <= -2) は m (a=-1) と同じ符号関係 (s, f が異符号)
            k = -a - 2
            if step_sgn == 1:  # 上方 (f < 0)
                f = f_base_flat - 7 * k
            else:  # 下方 (f > 0)
                f = f_base_sharp + 7 * k

        # 2. f が決まったので、 o を計算する
        residual = s - 4 * f
        if residual % 7 != 0:
            # 到達しないはず
            raise RuntimeError(
                f"Internal logic error: (s - 4f) not divisible by 7. s={s}, a={a}, f={f}, rem={residual % 7}"
            )

        o = residual // 7
        return cls(octave=o, fifth=f)

    def name(self) -> str:
        """
        この音程を以下のような方式で文字列に変換する
        - 完全1度: P1
        - 長2度上: M2
        - 短3度上: m3
        - 増4度上: A4 (Augumented)
        - 減5度上: d5 (Diminished)
        - 完全8度上: P8
        - 完全8度下: -P8
        - 長2度下: -M2
        - 重増4度上: AA4
        - 重減5度下: -dd5

        増1度下は存在せず、減1度上(d1)として扱うことに注意。
        """
        step = self.step()
        sgn = "" if step.value >= 0 else "-"
        num = f"{abs(step.value) + 1}"

        alter = self.alter()
        match alter:
            case IntervalAlter(0):
                alph = "P"
            case IntervalAlter(1):
                alph = "M"
            case IntervalAlter(-1):
                alph = "m"
            case _ if alter.value >= 2:
                alph = "A" * (alter.value - 1)
            case _:
                alph = "d" * (-alter.value - 1)

        return f"{sgn}{alph}{num}"

    @classmethod
    def parse(cls, name: str) -> "Interval":
        """
        name()メソッドの逆変換。"P1", "-m3", "AA4" 等の文字列をパースしてIntervalを生成する
        """
        pattern = re.compile(r"^([-]?)([PMm]|A+|d+)(\d+)$")
        match = pattern.match(name)

        if not match:
            raise ValueError(f"Invalid interval name format: '{name}'")

        sgn_str, qual_str, num_str = match.groups()

        num = int(num_str)
        if num < 1:
            raise ValueError(f"Interval degree must be 1 or greater, got {num}")

        step_val_base = num - 1

        sgn = -1 if sgn_str == "-" else 1
        step_value = step_val_base * sgn
        step = IntervalStep(step_value)

        alter_val = 0
        if qual_str == "P":
            alter_val = 0
        elif qual_str == "M":
            alter_val = 1
        elif qual_str == "m":
            alter_val = -1
        elif qual_str.startswith("A"):
            # A (k=1) -> alter=2
            # AA (k=2) -> alter=3
            k = len(qual_str)
            alter_val = k + 1
        elif qual_str.startswith("d"):
            # d (k=1) -> alter=-2
            # dd (k=2) -> alter=-3
            k = len(qual_str)
            alter_val = -(k + 1)

        alter = IntervalAlter(alter_val)

        return cls.from_step_alter(step, alter)

    def normalize(self) -> "Interval":
        """
        音程を正規化する。

        複音程は単音程にする。長10度は長3度, 完全8度は完全1度となる。
        下方の音程の場合、長3度下は長3度上となるように変換される(転回ではないことに注意)
        """
        step = self.step()
        alter = self.alter()  # 修正後の alter が呼ばれる

        if step.value == 0:
            pass  # P1
        elif step.value > 0:
            # 上方複音程
            step = IntervalStep(step.value % 7)
        else:
            # 下方音程
            step = IntervalStep((-1 * step.value) % 7)
            # docstring の仕様 (長3度下 -> 長3度上) に従い、alter は反転させない
            # alter = IntervalAlter(-1 * alter.value) # この行を削除

        return Interval.from_step_alter(step, alter)


@dataclass(frozen=True, order=True)
class IntervalStep:
    """
    音程のうち、派生音を無視して五線譜上で何度移動されたを表すもの。一般に言われる「3度」など。上方・下方やオクターブの区別がされる。
    ただし、ユニゾンを0度として数える。

    増1度下は存在せず、減1度上(d1)として扱うことに注意。
    """

    value: int

    def to_inverval(self, alter: "IntervalAlter") -> Interval:
        """
        IntervalAlter と組み合わせて、元の Interval を復元します。
        """
        return Interval.from_step_alter(self, alter)

    @classmethod
    def idx_1(cls, value: int) -> "IntervalStep":
        """
        i-indexed の音程 (ユニゾンは1度など) で IntervalStep を作成する
        """
        if value == 0:
            raise ValueError("idx_1 に 0 は指定できません")
        elif value >= 1:
            return cls(value - 1)
        else:
            return cls(value + 1)

    def abs(self) -> "IntervalStep":
        return IntervalStep(abs(self.value))

    @classmethod
    def octave(cls) -> "IntervalStep":
        return cls(7)


@dataclass(frozen=True, order=True)
class IntervalAlter:
    """
    音程の長短・完全などを表す。
    0: 完全, 1: 長, -1: 短, 2: 増, -2: 減, 3: 重増, -3: 重減, ...

    増1度下は存在せず、減1度上(d1)として扱うことに注意。
    """

    value: int

    def to_inverval(self, step: "IntervalStep") -> Interval:
        """
        IntervalStep と組み合わせて、元の Interval を復元します。
        """
        return Interval.from_step_alter(step, self)

    def abs(self) -> "IntervalAlter":
        return IntervalAlter(abs(self.value))

    PERFECT: ClassVar["IntervalAlter"]
    MAJOR: ClassVar["IntervalAlter"]
    MINOR: ClassVar["IntervalAlter"]
    AUGMENTED: ClassVar["IntervalAlter"]
    DIMINISHED: ClassVar["IntervalAlter"]


IntervalAlter.PERFECT = IntervalAlter(0)
IntervalAlter.MAJOR = IntervalAlter(1)
IntervalAlter.MINOR = IntervalAlter(-1)
IntervalAlter.AUGMENTED = IntervalAlter(2)
IntervalAlter.DIMINISHED = IntervalAlter(-2)

## ----- 音価の定義


@dataclass(frozen=True, order=True)
class Duration:
    """
    音価を、四分音符を1とする有理数で表現します。
    例: 四分音符 = 1, 八分音符 = 1/2, 全音符 = 4
    """

    value: Fraction

    def __add__(self, other: "Duration") -> "Duration":
        return Duration(self.value + other.value)

    def __sub__(self, other: "Duration") -> "Duration":
        return Duration(self.value - other.value)

    @classmethod
    def of(
        cls,
        numerator: int,
        denominator: int | None = None,
    ) -> "Duration":
        if denominator is not None:
            return cls(Fraction(numerator, denominator))
        else:
            return cls(Fraction(numerator))


## ----- 音符の定義


@dataclass(frozen=True)
class Note:
    """
    単一の音符、または休符を表現します。
    タイの情報を保持します。
    """

    pitch: Pitch | None  # Noneの場合は休符
    duration: Duration
    is_tied_start: bool = False


## ----- 楽譜の定義


@dataclass(frozen=True, order=True)
class Offset:
    """
    小節における音符の位置。0から始まる。Durationと同様に四分音符を1と数える
    """

    value: Fraction

    def __add__(self, other: "Offset") -> "Offset":
        return Offset(self.value + other.value)

    def __sub__(self, other: "Offset") -> "Offset":
        return Offset(self.value - other.value)


@dataclass(frozen=True)
class Measure:
    """
    1小節分のデータを表現します。
    """

    notes: list[Note]

    def total_duration(self) -> Duration:
        durations = [note.duration for note in self.notes]
        return sum(durations, Duration(Fraction(0)))

    def at(self, offset: Offset) -> Pitch | None:
        """
        小節内のある音符の位置における音符または休符を返す
        範囲外の位置を指定された場合は例外となる
        """
        current_offset = Offset(Fraction(0))
        for note in self.notes:
            note_end_offset = Offset(current_offset.value + note.duration.value)
            if current_offset <= offset < note_end_offset:
                return note.pitch
        raise ValueError(f"offset {offset} not found in this measure: {self.notes}")


class PartId(Enum):
    SOPRANO = 1
    ALTO = 2
    TENOR = 3
    BASS = 4


@dataclass(frozen=True)
class Part:
    """
    楽曲のうちの一つの声部の小節の情報を表す
    """

    part_id: PartId
    measures: list[Measure]


@dataclass(frozen=True)
class TimeSignature:
    """
    拍子。
    - 拍子記号の分母にあたる beat_type と 分子にあたる beats を持つ。
    - beat_type は Duration と同様に **四分音符を1** とする有理数で表現する。
    - 例えば 3/4 拍子は beats=3, beat_type=1 、 6/8 拍子は beats=6, beat_type=1/2 となる。
    """

    beats: int
    beat_type: Fraction

    def duration(self) -> Fraction:
        return self.beats * self.beat_type

    def name(self) -> str:
        denominator = 4 / self.beat_type
        if denominator.denominator != 1:
            # 分数が残る場合は、そのまま表示(例: 2.666... のようなケースを避ける)
            return f"{self.beats}/{denominator}"
        return f"{self.beats}/{int(denominator)}"


@dataclass
class Score:
    """
    楽曲全体を表す
    """

    key: Key
    time_signature: TimeSignature
    parts: list[Part]

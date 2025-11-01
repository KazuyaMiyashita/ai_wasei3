import re
from dataclasses import dataclass
from enum import Enum
from typing import ClassVar

## ----- 音名に対する定義


@dataclass(frozen=True, order=True)
class NoteName:
    value: int

    def __post_init__(self) -> None:
        if not -15 <= self.value <= 19:
            raise ValueError("NoteName must be between -15 and 19.")

    def name(self) -> str:
        """
        この音名の英語表記の名称を返す。C, F#, Bb など。
        """
        step, alter = self.get_spelling()
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

        base_fifth = cls._STEP_TO_BASE_FIFTH[step_str]
        alter = accidental_str.count("#") - accidental_str.count("b")

        return NoteName(base_fifth + alter * 7)

    # --- other utility methods below --- #

    def get_spelling(self) -> tuple[str, int]:
        """
        音名の値から、一般的な表記(幹音と変化記号)を計算する。
        例: 6 -> ("F", 1), -2 -> ("B", -1)
        """
        # F, C, G, D, A, E, B の順で、7で割った余りが一致するものを探す
        # これにより、変化記号が最も少なくなるスペリングを選ぶ
        for base_fifth in [-1, 0, 1, 2, 3, 4, 5]:
            if (self.value - base_fifth) % 7 == 0:
                alter = (self.value - base_fifth) // 7
                step = self._BASE_FIFTH_TO_STEP[base_fifth]
                return step, alter
        raise ValueError(f"Invalid NoteName value: {self.value}")  # Should not happen

    # --- private maps for name/parse ---

    _STEP_TO_BASE_FIFTH: ClassVar[dict[str, int]] = {
        "C": 0,
        "D": 2,
        "E": 4,
        "F": -1,
        "G": 1,
        "A": 3,
        "B": 5,
    }
    _BASE_FIFTH_TO_STEP: ClassVar[dict[int, str]] = {v: k for k, v in _STEP_TO_BASE_FIFTH.items()}


@dataclass(frozen=True, order=True)
class Octave:
    """
    音高のオクターブ成分を整数で表す。
    O := Z
    """

    value: int


@dataclass(frozen=True)
class Pitch:
    """
    音高は、オクターブと音名の直積集合の元として定義される。
    P := O x N
    """

    octave: Octave
    note_name: NoteName

    def name(self) -> str:
        """
        この音高の英語表記の名称を返す。C4, F#3, Bb5 など。
        """
        step, alter = self.note_name.get_spelling()
        base_octave = self._STEP_TO_BASE_OCTAVE[step]

        # self.octave.value = base_octave + alter * -4 + notation_octave - 4
        # を notation_octave について解く
        notation_octave = self.octave.value - base_octave + 4 * alter + 4
        return f"{self.note_name.name()!s}{notation_octave}"

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

        step, alter = note_name.get_spelling()

        base_octave = cls._STEP_TO_BASE_OCTAVE[step]

        pitch_octave_value = base_octave + alter * -4 + octave - 4

        return cls(Octave(pitch_octave_value), note_name)

    # --- private map for name/parse ---
    _STEP_TO_BASE_OCTAVE: ClassVar[dict[str, int]] = {
        "C": 0,
        "D": -1,
        "E": -2,
        "F": 1,
        "G": 0,
        "A": -1,
        "B": -2,
    }


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
    調。主音の音名と旋法の直積集合の元。
    K := N x M
    """

    tonic: NoteName
    mode: Mode


## ----- 調性と音高から導けるもの


@dataclass(frozen=True, order=True)
class Step:
    """
    音度距離。調の音階上の位置を示す。
    0 ~ 6 で扱う
    """

    value: int

    def __post_init__(self) -> None:
        if not 0 <= self.value <= 6:
            raise ValueError("Step must be between 0 and 6.")

    @classmethod
    def of(cls, step: int) -> "Step":
        """
        step (ただし 1 ~ 7) と alter から Degree を作成
        """
        s = step - 1
        return cls(s)


@dataclass(frozen=True, order=True)
class Alter:
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
    音度。音度距離と変化度のペア。
    """

    step: Step
    alter: Alter

    @classmethod
    def from_pitch_key(cls, note_name: NoteName, key: Key) -> "Degree":
        # 調の主音から見た音高の音程(定位相対音名)を求める
        r = note_name.value - key.tonic.value

        # 1. 変化度 a の計算
        #
        # 定位相対音名 Rm: { -1 + m <= r_0 <= 5 + m } に対し、
        # r_0 = r - 7a を代入して a について解く。
        m = key.mode.offset()
        alter = Alter(round((r - m - 2) / 7))

        # 2. 基準となる定位相対音名 r_0 の特定
        # 上記で求めた a を使って r から逆算する
        r_0 = r - (7 * alter.value)

        # 3. 音度距離 d の計算
        step = Step((4 * r_0) % 7)
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
    def of(cls, step: int, alter: int) -> "Degree":
        """
        step (ただし 1 ~ 7) と alter から Degree を作成
        """
        return cls(Step.of(step), Alter(alter))

import re
from dataclasses import dataclass
from enum import Enum, auto
from fractions import Fraction

from my_project.model import (
    Duration,
    Measure,
    Pitch,
)


class Species(Enum):
    """
    課題の類を表す
    """

    FIRST_SPECIES = 1
    """第一類、一音符対一音符"""

    SECOND_SPECIES = 2
    """第二類、二音符対一音符"""

    THIRD_SPECIES = 3
    """第三類、四音符対一音符"""

    FOURTH_SPECIES = 4
    """第四類、移勢"""

    FIFTH_SPECIES = 5
    """第五類、華麗"""


class ToneType(Enum):
    """
    探索した音に対し、その音が和声音か非和声音かを記録しておく必要がある。そのための音の種別
    """

    # 和声音。冒頭の休符も便宜上和声音として扱う。
    HARMONIC_TONE = auto()
    # 経過音
    PASSING_TONE = auto()
    # 刺繍音
    NEIGHBOR_TONE = auto()
    # 掛留音
    SUSPENDED_TONE = auto()
    # 掛留音が解決する前に進行する和声構成音や、掛留の先取解決で用いる音
    SUSPENDED_RESOLVING_HARMONIC_TONE = auto()


@dataclass(frozen=True)
class NoteAnnotation:
    is_tied_start: bool
    tone_type: ToneType


AnnotatedMeasure = Measure[Pitch | None, NoteAnnotation]


@dataclass(frozen=True)
class MeasureRythmn:
    """
    一小節のリズムを表す。これらの情報で表されるもののうち実際に利用できるものは MeasureRythmnPattern で定義される。
    """

    is_previous_tied: bool
    is_next_tied: bool
    durations: list[Duration]
    init_rest_duration: Duration

    def __post_init__(self) -> None:
        assert sum(self.durations, Duration.of(0)) + self.init_rest_duration == Duration.of(4), (
            f"durations: {self.durations}, init_rest_duration: {self.init_rest_duration}"
        )
        assert 1 <= len(self.durations) <= 4

    def num_durations(self) -> int:
        return len(self.durations)


class MeasureRythmnPattern(Enum):
    """
    利用できるリズムの一覧
    """

    R_1 = "1"
    R_22 = "22"
    R_t22 = "t22"
    R_22t = "22t"
    R_t22t = "t22t"
    R_4444 = "4444"
    R_244 = "244"
    R_442 = "442"
    R_t4444 = "t4444"
    R_t244 = "t244"
    R_t442 = "t442"
    R_4444t = "4444t"
    R_244t = "244t"
    R_2488 = "2488"
    R_4882 = "4882"
    R_t2488 = "t2488"
    R_t4882 = "t4882"
    R_4882t = "4882t"
    R_2d4 = "2d4"
    R_2d88 = "2d88"
    R_rr2 = "rr2"
    R_rr2t = "rr2t"
    R_r444 = "r444"
    R_r42 = "r42"

    def measure_rythmn(self) -> MeasureRythmn:
        pattern = r"^(r*|t)?(\d+(?:d\d*)*)(t)?$"
        match = re.fullmatch(pattern, self.value)
        if not match:
            raise ValueError(f"cannot parse pattern: {self.value}")
        init, middle, last = match.groups()
        init_rest_duration = Duration.of(1) * init.count("r")
        is_previous_tied = init == "t"
        is_next_tied = last == "t"

        note_patterns = re.findall(r"\dd?", middle)
        durations: list[Duration] = []
        for note_pattern in note_patterns:
            note_match = re.fullmatch(r"(\d)(d)?", note_pattern)
            if not note_match:
                raise ValueError(f"cannot parse note pattern: {note_pattern}")
            duration_str, dotted_str = note_match.groups()
            duration = Duration.of(4, int(duration_str))
            if dotted_str:
                duration *= Fraction(3, 2)
            durations.append(duration)
        return MeasureRythmn(
            is_previous_tied=is_previous_tied,
            is_next_tied=is_next_tied,
            durations=durations,
            init_rest_duration=init_rest_duration,
        )


class MeasurePosition(Enum):
    """
    課題全体の小節の位置を表す
    """

    FIRST = auto()
    "冒頭小節"

    MIDDLE = auto()
    "途中の小節"

    PENULTIMATE = auto()
    "最終小節の1小節前"

    LAST = auto()
    "最終小節"

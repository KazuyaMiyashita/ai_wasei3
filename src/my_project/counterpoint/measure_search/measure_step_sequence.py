import re
from dataclasses import dataclass
from functools import cached_property

from my_project.counterpoint.model import (
    ToneType,
)
from my_project.model import Duration, IntervalStep, Melody, Note, Pitch

AnnotatedIntervalStep = Note[IntervalStep, ToneType]
"""
音価1のNoteをIntervalStepとToneTypeの組として利用する
"""


@dataclass(frozen=True)
class AbstractMeasureStepSequence[T]:
    measure: Melody[Note[T, ToneType]]
    next_measure_step: T

    def num_notes_in_measure(self) -> int:
        """
        小節内で利用した音数
        """
        return len(self.measure.notes)

    def is_tied_to_next_measure_required(self) -> bool:
        """
        小節の最後の音と次の小節の音をタイで繋げる必要があるか
        """
        return self.measure.notes[-1].value == self.next_measure_step


@dataclass(frozen=True)
class MeasureStepSequence(AbstractMeasureStepSequence[IntervalStep]):
    """
    1小節およびその次の音で利用する候補となる音列を、一般化された形で表現する。

    - 音列の各要素は、上下方向の移動の IntervalStep と、その音の和声音・非和声音の種別 ToneType を持つ
    - 音列は、和声音の場合 IntervalStep(0)、 掛留音の場合は IntervalStep(1) または IntervalStep(-1) から始まる
    - リズムの情報は持たない。便宜上 Duration.of(1) を指定した Note として扱う
    - 小節内の音数は 1 ~ 4 の範囲である
    - 次の小節で進行可能な音は、小節の最後の音と同じ音となることがある。その場合はタイで繋げて利用する必要がある
    - 小節内の音列で利用した和声音 (used_harmonic_steps) には必ず IntervalStep(0) が含まれる。

    TODO: 小節内の和音の切り替えを考慮しなければならないが、現在の小節単位の生成のモデリングでは実現が難しい。
    """

    def __post_init__(self) -> None:
        assert 1 <= self.num_notes_in_measure() <= 4
        first_note = self.measure.notes[0]
        assert first_note.value in {IntervalStep(-1), IntervalStep(0), IntervalStep(1)}
        assert first_note.attribute in {ToneType.HARMONIC_TONE, ToneType.SUSPENDED_TONE}
        assert (first_note.attribute == ToneType.HARMONIC_TONE) == (first_note.value == IntervalStep(0))
        assert (first_note.attribute == ToneType.SUSPENDED_TONE) == (
            first_note.value in {IntervalStep(-1), IntervalStep(1)}
        )
        assert IntervalStep(0) in self.used_harmonic_steps

    def first_note_interval_step_of_measure(self) -> IntervalStep:
        return self.measure.notes[0].value

    def first_note_interval_step(self) -> IntervalStep:
        return self.measure.notes[-1].value

    @cached_property
    def used_harmonic_steps(self) -> frozenset[IntervalStep]:
        """
        小節内の音列で和声音として利用されている IntervalStep の集合を返す。

        - IntervalStep はユニゾン~7度までの範囲に正規化(inversion_normalized)される。
        """
        return frozenset(
            map(
                lambda step: step.value.inversion_normalized(),
                filter(
                    lambda step: step.attribute == ToneType.HARMONIC_TONE,
                    self.measure.notes,
                ),
            )
        )

    @cached_property
    def min_step(self) -> IntervalStep:
        return min([*[note.value for note in self.measure.notes], self.next_measure_step])

    @cached_property
    def max_step(self) -> IntervalStep:
        return max([*[note.value for note in self.measure.notes], self.next_measure_step])

    # ---

    @classmethod
    def parse(cls, s: str) -> "MeasureStepSequence":
        """
        "-1r,0,10|1" といった文字列をパースする

        ここでは IntervalStep の 0-indexed 表現を用いていることに注意。

        数字の後には H, p, br, r, のいずれかの文字が入ることがある。省略されていたらHとみなす。
        """
        tone_map = {
            "H": ToneType.HARMONIC_TONE,
            "p": ToneType.PASSING_TONE,
            "br": ToneType.NEIGHBOR_TONE,
            "r": ToneType.SUSPENDED_TONE,
            "srh": ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE,
        }

        step_part = r"-?\d+(?:H|p|br|r|srh)?"
        pattern = rf"^({step_part}(?:,{step_part})*)\|(-?\d+)$"

        match = re.fullmatch(pattern, s)
        if not match:
            raise ValueError(f"cannot parse result: {s}")
        steps_str, next_measure_step_str = match.groups()
        step_pattern = re.compile(r"^(-?\d+)(H|p|br|r|srh)?$")
        parsed_steps: list[AnnotatedIntervalStep] = []
        for step_s in steps_str.split(","):
            step_match = step_pattern.fullmatch(step_s)
            if not step_match:
                raise ValueError(f"Internal error parsing step: {step_s}")
            number_str, suffix_str = step_match.groups()  # e.g., ("-2", None) or ("1", "p")
            interval_step = IntervalStep(int(number_str))
            tone_type = tone_map.get(suffix_str, ToneType.HARMONIC_TONE)
            parsed_steps.append(Note(interval_step, Duration.of(1), tone_type))

        return cls(
            measure=Melody.of(*parsed_steps),
            next_measure_step=IntervalStep(int(next_measure_step_str)),
        )

    def name(self) -> str:
        """
        parseの逆。Hは省略される
        """
        tone_map = {
            ToneType.HARMONIC_TONE: "",
            ToneType.PASSING_TONE: "p",
            ToneType.NEIGHBOR_TONE: "br",
            ToneType.SUSPENDED_TONE: "r",
            ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE: "srh",
        }

        steps_str = ",".join([f"{step.value.value}{tone_map[step.attribute]}" for step in self.measure.notes])
        next_measure_step_str = str(self.next_measure_step.value)
        return f"{steps_str}|{next_measure_step_str}"


PitchMeasureStepSequence = AbstractMeasureStepSequence[Pitch]

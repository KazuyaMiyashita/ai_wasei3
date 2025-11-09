from my_project.counterpoint.local_measure_context import LocalMeasureContext
from my_project.counterpoint.model import (
    KEY,
    REALIZE_PART_ID,
)
from my_project.model import (
    Interval,
    IntervalStep,
    Pitch,
)
from my_project.util import part_range, scale_pitches

AVAILABLE_PITCHES_LIST: list[Pitch] = scale_pitches(KEY, part_range(REALIZE_PART_ID))
AVAILABLE_PITCHES_SET: set[Pitch] = set(AVAILABLE_PITCHES_LIST)


def available_harmonic_pitches_with_chord(local_ctx: LocalMeasureContext) -> list[tuple[Pitch, bool | None]]:
    """
    課題の冒頭の音または最終小節以外で、協和音として利用できる音と、利用したことにより確定した和音を返す。
    確定した和音は is_first_inversion_chord と同様に bool | None で返す。
    和音が未設定の場合はCFの上方の1,3,5,6度とその複音程で、2オクターブの範囲、声域内。
    """

    cf = local_ctx.current_cf

    step_and_next_chord_dict: dict[IntervalStep, bool | None] = {}
    if local_ctx.is_root_chord is None:
        step_and_next_chord_dict = {
            IntervalStep.idx_1(1): None,
            IntervalStep.idx_1(3): None,
            IntervalStep.idx_1(5): True,
            IntervalStep.idx_1(6): False,
        }
    elif local_ctx.is_root_chord:
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
        for pitch in AVAILABLE_PITCHES_LIST  # 声域内の調の音
        if cf.num() <= pitch.num() and Interval.of(cf, pitch).step() <= IntervalStep.idx_1(15)  # 2オクターブ未満
    ]

    result: list[tuple[Pitch, bool | None]] = []
    for pitch in all_available_pitches:
        step = Interval.of(cf, pitch).normalize().step()
        if step in step_and_next_chord_dict.keys():
            result.append((pitch, step_and_next_chord_dict[step]))
    return result


# def filter_available_pitches(local_ctx: LocalMeasureContext, pitches: list[Pitch]) -> list[Pitch]:
#     return [pitch for pitch in pitches if pitch in AVAILABLE_PITCHES_SET]


def start_available_pitches(local_ctx: LocalMeasureContext, cf: Pitch) -> list[Pitch]:
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
    return [pitch for pitch in [cf + interval for interval in intervals] if pitch in AVAILABLE_PITCHES_SET]


def end_available_pitches(local_ctx: LocalMeasureContext, cf: Pitch) -> list[Pitch]:
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
    return [pitch for pitch in [cf + interval for interval in intervals] if pitch in AVAILABLE_PITCHES_SET]


def available_pitches(local_ctx: LocalMeasureContext, cf: Pitch) -> list[Pitch]:
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


VALID_MELODIC_INTERVAL_LIST: list[Interval] = [
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
VALID_MELODIC_INTERVAL_SET: set[Interval] = set(VALID_MELODIC_INTERVAL_LIST)


def is_valid_melodic_interval(local_ctx: LocalMeasureContext, interval: Interval) -> bool:
    """
    ある音程が旋律的音程として認められるかどうかを返す。
    同音の連続を行わないようにするため、ユニゾンはFalseとしている。
    """
    return interval.abs() in VALID_MELODIC_INTERVAL_SET

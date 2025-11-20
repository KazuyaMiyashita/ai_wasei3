from my_project.counterpoint.model import MeasurePosition, MeasureRythmnPattern, Species
from my_project.model import Interval


def get_measure_rythmn_patterns(species: Species, measure_position: MeasurePosition) -> list[MeasureRythmnPattern]:
    """
    与えられた種と小節の位置に基づいて、利用可能なリズムパターンのリストを返す
    """

    match species:
        case Species.FIRST_SPECIES:
            return [MeasureRythmnPattern.R_1]
        case Species.SECOND_SPECIES:
            match measure_position:
                case MeasurePosition.FIRST:
                    return [MeasureRythmnPattern.R_rr2]
                case MeasurePosition.LAST:
                    return [MeasureRythmnPattern.R_1]
                case _:
                    return [MeasureRythmnPattern.R_22]
        case Species.THIRD_SPECIES:
            match measure_position:
                case MeasurePosition.FIRST:
                    return [MeasureRythmnPattern.R_r444]
                case MeasurePosition.LAST:
                    return [MeasureRythmnPattern.R_1]
                case _:
                    return [MeasureRythmnPattern.R_4444]
        case Species.FOURTH_SPECIES:
            match measure_position:
                case MeasurePosition.FIRST:
                    return [
                        MeasureRythmnPattern.R_rr2,
                        MeasureRythmnPattern.R_rr2t,
                    ]
                case MeasurePosition.MIDDLE:
                    return [
                        MeasureRythmnPattern.R_22,
                        MeasureRythmnPattern.R_t22,
                        MeasureRythmnPattern.R_22t,
                        MeasureRythmnPattern.R_t22t,
                    ]
                case MeasurePosition.PENULTIMATE:
                    return [
                        MeasureRythmnPattern.R_22,
                        MeasureRythmnPattern.R_t22,
                    ]
                case MeasurePosition.LAST:
                    return [MeasureRythmnPattern.R_1]
        case Species.FIFTH_SPECIES:
            match measure_position:
                case MeasurePosition.FIRST:
                    return [
                        MeasureRythmnPattern.R_rr2,
                        MeasureRythmnPattern.R_rr2t,
                        MeasureRythmnPattern.R_r444,
                        MeasureRythmnPattern.R_r42,
                    ]
                case MeasurePosition.MIDDLE:
                    return [
                        MeasureRythmnPattern.R_22,
                        MeasureRythmnPattern.R_t22,
                        MeasureRythmnPattern.R_22t,
                        MeasureRythmnPattern.R_t22t,
                        MeasureRythmnPattern.R_4444,
                        MeasureRythmnPattern.R_244,
                        MeasureRythmnPattern.R_442,
                        MeasureRythmnPattern.R_t4444,
                        MeasureRythmnPattern.R_t244,
                        MeasureRythmnPattern.R_t442,
                        MeasureRythmnPattern.R_4444t,
                        MeasureRythmnPattern.R_244t,
                        MeasureRythmnPattern.R_2488,
                        MeasureRythmnPattern.R_4882,
                        MeasureRythmnPattern.R_t2488,
                        MeasureRythmnPattern.R_t4882,
                        MeasureRythmnPattern.R_4882t,
                        MeasureRythmnPattern.R_2d4,
                        MeasureRythmnPattern.R_2d88,
                    ]
                case MeasurePosition.PENULTIMATE:
                    return [
                        MeasureRythmnPattern.R_22,
                        MeasureRythmnPattern.R_t22,
                        MeasureRythmnPattern.R_4444,
                        MeasureRythmnPattern.R_244,
                        MeasureRythmnPattern.R_442,
                        MeasureRythmnPattern.R_t4444,
                        MeasureRythmnPattern.R_t244,
                        MeasureRythmnPattern.R_t442,
                        MeasureRythmnPattern.R_2488,
                        MeasureRythmnPattern.R_4882,
                        MeasureRythmnPattern.R_t2488,
                        MeasureRythmnPattern.R_t4882,
                        MeasureRythmnPattern.R_2d4,
                        MeasureRythmnPattern.R_2d88,
                    ]
                case MeasurePosition.LAST:
                    return [MeasureRythmnPattern.R_1]


_HARMONIC_INTERVALS_BASE = set(
    [
        Interval.parse("P8"),
        Interval.parse("P5"),
        Interval.parse("M3"),
        Interval.parse("m3"),
        Interval.parse("M6"),
        Interval.parse("m6"),
    ]
)
"バスを含まない2声部において利用できる音程。1オクターブ以下のもののみ記載"

_HARMONIC_INTERVALS_EXTRA = set(
    [
        Interval.parse("P4"),
        Interval.parse("A4"),
        Interval.parse("d5"),
    ]
)
"バスを含む2声部において利用できる音程。1オクターブ以下のもののみ記載"


def harmonic_intervals(include_unison: bool, include_fourth_and_triton: bool) -> set[Interval]:
    """
    声部間で認められる垂直的な音程。和声音を利用する場合はこの音程に含まれていないといけない。
    2オクターブ以下の可能なIntervalを全て返す。

    - include_unison: 完全1度を含めたい場合に指定する
    - include_fourth_and_triton: 完全4度・増4度・減5度を含めたい場合に指定する。
                                 いずれかのバスとの音程では利用できないことに注意。
    """
    valid_intervals: set[Interval] = set()

    # 基本となる協和音程のセット
    base_set = set(_HARMONIC_INTERVALS_BASE)
    if include_fourth_and_triton:
        base_set.update(_HARMONIC_INTERVALS_EXTRA)

    # 1オクターブ以下の音程を追加
    valid_intervals.update(base_set)

    # 1オクターブ上の音程を追加
    octave_up = Interval.parse("P8")
    for interval in base_set:
        valid_intervals.add(interval + octave_up)

    # 完全1度の処理
    if include_unison:
        valid_intervals.add(Interval.parse("P1"))

    return valid_intervals

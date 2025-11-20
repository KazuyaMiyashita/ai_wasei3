import logging
import re
from dataclasses import dataclass
from functools import lru_cache

from my_project.counterpoint.measure_search.measure_step_sequence import (
    AbstractMeasureStepSequence,
    MeasureStepSequence,
)
from my_project.counterpoint.measure_search.measure_step_sequence_generator import generate
from my_project.counterpoint.measure_search.measure_step_sequence_indexer import (
    MeasureStepSequenceIndexer,
    Q,
    SearchField,
)
from my_project.counterpoint.measure_search.pitch_applyer import apply_pitch_candidates
from my_project.counterpoint.measure_search.pitch_filter import filter_pitch_sequences
from my_project.counterpoint.measure_search.rythmn_applyer import apply_rythmn
from my_project.counterpoint.model import AnnotatedMeasure, MeasureRythmnPattern, NoteAnnotation, ToneType
from my_project.model import Degree, Duration, IntervalStep, Key, Measure, Note, NoteName, Octave, Pitch

logger = logging.getLogger(__name__)


TONE_TYPE_SHORTHAND = {
    ToneType.PASSING_TONE: "p",
    ToneType.NEIGHBOR_TONE: "br",
    ToneType.SUSPENDED_TONE: "r",
    ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE: "srh",
}
SHORTHAND_TO_TONE_TYPE = {v: k for k, v in TONE_TYPE_SHORTHAND.items()}


@dataclass(frozen=True)
class MeasureSearchResult:
    measure: AnnotatedMeasure
    next_measure_start_pitch: Pitch

    def to_string(self) -> str:
        notes_str_parts: list[str] = []
        for note in self.measure.notes:
            pitch_str = note.value.name() if note.value else "R"
            attrs: list[str] = []

            # Duration
            d_val = note.duration.value
            if d_val.denominator == 1:
                attrs.append(f"d={d_val.numerator}")
            else:
                attrs.append(f"d={d_val.numerator}/{d_val.denominator}")

            # ToneType
            if note.attribute.tone_type in TONE_TYPE_SHORTHAND:
                attrs.append(TONE_TYPE_SHORTHAND[note.attribute.tone_type])

            # Tied
            if note.attribute.is_tied_start:
                attrs.append("tied")

            attrs_str = ", ".join(attrs)
            notes_str_parts.append(f"{pitch_str}({attrs_str})")

        notes_str = " ".join(notes_str_parts)
        next_pitch_str = self.next_measure_start_pitch.name()
        return f"[{notes_str} | {next_pitch_str}]"

    @classmethod
    def parse(cls, text: str) -> "MeasureSearchResult":
        match = re.fullmatch(r"\[(.+?)\s*\|\s*(.+?)\]", text)
        if not match:
            raise ValueError(f"Invalid format for parse: {text}")

        notes_str, next_pitch_str = match.groups()
        next_pitch = Pitch.parse(next_pitch_str)

        notes: list[Note[Pitch | None, NoteAnnotation]] = []
        note_pattern = re.compile(r"([A-G][#b]*\d+|R)\s*\((.*?)\)")

        for note_match in note_pattern.finditer(notes_str):
            pitch_str, attrs_str = note_match.groups()
            pitch = Pitch.parse(pitch_str) if pitch_str != "R" else None

            duration = Duration.of(1)  # Default duration
            is_tied_start = False
            tone_type = ToneType.HARMONIC_TONE

            attrs = [s.strip() for s in attrs_str.split(",")]
            for attr in attrs:
                if not attr:
                    continue
                if "=" in attr:
                    key, value = attr.split("=")
                    if key == "d":
                        if "/" in value:
                            num, den = value.split("/")
                            duration = Duration.of(int(num), int(den))
                        else:
                            duration = Duration.of(int(value))
                elif attr == "tied":
                    is_tied_start = True
                elif attr in SHORTHAND_TO_TONE_TYPE:
                    tone_type = SHORTHAND_TO_TONE_TYPE[attr]

            notes.append(Note(pitch, duration, NoteAnnotation(is_tied_start, tone_type)))

        return cls(Measure.of(*notes), next_pitch)


class MeasureSearch:
    _indexer: MeasureStepSequenceIndexer

    def __init__(self, sequences: list[MeasureStepSequence], all_rythmn_patterns: list[MeasureRythmnPattern]) -> None:
        self._indexer = MeasureStepSequenceIndexer(sequences, all_rythmn_patterns)

    @classmethod
    def default(cls) -> "MeasureSearch":
        sequences = generate()
        return cls(sequences, list(MeasureRythmnPattern))

    @lru_cache(maxsize=10000)
    def search(
        self,
        start_pitch: Pitch,
        start_harmonic_pitch: Pitch,
        next_measure_start_harmonic_pitch: Pitch,
        harmonic_note_names: tuple[NoteName, ...],
        key: Key,
        measure_rythmn_patterns: tuple[MeasureRythmnPattern, ...],
        pitch_range: tuple[Pitch, Pitch],
    ) -> list[MeasureSearchResult]:
        """
        指定された条件に合致する旋律を返す。

        Args:
            start_pitch: 旋律の開始音
            start_harmonic_pitch: 旋律が最初に利用する和声音。
                start_pitch と IntervalStep の差の絶対値が 1 以下(0-indexed)のものを指定する必要がある。
                start_pitch と一致する場合は和声音から始まる旋律が返され、異なる場合は掛留音から始まる旋律が返される。
                全音符単位で骨格となる和声音を実施した後、旋律を埋めるといった探索を行う場合、その和声音が指定される。
            next_measure_start_harmonic_pitch: 次の小節で最初に利用する和声音。
                生成した旋律の next_measure_start_pitch はこの音と同じか、
                または IntervalStep の差の絶対値が 1 以下(0-indexed)のものになる。
                全音符単位で骨格となる和声音を実施した後、旋律を埋めるといった探索を行う場合、次の小節の和声音が指定される。
            harmonic_note_names: 和声音として利用できる音名の一覧
                結果の旋律に含まれる和声音はここで指定した一覧の一部が利用される。
                調に含まれる三和音の構成音を想定しており、それ以外を指定した場合は結果が空になったり不適切になる可能性がある。
            key: 調
            measure_rythmn_patterns: 結果に含めたいリズムパターン
                結果の旋律の形状や非和声音の扱い方に応じてリズムパターンが適切かどうかが検証される。
            pitch_range: 旋律の音域
        """
        first_note_interval_step = (start_pitch - start_harmonic_pitch).step()
        assert IntervalStep(-1) <= first_note_interval_step <= IntervalStep(1)

        available_harmonic_steps_frozenset: frozenset[IntervalStep] = frozenset(
            map(
                lambda hn: (Pitch(Octave(0), hn) - start_harmonic_pitch).step().inversion_normalized(),
                harmonic_note_names,
            )
        )

        next_measure_step = (next_measure_start_harmonic_pitch - start_harmonic_pitch).step()
        next_measure_adjacent_steps = [next_measure_step + s for s in [IntervalStep(-1), IntervalStep(1)]]

        next_measure_condition = Q(SearchField.NEXT_MEASURE_STEP).eq(next_measure_step)

        adjacent_condition = (Q(SearchField.NEXT_MEASURE_STEP).is_in(next_measure_adjacent_steps)).and_(
            Q(SearchField.IS_TIED_TO_NEXT_MEASURE_REQUIRED).eq(True)
        )

        min_pitch, max_pitch = pitch_range
        min_step = (min_pitch - start_harmonic_pitch).step()
        max_step = (max_pitch - start_harmonic_pitch).step()
        pitch_condition = (Q(SearchField.MIN_STEP).ge(min_step)).and_(Q(SearchField.MAX_STEP).le(max_step))

        condition = (
            (Q(SearchField.FIRST_NOTE_INTERVAL_STEP).eq(first_note_interval_step))
            .and_(Q(SearchField.USED_HARMONIC_STEPS).is_subset_of(available_harmonic_steps_frozenset))
            .and_(Q(SearchField.RYTHMN_PATTERNS).is_in(measure_rythmn_patterns))
            .and_((next_measure_condition).or_(adjacent_condition))
            .and_(pitch_condition)
        )

        candidates = self._indexer.find(condition)

        chord_degrees = {Degree.from_note_name_key(nn, key) for nn in harmonic_note_names}

        results: list[MeasureSearchResult] = []
        for candidate in candidates:
            results.extend(
                _to_measure_search_result(
                    candidate,
                    start_harmonic_pitch,
                    key,
                    measure_rythmn_patterns,
                    chord_degrees,
                    next_measure_start_harmonic_pitch,
                    pitch_range,
                    self._indexer,
                )
            )

        return results


def _to_measure_search_result(
    measure_step_sequence: MeasureStepSequence,
    start_pitch: Pitch,
    key: Key,
    measure_rythmn_patterns: tuple[MeasureRythmnPattern, ...],
    chord_degrees: set[Degree],
    next_measure_start_harmonic_pitch: Pitch,
    pitch_range: tuple[Pitch, Pitch],
    indexer: MeasureStepSequenceIndexer,
) -> list[MeasureSearchResult]:
    pitch_candidates = apply_pitch_candidates(key, chord_degrees, start_pitch, measure_step_sequence)
    pitch_measure_sequences: list[AbstractMeasureStepSequence[Pitch]] = filter_pitch_sequences(
        pitch_candidates, next_measure_start_harmonic_pitch, pitch_range, key
    )

    results: list[MeasureSearchResult] = []

    compatible_patterns = indexer.get_compatible_rythmn_patterns(measure_step_sequence)
    target_patterns = [p for p in measure_rythmn_patterns if p in compatible_patterns]

    for pitch_measure_sequence in pitch_measure_sequences:
        for rythmn_pattern in target_patterns:
            measure = apply_rythmn(pitch_measure_sequence, rythmn_pattern)
            results.append(MeasureSearchResult(measure, pitch_measure_sequence.next_measure_step))

    return results


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Search for counterpoint measures.")
    parser.add_argument("--start_pitch", required=True, type=str)
    parser.add_argument("--start_harmonic_pitch", required=True, type=str)
    parser.add_argument("--next_measure_start_harmonic_pitch", required=True, type=str)
    parser.add_argument("--harmonic_note_names", required=True, nargs="+", type=str)
    parser.add_argument("--key", required=True, type=str)
    parser.add_argument("--rythmn_patterns", required=True, nargs="+", type=str)
    parser.add_argument("--pitch_range", required=True, nargs=2, type=str)

    args = parser.parse_args()

    start_pitch = Pitch.parse(args.start_pitch)
    start_harmonic_pitch = Pitch.parse(args.start_harmonic_pitch)
    next_measure_start_harmonic_pitch = Pitch.parse(args.next_measure_start_harmonic_pitch)
    harmonic_note_names = [NoteName.parse(n) for n in args.harmonic_note_names]
    key = Key.parse(args.key)
    rythmn_patterns = [MeasureRythmnPattern[p] for p in args.rythmn_patterns]
    pitch_range = (Pitch.parse(args.pitch_range[0]), Pitch.parse(args.pitch_range[1]))

    searcher = MeasureSearch.default()
    results = searcher.search(
        start_pitch=start_pitch,
        start_harmonic_pitch=start_harmonic_pitch,
        next_measure_start_harmonic_pitch=next_measure_start_harmonic_pitch,
        harmonic_note_names=tuple(harmonic_note_names),
        key=key,
        measure_rythmn_patterns=tuple(rythmn_patterns),
        pitch_range=pitch_range,
    )

    for result in results:
        print(result.to_string())


if __name__ == "__main__":
    main()

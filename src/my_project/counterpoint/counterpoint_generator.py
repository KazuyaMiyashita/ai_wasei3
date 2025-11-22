import logging
import random
from collections.abc import Iterator
from dataclasses import dataclass
from functools import cached_property

from my_project.counterpoint import all_measure_validator, validator
from my_project.counterpoint.measure_search.measure_search import MeasureSearch
from my_project.counterpoint.model import (
    AnnotatedMeasure,
    MeasurePosition,
    NoteAnnotation,
    Species,
    ToneType,
)
from my_project.counterpoint.rules import get_measure_rythmn_patterns
from my_project.counterpoint.skeleton_generator import Skeleton, SkeletonGenerator
from my_project.model import (
    Duration,
    FullScore,
    Key,
    Measure,
    Melody,
    Note,
    PartId,
    Pitch,
    Score,
    TimeSignature,
)
from my_project.util import part_range

logger = logging.getLogger(__name__)


@dataclass
class CounterpointGenerator:
    cantus_firmus: list[Pitch]
    cf_part_id: PartId
    key: Key
    species: Species
    part_id: PartId

    measure_search: MeasureSearch
    skeleton_generator: SkeletonGenerator
    rand: random.Random

    _MAX_TRIES = 30

    def __init__(
        self,
        cantus_firmus: list[Pitch],
        cf_part_id: PartId,
        key: Key,
        species: Species,
        part_id: PartId,
        rand: random.Random,
        measure_search: MeasureSearch,
        skeleton_generator: SkeletonGenerator,
    ) -> None:
        self.cantus_firmus = cantus_firmus
        self.cf_part_id = cf_part_id
        self.key = key
        self.species = species
        self.part_id = part_id
        self.rand = rand
        self.measure_search = measure_search
        self.skeleton_generator = skeleton_generator
        pass

    @classmethod
    def default(
        cls,
        cantus_firmus: list[Pitch],
        cf_part_id: PartId,
        key: Key,
        species: Species,
        part_id: PartId,
        seed: int | None = None,
    ) -> "CounterpointGenerator":
        rand = random.Random(seed)
        measure_search = MeasureSearch.default()
        # _measure_length は cantus_firmus の長さから計算されるため、ここで渡す
        _measure_length = len(cantus_firmus)
        skeleton_generator = SkeletonGenerator(
            cantus_firmus=cantus_firmus,
            key=key,
            part_id=part_id,
            rand=rand,
            _measure_length=_measure_length,
        )
        return cls(
            cantus_firmus=cantus_firmus,
            cf_part_id=cf_part_id,
            key=key,
            species=species,
            part_id=part_id,
            rand=rand,
            measure_search=measure_search,
            skeleton_generator=skeleton_generator,
        )

    def __post_init__(self) -> None:
        assert self.cantus_firmus

    @cached_property
    def _measure_length(self) -> int:
        return len(self.cantus_firmus)

    @cached_property
    def _pitch_range(self) -> tuple[Pitch, Pitch]:
        return part_range(self.part_id)

    class AbortAttempt(Exception):
        pass

    def generate_scores(self) -> Iterator[FullScore[NoteAnnotation]]:
        # logger.info("generate_scoresを開始します")

        attempt_count = 0
        while True:
            attempt_count += 1
            # 課題全体の骨格を作成
            skeleton: Skeleton = self.skeleton_generator.generate_skeleton()
            logger.debug(f"Attempt {attempt_count}: Start")

            initial_start_pitch = skeleton.measures[0].notes[0].value
            try:
                yield from self._generate_recursive(skeleton, [], 0, initial_start_pitch)
            except CounterpointGenerator.AbortAttempt:
                logger.debug(f"Attempt {attempt_count}: Aborted. Restarting from scratch.")
                continue

    def _generate_recursive(
        self,
        skeleton: Skeleton,
        completed_measures: list[AnnotatedMeasure],
        measure_index: int,
        current_start_pitch: Pitch,
    ) -> Iterator[FullScore[NoteAnnotation]]:
        log_indents = 2
        indent = " " * (measure_index * log_indents)
        mn_for_log = measure_index + 1

        previous_measure: AnnotatedMeasure | None = None
        previous_cf: Pitch | None = None
        if measure_index > 0:
            previous_measure = completed_measures[measure_index - 1]
            previous_cf = self.cantus_firmus[measure_index - 1]

        current_cf = self.cantus_firmus[measure_index]

        # 最終小節の場合
        if measure_index == self._measure_length - 1:
            last_measure_candidate: AnnotatedMeasure = Melody.of(
                Note(
                    skeleton.measures[measure_index].notes[0].value,
                    Duration.of(4),
                    NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
                )
            )

            if validator.validate(previous_measure, last_measure_candidate, previous_cf, current_cf):
                logger.debug(f"{indent}Measure {mn_for_log}: [SUCCEED] Last measure created and validated.")
                final_measures = [*completed_measures, last_measure_candidate]
                if not all_measure_validator.validate(final_measures):
                    logger.debug(f"{indent}Attempt failed: [FAILED] All measure validation.]")
                    raise CounterpointGenerator.AbortAttempt()
                else:
                    logger.debug(f"{indent}Attempt succeeded: [SUCCEED] All measures passed validation.]")
                    yield self._to_score(final_measures)
            else:
                logger.debug(f"{indent}Measure {mn_for_log}: [FAILED] Last measure created but failed validation.]")
            return
        # 最終小節以外の場合
        else:
            current_measure_position = self._current_measure_position(measure_index)
            results = self.measure_search.search(
                start_pitch=current_start_pitch,
                start_harmonic_pitch=skeleton.measures[measure_index].notes[0].value,
                next_measure_start_harmonic_pitch=skeleton.measures[measure_index + 1].notes[0].value,
                harmonic_note_names=tuple(skeleton.measures[measure_index].notes[0].attribute),
                key=self.key,
                measure_rythmn_patterns=tuple(get_measure_rythmn_patterns(self.species, current_measure_position)),
                pitch_range=self._pitch_range,
            )
            self.rand.shuffle(results)

            valid_candidates = []
            for chosen_result in results:
                if validator.validate(previous_measure, chosen_result.measure, previous_cf, current_cf):
                    valid_candidates.append(chosen_result)

            if not valid_candidates:
                if len(results) == 0:
                    logger.debug(f"{indent}Measure {mn_for_log}: [FAILED] No candidates found by measure_search.")
                else:
                    logger.debug(
                        f"{indent}Measure {mn_for_log}: [FAILED] Found {len(results)} candidates."
                        " All candidates failed validation."
                    )
                return

            logger.debug(
                f"{indent}Measure {mn_for_log}: [SUCCEED] Found {len(results)} candidates."
                f" Valid {len(valid_candidates)} candidates found."
            )

            for candidate in valid_candidates:
                next_start_pitch = candidate.next_measure_start_pitch
                yield from self._generate_recursive(
                    skeleton,
                    [*completed_measures, candidate.measure],
                    measure_index + 1,
                    next_start_pitch,
                )

    def _current_measure_position(self, current_measure_idx: int) -> MeasurePosition:
        if current_measure_idx == 0:
            return MeasurePosition.FIRST
        elif current_measure_idx == self._measure_length - 1:
            return MeasurePosition.LAST
        elif current_measure_idx == self._measure_length - 2:
            return MeasurePosition.PENULTIMATE
        else:
            return MeasurePosition.MIDDLE

    def _to_score(self, completed_measures: list[AnnotatedMeasure]) -> FullScore[NoteAnnotation]:
        cf_notes: list[Note[Pitch | None, NoteAnnotation]] = [
            Note(
                pitch,
                Duration.of(4),
                NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
            )
            for pitch in self.cantus_firmus
        ]
        cf_measures: list[AnnotatedMeasure] = [Melody.of(note) for note in cf_notes]

        # body Construction
        time_signature = TimeSignature(2, Duration.of(2))

        parts = {
            self.cf_part_id: [Measure(m) for m in cf_measures],
            self.part_id: [Measure(m) for m in completed_measures],
        }

        body = Score(parts)

        return FullScore(
            key=self.key,
            time_signature=time_signature,
            body=body,
        )

import logging
import random
from dataclasses import dataclass
from functools import cached_property

from my_project.chords import ChordWithBass, DegreeChord, DegreeStepChordWithBass, Inversion
from my_project.counterpoint import all_measure_validator, validator
from my_project.counterpoint.model import AnnotatedMeasure, MeasurePosition, NoteAnnotation, ToneType
from my_project.model import (
    Chord,
    Degree,
    DegreeStep,
    Duration,
    IntervalStep,
    Key,
    Measure,
    Melody,
    Mode,
    Note,
    NoteName,
    Octave,
    PartId,
    Pitch,
)
from my_project.util import part_range

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class Skeleton:
    """
    課題全体を仮に全音符で実施した際の、協和音の音高と和音の設定を表す。
    """

    measures: list[Melody[Note[Pitch, ChordWithBass[NoteName]]]]
    # TODO: attribute は Chord[Degree] なのかで迷っている


@dataclass
class SkeletonGenerator:
    cantus_firmus: list[Pitch]
    key: Key
    part_id: PartId
    rand: random.Random
    _measure_length: int

    _MAX_RETRIES = 100

    def generate_skeleton(self) -> Skeleton:
        for attempt in range(self._MAX_RETRIES):
            try:
                return self.generate_skeleton_impl()
            except ValueError as e:
                if attempt == self._MAX_RETRIES - 1:
                    logger.error(f"Failed to generate skeleton after {self._MAX_RETRIES} attempts.")
                    raise e
                continue
        raise ValueError("Unexpected error in generate_skeleton")

    def generate_skeleton_impl(self) -> Skeleton:
        """
        CFを元に実施する声部のそれぞれの小節の最初に利用する和声音を定める。
        この和声音を骨格として、間に旋律を埋めることで課題が実施される。
        そのため、ここでは旋律の同音の連続や7度などの不協和音程を選択することができる。
        旋律はオクターブより大きい音程は不可
        CFと旋律は2オクターブまで

        和音と旋律の規則。和音はCFと協和する
        - 冒頭小節
            - 和音は I の基本形
            - 対位旋律は i, v 度音から始める。(移勢の類の場合は iii 度音が可能)
            - CFと1度になることが可能
        - 途中の小節
            - CFと
            - CFと1度になることはできない
        - 最終小節の1小節前
            - V の基本形、第一転回形
            - VII の第一転回形
            - II の基本形、第一転回形(!)。ただし対位旋律は経過音として導音を経過する場合に限られる。
            - CFと1度になることはできない
        - 最終小節
            - 和音は I の基本形
            - 対位旋律は i 度音 (2声の場合)
            - CFと1度になることが可能
        """
        measures: list[Melody[Note[Pitch, ChordWithBass[NoteName]]]] = []
        chord_palette = self._get_chord_palette()
        candidate_pitches_pool = self._get_all_pitches_in_range()

        for measure_idx, cf_pitch in enumerate(self.cantus_firmus):
            cf_degree = Degree.from_note_name_key(cf_pitch.note_name, self.key)
            current_measure_pos = self._current_measure_position(measure_idx)

            chord_candidates: list[DegreeChord] = []

            if current_measure_pos in {MeasurePosition.FIRST, MeasurePosition.LAST}:
                chord_candidates = [DegreeChord.I]
            elif current_measure_pos == MeasurePosition.PENULTIMATE:
                if cf_degree.step == DegreeStep.idx_1(5):
                    if self.key.mode == Mode.MAJOR:
                        chord_candidates = [DegreeChord.V]
                    else:
                        chord_candidates = [DegreeChord.V_leading]
                elif cf_degree.step == DegreeStep.idx_1(2):
                    if self.key.mode == Mode.MAJOR:
                        chord_candidates = [DegreeChord.II, DegreeChord.VII]
                    else:
                        chord_candidates = [DegreeChord.VII_leading]
                elif cf_degree.step == DegreeStep.idx_1(7):
                    if self.key.mode == Mode.MAJOR:
                        chord_candidates = [DegreeChord.V]
                    else:
                        chord_candidates = [DegreeChord.V_leading]
                else:
                    chord_candidates = self._filter_chords_containing(chord_palette, cf_degree)
            else:
                chord_candidates = self._filter_chords_containing(chord_palette, cf_degree)

            if not chord_candidates:
                chord_candidates = self._filter_chords_containing(chord_palette, cf_degree)
                if not chord_candidates:
                    raise ValueError(
                        f"No valid chord found for CF note {cf_pitch} ({cf_degree}) at measure {measure_idx}"
                    )

            self.rand.shuffle(chord_candidates)
            measure_succeeded = False

            for selected_chord in chord_candidates:
                chord_degrees = selected_chord.elements
                chord_note_names = [d.note_name(self.key) for d in chord_degrees]

                valid_candidates: list[tuple[Pitch, ChordWithBass[NoteName]]] = []
                for p in candidate_pitches_pool:
                    if p.note_name not in chord_note_names:
                        continue

                    interval = (p - cf_pitch).abs()
                    interval_semitones = interval.num().value

                    # CFと旋律は2オクターブまで (P15 = 24 semitones)
                    if interval_semitones > 24:
                        continue

                    is_unison = interval_semitones == 0

                    if current_measure_pos == MeasurePosition.FIRST:
                        deg = Degree.from_note_name_key(p.note_name, self.key)
                        # i, v 度音から始める (1 or 5)
                        if deg.step.value not in {0, 4}:
                            continue
                    elif current_measure_pos == MeasurePosition.LAST:
                        deg = Degree.from_note_name_key(p.note_name, self.key)
                        # i 度音 (1)
                        if deg.step.value != 0:
                            continue
                    elif current_measure_pos == MeasurePosition.PENULTIMATE:
                        if is_unison:
                            continue
                    else:  # MIDDLE
                        if is_unison:
                            continue

                    # Inversion Check
                    bass_pitch = cf_pitch if cf_pitch.num() < p.num() else p
                    chord_with_bass = ChordWithBass(Chord(frozenset(chord_note_names)), bass_pitch.note_name)

                    # Convert to DegreeStepChordWithBass to check inversion
                    ds_elements = frozenset(Degree.from_note_name_key(nn, self.key).step for nn in chord_note_names)
                    bass_ds = Degree.from_note_name_key(bass_pitch.note_name, self.key).step
                    ds_chord_with_bass = DegreeStepChordWithBass(Chord(ds_elements), bass_ds)

                    try:
                        if ds_chord_with_bass.inversion_type() == Inversion.SECOND:
                            continue
                    except ValueError:
                        # Triad ではない場合など
                        pass

                    # Check validation with existing validator
                    if measures:
                        previous_pitch = measures[-1].notes[0].value
                        previous_cf_pitch = self.cantus_firmus[measure_idx - 1]

                        # Create AnnotatedMeasure for validation
                        prev_measure = _create_annotated_measure(previous_pitch)
                        curr_measure = _create_annotated_measure(p)

                        if not validator.validate(prev_measure, curr_measure, previous_cf_pitch, cf_pitch):
                            continue

                    valid_candidates.append((p, chord_with_bass))

                if valid_candidates:
                    chosen_pitch, chosen_chord = self.rand.choice(valid_candidates)
                    measures.append(_create_measure(chosen_pitch, chosen_chord))
                    measure_succeeded = True
                    break

            if not measure_succeeded:
                raise ValueError(
                    f"No valid chord and pitch combination found for CF note {cf_pitch} at measure {measure_idx}"
                )

            # Check total range validation
            all_annotated_measures = [_create_annotated_measure(m.notes[0].value) for m in measures]
        if not all_measure_validator.validate(all_annotated_measures):
            raise ValueError("Failed all measure validation (e.g. total range)")

        return Skeleton(measures)

    def _get_chord_palette(self) -> list[DegreeChord]:
        palette = []

        def get_diatonic_degree(step_idx_1: int) -> Degree:
            interval_step = IntervalStep.idx_1(step_idx_1)
            p = self.key.diatonic_scale_pitch(interval_step)
            return Degree.from_note_name_key(p.note_name, self.key)

        def make_chord(roots: list[int]) -> DegreeChord:
            return DegreeChord.of(*[get_diatonic_degree(r) for r in roots])

        # I (1, 3, 5)
        palette.append(DegreeChord.I)

        # II (2, 4, 6)
        palette.append(DegreeChord.II)

        # III (3, 5, 7)
        palette.append(make_chord([3, 5, 7]))

        # IV (4, 6, 1)
        palette.append(make_chord([4, 6, 8]))

        # V (5, 7, 2)
        if self.key.mode == Mode.MAJOR:
            palette.append(DegreeChord.V)
        else:
            palette.append(DegreeChord.V_leading)

        # VI (6, 1, 3)
        palette.append(make_chord([6, 8, 10]))

        # VII (7, 2, 4)
        if self.key.mode == Mode.MAJOR:
            palette.append(DegreeChord.VII)
        else:
            palette.append(DegreeChord.VII_leading)

        return palette

    def _filter_chords_containing(self, pool: list[DegreeChord], degree: Degree) -> list[DegreeChord]:
        return [c for c in pool if degree in c.elements]

    def _get_all_pitches_in_range(self) -> list[Pitch]:
        min_p, max_p = self._pitch_range
        pitches = []
        # -15 to 19 covers all reasonable key signatures
        for oct_val in range(min_p.octave.value - 1, max_p.octave.value + 2):
            for nn_val in range(-15, 20):
                p = Pitch(Octave(oct_val), NoteName(nn_val))
                if min_p.num() <= p.num() <= max_p.num():
                    pitches.append(p)
        return pitches

    @cached_property
    def _pitch_range(self) -> tuple[Pitch, Pitch]:
        return part_range(self.part_id)

    def _current_measure_position(self, current_measure_idx: int) -> MeasurePosition:
        if current_measure_idx == 0:
            return MeasurePosition.FIRST
        elif current_measure_idx == self._measure_length - 1:
            return MeasurePosition.LAST
        elif current_measure_idx == self._measure_length - 2:
            return MeasurePosition.PENULTIMATE
        else:
            return MeasurePosition.MIDDLE


def _create_measure(pitch: Pitch, chord: ChordWithBass[NoteName]) -> Melody[Note[Pitch, ChordWithBass[NoteName]]]:
    return Melody.of(Note(pitch, Duration.of(4), chord))


def _create_annotated_measure(pitch: Pitch) -> AnnotatedMeasure:
    """Helper to create an AnnotatedMeasure for validation"""
    return Measure.of(
        Note(pitch, Duration.of(4), NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE))
    )

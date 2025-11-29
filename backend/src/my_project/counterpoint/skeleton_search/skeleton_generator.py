from collections import defaultdict

from my_project.counterpoint.model import MeasurePosition
from my_project.counterpoint.skeleton_search.top_k_viterbi_solver import TopKViterbiSolver
from my_project.model import Chord, Duration, Key, Measure, Note, PartId, Pitch, Score

PartChord = Chord[tuple[PartId, Pitch]]


class SkeletonGenerator:
    cantus_films: list[Pitch]
    key: Key
    cf_part_id: PartId
    solve_part_ids: set[PartId]

    def __init__(
        self,
        cantus_films: list[Pitch],
        key: Key,
        cf_part_id: PartId,
        solve_part_ids: set[PartId],
    ) -> None:
        self.cantus_films = cantus_films
        self.key = key
        self.cf_part_id = cf_part_id
        self.solve_part_ids = solve_part_ids

    def generate(
        self,
    ) -> list[tuple[Score[PartId, Pitch, None], float]]:
        """
        定旋律から
        """

        solver = TopKViterbiSolver[PartChord](
            length=len(self.cantus_films),
            get_candidates=self._get_candidates,
            get_node_score=self._get_node_score,
            get_transition_score=self._get_transition_score,
        )
        results: list[tuple[list[PartChord], float]] = solver.solve(k=50)

        return list(map(lambda t: (_to_score(t[0]), t[1]), results))

    def _get_candidates(self, measure_idx: int) -> list[PartChord]:
        raise NotImplementedError

    def _get_node_score(self, part_chord: PartChord, measure_idx: int) -> float:
        raise NotImplementedError

    def _get_transition_score(self, prev: PartChord, current: PartChord, measure_idx: int) -> float:
        raise NotImplementedError

    # ---

    def _current_cf(self, measure_idx: int) -> Pitch:
        return self.cantus_films[measure_idx]

    def _current_measure_position(self, current_measure_idx: int) -> MeasurePosition:
        measure_length = len(self.cantus_films)
        if current_measure_idx == 0:
            return MeasurePosition.FIRST
        elif current_measure_idx == measure_length - 1:
            return MeasurePosition.LAST
        elif current_measure_idx == measure_length - 2:
            return MeasurePosition.PENULTIMATE
        else:
            return MeasurePosition.MIDDLE


def _to_score(part_chords: list[PartChord]) -> Score[PartId, Pitch, None]:
    parts: dict[PartId, list[Measure[Note[Pitch, None]]]] = defaultdict()

    for part_chord in part_chords:
        for part_id, pitch in part_chord.elements:
            parts[part_id].append(Measure.of(Note(pitch, Duration.of(4), None)))

    return Score({k: v for k, v in parts.items()})

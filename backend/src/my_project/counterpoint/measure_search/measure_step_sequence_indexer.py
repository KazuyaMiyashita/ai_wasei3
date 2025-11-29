import logging
from abc import ABC, abstractmethod
from collections import defaultdict
from dataclasses import dataclass
from enum import Enum, auto
from typing import Any

from tqdm import tqdm

from my_project.counterpoint.measure_search.measure_step_sequence import MeasureStepSequence
from my_project.counterpoint.measure_search.rythmn_applyer import try_apply_rythmn
from my_project.counterpoint.model import MeasureRythmnPattern
from my_project.model import IntervalStep

logger = logging.getLogger(__name__)


class Operator(Enum):
    EQUAL = auto()
    IN = auto()
    IS_SUBSET_OF = auto()
    AND = auto()
    OR = auto()
    GREATER_THAN_OR_EQUAL = auto()
    LESS_THAN_OR_EQUAL = auto()


class SearchField(str, Enum):
    NUM_NOTES_IN_MEASURE = "num_notes_in_measure"
    NEXT_MEASURE_STEP = "next_measure_step"
    FIRST_NOTE_INTERVAL_STEP = "first_note_interval_step"
    USED_HARMONIC_STEPS = "used_harmonic_steps"
    IS_TIED_TO_NEXT_MEASURE_REQUIRED = "is_tied_to_next_measure_required"
    RYTHMN_PATTERNS = "rythmn_patterns"
    MIN_STEP = "min_step"
    MAX_STEP = "max_step"


class Condition(ABC):
    @abstractmethod
    def and_(self, other: "Condition") -> "CompositeCondition": ...

    @abstractmethod
    def or_(self, other: "Condition") -> "CompositeCondition": ...


@dataclass(frozen=True)
class LeafCondition(Condition):
    field: SearchField
    op: Operator
    value: Any

    def and_(self, other: "Condition") -> "CompositeCondition":
        return CompositeCondition(Operator.AND, [self, other])

    def or_(self, other: "Condition") -> "CompositeCondition":
        return CompositeCondition(Operator.OR, [self, other])


@dataclass(frozen=True)
class CompositeCondition(Condition):
    op: Operator
    conditions: list[Condition]

    def and_(self, other: "Condition") -> "CompositeCondition":
        if self.op == Operator.AND:
            return CompositeCondition(Operator.AND, [*self.conditions, other])
        return CompositeCondition(Operator.AND, [self, other])

    def or_(self, other: "Condition") -> "CompositeCondition":
        if self.op == Operator.OR:
            return CompositeCondition(Operator.OR, [*self.conditions, other])
        return CompositeCondition(Operator.OR, [self, other])


@dataclass(frozen=True)
class QueryField:
    field: SearchField

    def eq(self, value: Any) -> LeafCondition:
        return LeafCondition(self.field, Operator.EQUAL, value)

    def and_(self, other: "Condition") -> "CompositeCondition":
        return CompositeCondition(Operator.AND, [LeafCondition(self.field, Operator.EQUAL, self), other])

    def or_(self, other: "Condition") -> "CompositeCondition":
        return CompositeCondition(Operator.OR, [LeafCondition(self.field, Operator.EQUAL, self), other])

    def is_in(self, value: list[Any] | tuple[Any, ...]) -> LeafCondition:
        return LeafCondition(self.field, Operator.IN, value)

    def is_subset_of(self, value: set[Any] | frozenset[Any]) -> LeafCondition:
        return LeafCondition(self.field, Operator.IS_SUBSET_OF, value)

    def ge(self, value: Any) -> LeafCondition:
        return LeafCondition(self.field, Operator.GREATER_THAN_OR_EQUAL, value)

    def le(self, value: Any) -> LeafCondition:
        return LeafCondition(self.field, Operator.LESS_THAN_OR_EQUAL, value)


Q = QueryField


class MeasureStepSequenceIndexer:
    _sequences: list[MeasureStepSequence]
    _index: dict[str, dict[Any, list[int]]]
    _all_indices: set[int]

    def __init__(self, sequences: list[MeasureStepSequence], all_rythmn_patterns: list[MeasureRythmnPattern]) -> None:
        self._sequences = sequences
        self._sequence_map = {seq: i for i, seq in enumerate(sequences)}
        self._index, self._id_to_rythmn_patterns = self._build_index(sequences, all_rythmn_patterns)
        self._all_indices = set(range(len(sequences)))

    def _build_index(
        self, sequences: list[MeasureStepSequence], all_rythmn_patterns: list[MeasureRythmnPattern]
    ) -> tuple[dict[str, dict[Any, list[int]]], dict[int, set[MeasureRythmnPattern]]]:
        logger.info("MeasureStepSequenceのインデックス構築を開始します")
        index: dict[str, dict[Any, list[int]]] = defaultdict(lambda: defaultdict(list))
        id_to_rythmn_patterns: dict[int, set[MeasureRythmnPattern]] = defaultdict(set)

        for i, result in tqdm(enumerate(sequences), total=len(sequences), desc="Building index"):
            index["num_notes_in_measure"][result.num_notes_in_measure()].append(i)
            index["next_measure_step"][result.next_measure_step].append(i)
            index["first_note_interval_step"][result.first_note_interval_step_of_measure()].append(i)
            index["used_harmonic_steps"][result.used_harmonic_steps].append(i)
            index["is_tied_to_next_measure_required"][result.is_tied_to_next_measure_required()].append(i)
            index["min_step"][result.min_step].append(i)
            index["max_step"][result.max_step].append(i)

            for rythmn_pattern in all_rythmn_patterns:
                if try_apply_rythmn(result, rythmn_pattern) is not None:
                    index["rythmn_patterns"][rythmn_pattern].append(i)
                    id_to_rythmn_patterns[i].add(rythmn_pattern)

        # defaultdict を通常の dict に変換
        logger.info("MeasureStepSequenceのインデックス構築を完了しました")
        return {k: dict(v) for k, v in index.items()}, dict(id_to_rythmn_patterns)

    def get_compatible_rythmn_patterns(self, sequence: MeasureStepSequence) -> set[MeasureRythmnPattern]:
        if sequence not in self._sequence_map:
            return set()
        idx = self._sequence_map[sequence]
        return self._id_to_rythmn_patterns.get(idx, set())

    def find(self, condition: Condition | None = None) -> list[MeasureStepSequence]:
        if condition is None:
            return self._sequences

        indices = self._evaluate_condition(condition)
        return [self._sequences[i] for i in sorted(list(indices))]

    def _evaluate_condition(self, condition: Condition) -> set[int]:
        if isinstance(condition, LeafCondition):
            if condition.op == Operator.EQUAL:
                return set(self._index.get(condition.field, {}).get(condition.value, []))
            elif condition.op == Operator.IN:
                indices = set()
                for val in condition.value:
                    indices.update(self._index.get(condition.field, {}).get(val, []))
                return indices
            elif condition.op == Operator.IS_SUBSET_OF:
                normalized_steps = frozenset(s.inversion_normalized() for s in condition.value)
                if IntervalStep(0) not in normalized_steps:
                    raise ValueError("available_harmonic_steps must always contain IntervalStep(0)")
                indices = set()
                for used_steps_key, idxs in self._index.get(condition.field, {}).items():
                    if used_steps_key.issubset(normalized_steps):
                        indices.update(idxs)
                return indices
            elif condition.op == Operator.GREATER_THAN_OR_EQUAL:
                indices = set()
                for key, idxs in self._index.get(condition.field, {}).items():
                    if key >= condition.value:
                        indices.update(idxs)
                return indices
            elif condition.op == Operator.LESS_THAN_OR_EQUAL:
                indices = set()
                for key, idxs in self._index.get(condition.field, {}).items():
                    if key <= condition.value:
                        indices.update(idxs)
                return indices
            else:
                raise ValueError(f"Unsupported operator for LeafCondition: {condition.op}")

        elif isinstance(condition, CompositeCondition):
            if condition.op == Operator.AND:
                # 要素数が少ない順にソートしてから intersection を取る
                sets = [self._evaluate_condition(c) for c in condition.conditions]
                sets.sort(key=len)
                return set.intersection(*sets) if sets else set()
            elif condition.op == Operator.OR:
                return set.union(*(self._evaluate_condition(c) for c in condition.conditions))
            else:
                raise ValueError(f"Unsupported operator for CompositeCondition: {condition.op}")

        else:
            raise TypeError(f"Unsupported condition type: {type(condition)}")

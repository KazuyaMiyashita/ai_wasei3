from dataclasses import dataclass
from enum import Enum, auto
from typing import ClassVar, TypeVar

from my_project.model import Chord, Degree, DegreeStep

T_Value = TypeVar("T_Value", covariant=True)


class Inversion(Enum):
    ROOT = auto()
    FIRST = auto()
    SECOND = auto()


@dataclass(frozen=True)
class ChordWithBass[T_Value]:
    """
    バス音を指定した和音。バスは和音の構成音に含まれていないといけない。
    """

    chord: Chord[T_Value]
    bass: T_Value

    def __post_init__(self) -> None:
        if self.bass not in self.chord.elements:
            raise ValueError("The bass must be included in the chord.")


@dataclass(frozen=True)
class DegreeStepChord(Chord[DegreeStep]):
    @staticmethod
    def of(*args: DegreeStep) -> "DegreeStepChord":
        return DegreeStepChord(frozenset(args))

    def is_triad(self) -> bool:
        """指定されたChordが三和音であるか"""
        if len(self.elements) != 3:
            return False
        return self.get_root() is not None

    def get_root(self) -> DegreeStep | None:
        """三和音としての根音を返す"""
        if len(self.elements) != 3:
            return None
        for step in self.elements:
            if (step + DegreeStep(2)) in self.elements and (step + DegreeStep(4)) in self.elements:
                return step
        return None

    @classmethod
    def triad_from_root(cls, root: DegreeStep) -> "DegreeStepChord":
        """与えられたDegreeStepを根音とする三和音を返す"""
        return DegreeStepChord(frozenset([root, root + DegreeStep(2), root + DegreeStep(4)]))

    @classmethod
    def triad_from_bass_as_first_inversion(cls, bass: DegreeStep) -> "DegreeStepChord":
        """与えられたDegreeStepが第一転回形のバス(第3音)となる三和音を返す"""
        root = bass - DegreeStep(2)
        return cls.triad_from_root(root)

    @classmethod
    def triads_containing(cls, step: DegreeStep) -> list["DegreeStepChord"]:
        """
        stepを「根音」「第3音」「第5音」として含む3つの三和音を返す
        """
        # 1. step が根音の場合
        as_root = cls.triad_from_root(step)
        # 2. step が第3音の場合 (ルートは step - 2)
        as_third = cls.triad_from_root(step - DegreeStep(2))
        # 3. step が第5音の場合 (ルートは step - 4)
        as_fifth = cls.triad_from_root(step - DegreeStep(4))

        return [as_root, as_third, as_fifth]


@dataclass(frozen=True)
class DegreeStepChordWithBass(ChordWithBass[DegreeStep]):
    @property
    def _chord(self) -> DegreeStepChord:
        return DegreeStepChord(self.chord.elements)

    def inversion_type(self) -> Inversion:
        """
        この和音の転回形を返す。
        前提: chordが三和音であること。
        """
        root = self._chord.get_root()
        if root is None:
            raise ValueError("Not a triad chord")

        # Modulo 7 の世界なので差分で判定する
        diff = (self.bass.value - root.value) % 7

        if diff == 0:
            return Inversion.ROOT
        elif diff == 2:
            return Inversion.FIRST
        elif diff == 4:
            return Inversion.SECOND
        else:
            # 三和音であれば通常ここには来ないが、7の和音などを考慮するなら拡張が必要
            raise ValueError(f"Unexpected bass relationship: {diff}")

    # --- 3. 三和音と転回形からバスを導出 ---
    @classmethod
    def get_bass_for_inversion(cls, chord: DegreeStepChord, inv: Inversion) -> DegreeStep:
        root = chord.get_root()
        if root is None:
            raise ValueError("Not a triad chord")

        match inv:
            case Inversion.ROOT:
                return root
            case Inversion.FIRST:
                return root + DegreeStep(2)
            case Inversion.SECOND:
                return root + DegreeStep(4)


@dataclass(frozen=True)
class DegreeChord(Chord[Degree]):
    @staticmethod
    def of(*args: Degree) -> "DegreeChord":
        return DegreeChord(frozenset(args))

    I: ClassVar["DegreeChord"]  # noqa: E741
    II: ClassVar["DegreeChord"]
    V: ClassVar["DegreeChord"]
    V_leading: ClassVar["DegreeChord"]
    VII: ClassVar["DegreeChord"]
    VII_leading: ClassVar["DegreeChord"]


DegreeChord.I = DegreeChord.of(Degree.idx_1(1, 0), Degree.idx_1(3, 0), Degree.idx_1(5, 0))
DegreeChord.II = DegreeChord.of(Degree.idx_1(2, 0), Degree.idx_1(4, 0), Degree.idx_1(6, 0))
DegreeChord.V = DegreeChord.of(Degree.idx_1(5, 0), Degree.idx_1(7, 0), Degree.idx_1(2, 0))
DegreeChord.V_leading = DegreeChord.of(Degree.idx_1(5, 0), Degree.idx_1(7, 1), Degree.idx_1(2, 0))

DegreeChord.VII = DegreeChord.of(Degree.idx_1(7, 0), Degree.idx_1(2, 0), Degree.idx_1(4, 0))
DegreeChord.VII_leading = DegreeChord.of(Degree.idx_1(7, 1), Degree.idx_1(2, 0), Degree.idx_1(4, 0))

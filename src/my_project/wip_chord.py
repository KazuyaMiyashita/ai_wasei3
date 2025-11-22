from collections.abc import Callable, Iterable
from dataclasses import dataclass
from enum import Enum, auto
from typing import TypeVar

# --- 基本モデル (DegreeStep) ---


@dataclass(frozen=True, order=True)
class DegreeStep:
    """
    mod 7 の演算機能付き DegreeStep
    """

    value: int

    def __post_init__(self) -> None:
        if not 0 <= self.value <= 6:
            raise ValueError(f"Step must be between 0 and 6. Got {self.value}")

    def __add__(self, other: int) -> "DegreeStep":
        return DegreeStep((self.value + other) % 7)

    def __sub__(self, other: int) -> "DegreeStep":
        return DegreeStep((self.value - other) % 7)

    def __str__(self) -> str:
        return str(self.value)


# --- Generics 定義 ---

T = TypeVar("T")  # 元の型
U = TypeVar("U")  # 変換後の型


@dataclass(frozen=True)
class Chord[T]:
    """
    型パラメータ T を持つ汎用的な和音コンテナ。
    PitchChord や DegreeStepChord を作る必要はありません。
    """

    elements: frozenset[T]

    def __post_init__(self) -> None:
        assert self.elements

    @staticmethod
    def of(*args: T) -> "Chord[T]":
        return Chord(frozenset(args))

    @staticmethod
    def from_iterable(iterable: Iterable[T]) -> "Chord[T]":
        return Chord(frozenset(iterable))

    # ★ Scala/Rustのような map メソッド
    def map(self, func: Callable[[T], U]) -> "Chord[U]":
        """
        中身の型を変換して新しいChordを返す。
        例: Chord[DegreeStep] -> Chord[Pitch]
        """
        return Chord(frozenset(func(e) for e in self.elements))

    def contains(self, element: T) -> bool:
        return element in self.elements

    def __len__(self) -> int:
        return len(self.elements)

    def with_bass(self, bass: T) -> "ChordWithBass[T]":
        return ChordWithBass(self, bass)

    def as_[S](self, subtype: type[S]) -> S:
        # S が Chord のサブクラスであることを期待して elements を渡す
        # データクラスのコンストラクタ規約に従う限り安全です
        return subtype(self.elements)  # type: ignore


@dataclass(frozen=True)
class ChordWithBass[T]:
    chord: Chord[T]
    bass: T

    def __post_init__(self) -> None:
        assert self.bass in self.chord.elements

    def as_[S](self, subtype: type[S]) -> S:
        # S が Chord のサブクラスであることを期待して elements を渡す
        # データクラスのコンストラクタ規約に従う限り安全です
        return subtype(self.elements)  # type: ignore


# --- DegreeStep 専用ロジック (Type Class的な扱い) ---
# Pythonでは「特定のTに対するメソッド」をクラス内に条件付きで定義できないため、
# これらを「関数」として定義し、型ヒントで Chord[DegreeStep] を要求するのが定石です。


class Inversion(Enum):
    ROOT = auto()
    FIRST = auto()
    SECOND = auto()


class DegreeStepChord(Chord[DegreeStep]):
    def is_triad(self) -> bool:
        """指定されたChordが三和音であるか"""
        if len(self.elements) != 3:
            return False
        return self.get_root() is not None

    def get_root(self) -> DegreeStep | None:
        """三和音としての根音を返す"""
        if len(self.elements) != 3:
            return None
        # self.elements に直接アクセスできるので self.chord.elements と書く必要がない
        # contains メソッドも継承しているのでそのまま使えます
        for step in self.elements:
            if self.contains(step + 2) and self.contains(step + 4):
                return step
        return None

    @staticmethod
    def of(*args: DegreeStep) -> "DegreeStepChord":
        return DegreeStepChord(frozenset(args))

    # --- 4a. DegreeStepを根音とする三和音を返す ---
    @classmethod
    def triad_from_root(cls, root: DegreeStep) -> "DegreeStepChord":
        return DegreeStepChord(frozenset([root, root + 2, root + 4]))

    # --- 4b. DegreeStepが第一転回形のバス(第3音)となる三和音を返す ---
    @classmethod
    def triad_from_bass_as_first_inversion(cls, bass: DegreeStep) -> "DegreeStepChord":
        # バスが第3音ということは、ルートはバスの2度下(あるいは5度上)
        root = bass - 2
        return cls.triad_from_root(root)

    # --- 5. DegreeStepを構成音に含む三和音の一覧を返す ---
    @classmethod
    def triads_containing(cls, step: DegreeStep) -> list["DegreeStepChord"]:
        """
        stepを「根音」「第3音」「第5音」として含む3つの三和音を返す
        """
        # 1. step が根音の場合
        as_root = cls.triad_from_root(step)
        # 2. step が第3音の場合 (ルートは step - 2)
        as_third = cls.triad_from_root(step - 2)
        # 3. step が第5音の場合 (ルートは step - 4)
        as_fifth = cls.triad_from_root(step - 4)

        return [as_root, as_third, as_fifth]


class DegreeStepChordWithBass(ChordWithBass[DegreeStep]):
    def inversion_type(self) -> Inversion:
        """
        この和音の転回形を返す。
        前提: chordが三和音であること。
        """
        root = self.chord.as_(DegreeStepChord).get_root()
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
                return root + 2
            case Inversion.SECOND:
                return root + 4


# --- 使用例 (ここが重要です) ---


def demo() -> None:
    # 1. 三和音判定
    c_major = Chord.of(0, 2, 4).map(DegreeStep).as_(DegreeStepChord)  # I
    print(f"c_major: {c_major}")
    print(f"I is triad: {c_major.is_triad()} (Root: {c_major.get_root()})")

    v_chord = Chord.of(4, 6, 1).map(DegreeStep).as_(DegreeStepChord)  # V (G, B, D in C major)
    print(f"V is triad: {v_chord.is_triad()} (Root: {v_chord.get_root()})")

    random_notes = Chord.of(0, 1, 4).map(DegreeStep).as_(DegreeStepChord)
    print(f"Random is triad: {random_notes.is_triad()}")

    # 2. 転回形判定
    on_chord = DegreeStepChordWithBass(v_chord, DegreeStep(6))  # Vの第一転回形 (Bass: 6/B)
    print(f"Inversion: {on_chord.inversion_type()}")  # Inversion.FIRST

    # 3. バス取得
    bass = DegreeStepChordWithBass.get_bass_for_inversion(c_major, Inversion.SECOND)
    print(f"I 2nd inv bass: {bass}")  # 4

    # 4. 指定音から和音生成
    target = DegreeStep(2)  # III (E)
    triad_6 = DegreeStepChord.triad_from_bass_as_first_inversion(target)
    print(f"Chord where {target} is bass of 1st inv: {triad_6.elements}")
    # -> {0, 2, 4} (Iの和音。EがバスならCメジャーの第一転回形)

    # 5. 含む和音一覧
    containing = DegreeStepChord.triads_containing(DegreeStep(0))  # I (C)
    for c in containing:
        print(f"Contains 0: {c.elements} Root: {c.get_root()}")
    # Root 0 (I), Root 5 (VI), Root 3 (IV) が出力される

    # 1. DegreeChord の作成
    c_major_deg = DegreeStepChord.of(DegreeStep(0), DegreeStep(2), DegreeStep(4))

    # 2. DegreeStep専用ロジックの使用
    print(f"Is triad: {c_major_deg.is_triad()}")  # True
    print(f"Root: {c_major_deg.get_root()}")  # 0

    # 3. map の威力: DegreeStep -> str への変換
    # Pitchへの変換などもこのロジック一発です
    note_names = ["C", "D", "E", "F", "G", "A", "B"]

    # ここで Chord[DegreeStep] が Chord[str] に変身します
    named_chord: Chord[str] = c_major_deg.map(lambda d: note_names[d.value])

    print(f"Named Chord: {named_chord.elements}")  # {'C', 'E', 'G'}

    # 4. map の威力: DegreeStep -> Pitch (ダミー実装)
    # 実際の実装では Key を渡したりするはずですが、クロージャで包めば map でいけます
    def to_pitch_dummy(d: DegreeStep) -> str:
        return f"Pitch({d.value + 60})"  # MIDI note的な

    pitch_chord = c_major_deg.map(to_pitch_dummy)
    print(f"Pitch Chord: {pitch_chord.elements}")

    int_chord = Chord.of(1, 2, 3)
    dsc = int_chord.map(DegreeStep).as_(DegreeStepChord)
    root = dsc.get_root()
    print(f"root: {root}")


if __name__ == "__main__":
    demo()

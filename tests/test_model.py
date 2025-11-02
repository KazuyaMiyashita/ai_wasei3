from my_project.model import Degree, Interval, IntervalAlter, IntervalStep, Key, Mode, NoteName, Pitch


def test_pitch() -> None:
    pitch = Pitch.parse("F#4")
    name = pitch.name()
    assert name == "F#4"


def test_degree() -> None:
    # ニ長調

    pitches = [
        Pitch.parse("D3"),
        Pitch.parse("E3"),
        Pitch.parse("F#3"),
        Pitch.parse("G3"),
        Pitch.parse("A3"),
        Pitch.parse("B3"),
        Pitch.parse("C#4"),
        Pitch.parse("D4"),
    ]
    key = Key(tonic=NoteName.parse("D"), mode=Mode.MAJOR)

    degrees = [Degree.from_note_name_key(p.note_name, key) for p in pitches]

    assert degrees == [
        Degree.idx_1(step=1, alter=0),
        Degree.idx_1(step=2, alter=0),
        Degree.idx_1(step=3, alter=0),
        Degree.idx_1(step=4, alter=0),
        Degree.idx_1(step=5, alter=0),
        Degree.idx_1(step=6, alter=0),
        Degree.idx_1(step=7, alter=0),
        Degree.idx_1(step=1, alter=0),
    ]

    # ハ短調・和声的短音階

    pitches = [
        Pitch.parse("C3"),
        Pitch.parse("D3"),
        Pitch.parse("Eb3"),
        Pitch.parse("F3"),
        Pitch.parse("G3"),
        Pitch.parse("Ab3"),
        Pitch.parse("B3"),
        Pitch.parse("C3"),
    ]
    key = Key(tonic=NoteName.parse("C"), mode=Mode.MINOR)

    degrees = [Degree.from_note_name_key(p.note_name, key) for p in pitches]

    assert degrees == [
        Degree.idx_1(step=1, alter=0),
        Degree.idx_1(step=2, alter=0),
        Degree.idx_1(step=3, alter=0),
        Degree.idx_1(step=4, alter=0),
        Degree.idx_1(step=5, alter=0),
        Degree.idx_1(step=6, alter=0),
        Degree.idx_1(step=7, alter=1),
        Degree.idx_1(step=1, alter=0),
    ]


def test_interval() -> None:
    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E4"))
    assert i == Interval(octave=-2, fifth=4)
    assert i.step() == IntervalStep.idx_1(3)
    assert i.alter() == IntervalAlter.MAJOR
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("D4"), target=Pitch.parse("F4"))
    assert i == Interval(octave=2, fifth=-3)
    assert i.step() == IntervalStep.idx_1(3)
    assert i.alter() == IntervalAlter.MINOR
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C5"))
    assert i == Interval(octave=1, fifth=0)
    assert i.step() == IntervalStep.idx_1(8)
    assert i.alter() == IntervalAlter.PERFECT
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Bb2"))
    assert i == Interval(octave=0, fifth=-2)
    assert i.step() == IntervalStep.idx_1(-9)
    assert i.alter() == IntervalAlter.MAJOR
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C#4"))
    assert i == Interval(octave=-4, fifth=7)
    assert i.step() == IntervalStep.idx_1(1)
    assert i.alter() == IntervalAlter.AUGMENTED
    assert i == Interval.from_step_alter(i.step(), i.alter())

    # 増1度下はモデル上は減1度上として扱う
    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Cb4"))
    assert i == Interval(octave=4, fifth=-7)
    assert i.step() == IntervalStep.idx_1(1)
    assert i.alter() == IntervalAlter.DIMINISHED
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Dbb4"))
    assert i == Interval(octave=7, fifth=-12)
    assert i.step() == IntervalStep.idx_1(2)
    assert i.alter() == IntervalAlter.DIMINISHED
    assert i == Interval.from_step_alter(i.step(), i.alter())

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("B#3"))
    assert i == Interval(octave=-7, fifth=12)
    assert i.step() == IntervalStep.idx_1(-2)
    assert i.alter() == IntervalAlter.DIMINISHED
    assert i == Interval.from_step_alter(i.step(), i.alter())


def test_name_parse() -> None:
    # --- 完全音程 (Perfect) ---
    name = "P1"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C4"))

    name = "P5"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("G4"))

    name = "P4"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("F4"))

    name = "P8"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C5"))

    # --- 長音程 (Major) ---
    name = "M2"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("D4"))

    name = "M3"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E4"))

    name = "M6"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("A4"))

    name = "M7"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("B4"))

    # --- 短音程 (Minor) ---
    name = "m2"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Db4"))

    name = "m3"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Eb4"))

    name = "m6"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Ab4"))

    name = "m7"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Bb4"))

    # --- 増音程 (Augmented) ---
    name = "A4"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("F#4"))

    name = "A1"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C#4"))

    # --- 減音程 (Diminished) ---
    name = "d5"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Gb4"))

    name = "d7"
    i = Interval.parse(name)
    assert i.name() == name
    # Bbb4 (Bのダブルフラット)
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Bbb4"))

    # --- 重増・重減 (Double) ---
    name = "AA4"
    i = Interval.parse(name)
    assert i.name() == name
    # F##4 (Fのダブルシャープ)
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("F##4"))

    name = "dd5"
    i = Interval.parse(name)
    assert i.name() == name
    # Gbb4 (Gのダブルフラット)
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Gbb4"))

    # --- 複合音程 (Compound) ---
    name = "M9"  # (M2 + P8)
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("D5"))

    name = "P11"  # (P4 + P8)
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("F5"))

    name = "m10"  # (m3 + P8)
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Eb5"))

    # --- 下方音程 (Downward) ---
    name = "-P1"
    i = Interval.parse(name)
    assert i.name() == "P1"  # マイナスの部分は消える
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C4"))

    name = "-P5"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("F3"))

    name = "-m3"
    i = Interval.parse(name)
    assert i.name() == name
    assert i.step() == IntervalStep.idx_1(-3)
    assert i.alter() == IntervalAlter.MINOR
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("A3"))

    name = "-M7"
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Db3"))

    name = "-M9"  # (M2 + P8) の下方
    i = Interval.parse(name)
    assert i.name() == name
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Bb2"))

    # --- 増1度下について ---

    name = "d1"  # 「増1度下」はモデル上「減1度上」として扱う。
    i = Interval.parse(name)
    assert i.name() == name
    assert i.step() == IntervalStep.idx_1(1)
    assert i.alter() == IntervalAlter.DIMINISHED
    assert i == Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Cb4"))


def test_interval_normalize() -> None:
    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E4"))
    a = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E4"))
    assert i.normalize() == a

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C5"))
    a = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("C4"))
    assert i.normalize() == a

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E5"))
    a = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E4"))
    assert i.normalize() == a

    i = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("E3"))  # 短6度下
    a = Interval.of(base=Pitch.parse("C4"), target=Pitch.parse("Ab4"))  # 短6度上
    assert i.normalize() == a

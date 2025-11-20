from my_project.counterpoint.model import AnnotatedMeasure, NoteAnnotation, ToneType
from my_project.model import (
    Duration,
    Interval,
    IntervalStep,
    Measure,
    Note,
    Offset,
    Pitch,
)
from my_project.util import sliding


def validate(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
    previous_cf: Pitch | None,
    current_cf: Pitch,
) -> bool:
    return _validate_interval(previous_measure, current_measure, previous_cf, current_cf) and _validate_melody(
        previous_measure, current_measure
    )


def _validate_interval(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
    previous_cf: Pitch | None,
    current_cf: Pitch,
) -> bool:
    """
    連続・並達に関するバリデーション。禁則があれば False を返す
    """

    # 冒頭小節には直前の小節が存在しないため、連続は起こり得ない。
    if previous_measure is None:
        return True
    assert previous_measure is not None
    assert previous_cf is not None

    # 2つの声部が同時に動いている場合の確認。CFが全音符なので小節を跨いだタイミングのみ。
    previous_measure_last_pitch = previous_measure.notes[-1].value
    current_measure_first_pitch = current_measure.notes[0].value
    if previous_measure_last_pitch is not None and current_measure_first_pitch is not None:
        # 連続
        if _check_is_parallel_violation(
            sequence_1=(previous_cf, current_cf),
            sequence_2=(previous_measure_last_pitch, current_measure_first_pitch),
        ):
            return False

        # 並達
        if _is_hidden_interval_violation(
            sequence_1=(previous_cf, current_cf),
            sequence_2=(previous_measure_last_pitch, current_measure_first_pitch),
        ):
            return False

    # 間接の連続の確認
    # 便宜上前の小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
    #
    # 間接の連続は、全音符1個に相当する長さが隔てられていれば許される。またもっと近くにあっても
    # 同時に打音されいるのではなく、かつ、反行している場合かいずれかの音が非和声音である場合は許される。
    #
    # すなわち、以下を満たした場合、禁則となる。
    # ある声部の Offset の差が Duration.of(4) 以下の異なる2音のうち、
    # ある他の声部の、それらの音に同時になっている2音を選び、
    # それら2声部の音が直接の連続の規則として禁則であり、
    # かつ、not (後続の5度・8度をなす音が同時に打音されていない and (反行している または いずれかの音が非和声音))

    # 簡単のため、小節と現在の小節を繋げた1小節を考え、Offset.of(4)以降のものに対して確認をする
    cf_measure: Measure[Pitch, ToneType] = Measure.of(
        Note(previous_cf, Duration.of(4), ToneType.HARMONIC_TONE),
        Note(current_cf, Duration.of(4), ToneType.HARMONIC_TONE),
    )
    realize_measure: Measure[Pitch | None, ToneType] = Measure.of(
        *[
            Note(note.value, note.duration, note.attribute.tone_type)
            for note in [*previous_measure.notes, *current_measure.notes]
        ]
    )
    for realize_current_offset, realize_current_a_note in realize_measure.offset_notes().items():
        if realize_current_offset < Offset.of(4):
            continue
        for realize_previous_offset, realize_previous_a_note in realize_measure.offset_notes().items():
            # Offset の差が Duration.of(4) 以下の異なる2音を選ぶ。
            if not (Offset.of(0) < realize_current_offset - realize_previous_offset <= Offset.of(4)):
                continue
            realize_current_pitch = realize_current_a_note.value
            realize_previous_pitch = realize_previous_a_note.value
            # (休符の場合は連続ではない)
            if realize_current_pitch is None:
                continue
            if realize_previous_pitch is None:
                continue

            # ある他の声部の、それらの音に同時になっている2音を選ぶ。
            # (現在は定旋律に対して確認しているので必ず音高が取得できる)
            cf_current_offset_note = cf_measure.at(realize_current_offset)
            cf_previous_offset_note = cf_measure.at(realize_previous_offset)
            cf_current_offset, cf_current_annotated_note = cf_current_offset_note
            _cf_previous_offset, cf_previous_annotated_note = cf_previous_offset_note
            cf_current_pitch = cf_current_annotated_note.value
            cf_previous_pitch = cf_previous_annotated_note.value

            # 直接の連続の規則として連続である
            is_parallel_violation = _check_is_parallel_violation(
                sequence_1=(cf_previous_pitch, cf_current_pitch),
                sequence_2=(realize_previous_pitch, realize_current_pitch),
            )

            # 後続の5度・8度をなす音が同時に打音されている
            has_following_notes_same_offset = cf_current_offset == realize_current_offset

            # 2声が反行している
            is_contrary_motion = _check_is_contrary_motion(
                sequence_1=(cf_previous_pitch, cf_current_pitch),
                sequence_2=(realize_previous_pitch, realize_current_pitch),
            )

            # いずれかの音が非和声音
            # (現在は定旋律に対して確認しているので実施声部のみを確認する)
            non_harmonic_tone_exists = (
                realize_current_a_note.attribute != ToneType.HARMONIC_TONE
                or realize_previous_a_note.attribute != ToneType.HARMONIC_TONE
            )

            if is_parallel_violation and not (
                not has_following_notes_same_offset and (is_contrary_motion or non_harmonic_tone_exists)
            ):
                return False

    return True


def _check_is_parallel_motion(sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
    """
    2つの旋律の進行が並行しているかどうかを返す
    """
    s1_start, s1_end = sequence_1
    s2_start, s2_end = sequence_2

    # どちらかが動いていない場合は並行ではない
    if s1_start == s1_end or s2_start == s2_end:
        return False

    dir1_up = s1_end.num() > s1_start.num()
    dir2_up = s2_end.num() > s2_start.num()
    return dir1_up == dir2_up


def _check_is_contrary_motion(sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
    """
    2つの旋律の進行が反行しているかどうかを返す
    """
    s1_start, s1_end = sequence_1
    s2_start, s2_end = sequence_2

    # どちらかが動いていない場合は反行していない
    if s1_start == s1_end or s2_start == s2_end:
        return False

    dir1_up = s1_end.num() > s1_start.num()
    dir2_up = s2_end.num() > s2_start.num()
    return dir1_up != dir2_up


def _check_is_parallel_violation(sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
    """
    連続5度・8度の禁則が含まれているかどうか。
    並行・反行のいずれも禁則とする。(斜行と同時保留はOK)
    """
    if not (_check_is_parallel_motion(sequence_1, sequence_2) or _check_is_contrary_motion(sequence_1, sequence_2)):
        return False

    first_interval_normalized = Interval.of(sequence_1[0], sequence_2[0]).normalize()
    second_interval_normalized = Interval.of(sequence_1[1], sequence_2[1]).normalize()

    # 連続8度(1度)
    if first_interval_normalized == second_interval_normalized == Interval.parse("P1"):
        return True

    # 連続5度(完全-完全)
    if first_interval_normalized == second_interval_normalized == Interval.parse("P5"):
        return True

    # 連続5度(減-完全)
    if first_interval_normalized == Interval.parse("d5") and second_interval_normalized == Interval.parse("P5"):
        return True

    # 連続5度(完全-減) 3声からは許されるが、現在は2声のみ扱うので禁則扱い
    if first_interval_normalized == Interval.parse("P5") and second_interval_normalized == Interval.parse("d5"):
        return True

    return False


def _is_hidden_interval_violation(sequence_1: tuple[Pitch, Pitch], sequence_2: tuple[Pitch, Pitch]) -> bool:
    """
    並達5度・8度の禁則が含まれているかどうか
    """
    if not _check_is_parallel_motion(sequence_1, sequence_2):
        return False

    second_interval_normalized = Interval.of(sequence_1[1], sequence_2[1]).normalize()
    if second_interval_normalized in [Interval.parse("P1"), Interval.parse("P5")]:
        return True
    else:
        return False


# --


def _validate_melody(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
) -> bool:
    """
    旋律に関するバリデーション

    DONE:
    - 分散和音をしない
    - 3音符で形成される7度・9度は順次進行を含める

    優先して実装したい:
    - 完全8度の跳躍はできるだけその前後に反対方向の進行を伴う
    - 旋律の対称系や繰り返し(特に同一音への3度続く回帰)

    後回し?:
    - できるだけ非順次進行を避ける(どの程度?)
    - 小節線をはさんだ非順次進行を避ける(どの程度?)
    - 3,4個の音符で形成される増4度は同方向の順次進行で先行または後続させる
    """
    return (
        _validate_melody_arpeggiio(previous_measure, current_measure)
        and _validate_melody_arpeggiio_extra(previous_measure, current_measure)
        and _validate_melody_interval_7_9(previous_measure, current_measure)
        and True
    )  # TODO


def _validate_melody_arpeggiio(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
) -> bool:
    """
    分散和音のバリデーション。旋律が分散和音の形になっているときFalseを返す

    TODO: 反転の分散和音はOKとしている
    """
    # 前の小節がもしあれば最後の2音を取得し、現在の小節と繋げた音列を作成
    pitches: list[Pitch] = [
        note.value for note in _extended_note_buffer(previous_measure, current_measure, 2) if note.value is not None
    ]

    arpeggiio_steps_list = [
        [IntervalStep.idx_1(3), IntervalStep.idx_1(5)],  # ドミソ
        [IntervalStep.idx_1(-3), IntervalStep.idx_1(-5)],
        [IntervalStep.idx_1(3), IntervalStep.idx_1(6)],  # ミソド
        [IntervalStep.idx_1(-3), IntervalStep.idx_1(-6)],
        [IntervalStep.idx_1(4), IntervalStep.idx_1(6)],  # ソドミ
        [IntervalStep.idx_1(-4), IntervalStep.idx_1(-6)],  # ソドミ
    ]

    for ps in sliding(pitches, window_size=3):
        base, p1, p2 = ps
        intervals = [p1 - base, p2 - base]
        # NOTE: interval を normalize すると [C4 A3 A4] が C4に対して3度・6度と判定されてしまう。
        # NOTE: 複音程は旋律の規則としてそもそも選ばれないので無視してよい。例えば [C4 *E4 *G5] の10度は選ばれない。
        # NOTE: 以下の steps を sort すると、反転の分散和音が判定に含まれる(その場合 arpeggiio_steps_list は上方だけでよい)
        steps = [i.step() for i in intervals]
        if steps in arpeggiio_steps_list:
            return False

    return True


def _validate_melody_arpeggiio_extra(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
) -> bool:
    """
    特殊な形態のいくつかの分散和音を禁止する。

    - (A-1): [C4 G4 C5], [G4 C5 G5], [G4, C4, C5] といった第3音を伴わない分散和音(反転なし)

    ---
    以下も考えられるが、現在は認めている

    - (A-2): [G4, C4, C5] といった第3音を伴わない分散和音(反転あり)
      - (しかしこれは [G4 A4 *G4 *C4 | *C5 B4 A4 G4] といった認めたくなるケースがある
    - (B)] [C4 C5 C4] といったオクターブの移動
      - (しかしこれは困難な場合には例外として許される)
      - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
    - (C): [C4 G4 C4 C4] や [C5 G4 C5 G4] といった4度・5度の反復
      - (できるだけ非順次進行を避けるといった規則で対応されるかもしれない)
    """
    pitches: list[Pitch] = [
        note.value for note in _extended_note_buffer(previous_measure, current_measure, 2) if note.value is not None
    ]

    arpeggiio_steps_list = [
        [IntervalStep.idx_1(5), IntervalStep.idx_1(8)],  # [C4 G4 C5]
        [IntervalStep.idx_1(-5), IntervalStep.idx_1(-8)],
        [IntervalStep.idx_1(4), IntervalStep.idx_1(8)],  # [G4 C5 G5]
        [IntervalStep.idx_1(-4), IntervalStep.idx_1(-8)],
    ]

    for ps in sliding(pitches, window_size=3):
        base, p1, p2 = ps
        intervals = [p1 - base, p2 - base]
        steps = [i.step() for i in intervals]
        if steps in arpeggiio_steps_list:
            return False

    return True


def _validate_melody_interval_7_9(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
) -> bool:
    """
    3音符で形成される7度・9度は順次進行を含める必要がある。そうなっていなければFalseを返す
    (9度より大きい音程になることは別の規則で禁止されそうだが、この規則で扱う)
    """

    # 前の小節がもしあれば最後の2音を取得し、現在の小節と繋げた音列を作成
    pitches: list[Pitch] = [
        note.value for note in _extended_note_buffer(previous_measure, current_measure, 2) if note.value is not None
    ]
    for ps in sliding(pitches, window_size=3):
        p1, p2, p3 = ps
        step_1_3 = (p1 - p3).abs().step()
        if step_1_3 == IntervalStep.idx_1(7) or step_1_3 > IntervalStep.idx_1(9):
            step_1_2 = (p1 - p2).abs().step()
            step_2_3 = (p2 - p3).abs().step()
            if step_1_2 == IntervalStep.idx_1(2) or step_2_3 == IntervalStep.idx_1(2):
                continue
            else:
                return False

    return True


def _extended_note_buffer(
    previous_measure: AnnotatedMeasure | None,
    current_measure: AnnotatedMeasure,
    num: int,
) -> list[Note[Pitch | None, NoteAnnotation]]:
    """
    前の小節の末尾から num 音取得し、 note_buffer と繋げたリストを返す
    """
    notes: list[Note[Pitch | None, NoteAnnotation]] = []
    if previous_measure is not None:
        notes.extend([note for note in previous_measure.notes[-num:]])
    notes.extend(current_measure.notes)
    return notes

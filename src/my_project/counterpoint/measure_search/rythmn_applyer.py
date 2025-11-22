from my_project.counterpoint.measure_search.measure_step_sequence import (
    AbstractMeasureStepSequence,
    MeasureStepSequence,
)
from my_project.counterpoint.model import MeasureRythmnPattern, NoteAnnotation, ToneType
from my_project.model import Duration, IntervalStep, Measure, Melody, Note, Offset
from my_project.util import sliding


def try_apply_rythmn(
    step_sequence: MeasureStepSequence, rythmn_pattern: MeasureRythmnPattern
) -> Melody[Note[IntervalStep | None, NoteAnnotation]] | None:
    """
    MeasureStepSequence に対して MeasureRythmnPattern を適用できるかを判断する。
    可能なら適用した Melody を返し、不可能なら None を返す。
    """

    if step_sequence.num_notes_in_measure() != rythmn_pattern.measure_rythmn().num_durations():
        return None
    if step_sequence.is_tied_to_next_measure_required() != rythmn_pattern.measure_rythmn().is_next_tied:
        return None

    melody: Melody[Note[IntervalStep | None, NoteAnnotation]] = apply_rythmn(step_sequence, rythmn_pattern).melody

    # 次にタイで繋ぐ必要がある場合、最後の音は3拍目の2分音符で、和声音である必要がある。
    if step_sequence.is_tied_to_next_measure_required():
        offset_notes = melody.offset_notes()
        note_at_beat3 = offset_notes.get(Offset.idx_1(3))
        if (
            not note_at_beat3
            or note_at_beat3.duration != Duration.of(2)
            or note_at_beat3 is not melody.notes[-1]
            or note_at_beat3.attribute.tone_type
            != ToneType.HARMONIC_TONE  # 和声音である必要があるというのはここでやることか？
        ):
            return None

    # 掛留音
    if melody.notes[0].attribute.tone_type == ToneType.SUSPENDED_TONE:
        # 掛留音から始まる場合、リズムは前にタイが付いていないといけない
        if not rythmn_pattern.measure_rythmn().is_previous_tied:
            return None

        # 掛留音は3拍目に解決しなければならない
        offset3_note = melody.offset_notes().get(Offset.idx_1(3))
        if not offset3_note:
            return None
        if not offset3_note.attribute.tone_type == ToneType.HARMONIC_TONE:
            return None

        # 3拍目より前に解決してはいけない
        for offset, note in melody.offset_notes().items():
            if Offset.of(0) < offset < Offset.idx_1(3):
                if note.attribute.tone_type == ToneType.HARMONIC_TONE:
                    return None

    # 八分音符の周りに非順次進行を含めない
    # 次の小節の音も考慮するため、リストにダミーのノートを追加してループする
    next_note_dummy: Note[IntervalStep | None, NoteAnnotation] = Note(
        value=step_sequence.next_measure_step,
        duration=Duration.of(1),
        attribute=NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
    )
    notes_to_check: list[Note[IntervalStep | None, NoteAnnotation]] = [*list(melody.notes), next_note_dummy]

    for current_note, next_note in sliding(notes_to_check, 2):
        if current_note.value is not None and current_note.duration == Duration.of(1, 2):
            if next_note is not None and next_note.value is not None:
                if (current_note.value - next_note.value).abs() >= IntervalStep.idx_1(3):
                    return None

    return melody


def apply_rythmn[T](
    step_sequence: AbstractMeasureStepSequence[T], rythmn_pattern: MeasureRythmnPattern
) -> Measure[Note[T | None, NoteAnnotation]]:
    """
    任意の AbstractMeasureStepSequence に対して MeasureRythmnPattern を適用した Melody を返す。
    ここではリズムの検証は行われない。事前に検証済みの内容に関して利用すること。
    """
    rythmn = rythmn_pattern.measure_rythmn()
    durations = rythmn.durations

    notes: list[Note[T | None, NoteAnnotation]] = []
    if rythmn.init_rest_duration > Duration.of(0):
        notes.append(
            Note(
                value=None,
                duration=rythmn.init_rest_duration,
                attribute=NoteAnnotation(is_tied_start=False, tone_type=ToneType.HARMONIC_TONE),
            )
        )

    for i, (note, duration) in enumerate(zip(step_sequence.measure.notes, durations)):
        is_last_note = i == step_sequence.num_notes_in_measure() - 1
        notes.append(
            Note(
                value=note.value,
                duration=duration,
                attribute=NoteAnnotation(
                    is_tied_start=step_sequence.is_tied_to_next_measure_required() and is_last_note,
                    tone_type=note.attribute,
                ),
            )
        )
    return Measure.of(*notes)

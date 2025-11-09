from dataclasses import dataclass
from enum import Enum
from fractions import Fraction

from my_project.model import (
    Duration,
    Key,
    Measure,
    Mode,
    Note,
    NoteName,
    Offset,
    PartId,
    Pitch,
    TimeSignature,
)

KEY = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
TIME_SIGNATURE = TimeSignature(4, Fraction(1))
NOTES_IN_MEASURE = 4
MEASURE_TOTAL_DURATION = Duration.of(NOTES_IN_MEASURE)
CF_PART_ID = PartId.BASS
REALIZE_PART_ID = PartId.SOPRANO


class RythmnType(Enum):
    """
    課題の実施で利用されるリズム
    """

    # 四部音符
    QUATER_NOTE = 1
    # 二部音符
    HALF_NOTE = 2
    # 全音符
    WHOLE_NOTE = 3

    def note_duration(self) -> Duration:
        match self:
            case RythmnType.QUATER_NOTE:
                return Duration.of(1)
            case RythmnType.HALF_NOTE:
                return Duration.of(2)
            case RythmnType.WHOLE_NOTE:
                return Duration.of(4)


class ToneType(Enum):
    """
    探索した音に対し、その音が和声音か非和声音かを記録しておく必要がある。そのための音の種別
    """

    # 和声音。冒頭の休符も便宜上和声音として扱う。
    HARMONIC_TONE = 1
    # 経過音
    PASSING_TONE = 2
    # 刺繍音
    NEIGHBOR_TONE = 3


@dataclass(frozen=True)
class AnnotatedNote:
    note: Note
    tone_type: ToneType


@dataclass(frozen=True)
class AnnotatedMeasure:
    """ToneType で注釈付けされた音符のリストを持つ小節"""

    annotated_notes: list[AnnotatedNote]

    def to_measure(self) -> Measure:
        """Score 生成のために model.Measure に変換する"""
        return Measure([an.note for an in self.annotated_notes])

    def offset_notes(self) -> dict[Offset, AnnotatedNote]:
        """
        この小節のannotated_notesのオフセットとAnnotatedNoteの組みに変換してdictで返す
        """
        result: dict[Offset, AnnotatedNote] = {}
        current_offset = Offset.of(0)

        for annotated_note in self.annotated_notes:
            result[current_offset] = annotated_note
            current_offset = current_offset.add_duration(annotated_note.note.duration)

        return result

    def offset_note_at(self, offset: Offset) -> tuple[Offset, AnnotatedNote] | None:
        """
        この小節の Offset の時刻に鳴っている音の、開始した Offset と AnnotatedNote を返す。
        その Offset の時に休符であれば None, 小節の範囲外の Offset を指定した場合は例外
        """
        current_offset = Offset.of(0)

        for annotated_note in self.annotated_notes:
            note_duration = annotated_note.note.duration
            note_end_offset = current_offset.add_duration(note_duration)
            if current_offset <= offset < note_end_offset:
                if annotated_note.note.pitch is None:
                    return None
                else:
                    return (current_offset, annotated_note)

            current_offset = note_end_offset

        raise ValueError(
            f"Offset {offset.value} is out of bounds for this measure. Total duration is {current_offset.value}."
        )

    def pitch_at(self, offset: Offset) -> Pitch | None:
        """
        この小節のOffsetの時刻に鳴っているPitchを返す。
        そのOffsetの時に休符であればNone, 小節の範囲外のOffsetを指定した場合は例外

        offset_note_at
        """
        offset_note = self.offset_note_at(offset)
        if offset_note is None:
            return None
        return offset_note[1].note.pitch

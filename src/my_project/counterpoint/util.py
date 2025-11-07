from my_project.counterpoint.model import (
    AnnotatedNote,
    ToneType,
)
from my_project.model import (
    Duration,
    Note,
    Pitch,
)


def make_annotated_note(pitch: Pitch | None, tone_type: ToneType, duration: Duration) -> AnnotatedNote:
    """Pitch, ToneType, Duration から AnnotatedNote を作成するヘルパー"""
    return AnnotatedNote(Note(pitch, duration), tone_type)

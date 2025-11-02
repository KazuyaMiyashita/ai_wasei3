from fractions import Fraction

from my_project.model import Duration, Mode, Note, NoteName, PartId, Pitch, Score


def score_to_lilypond(score: Score) -> str:
    """
    与えられたScoreオブジェクトをもとに、LilyPond形式の文字列を作成する
    ピアノ譜を用い、バスはへ音記号で音符の棒を下向き、テノールはヘ音記号で棒を上向き、アルトはト音記号で棒を下向き、ソプラノはト音記号で棒を上向きとする。
    """
    # Key signature
    tonic_name = note_name_to_lilypond(score.key.tonic)
    mode_name = "major" if score.key.mode == Mode.MAJOR else "minor"
    key_string = f"\\key {tonic_name} \\{mode_name}"
    time_signature_string = score.time_signature.name()

    # Notes for each part
    soprano_notes = ""
    alto_notes = ""
    tenor_notes = ""
    bass_notes = ""

    for part in score.parts:
        # パート内の全ての音符を連結する
        notes_str = " ".join(note_to_lilypond(note) for measure in part.measures for note in measure.notes)
        if part.part_id == PartId.SOPRANO:
            soprano_notes = notes_str
        elif part.part_id == PartId.ALTO:
            alto_notes = notes_str
        elif part.part_id == PartId.TENOR:
            tenor_notes = notes_str
        elif part.part_id == PartId.BASS:
            bass_notes = notes_str

    return f"""\\version "2.24.4"

keyTime = {{ {key_string} \\time {time_signature_string} }}

SopMusic   = {{ {soprano_notes} }}
AltoMusic  = {{ {alto_notes} }}
TenorMusic = {{ {tenor_notes} }}
BassMusic  = {{ {bass_notes} }}

\\score {{
  \\new PianoStaff <<
    \\new Staff <<
      \\clef "treble"
      \\new Voice = "Sop"  {{ \\voiceOne \\keyTime \\SopMusic }}
      \\new Voice = "Alto" {{ \\voiceTwo \\AltoMusic }}
    >>
    \\new Staff <<
      \\clef "bass"
      \\new Voice = "Tenor" {{ \\voiceOne \\keyTime \\TenorMusic }}
      \\new Voice = "Bass"  {{ \\voiceTwo \\BassMusic }}
    >>
  >>
}}
"""


def note_to_lilypond(note: Note) -> str:
    pitch_rest_str = pitch_to_lilypond(note.pitch) if note.pitch else "r"
    duration_str = duration_to_lilypond(note.duration)
    return f"{pitch_rest_str}{duration_str}"


def pitch_to_lilypond(pitch: Pitch) -> str:
    """
    PitchオブジェクトをLilyPondの音符文字列に変換する
    """
    _, _, octave = pitch.internal_pitch_notation()

    lp_note = note_name_to_lilypond(pitch.note_name)

    # Octave marks
    # Middle C (C4) is c' in LilyPond.
    # Octave 3 is the base octave (no mark).
    if octave >= 3:
        lp_octave = "'" * (octave - 3)
    else:  # octave < 3
        lp_octave = "," * (3 - octave)

    return lp_note + lp_octave


def note_name_to_lilypond(note_name: NoteName) -> str:
    step, alter = note_name.internal_pitch_notation()

    lp_note = step.lower()

    if alter > 0:
        lp_note += "is" * alter
    elif alter < 0:
        if alter == -1:
            if lp_note == "a":
                lp_note = "as"
            elif lp_note == "e":
                lp_note = "es"
            else:
                lp_note += "es"
        else:  # alter < -1
            if lp_note in "ae":
                lp_note += "s" * (-alter)
            else:
                lp_note += "es" * (-alter)

    return lp_note


def duration_to_lilypond(duration: Duration) -> str:
    val = duration.value
    if val == 0:
        return ""

    base_duration_map = {
        Fraction(4, 1): "1",
        Fraction(2, 1): "2",
        Fraction(1, 1): "4",
        Fraction(1, 2): "8",
        Fraction(1, 4): "16",
        Fraction(1, 8): "32",
    }

    # Handle common dotted notes for cleaner output
    for base_val, ly_dur in base_duration_map.items():
        if val == base_val:
            return ly_dur
        if val == base_val * Fraction(3, 2):
            return ly_dur + "."
        if val == base_val * Fraction(7, 4):
            return ly_dur + ".."

    # Find closest base duration for scaling
    closest_base = min(base_duration_map.keys(), key=lambda base: abs(val - base))

    ly_base = base_duration_map[closest_base]
    multiplier = val / closest_base

    if multiplier.denominator == 1:
        return f"{ly_base}*{multiplier.numerator}"
    else:
        return f"{ly_base}*{multiplier.numerator}/{multiplier.denominator}"

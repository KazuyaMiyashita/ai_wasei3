import re

from my_project.harmony import Harmony
from my_project.model import Key, Mode, Pitch


def write(harmonies: list[Harmony], key: Key) -> str:
    """
    与えられた和音(Harmony)のリストと調をもとに、LilyPond形式の文字列を作成する
    ピアノ譜を用い、バスはへ音記号で音符の棒を下向き、テノールはヘ音記号で棒を上向き、アルトはト音記号で棒を下向き、ソプラノはト音記号で棒を上向きとする。
    音価は全て2分音符とし、拍子は2/2とする。
    """
    # Key signature
    tonic_name, _ = key.tonic.get_spelling()
    mode_name = "major" if key.mode == Mode.MAJOR else "minor"
    key_string = f"\\key {tonic_name.lower()} \\{mode_name}"

    # Notes for each part
    soprano_notes = " ".join([f"{pitch_to_lilypond(h.soprano)}2" for h in harmonies])
    alto_notes = " ".join([f"{pitch_to_lilypond(h.alto)}2" for h in harmonies])
    tenor_notes = " ".join([f"{pitch_to_lilypond(h.tenor)}2" for h in harmonies])
    bass_notes = " ".join([f"{pitch_to_lilypond(h.bass)}2" for h in harmonies])

    return f"""\\version "2.24.4"

keyTime = {{ {key_string} \\time 2/2 }}

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


def pitch_to_lilypond(pitch: Pitch) -> str:
    """
    PitchオブジェクトをLilyPondの音符文字列に変換する
    """
    # pitch.name()からオクターブ番号を取得

    match = re.search(r"(\d+)$", pitch.name())
    if not match:
        raise ValueError(f"Could not parse octave from pitch name: {pitch.name()}")
    octave = int(match.group(1))

    step, alter = pitch.note_name.get_spelling()

    lp_note = step.lower()

    if alter > 0:
        lp_note += "is" * alter
    elif alter < 0:
        # LilyPondのオランダ語音名では A♭ は as, E♭ は es となる
        if alter == -1:
            if lp_note == "a":
                lp_note = "as"
            elif lp_note == "e":
                lp_note = "es"
            else:
                lp_note += "es"
        else:  # alter < -1
            # ダブルフラットなど
            if lp_note in "ae":
                lp_note += "s" * (-alter)
            else:
                lp_note += "es" * (-alter)

    # Octave marks
    # Middle C (C4) is c' in LilyPond.
    # Octave 3 is the base octave (no mark).
    if octave >= 3:
        lp_octave = "'" * (octave - 3)
    else:  # octave < 3
        lp_octave = "," * (3 - octave)

    return lp_note + lp_octave

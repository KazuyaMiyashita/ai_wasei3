import re
from typing import Final

# --- Constants ---

_STEP_TO_BASE_FIFTH: Final[dict[str, int]] = {"C": 0, "D": 2, "E": 4, "F": -1, "G": 1, "A": 3, "B": 5}
_BASE_FIFTH_TO_STEP: Final[dict[int, str]] = {v: k for k, v in _STEP_TO_BASE_FIFTH.items()}
_STEP_TO_BASE_OCTAVE: Final[dict[str, int]] = {"C": 0, "D": -1, "E": -2, "F": 1, "G": 0, "A": -1, "B": -2}


# --- NoteName Logic ---


def parse_note_name_to_value(name: str) -> int:
    """ "C#", "Bb" -> int (value)"""
    pattern = r"^([A-G])([#b]*)$"
    match = re.fullmatch(pattern, name)
    if not match:
        raise ValueError(f"Invalid note name format: {name}")

    step_str, accidental_str = match.groups()
    alter = accidental_str.count("#") - accidental_str.count("b")
    base_fifth = _STEP_TO_BASE_FIFTH[step_str]
    return base_fifth + alter * 7


def note_name_to_string_parts(value: int) -> tuple[str, int]:
    """int (value) -> ("C", 1) for C#"""
    # F, C, G, D, A, E, B の順で、7で割った余りが一致するものを探す
    for base_fifth in [-1, 0, 1, 2, 3, 4, 5]:
        if (value - base_fifth) % 7 == 0:
            alter = (value - base_fifth) // 7
            step = _BASE_FIFTH_TO_STEP[base_fifth]
            return step, alter
    raise RuntimeError("unreachable")


def format_note_name(value: int) -> str:
    step, alter = note_name_to_string_parts(value)
    return f"{step}{'#' * alter if alter > 0 else 'b' * -alter}"


# --- Pitch Logic ---


def parse_pitch_to_values(name: str) -> tuple[int, int]:
    """ "C#4" -> (octave_val, note_name_val)"""
    pattern = r"^([A-G][#b]*)(\d+)$"
    match = re.fullmatch(pattern, name)
    if not match:
        raise ValueError(f"Invalid pitch name format: {name}")

    note_name_str, octave_str = match.groups()
    note_name_val = parse_note_name_to_value(note_name_str)
    octave_num = int(octave_str)

    step, alter = note_name_to_string_parts(note_name_val)
    base_octave = _STEP_TO_BASE_OCTAVE[step]

    # 計算式: octave_val = base_octave + alter * -4 + octave_num - 4
    octave_val = base_octave + alter * -4 + octave_num - 4
    return octave_val, note_name_val


def pitch_to_notation_octave(octave_val: int, note_name_val: int) -> int:
    """Pitchの内部値から、"C4"の"4"にあたる数字を計算する"""
    step, alter = note_name_to_string_parts(note_name_val)
    base_octave = _STEP_TO_BASE_OCTAVE[step]
    # 逆算: notation_octave = octave_val - base_octave + 4 * alter + 4
    return octave_val - base_octave + 4 * alter + 4


def format_pitch(octave_val: int, note_name_val: int) -> str:
    name_str = format_note_name(note_name_val)
    not_oct = pitch_to_notation_octave(octave_val, note_name_val)
    return f"{name_str}{not_oct}"


# --- Interval Logic ---


def parse_interval_to_components(name: str) -> tuple[int, int]:
    """ "P1", "-m3" -> (step_val, alter_val)"""
    pattern = re.compile(r"^([-]?)([PMm]|A+|d+)(\d+)$")
    match = pattern.match(name)
    if not match:
        raise ValueError(f"Invalid interval name format: '{name}'")

    sgn_str, qual_str, num_str = match.groups()
    num = int(num_str)
    if num < 1:
        raise ValueError(f"Interval degree must be 1 or greater, got {num}")

    sgn = -1 if sgn_str == "-" else 1
    step_val = (num - 1) * sgn

    if qual_str == "P":
        alter_val = 0
    elif qual_str == "M":
        alter_val = 1
    elif qual_str == "m":
        alter_val = -1
    elif qual_str.startswith("A"):
        alter_val = len(qual_str) + 1
    elif qual_str.startswith("d"):
        alter_val = -(len(qual_str) + 1)
    else:
        alter_val = 0  # fallback

    return step_val, alter_val


def interval_from_step_alter(s: int, a: int) -> tuple[int, int]:
    """step, alter -> (octave, fifth)"""
    # 1. f_class (f % 7) は s から決まる (f ≡ 2s (mod 7))
    f_class = (2 * s) % 7
    step_sgn = -1 if s < 0 else 1
    f_base_sharp = (f_class - 6) % 7 + 6
    f_base_flat = (f_class - 2) % 7 - 12
    f = 0

    if a == 0:  # Perfect
        f_map = {0: 0, 1: 1, 6: -1}
        if f_class not in f_map:
            raise ValueError(f"Invalid Perfect interval step: {s}")
        f = f_map[f_class]
    elif a == 1:  # Major
        if f_class not in [2, 3, 4, 5]:
            raise ValueError(f"Invalid Major interval step: {s}")
        f = f_class if step_sgn == 1 else f_class - 7
    elif a == -1:  # Minor
        if f_class not in [2, 3, 4, 5]:
            raise ValueError(f"Invalid Minor interval step: {s}")
        f = f_class - 7 if step_sgn == 1 else f_class
    elif a >= 2:  # Augmented
        k = a - 2
        f = (f_base_sharp + 7 * k) if step_sgn == 1 else (f_base_flat - 7 * k)
    elif a <= -2:  # Diminished
        k = -a - 2
        f = (f_base_flat - 7 * k) if step_sgn == 1 else (f_base_sharp + 7 * k)

    residual = s - 4 * f
    return residual // 7, f


def calculate_interval_alter(fifth: int, step_val: int) -> int:
    abs_fifth = abs(fifth)
    step_sgn = -1 if step_val < 0 else 1
    fifth_sgn = -1 if fifth < 0 else 1
    sgn = step_sgn * fifth_sgn

    if abs_fifth <= 1:
        return 0
    elif abs_fifth <= 5:
        return sgn * 1
    else:
        return sgn * (2 + ((abs_fifth - 6) // 7))


def format_interval(octave: int, fifth: int) -> str:
    step_val = 4 * fifth + 7 * octave
    sgn = "" if step_val >= 0 else "-"
    num = f"{abs(step_val) + 1}"

    alter_val = calculate_interval_alter(fifth, step_val)
    if alter_val == 0:
        alph = "P"
    elif alter_val == 1:
        alph = "M"
    elif alter_val == -1:
        alph = "m"
    elif alter_val >= 2:
        alph = "A" * (alter_val - 1)
    else:
        alph = "d" * (-alter_val - 1)

    return f"{sgn}{alph}{num}"


# --- Key / Scale Logic ---


def calculate_scale_pitch(key_tonic_val: int, mode_offset: int, interval_step_val: int) -> tuple[int, int]:
    """
    指定されたKeyとIntervalStep(何度音か)から、対象のPitch(octave, note_name)を計算する。
    基準はC4。
    """
    # 1. Key Signature
    signature_num = key_tonic_val + mode_offset

    # 2. Determine alter
    def get_alters(num: int) -> list[int]:
        # Returns number of sharps/flats for [F, C, G, D, A, E, B]
        q, r = num // 7, num % 7
        return [q + 1 if i < r else q for i in range(7)]

    # Map [F, C, G, D, A, E, B] to steps from C: [3, 0, 4, 1, 5, 2, 6]
    steps_map = [3, 0, 4, 1, 5, 2, 6]
    target_alter = 0

    inv_step = interval_step_val % 7

    current_alters = get_alters(signature_num)
    for idx, step_from_c in enumerate(steps_map):
        if step_from_c == inv_step:
            target_alter = current_alters[idx]
            break

    # 3. Calculate C Major Pitch (Base)
    # Order: C, D, E, F, G, A, B
    c_major_fifths = [0, 2, 4, -1, 1, 3, 5]

    # Base octave values for C4-B4 based on the specific Pitch definition
    # e.g., D4 maps to octave=-1
    c_major_base_octaves = [0, -1, -2, 1, 0, -1, -2]

    base_fifth = c_major_fifths[inv_step]
    base_octave = c_major_base_octaves[inv_step]

    # Add Octave shift from interval step (e.g., 8th note -> +1 octave)
    octave_shift = interval_step_val // 7

    # 4. Apply alter
    # Equivalent to (Interval.A1 * alter).
    # Interval.A1 is internally defined as (octave=-4, fifth=7).
    final_fifth = base_fifth + (7 * target_alter)
    final_octave = base_octave + octave_shift + (-4 * target_alter)

    return final_octave, final_fifth


# --- Degree Logic ---


def calculate_degree_components(note_val: int, key_tonic_val: int, mode_offset: int) -> tuple[int, int]:
    """-> (step_val, alter_val)"""
    r = note_val - key_tonic_val
    # alter (a) calculation
    alter_val = round((r - mode_offset - 2) / 7)
    r_0 = r - (7 * alter_val)
    step_val = (4 * r_0) % 7
    return step_val, alter_val


def calculate_degree_note_name(step_val: int, alter_val: int, key_tonic_val: int, mode_offset: int) -> int:
    """-> note_name_value"""
    # r_0 calculation
    r_0_candidate = (2 * step_val) % 7
    r_0 = 0
    # Range check -1+m <= r0 <= 5+m
    lower = -1 + mode_offset
    upper = 5 + mode_offset

    if lower <= r_0_candidate <= upper:
        r_0 = r_0_candidate
    elif lower <= r_0_candidate - 7 <= upper:
        r_0 = r_0_candidate - 7
    elif lower <= r_0_candidate + 7 <= upper:
        r_0 = r_0_candidate + 7
    else:
        raise ValueError("Cannot find r_0")

    r = r_0 + 7 * alter_val
    return key_tonic_val + r

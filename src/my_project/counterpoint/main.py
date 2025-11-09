import argparse

from my_project.counterpoint.global_state import generate
from my_project.counterpoint.model import RythmnType
from my_project.lilypond_writer import score_to_lilypond
from my_project.model import PartId, Pitch


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Find counterpoint for a given bass sequence.",
        epilog=("Example usage:\nuv run python -m my_project.main_counterpoint --cf C4 A3 G3 E3 F3 A3 G3 E3 D3 C3"),
    )

    parser.add_argument("--cf", nargs="+", required=True, help="A space-separated list of bass notes (e.g., C4 A3 F3)")
    parser.add_argument(
        "--rythmn",
        type=str,
        default="quater",
        choices=["quater", "half", "whole"],
        help="Rythmn type for counterpoint generation (e.g., quater, half, whole). Defaults to quater.",
    )

    parser.add_argument(
        "--debug",
        action="store_true",
        help="Enable debug mode (sets 'debug' to True)",
    )

    args = parser.parse_args()

    try:
        cantus_firmus: list[Pitch] = [Pitch.parse(p_str) for p_str in args.cf]
    except Exception as e:
        parser.error(f"Failed to parse bass sequence '{args.cf}'. Error: {e}")
        return

    rythmn_type: RythmnType
    if args.rythmn == "quater":
        rythmn_type = RythmnType.QUATER_NOTE
    elif args.rythmn == "half":
        rythmn_type = RythmnType.HALF_NOTE
    elif args.rythmn == "whole":
        rythmn_type = RythmnType.WHOLE_NOTE
    else:
        # This case should ideally not be reached due to 'choices' in argparse
        rythmn_type = RythmnType.QUATER_NOTE

    if args.debug:
        for i, solved in enumerate(generate(cantus_firmus, rythmn_type=rythmn_type)):
            mesaures = next(part.measures for part in solved.parts if part.part_id == PartId.SOPRANO)
            pitches = [note.pitch.name() if note.pitch else "None" for measure in mesaures for note in measure.notes]
            print(f"試行 {i=}, {pitches}")
            # lily_str = score_to_lilypond(solved)
            # print(lily_str)
    else:
        solved = next(generate(cantus_firmus, rythmn_type=rythmn_type))
        lily_str = score_to_lilypond(solved)
        print(lily_str)


if __name__ == "__main__":
    main()

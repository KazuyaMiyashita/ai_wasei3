import argparse

from my_project.counterpoint import RythmnType, generate
from my_project.lilypond_writer import score_to_lilypond
from my_project.model import Pitch


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
        choices=["quater", "half"],
        help="Rythmn type for counterpoint generation (e.g., quater, half). Defaults to quater.",
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
    else:
        # This case should ideally not be reached due to 'choices' in argparse
        rythmn_type = RythmnType.QUATER_NOTE

    solved = next(generate(cantus_firmus, rythmn_type=rythmn_type))
    lily_str = score_to_lilypond(solved)
    print(lily_str)
    # for i, solved in enumerate(generate(cantus_firmus)):
    #     print(f"試行: {i + 1}")
    #     lily_str = score_to_lilypond(solved)
    #     print(lily_str)


if __name__ == "__main__":
    main()

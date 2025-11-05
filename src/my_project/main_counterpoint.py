import argparse

from my_project.counterpoint import generate
from my_project.lilypond_writer import score_to_lilypond
from my_project.model import (
    Pitch,
)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Find counterpoint for a given bass sequence.",
        epilog=("Example usage:\nuv run python -m my_project.main_counterpoint --cf C4 A3 G3 E3 F3 A3 G3 E3 D3 C3"),
    )

    parser.add_argument("--cf", nargs="+", required=True, help="A space-separated list of bass notes (e.g., C4 A3 F3)")

    args = parser.parse_args()

    try:
        cantus_firmus: list[Pitch] = [Pitch.parse(p_str) for p_str in args.cf]
    except Exception as e:
        parser.error(f"Failed to parse bass sequence '{args.bass}'. Error: {e}")
        return

    solved = next(generate(cantus_firmus))
    lily_str = score_to_lilypond(solved)
    print(lily_str)
    # for i, solved in enumerate(generate(cantus_firmus)):
    #     print(f"試行: {i + 1}")
    #     lily_str = score_to_lilypond(solved)
    #     print(lily_str)


if __name__ == "__main__":
    main()

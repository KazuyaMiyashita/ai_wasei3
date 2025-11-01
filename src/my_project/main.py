import argparse

from my_project.harmony import solve
from my_project.lilypond_writer import write
from my_project.model import Key, Mode, NoteName, Pitch


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Find harmony for a given bass sequence and key.",
        epilog=("Example usage:\nuv run python -m my_project.main -bass C4 A3 F3 D3 G3 C3 --tonic C --mode MAJOR"),
    )

    parser.add_argument(
        "--bass", nargs="+", required=True, help="A space-separated list of bass notes (e.g., C4 A3 F3)"
    )
    parser.add_argument("--tonic", required=True, help="Tonic note name of the key (e.g., C, G, F#)")
    parser.add_argument(
        "--mode",
        choices=["MAJOR", "MINOR"],
        help="Mode of the key",
    )

    args = parser.parse_args()

    try:
        bass_sequence: list[Pitch] = [Pitch.parse(p_str) for p_str in args.bass]
    except Exception as e:
        parser.error(f"Failed to parse bass sequence '{args.bass}'. Error: {e}")
        return

    try:
        tonic = NoteName.parse(args.tonic)
        match args.mode:
            case "MAJOR":
                mode = Mode.MAJOR
            case "MINOR":
                mode = Mode.MINOR
            case _:
                parser.error(f"Failed to parse mode '{args.mode}'")
                return
        key = Key(tonic=tonic, mode=mode)
    except Exception as e:
        parser.error(f"Failed to parse key '{args.tonic} {args.mode}'. Error: {e}")
        return

    solved = solve(bass_sequence, key)

    lily_str = write(solved, key)
    print(lily_str)


if __name__ == "__main__":
    main()

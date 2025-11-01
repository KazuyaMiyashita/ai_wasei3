from my_project.harmony import solve
from my_project.lilypond_writer import write
from my_project.model import Key, Mode, NoteName, Pitch


def main() -> None:
    bass_sequence = [
        Pitch.parse("C4"),
        Pitch.parse("A3"),
        Pitch.parse("F3"),
        Pitch.parse("D3"),
        Pitch.parse("G3"),
        Pitch.parse("C3"),
    ]
    key = Key(tonic=NoteName.parse("C"), mode=Mode.MAJOR)
    # print(f"bass: {[bass.name() for bass in bass_sequence]}")
    # print(f"key: {key}")

    solved = solve(bass_sequence, key)

    lily_str = write(solved, key)
    print(lily_str)


if __name__ == "__main__":
    main()

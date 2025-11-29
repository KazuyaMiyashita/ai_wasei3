import argparse
import logging
import shutil
import subprocess
import sys
from itertools import islice
from pathlib import Path

from my_project.counterpoint.counterpoint_generator import CounterpointGenerator
from my_project.counterpoint.model import NoteAnnotation, Species, ToneType
from my_project.lilypond_writer import score_to_lilypond
from my_project.logging_utils import PackagePathFilter
from my_project.model import (
    FullScore,
    Key,
    PartId,
    Pitch,
)

logger = logging.getLogger(__name__)


def main() -> None:
    args = parse_args()
    logging.basicConfig(
        level=args.log_level,
        format="[%(levelname)s] %(asctime)s.%(msecs)03d - %(short_name)s -- %(message)s",
        datefmt="%H:%M:%S",
    )
    root_logger = logging.getLogger()
    for handler in root_logger.handlers:
        handler.addFilter(PackagePathFilter())

    if args.batch:
        output_dir = prepare_batch_mode(args.output_dir, args.clean)

    try:
        cantus_firmus: list[Pitch] = [Pitch.parse(p_str) for p_str in args.cf]
    except Exception as e:
        logger.error(f"Failed to parse bass sequence '{args.cf}'. Error: {e}")
        return
    cf_part_id = PartId[args.cf_part_id]
    species: Species
    if args.species == "first":
        species = Species.FIRST_SPECIES
    elif args.species == "second":
        species = Species.SECOND_SPECIES
    elif args.species == "third":
        species = Species.THIRD_SPECIES
    elif args.species == "fourth":
        species = Species.FOURTH_SPECIES
    elif args.species == "fifth":
        species = Species.FIFTH_SPECIES
    else:
        species = Species.THIRD_SPECIES

    key = Key.parse(args.key)

    output_limit = None if args.limit == "infinity" else int(args.limit)
    if output_limit is not None and output_limit <= 0:
        logger.error("--limit must be a positive integer or 'infinity'.")
        return

    generator = CounterpointGenerator.default(
        cantus_firmus=cantus_firmus,
        cf_part_id=cf_part_id,
        key=key,
        species=species,
        part_id=PartId[args.part_id],
        seed=args.seed,
    )

    try:
        for _i, score in islice(enumerate(generator.generate_scores()), output_limit):
            iter_num = _i + 1
            if args.batch:
                lily_str = score_to_lilypond(score)
                run_lilypond(output_dir, iter_num, lily_str)
                pass
            else:
                if args.output == "simple":
                    output_str = format_score_for_debug(score)
                    print(f"試行 {iter_num}: {output_str}")
                elif args.output == "lilypond":
                    lily_str = score_to_lilypond(score)
                    print(lily_str)
    except StopIteration:
        logger.warning("対位法の候補がなくなったため、処理を中断します。")
    except KeyboardInterrupt:
        logger.info("interrupted.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Find counterpoint for a given bass sequence.",
        epilog=("Example usage:\nuv run python -m my_project.main_counterpoint --cf C4 A3 G3 E3 F3 A3 G3 E3 D3 C3"),
    )

    # --- General arguments ---
    parser.add_argument("--cf", nargs="+", required=True, help="A space-separated list of bass notes (e.g., C4 A3 F3)")
    parser.add_argument(
        "--species",
        type=str,
        default="third",
        choices=["first", "second", "third", "fourth", "fifth"],
        help="Rythmn type for counterpoint generation. Defaults to third.",
    )
    parser.add_argument("--seed", type=int, default=None, help="Random seed for reproducibility")
    parser.add_argument(
        "--key",
        type=str,
        default="C Major",
        help="Key for counterpoint generation (e.g., 'C Major', 'A Minor'). Defaults to C Major.",
    )
    parser.add_argument(
        "--part_id",
        type=str,
        default="SOPRANO",
        choices=["SOPRANO", "ALTO", "TENOR", "BASS"],
        help="Part ID for the generated counterpoint. Defaults to SOPRANO.",
    )
    parser.add_argument(
        "--cf_part_id",
        type=str,
        default="BASS",
        choices=["SOPRANO", "ALTO", "TENOR", "BASS"],
        help="Part ID for the Cantus Firmus. Defaults to BASS.",
    )
    parser.add_argument(
        "--log-level",
        type=str,
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
        help="Logging level. Defaults to INFO.",
    )

    # --- Console output mode arguments ---
    parser.add_argument(
        "--output",
        type=str,
        default="simple",
        choices=["lilypond", "simple"],
        help="Output format for console mode. Defaults to simple.",
    )
    parser.add_argument(
        "--limit",
        type=str,
        default="infinity",
        help="Maximum number of outputs for console mode (a positive integer or 'infinity'). Defaults to infinity.",
    )

    # --- PNG generation mode arguments ---
    parser.add_argument("--batch", action="store_true", help="Enables PNG generation mode.")
    parser.add_argument(
        "--output-dir", type=str, default="dist", help="Output directory for generated files. Defaults to 'dist'."
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean the output directory before generating files. Use with --generate-pngs.",
    )

    return parser.parse_args()


def prepare_batch_mode(output_dir: str, clean: bool) -> Path:
    """
    バッチモード開始時の処理。lilypondコマンド探したりディレクトリ作ったり既存ファイル消したり。保存先ディレクトリのパスを返す
    """

    if not shutil.which("lilypond"):
        logger.error("エラー: 'lilypond' コマンドが見つかりません。LilyPondをインストールしてパスを通してください。")
        sys.exit(1)

    output_dir_path = Path(output_dir)
    output_dir_path.mkdir(parents=True, exist_ok=True)

    if clean:
        logger.info(f"'{output_dir_path}' 内の既存の .png, .ly ファイルを削除します...")
        for f in output_dir_path.glob("*.png"):
            f.unlink()
        for f in output_dir_path.glob("*.ly"):
            f.unlink()
    return output_dir_path


def run_lilypond(output_dir: Path, iter_num: int, lily_str: str) -> None:
    """
    指定された .ly ファイルを lilypond でコンパイルし、よしなにリネーム処理を行う。
    """

    base_name = str(iter_num)
    ly_path = output_dir / f"{base_name}.ly"
    ly_path.write_text(lily_str, encoding="utf-8")

    output_prefix = ly_path.parent / ly_path.stem

    command = [
        "lilypond",
        "--png",
        "-dcrop",
        "-dno-print-pages",
        "-dresolution=300",
        "-o",
        str(output_prefix),
        str(ly_path),
    ]

    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        logger.error(f"LilyPond compilation failed for {ly_path.name}.\n{result.stderr}")
        raise RuntimeError("LilyPond compilation failed.")

    uncropped_png = output_prefix.with_suffix(".png")
    cropped_png = output_prefix.with_suffix(".cropped.png")

    if cropped_png.exists():
        if uncropped_png.exists():
            uncropped_png.unlink()
        cropped_png.rename(uncropped_png)


def format_score_for_debug(score: FullScore[NoteAnnotation]) -> str:
    """
    デバッグ用に Score オブジェクトを整形して文字列として返す
    """
    tone_map = {
        ToneType.HARMONIC_TONE: "",
        ToneType.PASSING_TONE: ",p",
        ToneType.NEIGHBOR_TONE: ",br",
        ToneType.SUSPENDED_TONE: ",r",
        ToneType.SUSPENDED_RESOLVING_HARMONIC_TONE: ",srh",
    }

    result_lines = []

    # Use body
    for part_id in [PartId.SOPRANO, PartId.ALTO, PartId.TENOR, PartId.BASS]:
        try:
            measures = score.body.part(part_id)
        except KeyError:
            continue

        all_measure_parts: list[str] = []

        for m in measures:
            note_parts_in_measure: list[str] = []

            for note_in_measure in m.notes:
                pitch = note_in_measure.value
                pitch_name = pitch.name() if pitch else "R"
                duration_str = f"d={note_in_measure.duration.value}"

                tone_type_str = ""
                if isinstance(note_in_measure.attribute, NoteAnnotation):
                    tone_type_str = tone_map.get(note_in_measure.attribute.tone_type, "")
                tied_str = ",t" if note_in_measure.attribute.is_tied_start else ""

                note_parts_in_measure.append(f"{pitch_name}({duration_str}{tone_type_str}{tied_str})")

            all_measure_parts.append(" ".join(note_parts_in_measure))

        result_lines.append(f"{part_id.name}: " + " | ".join(all_measure_parts))

    return "\n" + "\n".join(result_lines)


if __name__ == "__main__":
    main()

import argparse
import logging
import shutil
import subprocess
from itertools import islice
from pathlib import Path

from my_project.counterpoint.counterpoint_generator import CounterpointGenerator
from my_project.counterpoint.model import NoteAnnotation, Species, ToneType
from my_project.lilypond_writer import score_to_lilypond
from my_project.model import FullScore, HasScoreAttrs, Key, PartId, Pitch

logger = logging.getLogger(__name__)


def format_score_for_debug(score: FullScore[HasScoreAttrs]) -> str:
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
    for part in score.parts:
        all_measure_parts: list[str] = []
        for measure in part.measures:
            note_parts_in_measure: list[str] = []
            for note in measure.notes:
                pitch_name = note.value.name() if note.value else "R"
                duration_str = f"d={note.duration.value}"

                tone_type_str = ""
                if isinstance(note.attribute, NoteAnnotation):
                    tone_type_str = tone_map.get(note.attribute.tone_type, "")
                tied_str = ",t" if note.attribute.is_tied_start else ""

                note_parts_in_measure.append(f"{pitch_name}({duration_str}{tone_type_str}{tied_str})")
            all_measure_parts.append(" ".join(note_parts_in_measure))
        result_lines.append(f"{part.part_id.name}: " + " | ".join(all_measure_parts))

    return "\n" + "\n".join(result_lines)


def main() -> None:
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
    parser.add_argument(
        "--generate-pngs", type=int, help="Number of PNG images to generate. Enables PNG generation mode."
    )
    parser.add_argument(
        "--output-dir", type=str, default="dist", help="Output directory for generated files. Defaults to 'dist'."
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean the output directory before generating files. Use with --generate-pngs.",
    )
    parser.add_argument("--keep-ly", action="store_true", help="Keep the intermediate .ly files after generation.")

    args = parser.parse_args()

    logging.basicConfig(level=args.log_level, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")

    if args.generate_pngs:
        batch_generate_pngs(args)
    else:
        console_output(args)


def console_output(args: argparse.Namespace) -> None:
    """
    対位法を生成し、結果をコンソールに出力する
    """
    try:
        cantus_firmus: list[Pitch] = [Pitch.parse(p_str) for p_str in args.cf]
    except Exception as e:
        logger.error(f"Failed to parse bass sequence '{args.cf}'. Error: {e}")
        return

    cf_part_id = PartId.BASS  # TODO

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

    for i, solved in islice(enumerate(generator.generate_scores()), output_limit):
        if args.output == "simple":
            output_str = format_score_for_debug(solved)
            print(f"試行 {i + 1}: {output_str}")
        elif args.output == "lilypond":
            lily_str = score_to_lilypond(solved)
            print(lily_str)


def batch_generate_pngs(args: argparse.Namespace) -> None:
    """
    対位法を生成し、LilyPond経由でPNGファイルとして出力する
    """
    if not shutil.which("lilypond"):
        logger.error("エラー: 'lilypond' コマンドが見つかりません。LilyPondをインストールしてパスを通してください。")
        return

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    if args.clean:
        logger.info(f"'{output_dir}' 内の既存の .png, .ly ファイルを削除します...")
        for f in output_dir.glob("*.png"):
            f.unlink()
        for f in output_dir.glob("*.ly"):
            f.unlink()

    try:
        cantus_firmus: list[Pitch] = [Pitch.parse(p_str) for p_str in args.cf]
    except Exception as e:
        logger.error(f"Failed to parse bass sequence '{args.cf}'. Error: {e}")
        return

    cf_part_id = PartId.BASS  # TODO

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

    generator = CounterpointGenerator.default(
        cantus_firmus=cantus_firmus,
        cf_part_id=cf_part_id,
        key=key,
        species=species,
        part_id=PartId[args.part_id],
        seed=args.seed,
    )

    logger.info(f"{args.generate_pngs}個のPNGファイルを生成します...")
    generated_count = 0
    for i in range(args.generate_pngs):
        try:
            score = next(generator.generate_scores())
            lily_str = score_to_lilypond(score)

            base_name = str(i + 1)
            ly_path = output_dir / f"{base_name}.ly"

            ly_path.write_text(lily_str, encoding="utf-8")
            compile_lilypond(ly_path)

            if not args.keep_ly:
                ly_path.unlink()

            generated_count += 1
        except StopIteration:
            logger.warning("対位法の候補がなくなったため、処理を中断します。")
            break
        except Exception as e:
            logger.error(f"ファイル {i + 1} の生成中にエラーが発生しました: {e}")

    logger.info(f"処理が完了しました。{generated_count}個のファイルを生成しました。")


def compile_lilypond(ly_path: Path) -> None:
    """
    指定された .ly ファイルを lilypond でコンパイルし、よしなにリネーム処理を行う。
    """
    output_dir = ly_path.parent
    base_name = ly_path.stem
    output_prefix = output_dir / base_name

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


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        logger.info("プログラムがユーザーによって中断されました。")
        # スタックトレースは出力しない

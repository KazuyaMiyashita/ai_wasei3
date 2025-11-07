#!/bin/bash

# --- 設定 ---

# 1. 生成する画像の枚数
NUM_IMAGES=100

# 2. 定旋律 (Cantus Firmus) の音符
# (main_counterpoint.py で指定されている --cf の引数)
# CF_NOTES="C4 A3 G3 E3 F3 A3 G3 E3 D3 C3"
CF_NOTES="C3 D3 C3 E3 F3 G3 E3 A3 G3 C3"
# CF_NOTES="C3 E3 D3 G3 A3 G3 E3 F3 D3 C3"

RYTHMN="quater"
# RYTHMN="half"

# 3. 出力ディレクトリ
OUTPUT_DIR="dist"

# --- スクリプト本体 ---

# 出力ディレクトリが存在しなければ作成する
mkdir -p "$OUTPUT_DIR"

echo "Generating $NUM_IMAGES counterpoint examples..."
echo "Cantus Firmus: $CF_NOTES"
echo "Output Directory: $OUTPUT_DIR"
echo "---"

# 1 から NUM_IMAGES までループ
for i in $(seq 1 $NUM_IMAGES)
do
    echo "Generating sample $i..."

    # 1. LilyPond ファイルを一時ファイルとして生成
    # (dist/out.ly は毎回上書きされる)
    uv run python -m my_project.main_counterpoint --cf $CF_NOTES --rythmn $RYTHMN > "$OUTPUT_DIR/out.ly"

    # 2. LilyPond を PNG にコンパイル
    # (dist/out.png と dist/out.png.cropped.png が生成・上書きされる)
    lilypond --png -dcrop -dno-print-pages -dresolution=300 -o "$OUTPUT_DIR/out" "$OUTPUT_DIR/out.ly"

    # 3. 生成された PNG をリネーム
    mv "$OUTPUT_DIR/out.cropped.png" "$OUTPUT_DIR/$i.png"

    # (一時ファイルをクリーンアップ)
    rm "$OUTPUT_DIR/out.ly" "$OUTPUT_DIR/out.png" 2>/dev/null
done

echo "---"
echo "Successfully generated $NUM_IMAGES images in $OUTPUT_DIR/"

# 最後に、生成された画像が保存されているフォルダを開く
open "$OUTPUT_DIR"

# ai_wasei3

バスと調を与えると和声を実施してくれるやつです。

楽譜の描画には別途lilypondをインストールしてください。

## 実行例

### 1

```
uv run python -m my_project.main --bass C4 A3 F3 D3 G3 C3 --tonic C --mode MAJOR > dist/out.ly
lilypond --png -dcrop -dno-print-pages -dresolution=300 -o dist/out.png dist/out.ly
open dist/out.png.cropped.png
```

![](./docs/example1.png)

### 2

```
uv run python -m my_project.main --bass B3 F#3 G3 E3 F#3 B2 --tonic B --mode MINOR > dist/out.ly
lilypond --png -dcrop -dno-print-pages -dresolution=300 -o dist/out.png dist/out.ly
open dist/out.png.cropped.png
```

![](./docs/example2.png)

V から VI の進行で導音を上行させる規則に対応できていません

### 3

```
uv run python -m my_project.main --bass F#3 C#3 D#3 A#2 B2 C#3 F#3 --tonic F# --mode MAJOR > dist/out.ly
lilypond --png -dcrop -dno-print-pages -dresolution=300 -o dist/out.png dist/out.ly
open dist/out.png.cropped.png
```

![](./docs/example3.png)

## よく使うコマンド

```
uv run python -m my_project.main
uv run pytest
uv run ruff format .
uv run mypy src
```

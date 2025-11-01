## よく使うコマンド

```
uv run python -m my_project.main
uv run pytest
uv run ruff format .
uv run mypy src
```

```
uv run python -m my_project.main > dist/out.ly; lilypond --png -dcrop -dno-print-pages -dresolution=300 -o dist/out.png dist/out.ly
```

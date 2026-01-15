# Demonstration Instructions: Full Differential Sync

## 1. カーソル維持の確認 (CodeEditor -> DocumentEditor)

1. **DocumentEditor** でテキストを入力します。
2. **CodeEditor** を確認し、入力内容が同期されていることを確認します。
3. **CodeEditor** の任意の場所にカーソルを置きます。
4. 再び **DocumentEditor** で入力を続けます。
5. **CodeEditor** のカーソルが飛ばずに、元の位置（または入力に伴う相対位置）に留まっていることを確認します。

## 2. MEI表示の確認 (DocumentViewer)

1. ドキュメント内に `<mei>` 要素が含まれていることを確認します。
2. **DocumentViewer** タブに切り替えます。
3. MEIの内容がコードブロック（`<pre>`）として、グレーの背景で表示されていることを確認します。

## 3. インクリメンタルレンダリングの確認 (Performance)

1. 非常に長いドキュメント（`SAMPLE_XML` 相当）を表示します。
2. 入力時のレスポンスが、全文置換時代よりも高速であることを体感します。
3. **Debug View** のログを確認し、`Apply (Incremental)` が記録されていることを確認します。

## 4. XMLヘッダーの非表示確認

1. エディタの冒頭に `<?xml ...?>` や `<html>`, `<head>` が表示されていないことを確認します。
2. 最初の見出し（`<h1>`）から表示が始まっていることを確認します。

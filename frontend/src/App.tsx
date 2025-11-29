import { useEffect, useState } from "react";
import "./App.css";
import { Controls } from "./components/Controls";
import { ScoreCanvas } from "./components/ScoreCanvas";
import { StatusBar } from "./components/StatusBar";
import { useScoreEditor } from "./hooks/useScoreEditor";
import { type GenerateRequest, generateCounterpoint } from "./lib/api";
import { FullScore, type FullScoreJSON } from "./lib/model";

function App() {
  const {
    score,
    setScore,
    selection,
    setSelection,
    updateSelectionPart,
    updateSelectionNote,
    updateNotePitch,
    updateNoteFixedPitch,
    updateNoteAlter,
    updateNoteAccidental,
    updateNoteOctave,
    updateNoteDuration,
    toggleDot,
    toggleTie,
    updateNoteToRest,
    initScore,
    updateKey,
  } = useScoreEditor();

  const [isGenerating, setIsGenerating] = useState(false);

  // Keyboard Navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      let handled = false;

      if (e.key === "ArrowRight") {
        updateSelectionNote(1);
        handled = true;
      } else if (e.key === "ArrowLeft") {
        updateSelectionNote(-1);
        handled = true;
      } else if (e.key === "ArrowUp") {
        if (e.metaKey) {
          updateNoteOctave(1); // オクターブアップ (Command)
        } else if (e.shiftKey) {
          updateSelectionPart(-1); // パート移動 (Shift)
        } else if (e.ctrlKey) {
          updateNoteAlter(1); // 半音アップ (Control)
        } else {
          updateNotePitch(1);
        }
        handled = true;
      } else if (e.key === "ArrowDown") {
        if (e.metaKey) {
          updateNoteOctave(-1); // オクターブダウン (Command)
        } else if (e.shiftKey) {
          updateSelectionPart(1); // パート移動 (Shift)
        } else if (e.ctrlKey) {
          updateNoteAlter(-1); // 半音ダウン (Control)
        } else {
          updateNotePitch(-1);
        }
        handled = true;
      } else if (
        ["a", "b", "c", "d", "e", "f", "g"].includes(e.key.toLowerCase())
      ) {
        updateNoteFixedPitch(e.key);
        handled = true;
      } else if (e.code === "Numpad7") {
        updateNoteAccidental(0); // Natural
        handled = true;
      } else if (e.code === "Numpad8") {
        updateNoteAccidental(1); // Sharp
        handled = true;
      } else if (e.code === "Numpad9") {
        updateNoteAccidental(-1); // Flat
        handled = true;
      } else if (e.code === "NumpadEnter") {
        toggleTie();
        handled = true;
      } else if (e.key === "3") {
        updateNoteDuration(0.5); // Eighth
        handled = true;
      } else if (e.key === "4") {
        updateNoteDuration(1); // Quarter
        handled = true;
      } else if (e.key === "5") {
        updateNoteDuration(2); // Half
        handled = true;
      } else if (e.key === "6") {
        updateNoteDuration(4); // Whole
        handled = true;
      } else if (e.key === ".") {
        toggleDot();
        handled = true;
      } else if (e.key === "0" || e.code === "Numpad0") {
        updateNoteToRest();
        handled = true;
      }

      if (handled) {
        // e.preventDefault(); // Might interfere with inputs if not careful
        // Check if active element is input
        if (
          document.activeElement?.tagName === "INPUT" ||
          document.activeElement?.tagName === "SELECT"
        ) {
          return;
        }
        e.preventDefault();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [
    updateSelectionNote,
    updateSelectionPart,
    updateNotePitch,
    updateNoteAlter,
    updateNoteAccidental,
    updateNoteOctave,
    updateNoteFixedPitch,
    toggleTie,
    updateNoteDuration,
    toggleDot,
    updateNoteToRest,
  ]);

  const handleGenerate = async (data: GenerateRequest) => {
    setIsGenerating(true);
    try {
      console.log("Sending request:", data);
      const resultScore = await generateCounterpoint(data);
      console.log("Received Score Model:", resultScore);
      setScore(resultScore);
      alert("生成成功！");
    } catch (error: unknown) {
      console.error(error);
      const msg = error instanceof Error ? error.message : String(error);
      alert(`生成エラー: ${msg}`);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleExport = () => {
    if (score) {
      const jsonStr = JSON.stringify(score.toJSON(), null, 2);
      const blob = new Blob([jsonStr], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      const date = new Date().toISOString().slice(0, 10).replace(/-/g, "");
      a.download = `score_${date}.json`;
      a.click();
      URL.revokeObjectURL(url);
    }
  };

  const handleImport = (jsonContent: string) => {
    try {
      const json = JSON.parse(jsonContent);
      // Validate or directly parse
      const newScore = FullScore.fromJSON(json as FullScoreJSON);
      setScore(newScore);
      alert("インポート成功！");
    } catch (e) {
      console.error(e);
      alert("インポートエラー: ファイル形式が正しくありません。");
    }
  };

  const handleReset = () => {
    if (confirm("スコアをリセットしますか？")) {
      initScore();
    }
  };

  return (
    <div className="app-container">
      <header>
        <div className="content-wrapper">
          <div className="title-group">
            <h1>
              Score Editor{" "}
              <span className="subtitle">Prototype (Vite+React)</span>
            </h1>
            <StatusBar score={score} selection={selection} />
          </div>
        </div>
      </header>

      <Controls
        onGenerate={handleGenerate}
        onReset={handleReset}
        onExport={handleExport}
        onImport={handleImport}
        onKeyChange={updateKey}
        isGenerating={isGenerating}
      />

      <ScoreCanvas
        score={score}
        selection={selection}
        onSelect={setSelection}
      />

      <div className="instructions">
        <div className="content-wrapper">
          <div className="key-guides">
            <div>
              <strong>🖱️ マウスで音符を選択</strong>
            </div>
            <div>
              <strong>⌨️ カーソル操作</strong>
              <div className="key-guide">
                <div className="key-item">
                  <div>
                    <kbd>←</kbd> <kbd>→</kbd>
                  </div>
                  <span>選択カーソルの移動</span>
                </div>
                <div className="key-item">
                  <div>
                    <kbd>↑</kbd> <kbd>↓</kbd>
                  </div>
                  <span>音の高さ変更</span>
                </div>
                <div className="key-item">
                  <div>
                    <kbd>Control</kbd> + <kbd>↑</kbd> <kbd>↓</kbd>
                  </div>
                  <span>半音変更</span>
                </div>
                <div className="key-item">
                  <div>
                    <kbd>Command</kbd> + <kbd>↑</kbd> <kbd>↓</kbd>
                  </div>
                  <span>オクターブ変更</span>
                </div>
                <div className="key-item">
                  <div>
                    <kbd>Shift</kbd> + <kbd>↑</kbd> <kbd>↓</kbd>
                  </div>
                  <span>パート移動</span>
                </div>
              </div>
            </div>
            <div>
              <div>
                <strong>⌨️ 音符入力</strong>
              </div>
              <div className="key-guide">
                <div className="keypad-grid">
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteAccidental(0)}
                  >
                    <kbd>♮</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteAccidental(1)}
                  >
                    <kbd>♯</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteAccidental(-1)}
                  >
                    <kbd>♭</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button disabled-keypad-button"
                    disabled
                  >
                    <kbd>-</kbd>
                  </button>

                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteDuration(1)}
                  >
                    <kbd>♩</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteDuration(2)}
                  >
                    <kbd>
                      {/** 二分音符 */}
                      <span className="replace-text half-note">&#x1D15E;</span>
                    </kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteDuration(4)}
                  >
                    <kbd>
                      {/** 全音符 */}
                      <span className="replace-text whole-note">&#x1D15D;</span>
                    </kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button disabled-keypad-button"
                    disabled
                  >
                    <kbd>+</kbd>
                  </button>

                  <button
                    type="button"
                    className="keypad-button disabled-keypad-button"
                    disabled
                  >
                    <kbd>1</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button disabled-keypad-button"
                    disabled
                  >
                    <kbd>2</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => updateNoteDuration(0.5)}
                  >
                    <kbd>♪</kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button large-v"
                    style={{ gridRow: "3 / 5", gridColumn: 4 }}
                    onClick={toggleTie}
                  >
                    <kbd>タイ</kbd>
                  </button>

                  <button
                    type="button"
                    className="keypad-button large-h"
                    style={{ gridColumn: "span 2" }}
                    onClick={updateNoteToRest}
                  >
                    <kbd>
                      {/** 四分休符 */}
                      <span className="replace-text quarter-rest">
                        &#x1D13D;
                      </span>
                      {/** 八分休符 */}
                      <span className="replace-text eighth-rest">
                        &#x1D13E;
                      </span>
                    </kbd>
                  </button>
                  <button
                    type="button"
                    className="keypad-button"
                    onClick={() => toggleDot()}
                  >
                    <kbd>.</kbd>
                  </button>
                </div>
                <div className="key-item">
                  <div>
                    <kbd>a</kbd> <kbd>b</kbd> <kbd>c</kbd> <kbd>d</kbd>{" "}
                    <kbd>e</kbd> <kbd>f</kbd> <kbd>g</kbd>
                  </div>
                  <span>音名変更</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;

import type React from "react";
import { useEffect, useLayoutEffect, useRef } from "react";
import type { Selection } from "../hooks/useScoreEditor";
import type { FullScore } from "../lib/model";
import { ScoreRenderer } from "../lib/renderer";

interface ScoreCanvasProps {
  score: FullScore | null;
  selection: Selection | null;
  onSelect: (sel: Selection | null) => void;
}

export const ScoreCanvas: React.FC<ScoreCanvasProps> = ({
  score,
  selection,
  onSelect,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const rendererRef = useRef<ScoreRenderer | null>(null);

  // Initialize renderer
  useLayoutEffect(() => {
    if (containerRef.current) {
      rendererRef.current = new ScoreRenderer(containerRef.current);
    }
  }, []);

  useEffect(() => {
    if (!score || !containerRef.current || !rendererRef.current) return;

    // Render
    const { measureRects } = rendererRef.current.render(score, selection);

    // Attach Events

    // 1. Note Events
    // notes is [Part][Measure][Note]
    const partKeys = ["SOPRANO", "ALTO", "TENOR", "BASS"]; // Should match renderer order logic

    // We need to access the DOM elements of the VexFlow notes.
    // Since VexFlow 4.x generates SVG, we can query them or VexFlow notes might store references.
    // Currently RenderContext returns Vex.StaveNote objects.
    // VexFlow StaveNote.getAttribute("id") or similar?
    // Actually, VexFlow SVG backend assigns classes "vf-stavenote".
    // But matching them back to our data is hard if we just queryAll.
    // The previous implementation used queryAll(".vf-stavenote") and assumed order.
    // We can stick to that assumption for now as it is deterministic.

    const domNotes = containerRef.current.querySelectorAll(".vf-stavenote");
    let noteCounter = 0;

    // Assuming the render order in renderer.ts is Part -> Measure -> Note
    // But wait, renderer iterates Part -> Measure.
    // So the order is: Part1(M1, M2...), Part2(M1, M2...)
    // Let's verify renderer.ts loop structure.
    // Yes: orderedPartKeys.forEach(part => measures.forEach(measure => ...))

    // We need to iterate in the same order as VexFlow draws them to attach events correctly.
    // Renderer draws: Measure 1 (All Parts) -> Measure 2 (All Parts) ...
    // Inside each measure: Part 0 -> Part 1 ...

    // Determine measure count from first available part
    const availableParts = partKeys.filter((k) => score.parts[k]);
    if (availableParts.length === 0) return;

    const measureCount = score.parts[availableParts[0]].length;

    for (let mIdx = 0; mIdx < measureCount; mIdx++) {
      availableParts.forEach((partId, _) => {
        // Note: pIdx here is the index in availableParts, which matches renderer's logic
        // But we need the original index for onSelect if onSelect expects index in PART_NAMES constant?
        // "selection.part" usually refers to index in PART_NAMES ["SOPRANO", "ALTO", "TENOR", "BASS"].
        // Let's find the true index.
        const truePartIndex = partKeys.indexOf(partId);

        const measures = score.parts[partId];
        if (!measures[mIdx]) return;

        const measure = measures[mIdx];
        measure.notes.forEach((_, nIdx) => {
          const el = domNotes[noteCounter];
          if (el) {
            el.addEventListener("click", (e) => {
              e.stopPropagation();
              onSelect({ part: truePartIndex, measure: mIdx, note: nIdx });
            });
            (el as HTMLElement).style.cursor = "pointer";

            el.addEventListener("mouseenter", () => {
              (el as HTMLElement).style.opacity = "0.6";
            });
            el.addEventListener("mouseleave", () => {
              (el as HTMLElement).style.opacity = "1.0";
            });
          }
          noteCounter++;
        });
      });
    }

    // 2. Measure Events
    // Renderer returns created SVG rects in structure [Part][Measure]
    // These already exist in DOM. We just need to attach events?
    // Actually, renderer creates them but doesn't attach our react callback.
    // But we cannot pass the react callback to renderer easily if we want to keep renderer pure JS.
    // Or we can return the elements and attach here.

    measureRects.forEach((partRects, pIdx) => {
      partRects.forEach((rect, mIdx) => {
        rect.addEventListener("click", (e) => {
          e.stopPropagation();
          onSelect({ part: pIdx, measure: mIdx, note: -1 });
        });
      });
    });
  }, [score, selection, onSelect]);

  return (
    // biome-ignore lint/a11y/useKeyWithClickEvents: canvas wrapper click clears selection
    // biome-ignore lint/a11y/useSemanticElements: wrapping the entire canvas in a <button> causes layout issues
    <div
      id="canvas-wrapper"
      style={{
        overflowX: "auto",
        padding: "40px 0",
        background: "white",
        userSelect: "none",
        WebkitUserSelect: "none",
      }}
      onClick={() => onSelect(null)}
      role="button"
      tabIndex={0}
    >
      <div
        ref={containerRef}
        style={{ display: "inline-block", minWidth: "100%" }}
      />
    </div>
  );
};

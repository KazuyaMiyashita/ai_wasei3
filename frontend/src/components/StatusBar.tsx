import type React from "react";
import type { Selection } from "../hooks/useScoreEditor";
import { PART_NAMES } from "../lib/constants";
import type { FullScore, Pitch } from "../lib/model";

interface StatusBarProps {
  score: FullScore | null;
  selection: Selection | null;
}

export const StatusBar: React.FC<StatusBarProps> = ({ score, selection }) => {
  if (!score || !score.parts) return <div className="status-bar">Ready</div>;
  if (!selection) return <div className="status-bar">Ready (No Selection)</div>;

  const partName = PART_NAMES[selection.part];
  const partKey = partName.toUpperCase();
  const measures = score.parts[partKey];

  if (!measures) return <div className="status-bar">Error: Part not found</div>;

  const measure = selection.measure + 1;

  // Handle Measure Selection (note === -1)
  if (selection.note === -1) {
    return (
      <div className="status-bar">
        Measure Selected: {partName} Part | Measure {measure}
      </div>
    );
  }

  const noteIdx = selection.note + 1;

  const note = measures[selection.measure]?.notes[selection.note];
  if (!note) return <div className="status-bar">Ready</div>;

  let keyStr = "";
  if (note.isRest()) {
    keyStr = "Rest";
  } else {
    keyStr = (note.value as unknown as Pitch).name(); // e.g., "C4"
  }
  return (
    <div className="status-bar">
      Note Selected: {partName} Part | Measure {measure} : Note {noteIdx} (
      {keyStr})
    </div>
  );
};

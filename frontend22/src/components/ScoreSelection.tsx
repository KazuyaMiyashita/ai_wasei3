import type React from "react";
import type { ScorePosition } from "./ScoreDisplay";

interface ScoreSelectionProps {
  mode: "none" | "note" | "staff";
  selectedIds: string[];
  latestPosition?: ScorePosition;
}

const ScoreSelection: React.FC<ScoreSelectionProps> = ({
  mode,
  selectedIds,
  latestPosition,
}) => {
  return (
    <div className="max-w-content border-border mx-auto mt-4 w-full rounded-md border bg-gray-50 p-4 font-mono text-sm whitespace-pre-wrap">
      <div>
        Select mode:{" "}
        {mode === "none" ? (
          <span className="text-gray-500 italic">None</span>
        ) : (
          <span>{mode}</span>
        )}
      </div>
      <div className="flex items-start gap-2">
        <span>Selected elements:</span>
        {selectedIds.length === 0 ? (
          <span className="text-gray-500 italic">None</span>
        ) : (
          <div className="flex flex-wrap gap-x-3 gap-y-1">
            {selectedIds.map((id) => (
              <span key={id}>{id}</span>
            ))}
          </div>
        )}
      </div>
      <div className="flex items-start gap-2">
        <span>Latest selected position:</span>
        {latestPosition ? (
          <div>
            Part {latestPosition.part}, Measure: {latestPosition.measure}, Beat:{" "}
            {latestPosition.beat}
          </div>
        ) : (
          <span className="text-gray-500 italic">None</span>
        )}
      </div>
    </div>
  );
};

export default ScoreSelection;

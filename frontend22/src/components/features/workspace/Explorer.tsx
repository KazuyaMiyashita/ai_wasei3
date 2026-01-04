import type React from "react";
import type { UseWorkspace } from "../../../hooks/workspace/useWorkspace";
import { CollapsibleSection } from "../../ui/CollapsibleSection";

interface ExplorerProps {
  scoreList: UseWorkspace["scoreList"];
  localScores: UseWorkspace["localScores"];
  currentFile: UseWorkspace["activeTabPath"];
  onFileSelect: UseWorkspace["handleFileSelect"];
  onFileDoubleClick: UseWorkspace["handleFileDoubleClick"];
  view?: "workspace" | "server" | "templates";
}

const Explorer: React.FC<ExplorerProps> = ({
  scoreList,
  localScores,
  currentFile,
  onFileSelect,
  onFileDoubleClick,
  view,
}) => {
  return (
    <div className="inline-scrollbar h-full">
      {(!view || view === "workspace") && (
        <CollapsibleSection
          title="Workspace"
          defaultOpen={true}
          contentClassName="pb-2"
        >
          {localScores.length === 0 ? (
            <div className="text-text-muted px-4 py-2 text-xs italic">
              No files in workspace
            </div>
          ) : (
            <ul className="space-y-0.5 px-2">
              {localScores.map((score) => (
                <li key={score.path}>
                  <button
                    type="button"
                    onClick={() => onFileSelect(score.path)}
                    onDoubleClick={() => onFileDoubleClick(score.path)}
                    className={`w-full truncate rounded px-3 py-1.5 text-left text-sm transition-colors ${
                      currentFile === score.path
                        ? "bg-brand-sub text-brand font-medium"
                        : "hover:bg-surface-header text-text-main"
                    }`}
                    title={score.name}
                  >
                    {score.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </CollapsibleSection>
      )}

      {(!view || view === "server") && (
        <CollapsibleSection
          title="Server Score"
          defaultOpen={true}
          contentClassName="pb-2"
        >
          <ul className="space-y-0.5 px-2">
            {scoreList.map((score) => (
              <li key={score.path}>
                <button
                  type="button"
                  onClick={() => onFileSelect(score.path)}
                  onDoubleClick={() => onFileDoubleClick(score.path)}
                  className={`w-full truncate rounded px-3 py-1.5 text-left text-sm transition-colors ${
                    currentFile === score.path
                      ? "bg-brand-sub text-brand font-medium"
                      : "hover:bg-surface-header text-text-main"
                  }`}
                  title={score.name}
                >
                  {score.name}
                </button>
              </li>
            ))}
          </ul>
        </CollapsibleSection>
      )}

      {(!view || view === "templates") && (
        <CollapsibleSection title="Templates" contentClassName="pb-2">
          <ul className="space-y-0.5 px-2">
            {["2-Part Counterpoint", "3-Part Counterpoint", "Choral Style"].map(
              (t) => (
                <li key={t}>
                  <button
                    type="button"
                    className="hover:bg-surface-header text-text-main w-full rounded px-3 py-1.5 text-left text-sm transition-colors"
                  >
                    {t}
                  </button>
                </li>
              ),
            )}
          </ul>
        </CollapsibleSection>
      )}
    </div>
  );
};

export default Explorer;

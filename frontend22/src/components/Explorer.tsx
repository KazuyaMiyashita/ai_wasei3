import { ChevronDown, Plus } from "lucide-react";
import type React from "react";
import { useState } from "react";
import type { ScoreEntry } from "../hooks/useScoreEditor";

interface ExplorerProps {
  scoreList: ScoreEntry[];
  localScores: ScoreEntry[];
  currentFile: string;
  onFileSelect: (path: string) => void;
  onFileDoubleClick: (path: string) => void;
  onLocalFileAdd: (file: File) => void;
}

const CollapsibleSection: React.FC<{
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}> = ({ title, children, defaultOpen = true }) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border-border-main border-b last:border-0">
      <button
        type="button"
        className="hover:bg-surface-header flex w-full items-center justify-between p-3 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <span className="text-text-muted text-[10px] font-bold tracking-wider uppercase">
          {title}
        </span>
        <ChevronDown
          className={`text-text-muted h-3 w-3 transition-transform ${
            isOpen ? "rotate-180" : ""
          }`}
        />
      </button>
      {isOpen && <div className="pb-2">{children}</div>}
    </div>
  );
};

const Explorer: React.FC<ExplorerProps> = ({
  scoreList,
  localScores,
  currentFile,
  onFileSelect,
  onFileDoubleClick,
  onLocalFileAdd,
}) => {
  const handleLocalFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    onLocalFileAdd(file);
    e.target.value = "";
  };

  return (
    <div className="bg-sidebar text-text-main flex h-full w-full flex-col overflow-hidden">
      <div className="border-border-main bg-surface-header/50 flex h-11 shrink-0 items-center justify-between border-b px-3">
        <span className="text-text-sub text-[10px] font-bold tracking-wider uppercase">
          Explorer
        </span>
        <label
          className="hover:bg-surface-muted text-text-sub cursor-pointer rounded p-1 transition-colors"
          title="Open Local File"
        >
          <Plus className="h-3.5 w-3.5" />
          <input
            type="file"
            className="hidden"
            accept=".mei,.xml"
            onChange={handleLocalFileChange}
          />
        </label>
      </div>

      <div className="flex-1 overflow-y-auto">
        <CollapsibleSection title="Local Scores">
          {localScores.length === 0 ? (
            <div className="text-text-muted px-4 py-2 text-xs italic">
              No local files open
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

        <CollapsibleSection title="Server Scores">
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

        <CollapsibleSection title="Templates">
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
      </div>
    </div>
  );
};

export default Explorer;

import type React from "react";
import { useState } from "react";
import type { UseScoreView } from "../../../hooks/score/useScoreView";
import { useNotification } from "../../../hooks/useNotification";
import { DropdownMenu } from "../../ui/DropdownMenu";

interface ScoreToolsMenuProps {
  viewSettings: UseScoreView;
}

export const ScoreToolsMenu: React.FC<ScoreToolsMenuProps> = ({
  viewSettings,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const { notify } = useNotification();

  return (
    <DropdownMenu
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      trigger={
        <button
          type="button"
          className={`hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors ${
            isOpen ? "bg-surface-hover text-text-main" : ""
          }`}
        >
          Tools
        </button>
      }
    >
      <button
        type="button"
        onClick={() => {
          viewSettings.handlePartwise();
          setIsOpen(false);
        }}
        className="text-text-main hover:bg-surface-muted flex w-full cursor-pointer items-center justify-between px-4 py-2 text-left text-sm transition-colors"
      >
        <span>Partwise Conversion</span>
      </button>

      <div className="border-border-main my-1 border-t" />

      <div className="px-4 py-2">
        <div className="text-text-muted mb-2 text-xs font-bold tracking-wider uppercase">
          Counterpoint Settings
        </div>
        <div className="space-y-2">
          <select className="border-border-main bg-background text-text-main focus:border-brand w-full cursor-pointer rounded border p-1.5 text-xs focus:outline-none">
            <option>Strict Counterpoint</option>
            <option>Palestrina Style</option>
            <option>Free Counterpoint</option>
          </select>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="check-rules-tool"
              className="border-border-main text-brand focus:ring-brand cursor-pointer rounded"
              defaultChecked
            />
            <label
              htmlFor="check-rules-tool"
              className="text-text-main cursor-pointer text-xs"
            >
              Real-time check
            </label>
          </div>
          <button
            type="button"
            className="bg-brand text-text-on-brand hover:bg-brand-hover w-full cursor-pointer rounded py-1.5 text-xs font-bold transition-colors"
          >
            Generate
          </button>
        </div>
      </div>

      <div className="border-border-main my-1 border-t" />

      <div className="px-4 py-2">
        <div className="text-text-muted mb-2 text-xs font-bold tracking-wider uppercase">
          Export
        </div>
        <div className="grid grid-cols-2 gap-2">
          {["MEI", "MusicXML", "MIDI", "PDF"].map((fmt) => (
            <button
              key={fmt}
              type="button"
              className="border-border-main hover:bg-surface-muted text-text-main cursor-pointer rounded border p-1 text-center text-xs transition-colors"
            >
              {fmt}
            </button>
          ))}
        </div>
      </div>

      <div className="border-border-main my-1 border-t" />

      <div className="px-4 py-2">
        <div className="text-text-muted mb-2 text-xs font-bold tracking-wider uppercase">
          Debug Notification
        </div>
        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            onClick={() => notify("Test Info", "info", "Debug")}
            className="border-border-main hover:bg-surface-muted text-text-main cursor-pointer rounded border p-1 text-center text-xs transition-colors"
          >
            Info
          </button>
          <button
            type="button"
            onClick={() => notify("Test Warn", "warn", "Debug")}
            className="border-border-main hover:bg-surface-muted text-warning cursor-pointer rounded border p-1 text-center text-xs transition-colors"
          >
            Warn
          </button>
          <button
            type="button"
            onClick={() => notify("Test Error", "error", "Debug")}
            className="border-border-main hover:bg-surface-muted text-error cursor-pointer rounded border p-1 text-center text-xs transition-colors"
          >
            Error
          </button>
        </div>
      </div>
    </DropdownMenu>
  );
};

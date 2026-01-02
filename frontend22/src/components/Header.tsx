import { Info, Menu, MousePointer2, Pencil } from "lucide-react";
import type React from "react";
import { useEffect, useRef, useState } from "react";
import ScorePlayer from "./ScorePlayer";

interface HeaderProps {
  isPlaying: boolean;
  onTogglePlayback: () => void;
  onStopPlayback: () => void;
  meiXML?: Document;
  scale: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onPartwise: () => void;
  isLeftSidebarOpen: boolean;
  onToggleLeftSidebar: () => void;
  isRightSidebarOpen: boolean;
  onToggleRightSidebar: () => void;
  interactionMode: "select" | "edit";
  onSetInteractionMode: (mode: "select" | "edit") => void;
}

const Header: React.FC<HeaderProps> = ({
  isPlaying,
  onTogglePlayback,
  onStopPlayback,
  meiXML,
  scale,
  onZoomIn,
  onZoomOut,
  onPartwise,
  isLeftSidebarOpen,
  onToggleLeftSidebar,
  isRightSidebarOpen,
  onToggleRightSidebar,
  interactionMode,
  onSetInteractionMode,
}) => {
  const [isToolsOpen, setIsToolsOpen] = useState(false);
  const toolsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        toolsRef.current &&
        !toolsRef.current.contains(event.target as Node)
      ) {
        setIsToolsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <header className="border-border-main bg-header text-text-main relative z-20 flex h-14 items-center justify-between border-b px-4">
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={onToggleLeftSidebar}
          className={`hover:bg-surface-hover cursor-pointer rounded p-2 transition-colors ${
            !isLeftSidebarOpen ? "text-text-muted" : "text-text-main"
          }`}
          title="Toggle Explorer"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="flex items-center gap-2 select-none">
          <div className="bg-brand text-text-on-brand flex h-8 w-8 items-center justify-center rounded font-bold shadow-sm">
            M
          </div>
          <div className="text-text-main hidden text-lg font-bold tracking-tight sm:block">
            Music Analysis
          </div>
        </div>

        <nav className="ml-6 flex gap-1">
          {["File", "Edit", "View"].map((item) => (
            <button
              key={item}
              type="button"
              className="hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors"
            >
              {item}
            </button>
          ))}

          <div className="relative" ref={toolsRef}>
            <button
              type="button"
              className={`hover:bg-surface-hover text-text-sub cursor-pointer rounded px-3 py-1.5 text-sm font-medium transition-colors ${
                isToolsOpen ? "bg-surface-hover text-text-main" : ""
              }`}
              onClick={() => setIsToolsOpen(!isToolsOpen)}
            >
              Tools
            </button>

            {isToolsOpen && (
              <div className="bg-surface border-border-main absolute top-full left-0 z-50 mt-1 w-64 rounded-md border py-1 shadow-xl">
                <button
                  type="button"
                  onClick={() => {
                    onPartwise();
                    setIsToolsOpen(false);
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
              </div>
            )}
          </div>
        </nav>
      </div>

      <div className="mx-4 flex flex-1 items-center justify-center gap-6">
        {/* Mode Toggle */}
        <div className="bg-surface-header border-border-main flex rounded-lg border p-1">
          <button
            type="button"
            onClick={() => onSetInteractionMode("select")}
            className={`cursor-pointer rounded p-1.5 transition-all active:scale-95 ${
              interactionMode === "select"
                ? "bg-surface text-brand shadow-sm"
                : "text-text-muted hover:bg-surface-hover hover:text-text-main"
            }`}
            title="Selection Mode"
          >
            <MousePointer2 className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => onSetInteractionMode("edit")}
            className={`cursor-pointer rounded p-1.5 transition-all active:scale-95 ${
              interactionMode === "edit"
                ? "bg-surface text-brand shadow-sm"
                : "text-text-muted hover:bg-surface-hover hover:text-text-main"
            }`}
            title="Edit Mode"
          >
            <Pencil className="h-4 w-4" />
          </button>
        </div>

        <div className="w-full max-w-md">
          <ScorePlayer
            isPlaying={isPlaying}
            onTogglePlayback={onTogglePlayback}
            onStopPlayback={onStopPlayback}
            meiXML={meiXML}
          />
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="bg-surface border-border-main flex items-center gap-px rounded-md border p-0.5">
          <button
            type="button"
            onClick={onZoomOut}
            className="hover:bg-surface text-text-sub flex h-7 w-7 cursor-pointer items-center justify-center rounded transition-colors"
            title="Zoom Out"
          >
            -
          </button>
          <span className="text-text-main min-w-10 text-center font-mono text-xs font-bold">
            {scale}%
          </span>
          <button
            type="button"
            onClick={onZoomIn}
            className="hover:bg-surface text-text-sub flex h-7 w-7 cursor-pointer items-center justify-center rounded transition-colors"
            title="Zoom In"
          >
            +
          </button>
        </div>

        <button
          type="button"
          onClick={onToggleRightSidebar}
          className={`hover:bg-surface-hover cursor-pointer rounded p-2 transition-colors ${
            !isRightSidebarOpen ? "text-text-muted" : "text-text-main"
          }`}
          title="Toggle Properties"
        >
          <Info className="h-5 w-5" />
        </button>
      </div>
    </header>
  );
};

export default Header;
